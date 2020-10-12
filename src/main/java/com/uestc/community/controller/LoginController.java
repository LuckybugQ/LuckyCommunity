package com.uestc.community.controller;

import com.google.code.kaptcha.Producer;
import com.uestc.community.entity.User;
import com.uestc.community.service.UserService;
import com.uestc.community.util.CommunityUtil;
import com.uestc.community.util.MailClient;
import com.uestc.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.uestc.community.util.CommunityConstant.*;

@Controller
public class LoginController {
    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private MailClient mailClient;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {
        return "/site/register";
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "/site/login";
    }

    @RequestMapping(path = "/forget", method = RequestMethod.GET)
    public String getForgetPage() {
        return "/site/forget";
    }

    @RequestMapping(path = "/register/success", method = RequestMethod.GET)
    public String getRegisterSuccess(Model model,HttpSession session) {
        User user = (User) session.getAttribute("activateUser");
        if(user!=null){
            // 激活邮件
            Context context = new Context();
            context.setVariable("email", user.getEmail());
            // http://localhost:8080/community/activation/101/code
            String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
            context.setVariable("url", url);
            //content=模板引擎根据context中的变量生成的HTML页面
            String content = templateEngine.process("/mail/activation", context);
            mailClient.sendMail(user.getEmail(), "激活账号", content);
        }

        model.addAttribute("msg", "注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活!");
        model.addAttribute("target", "/");
        return "/site/operate-result";
    }

    @RequestMapping(path = "/register", method = RequestMethod.POST)
    @ResponseBody
    public String register(Model model, User user,HttpSession session,String code,@CookieValue("kaptchaOwner") String kaptchaOwner) {
        if(kaptchaOwner==null){
            return CommunityUtil.getJSONString(1, "验证码已过期！");
        }

        //检查密码长度
        String password = user.getPassword();
        if(password!=null && (password.length()<6 || password.length()>16)){
            return CommunityUtil.getJSONString(1,"密码长度不符合要求！");
        }

        // 检查验证码
        String kaptcha = null;
        if(StringUtils.isNotBlank(kaptchaOwner)){
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            return CommunityUtil.getJSONString(1, "验证码不正确");
        }

        Map<String, Object> map = userService.register(user,session);

        if (map == null || map.isEmpty()) {
            map.put("status",0);
            map.put("action","/register/success");
            return CommunityUtil.getJSONString(0, "注册成功",map);
//            model.addAttribute("msg", "注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活!");
//            model.addAttribute("target", "/index");
//            return "/site/operate-result";
        } else {
            return CommunityUtil.getJSONString(1, (String) map.get("msg"));
//            model.addAttribute("msg", map.get("msg"));
//            return "/site/register";
        }
    }

    // http://localhost:8080/community/activation/101/code
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        if (result == ACTIVATION_SUCCESS) {
            userService.newUserNotice(userId);
            model.addAttribute("msg", "激活成功,您的账号已经可以正常使用了!");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作,该账号已经激活过了!");
            model.addAttribute("target", "/");
        } else {
            model.addAttribute("msg", "激活失败,您提供的激活码不正确!");
            model.addAttribute("target", "/");
        }
        return "/site/operate-result";
    }

    @RequestMapping(path = "/kaptcha",method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response){
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);
//        session.setAttribute("kaptcha",text);

        //验证码的归属
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner",kaptchaOwner);
        cookie.setMaxAge(600);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey,text,600, TimeUnit.SECONDS);

        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image,"png",os);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping(path = "/login", method = RequestMethod.POST)
    @ResponseBody
    public String login(String username, String password, String code, boolean rememberme,
                        Model model, HttpServletResponse response,@CookieValue("kaptchaOwner") String kaptchaOwner) {

        // 检查验证码
        String kaptcha = null;
        if(StringUtils.isNotBlank(kaptchaOwner)){
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            return CommunityUtil.getJSONString(1, "验证码不正确");
        }

        // 检查账号,密码
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")) {
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
//            return "redirect:/index";
            map.put("status",0);
            map.put("action","/");
            return CommunityUtil.getJSONString(0, "登录成功",map);
        } else {
              return CommunityUtil.getJSONString(1, (String) map.get("msg"));
//            model.addAttribute("usernameMsg", map.get("usernameMsg"));
//            model.addAttribute("passwordMsg", map.get("passwordMsg"));
//            return "/site/login";
        }
    }


    @RequestMapping(path = "/forget/success", method = RequestMethod.GET)
    public String getForgetSuccess(Model model,HttpSession session) {
        model.addAttribute("msg", "密码修改成功，请您重新登陆!");
        model.addAttribute("target", "/");
        return "/site/operate-result";
    }

    @RequestMapping(path = "/forget", method = RequestMethod.POST)
    @ResponseBody
    public String forget(String email, String verifycode, String password, Model model, HttpSession session) {

        //检查密码长度
        if(password!=null && (password.length()<6 || password.length()>16)){
            return CommunityUtil.getJSONString(1,"密码长度不符合要求！");
        }

        User user = userService.findUserByEmail(email);
        if (user==null) {
            return CommunityUtil.getJSONString(1, "不存在该用户！");
        }
        String code = session.getAttribute("verifycode").toString();

        if (!verifycode.equals(code)) {
            return CommunityUtil.getJSONString(1, "验证码不正确");
        }

        String updatedPassword = CommunityUtil.md5(password + user.getSalt());
        user.setPassword(updatedPassword);
        userService.updatePassword(user.getId(),updatedPassword);

        Map<String, Object> map = new HashMap<>();
        map.put("status",0);
        map.put("action","/forget/success");
        return CommunityUtil.getJSONString(0, "密码修改成功",map);

    }

    @RequestMapping(path = "/forget/verifycode", method = RequestMethod.POST)
    @ResponseBody
    public String verifyCode(String email,HttpSession session) {

        User user = userService.findUserByEmail(email);
        if (user==null) {
            return CommunityUtil.getJSONString(1, "用户不存在");
        }else{
            sendThread thread = new sendThread(email,session);
            thread.start();
            return CommunityUtil.getJSONString(0);
        }
    }




    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }

    class sendThread extends Thread{
        private String email;
        private HttpSession session;

        public sendThread(String email,HttpSession session){
            this.email = email;
            this.session = session;
        }
        public void run() {
            // 激活邮件
            Context context = new Context();
            context.setVariable("email", email);
            // http://localhost:8080/community/activation/101/code
            StringBuilder builder = new StringBuilder();
            Random r = new Random();
            for(int i=0;i<5;i++){
                char c = (char) (r.nextInt(26)+'A');
                builder.append(c);
            }
            String verifycode = builder.toString();
            session.setAttribute("verifycode",verifycode);
            context.setVariable("verifycode", verifycode);
            //content=模板引擎根据context中的变量生成的HTML页面
            String content = templateEngine.process("/mail/forget", context);
            mailClient.sendMail(email, "找回密码", content);

        }
    }





}

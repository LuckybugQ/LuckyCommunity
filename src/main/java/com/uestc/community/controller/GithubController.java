package com.uestc.community.controller;

import com.uestc.community.entity.AccessTokenDTO;
import com.uestc.community.entity.GithubUser;
import com.uestc.community.entity.User;
import com.uestc.community.service.UserService;
import com.uestc.community.util.CommunityConstant;
import com.uestc.community.util.GithubProvider;
import com.uestc.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Controller
public class GithubController implements CommunityConstant {


    @Autowired
    private GithubProvider githubProvider;

    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    @Value("${github.redirect.uri}")
    private String redirectUri;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @RequestMapping("/callback")
    public String callback(@RequestParam(name = "code")String code,
                           @RequestParam(name = "state")String state,
                           HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirectAttributes, Model model){

        AccessTokenDTO accessTokenDTO = new AccessTokenDTO();
        accessTokenDTO.setRedirect_uri(redirectUri);
        accessTokenDTO.setClient_id(clientId);
        accessTokenDTO.setCode(code);
        accessTokenDTO.setState(state);
        accessTokenDTO.setClient_secret(clientSecret);

        String accessToken = githubProvider.getAccessToken(accessTokenDTO);
        //System.out.println(accessToken);

        GithubUser githubUser = githubProvider.getUser(accessToken);

        //System.out.println(githubUser.toString());

        //测试用
        //githubUser.setEmail("75075188-3@qq.com");

        if(githubUser != null){
            //登录成功 判断e-mail是否存在
            String email = githubUser.getEmail();
            User user = userService.findUserByEmail(email);
            if(user!=null){
                Map<String, Object> map = userService.login(user);
                if (map.containsKey("ticket")) {
                    Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
                    cookie.setPath(contextPath);
                    cookie.setMaxAge(DEFAULT_EXPIRED_SECONDS);
                    response.addCookie(cookie);
                    return "redirect:/";
                }
            }else{
                userService.register(githubUser);
                user = userService.findUserByEmail(githubUser.getEmail());
                if(user!=null){
                    userService.newUserNotice(user.getId());
                    Map<String, Object> map = userService.login(user);
                    if (map.containsKey("ticket")) {
                        Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
                        cookie.setPath(contextPath);
                        cookie.setMaxAge(DEFAULT_EXPIRED_SECONDS);
                        response.addCookie(cookie);
                        //hostHolder.setUser(user);
                        redirectAttributes.addFlashAttribute("tab", 2);
                        return "redirect:/user/setting";
                    }
                }
            }
        }

        return "redirect:/";
    }
}

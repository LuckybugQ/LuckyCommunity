package com.uestc.community.controller;

import com.uestc.community.entity.User;
import com.uestc.community.service.UserService;
import com.uestc.community.util.CommunityConstant;
import com.uestc.community.util.CommunityUtil;
import com.uestc.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Controller
public class VisitorController implements CommunityConstant {

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @RequestMapping("/visitor")
    public String callback(Model model, Session session, HttpServletResponse response){
        String username = "游客"+CommunityUtil.generateUUID().substring(0, 5);
        while(userService.findUserByName(username)!=null){
            username = "游客"+CommunityUtil.generateUUID().substring(0, 5);
        }
        userService.register(username);
        User user = userService.findUserByName(username);
        if(user!=null){
             userService.newVisitorNotice(user.getId());
             Map<String, Object> map = userService.login(user);
             if (map.containsKey("ticket")) {
                   Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
                   cookie.setPath(contextPath);
                   cookie.setMaxAge(DEFAULT_EXPIRED_SECONDS);
                   response.addCookie(cookie);
                   return "redirect:/";
             }
        }
        return "redirect:/";
    }
}

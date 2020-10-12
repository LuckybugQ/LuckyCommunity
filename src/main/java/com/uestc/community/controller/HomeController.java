package com.uestc.community.controller;

import com.uestc.community.entity.DiscussPost;
import com.uestc.community.entity.Page;
import com.uestc.community.entity.User;
import com.uestc.community.service.DiscussPostService;
import com.uestc.community.service.LikeService;
import com.uestc.community.service.SignService;
import com.uestc.community.service.UserService;
import com.uestc.community.util.HostHolder;
import com.uestc.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.uestc.community.util.CommunityConstant.ENTITY_TYPE_POST;

@Controller
public class HomeController {

    @Autowired
    HostHolder hostHolder;

    @Autowired
    SignService signService;

    @Autowired
    DiscussPostService discussPostService;

    @Autowired
    UserService userService;

    @Autowired
    LikeService likeService;

    @Autowired
    RedisTemplate redisTemplate;

    @RequestMapping(path = "/", method = RequestMethod.GET)
    public String getRoot(){
        return "forward:/index";
    }

    @RequestMapping(path = "/index", method = RequestMethod.GET)
    public String getIndexPage(Model model, Page page, @RequestParam(name = "type", defaultValue = "-1") int type,
                               @RequestParam(name = "orderMode", defaultValue = "0") int orderMode,
                               @RequestParam(name = "filter", defaultValue = "0") int filter, HttpSession session) {
        // 方法调用钱,SpringMVC会自动实例化Model和Page,并将Page注入Model.
        // 所以,在thymeleaf中可以直接访问Page对象中的数据.

        int rows = discussPostService.findDiscussPostRowsByType(type,filter);
        List<DiscussPost> list = discussPostService.findDiscussPostsByType(page.getOffset(), page.getLimit(),orderMode,type,filter);


        page.setRows(rows);
        //封装为Map，可以为Model展示
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (list != null) {
            for (DiscussPost post : list) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("post", post);
                    User user = userService.findUserById(post.getUserId());
                    map.put("user", user);

                    long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                    map.put("likeCount", likeCount);

                    discussPosts.add(map);

            }
        }
        model.addAttribute("discussPosts", discussPosts);

        List<Map<String, Object>> topDiscussPosts = new ArrayList<>();
        String topDiscussPostsCacheKey = RedisKeyUtil.getCachekey("topDiscussPosts");
        if(redisTemplate.hasKey(topDiscussPostsCacheKey)){
            topDiscussPosts = (List<Map<String, Object>>) redisTemplate.opsForValue().get(topDiscussPostsCacheKey);
        }else {
            List<DiscussPost> toplist = discussPostService.findDiscussPostsByType(0, 5,0,1,0);
            if (toplist != null) {
                for (DiscussPost post : toplist) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("post", post);
                    User user = userService.findUserById(post.getUserId());
                    map.put("user", user);

                    long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                    map.put("likeCount", likeCount);

                    topDiscussPosts.add(map);
                }
            }
            redisTemplate.opsForValue().set(topDiscussPostsCacheKey,topDiscussPosts,1, TimeUnit.MINUTES);
        }
        model.addAttribute("topDiscussPosts", topDiscussPosts);


        model.addAttribute("filter",filter);
        model.addAttribute("orderMode", orderMode);
        model.addAttribute("type",type);


        return "/index";
    }

    @RequestMapping(path = "/error", method = RequestMethod.GET)
    public String getErrorPage() {
        return "/error/500";
    }

    @RequestMapping(path = "/denied", method = RequestMethod.GET)
    public String getDeniedPage() {
        return "/error/404";
    }
}


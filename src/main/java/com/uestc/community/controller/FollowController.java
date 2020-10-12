package com.uestc.community.controller;

import com.uestc.community.entity.Event;
import com.uestc.community.entity.Page;
import com.uestc.community.entity.User;
import com.uestc.community.event.EventProducer;
import com.uestc.community.service.FollowService;
import com.uestc.community.service.LikeService;
import com.uestc.community.service.UserService;
import com.uestc.community.util.CommunityUtil;
import com.uestc.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.uestc.community.util.CommunityConstant.ENTITY_TYPE_USER;
import static com.uestc.community.util.CommunityConstant.TOPIC_FOLLOW;

@Controller
public class FollowController {

    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private LikeService likeService;
    @RequestMapping(path = "/favorite", method = RequestMethod.POST)
    @ResponseBody
    public String favorite(int entityType, int entityId) {
        User user = hostHolder.getUser();
        if (user == null) {
            return CommunityUtil.getJSONString(1, "你还没有登录哦!");
        }
        boolean favoriteStatus = followService.hasFollowed(user.getId(),entityType,entityId);
        // 返回的结果
        Map<String, Object> map = new HashMap<>();
        if(favoriteStatus){
            followService.unfollow(user.getId(), entityType, entityId);
            // 返回的结果
            map.put("favoriteStatus", 0);
            return CommunityUtil.getJSONString(0, "已取消收藏!",map);
        }else{
            followService.follow(user.getId(), entityType, entityId);
            // 返回的结果
            map.put("favoriteStatus", 1);
            return CommunityUtil.getJSONString(0, "已收藏!",map);
        }
    }



    @RequestMapping(path = "/follow", method = RequestMethod.POST)
    @ResponseBody
    public String follow(int entityType, int entityId) {
        User user = hostHolder.getUser();
        if (user == null) {
            return CommunityUtil.getJSONString(1, "你还没有登录哦!");
        }
        followService.follow(user.getId(), entityType, entityId);

        // 触发关注事件
        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityUserId(entityId);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0, "已关注!");
    }

    @RequestMapping(path = "/unfollow", method = RequestMethod.POST)
    @ResponseBody
    public String unfollow(int entityType, int entityId) {
        User user = hostHolder.getUser();
        if (user == null) {
            return CommunityUtil.getJSONString(1, "你还没有登录哦!");
        }
        followService.unfollow(user.getId(), entityType, entityId);

        return CommunityUtil.getJSONString(0, "已取消关注!");
    }

    @RequestMapping(path = "/followees/{userId}", method = RequestMethod.GET)
    public String getFollowees(@PathVariable("userId") int userId, Page page, Model model) {

        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        // 判断是否为新用户
        Date now = new Date();
        Date createTime = user.getCreateTime();
        boolean isNewUser = ((now.getTime()-createTime.getTime())/(24*60*60*1000)) < 1 ? true:false;
        model.addAttribute("isNewUser",isNewUser);

        model.addAttribute("user", user);
        // 点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);
        // 关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // 粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);

        page.setLimit(8);
        page.setPath("/followees/" + userId);
        page.setRows((int) followService.findFolloweeCount(userId, ENTITY_TYPE_USER));

        List<Map<String, Object>> userList = followService.findFollowees(userId, page.getOffset(), page.getLimit());
        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
                int like = likeService.findUserLikeCount(u.getId());
                map.put("likeCount",like);

            }
        }
        model.addAttribute("users", userList);
        // 是否已关注
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/followee";
    }

    @RequestMapping(path = "/followers/{userId}", method = RequestMethod.GET)
    public String getFollowers(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        // 判断是否为新用户
        Date now = new Date();
        Date createTime = user.getCreateTime();
        boolean isNewUser = ((now.getTime()-createTime.getTime())/(24*60*60*1000)) < 1 ? true:false;
        model.addAttribute("isNewUser",isNewUser);

        model.addAttribute("user", user);
        // 点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);
        // 关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // 粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);


        page.setLimit(8);
        page.setPath("/followers/" + userId);
        page.setRows((int) followService.findFollowerCount(ENTITY_TYPE_USER, userId));

        List<Map<String, Object>> userList = followService.findFollowers(userId, page.getOffset(), page.getLimit());
        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
                int like = likeService.findUserLikeCount(u.getId());
                map.put("likeCount",like);
            }
        }
        model.addAttribute("users", userList);

        // 是否已关注
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/follower";
    }

    private boolean hasFollowed(int userId) {
        if (hostHolder.getUser() == null) {
            return false;
        }

        return followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
    }

}

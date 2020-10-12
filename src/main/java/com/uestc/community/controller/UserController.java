package com.uestc.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.uestc.community.annotation.LoginRequired;
import com.uestc.community.entity.*;
import com.uestc.community.service.*;
import com.uestc.community.util.AliyunOssUtil;
import com.uestc.community.util.CommunityConstant;
import com.uestc.community.util.CommunityUtil;
import com.uestc.community.util.HostHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Autowired
    private AliyunOssUtil aliyunOssUtil;

    @LoginRequired
    @RequestMapping(path = "/setting",method = RequestMethod.GET)
    public String getSettingPage(){
        return "/site/setting";
    }


    @RequestMapping(path="/updateHeader",method = RequestMethod.POST)
    @ResponseBody
    public String updateHeader(String url){
        // 更新当前用户的头像的路径(web访问路径)
        // http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        userService.updateHeader(user.getId(), url);
        return CommunityUtil.getJSONString(0);
    }

    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        // 服务器存放路径
        fileName = uploadPath + "/" + fileName;
        // 文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
        // 响应图片
        response.setContentType("image/" + suffix);
        try (   //括号中内容会自动加上finally进行close()操作！
                FileInputStream fis = new FileInputStream(fileName);
                OutputStream os = response.getOutputStream();
        ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败: " + e.getMessage());
        }
    }

    // 个人主页
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");
        }
        //最近发帖
        List<DiscussPost> postList = discussPostService.findDiscussPosts(user.getId(), 0, 12,0);
        List<Map<String, Object>> posts = new ArrayList<>();
        if (postList != null) {
            for (DiscussPost discussPost : postList) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", discussPost);
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST,discussPost.getId()));
                posts.add(map);
            }
        }
        model.addAttribute("discussPosts", posts);

        //最近回复
        List<Comment> commentList = commentService.findPostComments(user.getId(), 0, 5);
        List<Map<String, Object>> comments = new ArrayList<>();
        if (commentList != null) {
            for (Comment comment : commentList) {
                Map<String, Object> map = new HashMap<>();
                DiscussPost post = discussPostService.findDiscussPostById(comment.getEntityId());
                map.put("post",post);
                map.put("comment", comment);
                comments.add(map);
            }
        }
        model.addAttribute("comments", comments);

        // 用户
        model.addAttribute("user", user);
        // 判断是否为新用户
        Date now = new Date();
        Date createTime = user.getCreateTime();
        boolean isNewUser = ((now.getTime()-createTime.getTime())/(24*60*60*1000)) < 1 ? true:false;
        model.addAttribute("isNewUser",isNewUser);
        // 点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);
        // 关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // 粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);


        // 是否已关注
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }

    @LoginRequired
    @RequestMapping(path = "/message",method = RequestMethod.GET)
    public String getMessagePage(Model model, Page page, @RequestParam(name = "tab", defaultValue = "1") int tab){
        model.addAttribute("tab",tab);
        User user = hostHolder.getUser();
        model.addAttribute("user",user);

        page.setLimit(10);

        int conversationCount = messageService.findConversationCount(user.getId());
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(),null);
        model.addAttribute("conversationCount",conversationCount);
        model.addAttribute("letterUnreadCount",letterUnreadCount);

        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(),null);
        int noticeCount = messageService.findNoticeCount(user.getId(),null);
        model.addAttribute("noticeCount",noticeCount);
        model.addAttribute("noticeUnreadCount",noticeUnreadCount);

        if(tab==1){

            page.setRows(conversationCount);
            // 会话列表
            List<Message> conversationList = messageService.findConversations(
                    user.getId(), page.getOffset(), page.getLimit());
            List<Map<String, Object>> conversations = new ArrayList<>();

            if (conversationList != null) {
                for (Message message : conversationList) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("conversation", message);
                    map.put("letterCount", messageService.findLetterCount(message.getConversationId()));
                    map.put("unreadCount", messageService.findLetterUnreadCount(user.getId(), message.getConversationId()));
                    int targetId = user.getId() == message.getFromId() ? message.getToId() : message.getFromId();
                    map.put("target", userService.findUserById(targetId));
                    conversations.add(map);
                }
            }
            model.addAttribute("conversations", conversations);

        }else if(tab == 2){
            page.setRows(3);
            // 查询评论类通知
            Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);
            if (message != null) {
                Map<String, Object> messageVO = new HashMap<>();
                messageVO.put("message", message);

                String content = HtmlUtils.htmlUnescape(message.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

                messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
                messageVO.put("entityType", data.get("entityType"));
                messageVO.put("entityId", data.get("entityId"));
                messageVO.put("postId", data.get("postId"));

                int count = messageService.findNoticeCount(user.getId(), TOPIC_COMMENT);
                messageVO.put("count", count);

                int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT);
                messageVO.put("unread", unread);

                model.addAttribute("commentNotice", messageVO);
            }
            // 查询点赞类通知
            message = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);

            if (message != null) {
                Map<String, Object> messageVO = new HashMap<>();
                messageVO.put("message", message);

                String content = HtmlUtils.htmlUnescape(message.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

                messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
                messageVO.put("entityType", data.get("entityType"));
                messageVO.put("entityId", data.get("entityId"));
                messageVO.put("postId", data.get("postId"));

                int count = messageService.findNoticeCount(user.getId(), TOPIC_LIKE);
                messageVO.put("count", count);

                int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_LIKE);
                messageVO.put("unread", unread);
                model.addAttribute("likeNotice", messageVO);
            }


            // 查询关注类通知
            message = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);

            if (message != null) {
                Map<String, Object> messageVO = new HashMap<>();
                messageVO.put("message", message);

                String content = HtmlUtils.htmlUnescape(message.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

                messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
                messageVO.put("entityType", data.get("entityType"));
                messageVO.put("entityId", data.get("entityId"));

                int count = messageService.findNoticeCount(user.getId(), TOPIC_FOLLOW);
                messageVO.put("count", count);

                int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
                messageVO.put("unread", unread);
                model.addAttribute("followNotice", messageVO);
            }
        }
        return "/site/message";
    }

    @LoginRequired
    @RequestMapping(path = "/message/detail/{conversationId}",method = RequestMethod.GET)
    public String getMessagePage(@PathVariable("conversationId")String conversationId, Model model, Page page){
        User user = hostHolder.getUser();
        model.addAttribute("user",user);

        page.setLimit(10);
        page.setRows(messageService.findLetterCount(conversationId));

        // 私信列表
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> letters = new ArrayList<>();
        if (letterList != null) {
            for (Message message : letterList) {
                Map<String, Object> map = new HashMap<>();
                map.put("letter", message);
                map.put("fromUser", userService.findUserById(message.getFromId()));
                letters.add(map);
            }
        }
        model.addAttribute("letters", letters);

        // 私信目标
        model.addAttribute("target", getLetterTarget(conversationId));

        // 设置已读
        List<Integer> ids = getLetterIds(letterList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }

        return "/site/message-detail";
    }



    @LoginRequired
    @RequestMapping(path = "/notice/{topic}",method = RequestMethod.GET)
    public String getNoticePage(@PathVariable("topic") String topic, Model model, Page page){
        User user = hostHolder.getUser();
        model.addAttribute("user",user);

        page.setLimit(10);
        page.setRows(messageService.findNoticeCount(user.getId(), topic));

        List<Message> noticeList = messageService.findNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String, Object>> noticeVoList = new ArrayList<>();
        if (noticeList != null) {
            for (Message notice : noticeList) {
                Map<String, Object> map = new HashMap<>();
                // 通知
                map.put("notice", notice);
                // 内容
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
                map.put("user", userService.findUserById((Integer) data.get("userId")));
                map.put("entityType", data.get("entityType"));
                map.put("entityId", data.get("entityId"));
                map.put("postId", data.get("postId"));
                // 通知作者
                map.put("fromUser", userService.findUserById(notice.getFromId()));

                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices", noticeVoList);

        // 设置已读
        List<Integer> ids = getLetterIds(noticeList);
        if (!ids.isEmpty()) {
            messageService.readMessage(ids);
        }

        return "/site/notice-detail";

    }

    @LoginRequired
    @RequestMapping(path = "/info",method = RequestMethod.GET)
    public String getInfoPage(Model model, Page page, @RequestParam(name = "tab", defaultValue = "1") int tab){
        model.addAttribute("tab",tab);
        User user = hostHolder.getUser();
        model.addAttribute("user",user);

        page.setLimit(10);

        int postCount = discussPostService.findDiscussPostRows(user.getId());
        model.addAttribute("postCount",postCount);
        int commentCount = commentService.findPostCommentsRows(user.getId());
        model.addAttribute("commentCount",commentCount);
        long favoriteCount = followService.findFolloweeCount(user.getId(),ENTITY_TYPE_POST);
        model.addAttribute("favoriteCount",favoriteCount);
        if(tab==1){
            page.setRows(Integer.parseInt(String.valueOf(favoriteCount)));
            // 会话列表
            List<Map<String, Object>> posts = followService.findFavorite(user.getId(), page.getOffset(), page.getLimit());
            model.addAttribute("favoritePosts", posts);
        }
        else if(tab==2){
            page.setRows(postCount);
            // 会话列表
            List<DiscussPost> postList = discussPostService.findDiscussPosts(user.getId(), page.getOffset(), page.getLimit(),0);
            List<Map<String, Object>> posts = new ArrayList<>();
            if (postList != null) {
                for (DiscussPost discussPost : postList) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("post", discussPost);
                    map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST,discussPost.getId()));
                    posts.add(map);
                }
            }
            model.addAttribute("discussPosts", posts);
        }else if(tab == 3){
            page.setRows(commentCount);
            // 会话列表
            List<Comment> commentList = commentService.findPostComments(user.getId(), page.getOffset(), page.getLimit());
            List<Map<String, Object>> comments = new ArrayList<>();
            if (commentList != null) {
                for (Comment comment : commentList) {
                    Map<String, Object> map = new HashMap<>();
                    DiscussPost post = discussPostService.findDiscussPostById(comment.getEntityId());
                    map.put("post",post);
                    map.put("comment", comment);
                    comments.add(map);
                }
            }
            model.addAttribute("comments", comments);
        }
        return "/site/info";
    }


    @RequestMapping(path = "/data", method = RequestMethod.POST)
    @ResponseBody
    public String setUserData(int gender,String city,String signature) {
        User user = hostHolder.getUser();
        userService.updateData(user.getId(),gender,city,signature);

        Map<String,Object> map = new HashMap<>();
        map.put("status",0);
        map.put("action","/user/setting");

        return CommunityUtil.getJSONString(0,"资料修改成功！",map);
    }

    @RequestMapping(path = "/password", method = RequestMethod.POST)
    @ResponseBody
    public String password(@CookieValue("ticket") String ticket,
                           String oldPassword, String password, Model model) {

        if(password.length()<6 || password.length()>16){
            return CommunityUtil.getJSONString(1,"密码长度不符合要求！");
        }

        User user = hostHolder.getUser();
        String pwd = CommunityUtil.md5(oldPassword+user.getSalt());

        if(user.getPassword()!=null && !pwd.equals(user.getPassword())){
            return CommunityUtil.getJSONString(1,"原密码不正确！");
        }


/*        if(!confirmPassword.equals(newPassword)){
            model.addAttribute("confirmPasswordMsg", "两次输入的密码不一致!");
            return "/site/setting";
        }*/

        String updatedPassword = CommunityUtil.md5(password + user.getSalt());
        user.setPassword(updatedPassword);
        userService.updatePassword(user.getId(),updatedPassword);

        Map<String,Object> map = new HashMap<>();
        map.put("status",0);
        map.put("action","/user/password/success");

        return CommunityUtil.getJSONString(0,"密码修改成功！",map);
    }

    @RequestMapping(path = "/password/success", method = RequestMethod.GET)
    public String passwordSuccess(@CookieValue("ticket") String ticket, Model model) {
        User user = hostHolder.getUser();
        // 修改密码
        model.addAttribute("msg", "密码修改成功，请重新登录!");
        model.addAttribute("target", "/login");
        userService.logout(ticket);
        SecurityContextHolder.clearContext();

        return "/site/operate-result";
    }


    private User getLetterTarget(String conversationId) {
        String[] ids = conversationId.split("_");
        int id0 = Integer.parseInt(ids[0]);
        int id1 = Integer.parseInt(ids[1]);

        if (hostHolder.getUser().getId() == id0) {
            return userService.findUserById(id1);
        } else {
            return userService.findUserById(id0);
        }
    }

    private List<Integer> getLetterIds(List<Message> letterList) {
        List<Integer> ids = new ArrayList<>();

        if (letterList != null) {
            for (Message message : letterList) {
                if (hostHolder.getUser().getId() == message.getToId() && message.getStatus() == 0) {
                    ids.add(message.getId());
                }
            }
        }

        return ids;
    }
}

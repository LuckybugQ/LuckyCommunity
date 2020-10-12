package com.uestc.community.controller;

import com.uestc.community.entity.*;
import com.uestc.community.event.EventProducer;
import com.uestc.community.service.*;
import com.uestc.community.util.CommunityUtil;
import com.uestc.community.util.HostHolder;
import com.uestc.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.*;

import static com.uestc.community.util.CommunityConstant.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController {

    @Autowired
    private SignService signService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;


    @RequestMapping(path = "/add",method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title, String content,int type ,int reward) {
        User user = hostHolder.getUser();
        if (user == null) {
            return CommunityUtil.getJSONString(1, "你还没有登录哦！");
        }
        if(type == 0){
            if (user.getReward() < reward) {
                return CommunityUtil.getJSONString(1, "你的钻石不够！");
            }
            userService.updateReward(user.getId(),user.getReward()-reward);
        }

        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setType(type);
        post.setReward(reward);
        post.setCreateTime(new Date());
        post.setUpdateTime(new Date());
        discussPostService.addDiscussPost(post);

        // 触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(post.getId());
        eventProducer.fireEvent(event);

        // 计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, post.getId());
        Map<String,Object> map = new HashMap<>();
        map.put("status",0);
        map.put("action","/");
        // 报错的情况,将来统一处理.
        return CommunityUtil.getJSONString(0, "发布成功！",map);
    }

    @RequestMapping(path = "/edit/{discussPostId}",method = RequestMethod.POST)
    @ResponseBody
    public String editDiscussPost(@PathVariable("discussPostId") int discussPostId,String title, String content) {

        User user = hostHolder.getUser();
        if (user == null) {
            return CommunityUtil.getJSONString(1, "你还没有登录哦！");
        }
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        if (user.getType()!=1 && post.getUserId() != user.getId()){
            return CommunityUtil.getJSONString(1, "你没有编辑的权限！");
        }
        discussPostService.updateTitle(discussPostId,title);
        discussPostService.updateContent(discussPostId,content);

        Map<String,Object> map = new HashMap<>();
        map.put("status",0);
        map.put("action","/discuss/detail/"+discussPostId);
        // 报错的情况,将来统一处理.
        return CommunityUtil.getJSONString(0, "编辑成功！",map);
    }

    @RequestMapping(path = "/edit/{discussPostId}", method = RequestMethod.GET)
    public String getDiscussPostEditPage(@PathVariable("discussPostId") int discussPostId, Model model, Page page, HttpSession session){
        // 帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post", post);
        // 作者
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user", user);
        return "/site/edit";
    }

    @RequestMapping(path = "/detail/{discussPostId}", method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page, HttpSession session) {
        // 帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post", post);
        // 作者
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user", user);
        // 点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount", likeCount);
        // 点赞状态
        int likeStatus = hostHolder.getUser() == null ? 0 :
                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeStatus", likeStatus);
        // 关注状态
        int favoriteStatus = hostHolder.getUser() == null ? 0 :
                (followService.hasFollowed(hostHolder.getUser().getId(),1,discussPostId)?1:0);
        model.addAttribute("favoriteStatus", favoriteStatus);
        //评论：给帖子的评论
        //评论：给评论的评论
        //评论列表
        page.setLimit(5);
        page.setPath("/discuss/detail/"+discussPostId);
        page.setRows(post.getCommentCount());
        //评论列表
        List<Comment> commentList = commentService.findCommentsByEntity(ENTITY_TYPE_POST,discussPostId,page.getOffset(),page.getLimit());
        //获取最佳答案
        if(post.getType()==0 && post.getAcceptId()!=0){
            Comment comment = commentService.findCommentById(post.getAcceptId());
            Map<String,Object> commentVo = new HashMap<>();
            commentVo.put("comment",comment);
            commentVo.put("user",userService.findUserById(comment.getUserId()));
            // 点赞数量
            likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
            commentVo.put("likeCount", likeCount);
            // 点赞状态
            likeStatus = hostHolder.getUser() == null ? 0 :
                    likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
            commentVo.put("likeStatus", likeStatus);

            //回复列表
            List<Comment> replyList = commentService.findCommentsByEntity(ENTITY_TYPE_COMMENT,comment.getId(),0,Integer.MAX_VALUE);
            //回复VO列表
            List<Map<String,Object>> replyVoList = new ArrayList<>();
            if(replyList != null){
                for (Comment reply:replyList){
                    Map<String,Object> replyVo = new HashMap<>();
                    replyVo.put("reply",reply);
                    replyVo.put("user",userService.findUserById(reply.getUserId()));

                    User target = reply.getTargetId() == 0? null: userService.findUserById(reply.getTargetId());
                    replyVo.put("target",target);
                    // 点赞数量
                    likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                    replyVo.put("likeCount", likeCount);
                    // 点赞状态
                    likeStatus = hostHolder.getUser() == null ? 0 :
                            likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                    replyVo.put("likeStatus", likeStatus);
                    replyVoList.add(replyVo);
                }
            }
            commentVo.put("replys",replyVoList);
            //回复数量
            int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT,comment.getId());
            commentVo.put("replyCount",replyCount);
            model.addAttribute("bestComment",commentVo);
        }


        //评论VO列表
        List<Map<String,Object>> commentVoList = new ArrayList<>();
        if(commentList != null){
            for (Comment comment:commentList){
                Map<String,Object> commentVo = new HashMap<>();
                commentVo.put("comment",comment);
                commentVo.put("user",userService.findUserById(comment.getUserId()));
                // 点赞数量
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount", likeCount);
                // 点赞状态
                likeStatus = hostHolder.getUser() == null ? 0 :
                        likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus", likeStatus);

                //回复列表
                List<Comment> replyList = commentService.findCommentsByEntity(ENTITY_TYPE_COMMENT,comment.getId(),0,Integer.MAX_VALUE);
                //回复VO列表
                List<Map<String,Object>> replyVoList = new ArrayList<>();
                if(replyList != null){
                    for (Comment reply:replyList){
                        Map<String,Object> replyVo = new HashMap<>();
                        replyVo.put("reply",reply);
                        replyVo.put("user",userService.findUserById(reply.getUserId()));

                        User target = reply.getTargetId() == 0? null: userService.findUserById(reply.getTargetId());
                        replyVo.put("target",target);
                        // 点赞数量
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount", likeCount);
                        // 点赞状态
                        likeStatus = hostHolder.getUser() == null ? 0 :
                                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeStatus", likeStatus);
                        replyVoList.add(replyVo);
                    }
                }
                commentVo.put("replys",replyVoList);
                //回复数量
                int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT,comment.getId());
                commentVo.put("replyCount",replyCount);
                commentVoList.add(commentVo);
            }
        }
        model.addAttribute("comments",commentVoList);


        return "/site/discuss-detail";
    }


    // 采纳
    @RequestMapping(path = "/accept", method = RequestMethod.POST)
    @ResponseBody
    public String setAccept(int postId ,int commentId) {
        discussPostService.updateAcceptId(postId,commentId);

        DiscussPost post = discussPostService.findDiscussPostById(postId);
        Comment comment = commentService.findCommentById(commentId);
        int commentUserId = comment.getUserId();
        User commentUser = userService.findUserById(commentUserId);

        userService.updateReward(commentUserId,commentUser.getReward()+post.getReward());


        Map<String,Object> map = new HashMap<>();
        map.put("status",0);
        // 报错的情况,将来统一处理.
        return CommunityUtil.getJSONString(0, "采纳成功!",map);
    }

    // 置顶
    @RequestMapping(path = "/top/{flag}", method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id,@PathVariable("flag") int flag) {
        discussPostService.updateType(id, flag);

        // 触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }

    // 加精
    @RequestMapping(path = "/wonderful/{flag}", method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id,@PathVariable("flag") int flag) {
        discussPostService.updateStatus(id, flag);

        // 触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        // 计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, id);

        return CommunityUtil.getJSONString(0);
    }

    // 删除
    @RequestMapping(path = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id) {
        discussPostService.updateStatus(id, 2);

        // 触发删帖事件
        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        Map<String,Object> map = new HashMap<>();
        map.put("status",0);

        return CommunityUtil.getJSONString(0,"删除成功！",map);
    }


}

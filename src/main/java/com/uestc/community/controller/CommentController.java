package com.uestc.community.controller;

import com.uestc.community.entity.Comment;
import com.uestc.community.entity.DiscussPost;
import com.uestc.community.entity.Event;
import com.uestc.community.entity.User;
import com.uestc.community.event.EventProducer;
import com.uestc.community.service.CommentService;
import com.uestc.community.service.DiscussPostService;
import com.uestc.community.service.UserService;
import com.uestc.community.util.CommunityConstant;
import com.uestc.community.util.CommunityUtil;
import com.uestc.community.util.HostHolder;
import com.uestc.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {

    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/add/{discussPostId}", method = RequestMethod.POST)
    //PathVariable：传入路径中的变量
    @ResponseBody
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment, HttpServletRequest request) {

        //这里会自动通过 html 传入  entityType 、 entityId 、 ( targetId )、 content 到 comment 实体 中。
        if (comment.getEntityType() == ENTITY_TYPE_COMMENT){
            String content = comment.getContent();
            if(content.length()>3 && content.substring(0,2).equals("回复")){
                int index = content.indexOf(":");
                if(index!=-1){
                    String username = content.substring(2,index);
                    User user = userService.findUserByName(username);
                    if(user != null){
                        comment.setTargetId(user.getId());
                        content = content.substring(index+1);
                        comment.setContent(content);
                    }
                }
            }
        }

        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);

        // 触发评论事件
        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId", discussPostId);
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        eventProducer.fireEvent(event);

        Map<String,Object> map = new HashMap<>();
        map.put("status",0);
        map.put("action","/discuss/detail/" + discussPostId);

        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            // 触发发帖事件
            event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(comment.getUserId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId);
            eventProducer.fireEvent(event);
            // 计算帖子分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, discussPostId);

            //更新帖子时间
            discussPostService.updateUpdateTime(discussPostId,new Date());

            //跳转到最后一页
            DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
            int rows = post.getCommentCount();
            int limit = 5;
            int curr = rows / limit;
            if (rows % limit != 0) {
                curr ++;
            }
            map.put("action","/discuss/detail/" + discussPostId+ "?current="+curr);
            //return "redirect:/discuss/detail/" + discussPostId + "?current="+curr;
        }

        return CommunityUtil.getJSONString(0,"回复成功！",map);

        //return "redirect:/discuss/detail/" + discussPostId;
    }

    @RequestMapping(path="/delete",method = RequestMethod.POST)
    @ResponseBody
    public String deleteComment(int commentId) {
        Comment comment = commentService.findCommentById(commentId);
        int rows = commentService.deleteComment(comment);
        Map<String,Object> map = new HashMap<>();
        map.put("status",0);
        // 报错的情况,将来统一处理.
        return CommunityUtil.getJSONString(0, "删除成功!",map);
    }

}

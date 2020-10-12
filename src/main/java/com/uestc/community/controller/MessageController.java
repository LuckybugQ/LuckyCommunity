package com.uestc.community.controller;

import com.uestc.community.entity.Message;
import com.uestc.community.entity.User;
import com.uestc.community.service.MessageService;
import com.uestc.community.service.UserService;
import com.uestc.community.util.CommunityConstant;
import com.uestc.community.util.CommunityUtil;
import com.uestc.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class MessageController implements CommunityConstant {

    @Autowired
    private MessageService messageService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;


    @RequestMapping(path = "/letter/send", method = RequestMethod.POST)
    //异步请求 所以需要声明ResponseBody 进行页面的局部更新
    @ResponseBody
    public String sendLetter(String toName, String content) {
        User target = userService.findUserByName(toName);
        if (target == null) {
            return CommunityUtil.getJSONString(1, "目标用户不存在!");
        }

        Message message = new Message();
        message.setFromId(hostHolder.getUser().getId());
        message.setToId(target.getId());
        String conversationId = "";
        if (message.getFromId() < message.getToId()) {
            conversationId = message.getFromId() + "_" + message.getToId();
        } else {
            conversationId = message.getToId() + "_" + message.getFromId();
        }
        message.setConversationId(conversationId);
        message.setContent(content);
        message.setCreateTime(new Date());
        messageService.addMessage(message);

        Map<String,Object> map = new HashMap<>();
        map.put("status",0);
        map.put("action","/user/message/detail/"+conversationId);

        return CommunityUtil.getJSONString(0,"发送成功！",map);
    }
}

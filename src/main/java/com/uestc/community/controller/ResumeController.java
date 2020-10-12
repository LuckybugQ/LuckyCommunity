package com.uestc.community.controller;

import com.uestc.community.util.CommunityConstant;
import com.uestc.community.util.CommunityUtil;
import com.uestc.community.util.MailClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.Context;

import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/resume")
public class ResumeController implements CommunityConstant {
    Logger logger = LoggerFactory.getLogger(ResumeController.class);

    @Autowired
    private MailClient mailClient;

    @RequestMapping(path = "/send", method = RequestMethod.POST)
    @ResponseBody
    public String verifyCode(String content, HttpServletResponse response) {
        sendThread thread = new sendThread(content);
        thread.start();
        //response.setHeader("Access-Control-Allow-Origin", null);
        return CommunityUtil.getJSONString(0);
    }

    class sendThread extends Thread{
        private String email;
        private String content;

        public sendThread(String content){
            this.email = "luckybugq@163.com";
            this.content = content;
        }
        public void run() {
            // 激活邮件
            Context context = new Context();
            context.setVariable("email", email);
            // http://localhost:8080/community/activation/101/code
            mailClient.sendMail(email, "简历消息", content);
            logger.info("发送简历消息成功！");
        }
    }




}

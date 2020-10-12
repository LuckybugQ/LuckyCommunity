package com.uestc.community;


import com.uestc.community.util.MailClient;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MailTests {

    private static final Logger log = LoggerFactory.getLogger(LoggerTests.class);

    @Autowired
    MailClient mailClient;

    @Autowired
    TemplateEngine templateEngine;

    @Test
    public void mailTest() {
        mailClient.sendMail("75075188@qq.com","TEST","Hello");
    }

    @Test
    public void HTMLmailTest(){
        Context context = new Context();
        context.setVariable("username","ZZQ");
        String content = templateEngine.process("mail/demo",context);
        System.out.println(content);

        mailClient.sendMail("75075188@qq.com","HTML",content);
    }


}


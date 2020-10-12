package com.uestc.community;

import com.uestc.community.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.text.ParseException;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MyTests {

    @Autowired
    private UserService userService;

    @Test
    public void insertTestUser() throws ParseException {
        //userService.registerTest("Luffy",0,"成都","我是要成为海贼王的男人！",0,1);
        userService.registerTest("Zoro",0,"成都","该用户很懒，什么都没有写",1,0);
        userService.registerTest("Sanji",0,"成都","❤_❤",2,0);
        userService.registerTest("Usopp",0,"成都","我得了一种不学习就会死的病",3,1);
        userService.registerTest("Nami",1,"成都","$_$",4,1);
        userService.registerTest("Chopper",0,"成都","诶~~~？",5,0);
        userService.registerTest("Robin",1,"成都","~",6,0);
        userService.registerTest("Franky",0,"成都","这周的我很super~",7,0);
        userService.registerTest("Brook",0,"成都","哟霍霍霍霍",8,0);
        userService.registerTest("Shanks",0,"上海","给我个面子",9,0);
        userService.registerTest("Mihawk",0,"广州","。",10,0);
        userService.registerTest("Jinbe",0,"厦门","鱼人永不为奴！",11,0);
        userService.registerTest("Crocodile",0,"乌鲁木齐","让沙尘暴来得更猛烈些吧",12,0);
        userService.registerTest("Kuma",0,"哈尔滨","要旅行的话 你想去哪里",13,0);
        userService.registerTest("Buggy",0,"上海","Call Me Joker",14,0);
        userService.registerTest("Doflamingo",0,"杭州","咈咈咈咈咈",15,0);
        userService.registerTest("Hancock",1,"苏州","（战术后仰）",16,0);
        userService.registerTest("Ace",0,"深圳","I dont wanna die",17,0);
        userService.registerTest("Rayleigh",0,"上海","活到老，学到老",18,0);
        userService.registerTest("Ivankov",0,"北京","❤❤❤❤❤",19,0);
        userService.registerTest("Kid",0,"南京","干！",20,0);
        userService.registerTest("Hawkins",0,"无锡","安静的稻草人是我~",21,0);
        userService.registerTest("Law",0,"徐州","R-O-O-M",22,0);
        userService.registerTest("Bepo",0,"徐州","我觉得全世界的熊都一个熊样",23,0);

    }
}

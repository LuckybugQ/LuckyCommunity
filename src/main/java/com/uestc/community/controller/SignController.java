package com.uestc.community.controller;

import com.uestc.community.entity.User;
import com.uestc.community.service.SignService;
import com.uestc.community.service.UserService;
import com.uestc.community.util.CommunityUtil;
import com.uestc.community.util.HostHolder;
import com.uestc.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class SignController {

    @Autowired
    HostHolder hostHolder;

    @Autowired
    SignService signService;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    UserService userService;

    @RequestMapping(path = "/sign",method = RequestMethod.POST)
    @ResponseBody
    public String signIn() throws ParseException {
        User user = hostHolder.getUser();
        if (user == null) {
            return CommunityUtil.getJSONString(1, "你还没有登录哦!");
        }
        int days = signService.getSignedDays(user.getId());
        int reward = signService.getSignedReward(days);
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String signKey = RedisKeyUtil.getSignKey(user.getId());
                String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                operations.multi();
                operations.opsForSet().add(signKey,date);
                return operations.exec();
            }
        });

        String signDaysKey = RedisKeyUtil.getSignDaysKey(user.getId());
        redisTemplate.opsForValue().set(signDaysKey,days+1);

        userService.updateReward(user.getId(),user.getReward()+reward);
        Map<String,Object> map = new HashMap<>();
        Map<String,Object> data = new HashMap<>();
        data.put("signed",true);
        data.put("experience",reward);
        data.put("days",days+1);

        map.put("data",data);
        map.put("status",0);

        // 报错的情况,将来统一处理.
        return CommunityUtil.getJSONString(0, "签到成功!",map);
    }

}

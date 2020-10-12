package com.uestc.community.service;

import com.uestc.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Service
public class SignService {

    @Autowired
    private RedisTemplate redisTemplate;

    public boolean getSigned(int userId) {
        String signKey = RedisKeyUtil.getSignKey(userId);
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        return redisTemplate.opsForSet().isMember(signKey, date);
    }

    public int getSignedDays(int userId) throws ParseException {
        String signKey = RedisKeyUtil.getSignKey(userId);
        SimpleDateFormat dft = new SimpleDateFormat("yyyy-MM-dd");
        Date today = new Date();
        String todayKey = new SimpleDateFormat("yyyy-MM-dd").format(today);
        if(redisTemplate.opsForSet().isMember(signKey, todayKey)){
            String signDaysKey = RedisKeyUtil.getSignDaysKey(userId);
            return (int) redisTemplate.opsForValue().get(signDaysKey);
        }
        Calendar date = Calendar.getInstance();
        date.setTime(today);
        date.set(Calendar.DATE, date.get(Calendar.DATE) - 1);
        Date yesterday = dft.parse(dft.format(date.getTime()));
        String yesterdayKey = new SimpleDateFormat("yyyy-MM-dd").format(yesterday);
        if(redisTemplate.opsForSet().isMember(signKey, yesterdayKey)){
            String signDaysKey = RedisKeyUtil.getSignDaysKey(userId);
            return (int) redisTemplate.opsForValue().get(signDaysKey);
        }else{
            return 0;
        }

    }

    public int getSignedReward(int signedDays){
        if(signedDays<1){
            return 5;
        }else if(signedDays<5){
            return 10;
        }else if(signedDays<10){
            return 20;
        }else{
            return 30;
        }
    }

}

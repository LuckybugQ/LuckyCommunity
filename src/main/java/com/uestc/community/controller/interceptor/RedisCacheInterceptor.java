package com.uestc.community.controller.interceptor;

import com.uestc.community.entity.DiscussPost;
import com.uestc.community.entity.User;
import com.uestc.community.service.*;
import com.uestc.community.util.HostHolder;
import com.uestc.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class RedisCacheInterceptor implements HandlerInterceptor {

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private DataService dataService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private UserService userService;

    @Autowired
    private SignService signService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${github.redirect.uri}")
    private String redirectUri;

    @Value("${github.client.id}")
    private String clientId;

    //按日统计
    private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {



        String UVcacheKey = RedisKeyUtil.getCachekey("UV");
        String DAUcacheKey = RedisKeyUtil.getCachekey("DAU");
        int UV = 0;
        int DAU = 0;
        if(redisTemplate.hasKey(UVcacheKey) && redisTemplate.hasKey(DAUcacheKey)){
            UV = (int) redisTemplate.opsForValue().get(UVcacheKey);
            DAU = (int) redisTemplate.opsForValue().get(DAUcacheKey);
        }
        else{
            //统计UV
            Date start = df.parse("20200101");
            Date end = new Date();
            UV = (int) dataService.calculateUV(start,end);
            DAU =(int) dataService.calculateDAU(start,end);
            redisTemplate.opsForValue().set(UVcacheKey,UV,30, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(DAUcacheKey,DAU,30, TimeUnit.MINUTES);
        }

        List<Map<String, Object>> hotUsers = new ArrayList<>();
        String hotUserCacheKey = RedisKeyUtil.getCachekey("hotUser");
        if(redisTemplate.hasKey(hotUserCacheKey)){
            hotUsers = (List<Map<String, Object>>) redisTemplate.opsForValue().get(hotUserCacheKey);
        }else{
            List<User> users = userService.findAllUser();
            if (users != null) {
                for (User user : users) {
                    long likeCount = likeService.findUserLikeCount(user.getId());
                    user.setLikeCount(likeCount);
                }
                Collections.sort(users, new Comparator<User>() {
                    @Override
                    public int compare(User o1, User o2) {
                        return (int) ((int) o2.getLikeCount() - o1.getLikeCount());
                    }
                });
                int i = 0 ;
                while (hotUsers.size()<12) {
                    User user = users.get(i);
                    Map<String, Object> map = new HashMap<>();
                    map.put("user", user);
                    hotUsers.add(map);
                    i++;
                }
            }
            redisTemplate.opsForValue().set(hotUserCacheKey,hotUsers,1, TimeUnit.MINUTES);
        }

        List<DiscussPost> recommendPosts = new ArrayList<>();
        String recommendPostsCacheKey = RedisKeyUtil.getCachekey("recommendPosts");
        if(redisTemplate.hasKey(recommendPostsCacheKey)){
            recommendPosts = (List<DiscussPost>) redisTemplate.opsForValue().get(recommendPostsCacheKey);
        }else {
            recommendPosts = discussPostService.findRecommendDiscussPosts();
            redisTemplate.opsForValue().set(recommendPostsCacheKey,recommendPosts,30, TimeUnit.MINUTES);
        }

        if (modelAndView != null){

            modelAndView.addObject("DAU",DAU);
            modelAndView.addObject("UV",UV);
            modelAndView.addObject("hotUsers",hotUsers);
            modelAndView.addObject("recommendPosts",recommendPosts);

            boolean hasSigned = false;
            int signedDays = 0;
            User user = hostHolder.getUser();
            int signedReward = 5;
            if(user!=null){
                hasSigned = signService.getSigned(user.getId());
                signedDays = signService.getSignedDays(user.getId());
                if(hasSigned){
                    signedReward = signService.getSignedReward(signedDays-1);
                }else{
                    signedReward = signService.getSignedReward(signedDays);
                }
            }
            modelAndView.addObject("hasSigned",hasSigned);
            modelAndView.addObject("signedDays",signedDays);
            modelAndView.addObject("signedReward",signedReward);

            String githubUrl = "https://github.com/login/oauth/authorize?client_id="+clientId+"&redirect_uri="+redirectUri+"&scope=user:email&state=1";
            modelAndView.addObject("githubUrl",githubUrl);

        }



    }

}

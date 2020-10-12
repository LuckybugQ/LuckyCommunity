package com.uestc.community.service;

import com.uestc.community.dao.UserMapper;
import com.uestc.community.entity.*;
import com.uestc.community.event.EventProducer;
import com.uestc.community.util.CommunityUtil;
import com.uestc.community.util.MailClient;
import com.uestc.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.uestc.community.util.CommunityConstant.*;

@Service
public class UserService {

    @Autowired
    private FollowService followService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

//    @Autowired
//    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;


    public User findUserById(int id) {
        //return userMapper.selectById(id);
        User user = getCache(id);
        if (user == null) {
            user = initCache(id);
        }
        return user;

    }

    public Map<String,Object> register(User user, HttpSession session){
        Map<String,Object> map = new HashMap<>();
        // 空值处理
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
//        if (StringUtils.isBlank(user.getUsername())) {
//            map.put("msg", "账号不能为空!");
//            return map;
//        }
//        if (StringUtils.isBlank(user.getPassword())) {
//            map.put("msg", "密码不能为空!");
//            return map;
//        }
//        if (StringUtils.isBlank(user.getEmail())) {
//            map.put("msg", "邮箱不能为空!");
//            return map;
//        }

        // 验证邮箱
        User u = userMapper.selectByEmail(user.getEmail());
        if (u != null && u.getStatus()!=0) {
            map.put("msg", "该邮箱已被注册!");
            return map;
        }
        boolean noActive = false;
        if(u != null && u.getStatus()==0){
            noActive = true;
        }
        // 验证账号
        User u2 = userMapper.selectByName(user.getUsername());
        if (u2 != null && !noActive) {
            map.put("msg", "该账号已存在!");
            return map;
        }
        if(noActive){
            userMapper.deleteById(u.getId());
        }

        if(u != null && u.getStatus()==0){
            userMapper.deleteById(u.getId());
        }

        // 注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(getRandomHeader());//随机数替代%d
        user.setCreateTime(new Date());
        user.setGender(0);
        user.setCity("成都");
        user.setSignature("该用户很懒，什么都没有写");
        user.setReward(100);
        userMapper.insertUser(user);
        session.setAttribute("activateUser",user);
        return map;
    }
    public void register(String username){
        User user = new User();
        // 注册用户
        user.setUsername(username);
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        //user.setPassword(CommunityUtil.md5("123456" + user.getSalt()));
        user.setType(0);
        user.setStatus(1);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(getRandomHeader());//随机数替代%d
        user.setCreateTime(new Date());
        user.setGender(0);
        user.setCity("成都");
        user.setSignature("该用户很懒，什么都没有写");
        user.setReward(0);
        userMapper.insertUser(user);
    }
    public void registerTest(String username,int gender,String city,String signature,int headerId,int type) throws ParseException {
        User user = new User();
        // 注册用户
        user.setUsername(username);
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5("153388" + user.getSalt()));
        user.setType(type);
        user.setStatus(1);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl("https://zzq-community-header.oss-cn-chengdu.aliyuncs.com/header/header-"+headerId+".png");//随机数替代%d
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        user.setCreateTime(sdf.parse("2020-01-01 00:00:00"));
        user.setGender(gender);
        user.setCity(city);
        user.setSignature(signature);
        user.setReward(new Random().nextInt(100)*10);
        userMapper.insertUser(user);
    }


    public String getRandomHeader(){
        int random = new Random().nextInt(24);
        return "https://zzq-community-header.oss-cn-chengdu.aliyuncs.com/header/header-"+random+".png";
    }

    public void register(GithubUser githubUser){
        Map<String,Object> map = new HashMap<>();
        // 验证邮箱
        User user = new User();
        // 注册用户
        String origin = githubUser.getLogin();
        String username = origin;
        int i = 0;
        while(findUserByName(username)!=null){
            username = origin + i;
            i++;
        }
        user.setUsername(username);
        user.setEmail(githubUser.getEmail());
        user.setHeaderUrl(githubUser.getAvatar_url());//随机数替代%d
        user.setSignature(githubUser.getBio()==null?"该用户很懒，什么都没有写":githubUser.getBio());
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setType(0);
        user.setStatus(1);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setCreateTime(new Date());
        user.setGender(0);
        user.setCity("成都");
        user.setReward(100);

        userMapper.insertUser(user);
    }




    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId, 1);
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAILURE;
        }
    }

    public Map<String, Object> login(String username, String password, long expiredSeconds) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(username)) {
            map.put("msg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("msg", "密码不能为空!");
            return map;
        }

        // 验证账号
        User user = userMapper.selectByName(username);
        if (user == null) {
            user = userMapper.selectByEmail(username);
            if (user == null) {
                map.put("msg", "该账号不存在!");
                return map;
            }
        }

        // 验证状态
        if (user.getStatus() == 0) {
            map.put("msg", "该账号未激活!");
            return map;
        }

        // 验证密码
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("msg", "密码不正确!");
            return map;
        }

        // 生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        //loginTicketMapper.insertLoginTicket(loginTicket);

        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey,loginTicket);

        map.put("ticket", loginTicket.getTicket());
        return map;
    }
    public Map<String, Object> login(User user){
        Map<String, Object> map = new HashMap<>();
        int expiredSeconds  = DEFAULT_EXPIRED_SECONDS;
        // 生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        //loginTicketMapper.insertLoginTicket(loginTicket);
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey,loginTicket);
        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    public void logout(String ticket) {

        //loginTicketMapper.updateStatus(ticket, 1);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey,loginTicket);
    }

    public LoginTicket findLoginTicket(String ticket) {
        //return loginTicketMapper.selectByTicket(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(RedisKeyUtil.getTicketKey(ticket));
    }

    public int updateHeader(int userId, String headerUrl) {
        //return userMapper.updateHeader(userId, headerUrl);
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }

    public int updatePassword(int userId, String password) {
        int rows = userMapper.updatePassword(userId,password);
        clearCache(userId);
        return rows;
    }

    public int updateReward(int userId, int reward) {
        int rows = userMapper.updateReward(userId,reward);
        clearCache(userId);
        return rows;
    }

    public int updateData(int userId, int gender,String city,String signature) {
        userMapper.updateGender(userId,gender);
        userMapper.updateCity(userId,city);
        userMapper.updateSignature(userId,signature);
        clearCache(userId);
        return 0;
    }


    public User findUserByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    public User findUserByName(String username) {
        return userMapper.selectByName(username);
    }

    public List<User> findAllUser() {
        return userMapper.selectAllUser();
    }

    // 1.优先从缓存中取值
    private User getCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    // 2.取不到时初始化缓存数据
    private User initCache(int userId) {
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }

    // 3.数据变更时清除缓存数据
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = this.findUserById(userId);

        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {

            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }

    public void newUserNotice(int userId){
        //小冰关注
        followService.follow(WELCOME_USER_ID,ENTITY_TYPE_USER,userId);
        // 触发关注事件
        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)
                .setUserId(WELCOME_USER_ID)
                .setEntityType(ENTITY_TYPE_USER)
                .setEntityId(userId)
                .setEntityUserId(userId);
        eventProducer.fireEvent(event);

        //小冰发送私信
        User target = findUserById(userId);
        Message message = new Message();
        message.setFromId(WELCOME_USER_ID);
        message.setToId(target.getId());
        String conversationId = "";
        if (message.getFromId() < message.getToId()) {
            conversationId = message.getFromId() + "_" + message.getToId();
        } else {
            conversationId = message.getToId() + "_" + message.getFromId();
        }
        message.setConversationId(conversationId);
        message.setContent("欢迎您加入Lucky社区！系统赠送了您100钻石，请享受知识带给您的乐趣吧！");
        message.setCreateTime(new Date());
        messageService.addMessage(message);
    }

    public void newVisitorNotice(int userId){
        //小冰关注
        followService.follow(WELCOME_USER_ID,ENTITY_TYPE_USER,userId);
        // 触发关注事件
        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)
                .setUserId(WELCOME_USER_ID)
                .setEntityType(ENTITY_TYPE_USER)
                .setEntityId(userId)
                .setEntityUserId(userId);
        eventProducer.fireEvent(event);

        //小冰发送私信
        User target = findUserById(userId);
        Message message = new Message();
        message.setFromId(WELCOME_USER_ID);
        message.setToId(target.getId());
        String conversationId = "";
        if (message.getFromId() < message.getToId()) {
            conversationId = message.getFromId() + "_" + message.getToId();
        } else {
            conversationId = message.getToId() + "_" + message.getFromId();
        }
        message.setConversationId(conversationId);
        message.setContent("欢迎您加入Lucky社区！游客身份没有钻石奖励，建议使用邮箱注册哦！");
        message.setCreateTime(new Date());
        messageService.addMessage(message);
    }
}


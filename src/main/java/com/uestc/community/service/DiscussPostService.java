package com.uestc.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.uestc.community.dao.DiscussPostMapper;
import com.uestc.community.entity.DiscussPost;
import com.uestc.community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    // Caffeine核心接口: Cache, LoadingCache, AsyncLoadingCache

    // 帖子列表缓存
    private LoadingCache<String, List<DiscussPost>> postListCache;

    // 帖子总数缓存
    private LoadingCache<String, Integer> postRowsCache;


    @PostConstruct
    public void init() {
        // 初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        if (key == null || key.length() == 0) {
                            throw new IllegalArgumentException("参数错误!");
                        }

                        String[] params = key.split(":");
                        if (params == null || params.length != 2) {
                            throw new IllegalArgumentException("参数错误!");
                        }

                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        // 二级缓存: Redis -> mysql

                        logger.debug("load post list from DB.");
                        return discussPostMapper.selectDiscussPostsByType(offset, limit, 1,-1,0);
                    }
                });
        // 初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String,Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull String key) throws Exception {
                        String[] params = key.split(":");
                        if (params == null || params.length != 2) {
                            throw new IllegalArgumentException("参数错误!");
                        }
                        int type = Integer.valueOf(params[0]);
                        int filter = Integer.valueOf(params[1]);
                        logger.debug("load post rows from DB.");
                        return discussPostMapper.selectDiscussPostRowsByType(type,filter);
                    }
                });
    }

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
        logger.debug("load post list from DB.");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
    }

    public List<DiscussPost> findDiscussPostsByType(int offset, int limit, int orderMode, int type ,int filter) {
        if (orderMode == 1 && type == -1 && filter == 0) {
            return postListCache.get(offset + ":" + limit);
        }
        logger.debug("load post list from DB.");
        return discussPostMapper.selectDiscussPostsByType(offset, limit, orderMode ,type, filter);
    }

    public List<DiscussPost> findRecommendDiscussPosts(){
        int total = findDiscussPostRowsByType(-1,0) ;
        int offset = new Random().nextInt(total>5?total-5:1);
        return findDiscussPostsByType(offset, 5, 1, -1, 0);
    }

    public int findDiscussPostRows(int userId) {
        logger.debug("load post rows from DB.");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    public int findDiscussPostRowsByType(int type,int filter) {
        return postRowsCache.get(type + ":" + filter);
        //return discussPostMapper.selectDiscussPostRowsByType(type,filter);
    }


    public List<DiscussPost> findDiscussPostsByKeyword(String keyword,int offset,int limit) {
        return discussPostMapper.selectDiscussPostsByKeyword(keyword,offset,limit);
    }
    public int findDiscussPostRowsByKeyword(String keyword) {
        return discussPostMapper.selectDiscussPostRowsByKeyword(keyword);
    }



    public int addDiscussPost(DiscussPost post) {
        if (post == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        // 转义HTML标记
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        // 过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        return discussPostMapper.insertDiscussPost(post);
    }

    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    public int updateCommentCount(int id, int commentCount) {
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }

    public int updateStatus(int id, int status) {
        return discussPostMapper.updateStatus(id, status);
    }

    public int updateScore(int id, double score) {
        return discussPostMapper.updateScore(id, score);
    }

    public int updateTitle(int id, String title) {
        return discussPostMapper.updateTitle(id, title);
    }

    public int updateContent(int id, String content) {
        return discussPostMapper.updateContent(id, content);
    }

    public int updateAcceptId(int id, int acceptId) {
        return discussPostMapper.updateAcceptId(id, acceptId);
    }

    public int updateUpdateTime(int id, Date date) {
        return discussPostMapper.updateUpdateTime(id, date);
    }
}


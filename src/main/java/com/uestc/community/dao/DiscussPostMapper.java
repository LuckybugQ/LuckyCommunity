package com.uestc.community.dao;

import com.uestc.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface DiscussPostMapper {

    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit, int orderMode);

    List<DiscussPost> selectDiscussPostsByType(int offset, int limit, int orderMode,int type ,int filter);

    List<DiscussPost> selectDiscussPostsByKeyword(String keyword,int offset, int limit);

    // @Param注解用于给参数取别名,
    // 如果只有一个参数,并且在<if>里使用,则必须加别名.
    int selectDiscussPostRows(@Param("userId") int userId);

    int selectDiscussPostRowsByKeyword(String keyword);

    int selectDiscussPostRowsByType(int type,int filter);

    int insertDiscussPost(DiscussPost discussPost);

    DiscussPost selectDiscussPostById(int id);

    int updateCommentCount(int id, int commentCount);

    int updateType(int id, int type);

    int updateStatus(int id, int status);

    int updateScore(int id, double score);

    int updateTitle(int id, String title);

    int updateContent(int id, String content);

    int updateAcceptId(int id, int acceptId);

    int updateUpdateTime(int id, Date updateTime);

}


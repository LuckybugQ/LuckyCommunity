package com.uestc.community.dao;

import com.uestc.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {

    List<Comment> selectCommentsByEntity(int entityType, int entityId, int offset, int limit);

    int selectCountByEntity(int entityType, int entityId);

    int insertComment(Comment comment);

    Comment selectCommentById(int id);

    int selectCommentsRows(int userId);

    int selectPostCommentsRows(int userId);

    List<Comment> selectComments(int userId, int offset, int limit);

    List<Comment> selectPostComments(int userId, int offset, int limit);

    int deleteCommentById(int id);

}

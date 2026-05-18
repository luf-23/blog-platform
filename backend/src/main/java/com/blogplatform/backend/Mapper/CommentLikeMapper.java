package com.blogplatform.backend.Mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import com.blogplatform.backend.entity.CommentLike;

@Mapper
public interface CommentLikeMapper {

    @Select("SELECT COUNT(*) FROM comment_like_record WHERE comment_id = #{commentId}")
    Integer count(Integer commentId);

    @Select("SELECT * FROM comment_like_record WHERE comment_id = #{commentId} AND user_id = #{userId}")
    CommentLike selectByCommentIdAndUserId(Integer commentId, Integer userId);

    @Insert("INSERT INTO comment_like_record (comment_id, user_id) VALUES (#{commentId}, #{userId})")
    void add(Integer commentId, Integer userId);

    @Delete("DELETE FROM comment_like_record WHERE comment_id = #{commentId} AND user_id = #{userId}")
    void delete(Integer commentId, Integer userId);
}

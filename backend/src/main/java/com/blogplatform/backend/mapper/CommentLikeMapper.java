package com.blogplatform.backend.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.blogplatform.backend.entity.CommentLike;
import com.blogplatform.backend.entity.CommentLikeCount;

import java.util.List;

@Mapper
public interface CommentLikeMapper {

    @Select("SELECT COUNT(*) FROM comment_like_record WHERE comment_id = #{commentId}")
    Integer count(Integer commentId);

    @Select("<script>SELECT comment_id AS commentId, COUNT(*) AS likeCount FROM comment_like_record " +
            "WHERE comment_id IN " +
            "<foreach collection='commentIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY comment_id</script>")
    List<CommentLikeCount> countByCommentIds(@Param("commentIds") List<Integer> commentIds);

    @Select("<script>SELECT comment_id FROM comment_like_record " +
            "WHERE user_id = #{userId} AND comment_id IN " +
            "<foreach collection='commentIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Integer> selectLikedCommentIds(@Param("userId") Integer userId,
                                        @Param("commentIds") List<Integer> commentIds);

    @Select("SELECT * FROM comment_like_record WHERE comment_id = #{commentId} AND user_id = #{userId}")
    CommentLike selectByCommentIdAndUserId(Integer commentId, Integer userId);

    @Insert("INSERT INTO comment_like_record (comment_id, user_id) VALUES (#{commentId}, #{userId})")
    void add(Integer commentId, Integer userId);

    @Delete("DELETE FROM comment_like_record WHERE comment_id = #{commentId} AND user_id = #{userId}")
    void delete(Integer commentId, Integer userId);
}

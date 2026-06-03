package com.blogplatform.backend.mapper;

import com.blogplatform.backend.entity.Comment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentMapper {

    @Select("SELECT * FROM comment WHERE article_id = #{articleId} AND parent_id IS NULL AND status = 1 " +
            "ORDER BY create_time DESC LIMIT #{offset}, #{pageSize}")
    List<Comment> selectRootList(@Param("articleId") Integer articleId,
                                  @Param("offset") Integer offset,
                                  @Param("pageSize") Integer pageSize);

    @Select("SELECT COUNT(*) FROM comment WHERE article_id = #{articleId} AND parent_id IS NULL AND status = 1")
    int countRoots(Integer articleId);

    @Select("SELECT COUNT(*) FROM comment WHERE article_id = #{articleId} AND status = 1")
    int countAll(Integer articleId);

    @Select("<script>" +
            "SELECT * FROM comment WHERE article_id = #{articleId} AND status = 1 AND root_id IN " +
            "<foreach collection='rootIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND parent_id IS NOT NULL " +
            "ORDER BY create_time ASC" +
            "</script>")
    List<Comment> selectRepliesByRootIds(@Param("articleId") Integer articleId,
                                          @Param("rootIds") List<Integer> rootIds);

    @Select("SELECT * FROM comment WHERE article_id = #{articleId} AND root_id = #{rootId} " +
            "AND parent_id IS NOT NULL AND status = 1 " +
            "ORDER BY create_time ASC LIMIT #{offset}, #{pageSize}")
    List<Comment> selectRepliesByRoot(@Param("articleId") Integer articleId,
                                       @Param("rootId") Integer rootId,
                                       @Param("offset") Integer offset,
                                       @Param("pageSize") Integer pageSize);

    @Select("SELECT COUNT(*) FROM comment WHERE article_id = #{articleId} AND root_id = #{rootId} " +
            "AND parent_id IS NOT NULL AND status = 1")
    int countRepliesByRoot(@Param("articleId") Integer articleId, @Param("rootId") Integer rootId);

    @Insert("INSERT INTO comment (article_id, user_id, parent_id, root_id, reply_to_user_id, content) " +
            "VALUES (#{articleId}, #{userId}, #{parentId}, #{rootId}, #{replyToUserId}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "commentId")
    void insert(Comment comment);

    @Update("UPDATE comment SET root_id = #{commentId} WHERE comment_id = #{commentId}")
    void setRootIdToSelf(Integer commentId);

    @Select("SELECT * FROM comment WHERE comment_id = #{commentId}")
    Comment selectById(Integer commentId);

    @Update("UPDATE comment SET status = 0 WHERE comment_id = #{commentId}")
    void softDelete(Integer commentId);

    @Delete("DELETE FROM comment WHERE comment_id = #{commentId}")
    void delete(Integer commentId);

    @Select("SELECT COUNT(*) FROM comment")
    int totalCount();
}

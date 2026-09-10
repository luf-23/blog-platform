package com.blogplatform.backend.mapper;

import com.blogplatform.backend.entity.Comment;
import com.blogplatform.backend.entity.CommentReplyCount;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentMapper {

    @Select("SELECT * FROM comment WHERE article_id = #{articleId} AND parent_id IS NULL AND status = 1 " +
            "ORDER BY create_time DESC, comment_id DESC LIMIT #{offset}, #{pageSize}")
    List<Comment> selectRootList(@Param("articleId") Integer articleId,
                                @Param("offset") Integer offset,
                                @Param("pageSize") Integer pageSize);

    @Select("SELECT COUNT(*) FROM comment WHERE article_id = #{articleId} AND parent_id IS NULL AND status = 1")
    int countRoots(Integer articleId);

    @Select("SELECT COUNT(*) FROM comment c WHERE c.article_id = #{articleId} AND c.status = 1 " +
            "AND (c.parent_id IS NULL OR EXISTS (SELECT 1 FROM comment root " +
            "WHERE root.comment_id = c.root_comment_id AND root.article_id = c.article_id " +
            "AND root.parent_id IS NULL AND root.status = 1))")
    int countAll(Integer articleId);

    @Select("<script>" +
            "SELECT root_comment_id AS rootId, COUNT(*) AS replyCount FROM comment " +
            "WHERE article_id = #{articleId} AND status = 1 AND root_comment_id IN " +
            "<foreach collection='rootIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY root_comment_id" +
            "</script>")
    List<CommentReplyCount> countRepliesByRootIds(@Param("articleId") Integer articleId,
                                                @Param("rootIds") List<Integer> rootIds);

    @Select("SELECT * FROM comment WHERE article_id = #{articleId} " +
            "AND root_comment_id = #{rootId} AND status = 1 " +
            "ORDER BY create_time DESC, comment_id DESC LIMIT #{offset}, #{pageSize}")
    List<Comment> selectRepliesByRoot(@Param("articleId") Integer articleId,
                                     @Param("rootId") Integer rootId,
                                     @Param("offset") Integer offset,
                                     @Param("pageSize") Integer pageSize);

    @Select("SELECT COUNT(*) FROM comment WHERE article_id = #{articleId} " +
            "AND root_comment_id = #{rootId} AND status = 1")
    int countRepliesByRoot(@Param("articleId") Integer articleId, @Param("rootId") Integer rootId);

    @Insert("INSERT INTO comment (article_id, user_id, parent_id, root_comment_id, reply_to_user_id, content) " +
            "VALUES (#{articleId}, #{userId}, #{parentId}, #{rootCommentId}, #{replyToUserId}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "commentId")
    void insert(Comment comment);

    @Select("SELECT * FROM comment WHERE comment_id = #{commentId}")
    Comment selectById(Integer commentId);

    @Update("UPDATE comment SET status = 0 WHERE comment_id = #{commentId}")
    void softDelete(Integer commentId);

    @Delete("DELETE FROM comment WHERE comment_id = #{commentId}")
    void delete(Integer commentId);

    @Select("SELECT COUNT(*) FROM comment")
    int totalCount();
}

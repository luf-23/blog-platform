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
            "WITH RECURSIVE comment_tree AS (" +
            "SELECT c.*, c.comment_id AS resolved_root_id FROM comment c " +
            "WHERE c.article_id = #{articleId} AND c.status = 1 AND c.comment_id IN " +
            "<foreach collection='rootIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "UNION ALL " +
            "SELECT child.*, tree.resolved_root_id FROM comment child " +
            "JOIN comment_tree tree ON child.parent_id = tree.comment_id " +
            "WHERE child.article_id = #{articleId} AND child.status = 1" +
            ") " +
            "SELECT comment_id, article_id, user_id, parent_id, reply_to_user_id, content, " +
            "like_count, status, create_time, resolved_root_id AS root_id " +
            "FROM comment_tree WHERE comment_id != resolved_root_id ORDER BY create_time ASC" +
            "</script>")
    List<Comment> selectRepliesByRootIds(@Param("articleId") Integer articleId,
                                          @Param("rootIds") List<Integer> rootIds);

    @Select("WITH RECURSIVE comment_tree AS (" +
            "SELECT c.*, c.comment_id AS resolved_root_id FROM comment c " +
            "WHERE c.article_id = #{articleId} AND c.comment_id = #{rootId} AND c.status = 1 " +
            "UNION ALL " +
            "SELECT child.*, tree.resolved_root_id FROM comment child " +
            "JOIN comment_tree tree ON child.parent_id = tree.comment_id " +
            "WHERE child.article_id = #{articleId} AND child.status = 1" +
            ") " +
            "SELECT comment_id, article_id, user_id, parent_id, reply_to_user_id, content, " +
            "like_count, status, create_time, resolved_root_id AS root_id " +
            "FROM comment_tree WHERE comment_id != resolved_root_id " +
            "ORDER BY create_time ASC LIMIT #{offset}, #{pageSize}")
    List<Comment> selectRepliesByRoot(@Param("articleId") Integer articleId,
                                       @Param("rootId") Integer rootId,
                                       @Param("offset") Integer offset,
                                       @Param("pageSize") Integer pageSize);

    @Select("WITH RECURSIVE comment_tree AS (" +
            "SELECT comment_id FROM comment WHERE article_id = #{articleId} AND comment_id = #{rootId} AND status = 1 " +
            "UNION ALL " +
            "SELECT child.comment_id FROM comment child JOIN comment_tree tree ON child.parent_id = tree.comment_id " +
            "WHERE child.article_id = #{articleId} AND child.status = 1" +
            ") SELECT GREATEST(COUNT(*) - 1, 0) FROM comment_tree")
    int countRepliesByRoot(@Param("articleId") Integer articleId, @Param("rootId") Integer rootId);

    @Insert("INSERT INTO comment (article_id, user_id, parent_id, reply_to_user_id, content) " +
            "VALUES (#{articleId}, #{userId}, #{parentId}, #{replyToUserId}, #{content})")
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

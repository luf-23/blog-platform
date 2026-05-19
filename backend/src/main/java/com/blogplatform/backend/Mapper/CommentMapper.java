package com.blogplatform.backend.Mapper;

import com.blogplatform.backend.entity.Comment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentMapper {

    @Select("SELECT * FROM comment WHERE article_id = #{articleId} AND parent_id IS NULL " +
            "ORDER BY create_time DESC LIMIT #{offset}, #{pageSize}")
    List<Comment> selectRootList(Integer articleId, Integer offset, Integer pageSize);

    @Select("SELECT COUNT(*) FROM comment WHERE article_id = #{articleId} AND parent_id IS NULL")
    Integer countRoots(Integer articleId);

    @Select("SELECT COUNT(*) FROM comment WHERE article_id = #{articleId}")
    Integer countAll(Integer articleId);

    @Select("<script>" +
            "SELECT * FROM comment WHERE article_id = #{articleId} AND root_id IN " +
            "<foreach collection='rootIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "ORDER BY create_time ASC" +
            "</script>")
    List<Comment> selectRepliesByRootIds(@Param("articleId") Integer articleId,
                                           @Param("rootIds") List<Integer> rootIds);

    @Insert("INSERT INTO comment(article_id, user_id, parent_id, root_id, content) " +
            "VALUES(#{articleId}, #{userId}, #{parentId}, #{rootId}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "commentId")
    void insert(Comment comment);

    @Update("UPDATE comment SET root_id = #{commentId} WHERE comment_id = #{commentId}")
    void setRootIdToSelf(Integer commentId);

    @Select("SELECT * FROM comment WHERE comment_id = #{commentId}")
    Comment selectById(Integer commentId);

    @Delete("DELETE FROM comment WHERE comment_id = #{commentId}")
    void delete(Integer commentId);
}

package com.blogplatform.backend.Mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import com.blogplatform.backend.entity.Comment;

import java.util.List;

@Mapper
public interface CommentMapper {

    @Select("select * from comment where article_id = #{articleId} limit #{offset} ,#{pageSize}")
    List<Comment> selectList(Integer articleId, Integer offset, Integer pageSize);

    @Select("select count(*) from comment where article_id = #{articleId}")
    Integer count(Integer articleId);

    @Insert("insert into comment(article_id, user_id, content) values(#{articleId}, #{userId}, #{content})")
    void add(Integer articleId, Integer userId, String content);

    @Select("select * from comment where comment_id = #{commentId}")
    Comment selectById(Integer commentId);

    @Delete("delete from comment where comment_id = #{commentId}")
    void delete(Integer commentId);
}

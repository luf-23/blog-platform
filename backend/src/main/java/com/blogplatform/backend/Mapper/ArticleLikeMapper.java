package com.blogplatform.backend.Mapper;

import com.blogplatform.backend.entity.ArticleLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArticleLikeMapper {

    @Select("SELECT COUNT(*) FROM article_like_record WHERE article_id = #{articleId}")
    Integer count(Integer articleId);

    @Select("SELECT * FROM article_like_record WHERE article_id = #{articleId} AND user_id = #{userId}")
    ArticleLike selectByArticleIdAndUserId(Integer articleId, Integer userId);

    @Insert("INSERT INTO article_like_record (article_id, user_id) VALUES (#{articleId}, #{userId})")
    void add(Integer articleId, Integer userId);

    @Delete("DELETE FROM article_like_record WHERE article_id = #{articleId} AND user_id = #{userId}")
    void delete(Integer articleId, Integer userId);
}

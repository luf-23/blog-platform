package com.blogplatform.backend.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ArticleLikeMapper {

    @Select("SELECT COUNT(*) FROM article_like_record WHERE article_id = #{articleId}")
    int count(Integer articleId);

    @Select("SELECT COUNT(*) FROM article_like_record WHERE article_id = #{articleId} AND user_id = #{userId}")
    int exists(@Param("articleId") Integer articleId, @Param("userId") Integer userId);

    @Insert("INSERT IGNORE INTO article_like_record (article_id, user_id) VALUES (#{articleId}, #{userId})")
    int add(@Param("articleId") Integer articleId, @Param("userId") Integer userId);

    @Delete("DELETE FROM article_like_record WHERE article_id = #{articleId} AND user_id = #{userId}")
    int delete(@Param("articleId") Integer articleId, @Param("userId") Integer userId);

    @Select("<script>" +
            "SELECT article_id FROM article_like_record WHERE user_id = #{userId} AND article_id IN " +
            "<foreach collection='articleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Integer> selectLikedArticleIds(@Param("userId") Integer userId,
                                         @Param("articleIds") List<Integer> articleIds);
}

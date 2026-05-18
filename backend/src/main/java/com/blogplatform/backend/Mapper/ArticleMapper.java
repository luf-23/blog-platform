package com.blogplatform.backend.Mapper;

import org.apache.ibatis.annotations.*;
import com.blogplatform.backend.entity.Article;

import java.util.List;

@Mapper
public interface ArticleMapper {

    @Select("select * from article where status = 'published' and category_id = #{categoryId}")
    List<Article> selectPublishedListByCategoryId(Integer categoryId);

    @Select("select * from article where status = 'draft' and category_id = #{categoryId}")
    List<Article> selectDraftListByCategoryId(Integer categoryId);

    @Select("select * from article where category_id = #{categoryId}")
    List<Article> selectListByCategoryId(Integer categoryId);

    @Delete("delete from article where article_id = #{articleId}")
    void deleteById(Integer articleId);

    @Select("<script>" +
            "select * from article " +
            "where title like concat('%', #{title}, '%') " +
            "and status in " +
            "<foreach collection='statusList' item='status' open='(' separator=',' close=')'>" +
            "#{status}" +
            "</foreach>" +
            "and category_id = #{categoryId}" +
            "</script>")
    List<Article> selectListByCase(String title, List<String> statusList, Integer categoryId);

    @Select("select * from article where article_id = #{articleId}")
    Article selectById(Integer articleId);

    @Insert("insert into article (category_id, title, content, status, create_time, update_time) values (#{categoryId},#{title},#{content},#{status},now(),now())")
    void add(Article article);

    @Update("update article set status = 'pending' where article_id = #{articleId}")
    void publishById(Integer articleId);

    @Update("update article set title = #{title}, content = #{content}, status = #{status}, update_time = now() where article_id = #{articleId}")
    void update(Article article);

    @Select("SELECT * FROM article where status='published'")
    List<Article> selectArticles();

    @Select("SELECT * FROM article where status='published' and title like concat('%', #{title}, '%') and content like concat('%', #{content}, '%')")
    List<Article> selectArticlesByTitleAndContent(String title, String content);

    @Update("update article set cover_image = #{coverImage} where article_id = #{articleId}")
    void uploadCoverImage(Integer articleId, String coverImage);
}

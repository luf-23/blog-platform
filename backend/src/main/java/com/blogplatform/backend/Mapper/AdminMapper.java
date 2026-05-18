package com.blogplatform.backend.Mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import com.blogplatform.backend.entity.Article;
import com.blogplatform.backend.entity.User;

import java.util.List;

@Mapper
public interface AdminMapper {
    @Select("SELECT * FROM article where status='pending'")
    List<Article> selectAllPendingArticles();

    @Select("SELECT * FROM article where status='published'")
    List<Article> selectAllPublishedArticles();

    @Select("UPDATE article SET status=#{status} WHERE article_id=#{id}")
    void updateArticleStatus(int id, String status);

    @Select("select * from user")
    List<User> selectAllUser();
}

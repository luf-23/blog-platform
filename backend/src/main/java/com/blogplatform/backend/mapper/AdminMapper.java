package com.blogplatform.backend.mapper;

import com.blogplatform.backend.entity.Article;
import com.blogplatform.backend.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdminMapper {

    @Select("SELECT * FROM article WHERE status='pending' ORDER BY create_time DESC")
    List<Article> selectAllPendingArticles();

    @Select("SELECT * FROM article WHERE status='published' ORDER BY create_time DESC")
    List<Article> selectAllPublishedArticles();

    @Update("UPDATE article SET status=#{status} WHERE article_id=#{id}")
    void updateArticleStatus(@Param("id") int id, @Param("status") String status);

    @Select("SELECT * FROM user ORDER BY create_time DESC")
    List<User> selectAllUser();
}

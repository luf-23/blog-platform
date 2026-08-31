package com.blogplatform.backend.mapper;

import com.blogplatform.backend.entity.Article;
import com.blogplatform.backend.entity.ArticleVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ArticleMapper {

    // ── Public discovery queries ──────────────────────────────────────────────

    @Select("<script>" +
            "SELECT a.*, u.username AS author_username, u.nickname AS author_nickname, u.avatar_image AS author_avatar, " +
            "c.category_name " +
            "FROM article a " +
            "LEFT JOIN user u ON a.user_id = u.user_id " +
            "LEFT JOIN category c ON a.category_id = c.category_id " +
            "WHERE a.status = 'published' " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (a.title LIKE CONCAT('%', #{keyword}, '%') OR a.summary LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='categoryId != null'>AND a.category_id = #{categoryId} </if>" +
            "<if test='authorId != null'>AND a.user_id = #{authorId} </if>" +
            "<if test='tagId != null'>" +
            "AND a.article_id IN (SELECT article_id FROM article_tag WHERE tag_id = #{tagId}) " +
            "</if>" +
            "<choose>" +
            "<when test='sort == \"hot\"'>ORDER BY a.view_count DESC </when>" +
            "<when test='sort == \"liked\"'>ORDER BY a.like_count DESC </when>" +
            "<otherwise>ORDER BY a.create_time DESC </otherwise>" +
            "</choose>" +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<ArticleVO> searchPublished(@Param("keyword") String keyword,
                                    @Param("categoryId") Integer categoryId,
                                    @Param("tagId") Integer tagId,
                                    @Param("authorId") Integer authorId,
                                    @Param("sort") String sort,
                                    @Param("offset") int offset,
                                    @Param("pageSize") int pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) FROM article a " +
            "WHERE a.status = 'published' " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (a.title LIKE CONCAT('%', #{keyword}, '%') OR a.summary LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='categoryId != null'>AND a.category_id = #{categoryId} </if>" +
            "<if test='authorId != null'>AND a.user_id = #{authorId} </if>" +
            "<if test='tagId != null'>" +
            "AND a.article_id IN (SELECT article_id FROM article_tag WHERE tag_id = #{tagId}) " +
            "</if>" +
            "</script>")
    int countPublished(@Param("keyword") String keyword,
                       @Param("categoryId") Integer categoryId,
                       @Param("tagId") Integer tagId,
                       @Param("authorId") Integer authorId);

    @Select("SELECT a.*, u.username AS author_username, u.nickname AS author_nickname, u.avatar_image AS author_avatar, " +
            "c.category_name " +
            "FROM article a " +
            "LEFT JOIN user u ON a.user_id = u.user_id " +
            "LEFT JOIN category c ON a.category_id = c.category_id " +
            "WHERE a.article_id = #{articleId}")
    ArticleVO selectVOById(Integer articleId);

    // ── Owner queries (my blog) ───────────────────────────────────────────────

    @Select("SELECT * FROM article WHERE user_id = #{userId} AND status = 'published' ORDER BY create_time DESC")
    List<Article> selectPublishedByUserId(Integer userId);

    @Select("SELECT * FROM article WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Article> selectAllByUserId(Integer userId);

    @Select("SELECT * FROM article WHERE category_id = #{categoryId} ORDER BY create_time DESC")
    List<Article> selectByCategoryId(Integer categoryId);

    @Select("<script>" +
            "SELECT * FROM article WHERE user_id = #{userId} " +
            "<if test='title != null and title != \"\"'>AND title LIKE CONCAT('%', #{title}, '%') </if>" +
            "<if test='status != null and status != \"\"'>AND status = #{status} </if>" +
            "<if test='categoryId != null'>AND category_id = #{categoryId} </if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    List<Article> selectByCondition(@Param("userId") Integer userId,
                                    @Param("title") String title,
                                    @Param("status") String status,
                                    @Param("categoryId") Integer categoryId);

    @Select("<script>" +
            "SELECT a.*, c.category_name FROM article a " +
            "LEFT JOIN category c ON c.category_id = a.category_id " +
            "WHERE a.user_id = #{userId} " +
            "<if test='title != null and title != \"\"'>AND (a.title LIKE CONCAT('%', #{title}, '%') OR a.summary LIKE CONCAT('%', #{title}, '%')) </if>" +
            "<if test='status != null and status != \"\"'>AND a.status = #{status} </if>" +
            "<if test='categoryId != null'>AND a.category_id = #{categoryId} </if>" +
            "ORDER BY a.update_time DESC" +
            "</script>")
    List<ArticleVO> selectMyByCondition(@Param("userId") Integer userId,
                                        @Param("title") String title,
                                        @Param("status") String status,
                                        @Param("categoryId") Integer categoryId);

    @Select("SELECT * FROM article WHERE article_id = #{articleId}")
    Article selectById(Integer articleId);

    // ── Write operations ──────────────────────────────────────────────────────

    @Insert("<script>" +
            "INSERT INTO article (user_id, category_id, title, summary, content, status" +
            "<if test='coverImage != null'>, cover_image</if>" +
            ") VALUES (#{userId}, #{categoryId}, #{title}, #{summary}, #{content}, #{status}" +
            "<if test='coverImage != null'>, #{coverImage}</if>" +
            ")" +
            "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "articleId")
    void insert(Article article);

    @Update("UPDATE article SET category_id=#{categoryId}, title=#{title}, summary=#{summary}, " +
            "content=#{content}, cover_image=#{coverImage}, status=#{status}, update_time=NOW() " +
            "WHERE article_id=#{articleId}")
    void update(Article article);

    @Update("UPDATE article SET status='pending', update_time=NOW() WHERE article_id=#{articleId}")
    void submitForReview(Integer articleId);

    @Update("UPDATE article SET status='published', update_time=NOW() WHERE article_id=#{articleId}")
    void approveArticle(Integer articleId);

    @Update("UPDATE article SET status='draft', update_time=NOW() WHERE article_id=#{articleId}")
    void rejectArticle(Integer articleId);

    @Update("UPDATE article SET cover_image=#{coverImage}, update_time=NOW() WHERE article_id=#{articleId}")
    void updateCoverImage(@Param("articleId") Integer articleId, @Param("coverImage") String coverImage);

    @Update("UPDATE article SET view_count = view_count + 1 WHERE article_id=#{articleId}")
    void incrementViewCount(Integer articleId);

    @Update("UPDATE article SET like_count = like_count + 1 WHERE article_id=#{articleId}")
    void incrementLikeCount(Integer articleId);

    @Update("UPDATE article SET like_count = GREATEST(like_count - 1, 0) WHERE article_id=#{articleId}")
    void decrementLikeCount(Integer articleId);

    @Update("UPDATE article SET comment_count = comment_count + 1 WHERE article_id=#{articleId}")
    void incrementCommentCount(Integer articleId);

    @Update("UPDATE article SET comment_count = GREATEST(comment_count - 1, 0) WHERE article_id=#{articleId}")
    void decrementCommentCount(Integer articleId);

    @Delete("DELETE FROM article WHERE article_id=#{articleId}")
    void deleteById(Integer articleId);

    // ── Admin queries ─────────────────────────────────────────────────────────

    @Select("<script>" +
            "SELECT a.*, u.username AS author_username, u.nickname AS author_nickname, u.avatar_image AS author_avatar, " +
            "c.category_name " +
            "FROM article a " +
            "LEFT JOIN user u ON a.user_id = u.user_id " +
            "LEFT JOIN category c ON a.category_id = c.category_id " +
            "<where>" +
            "<if test='status != null and status != \"\"'>AND a.status = #{status} </if>" +
            "<if test='keyword != null and keyword != \"\"'>AND a.title LIKE CONCAT('%', #{keyword}, '%') </if>" +
            "</where>" +
            "ORDER BY a.create_time DESC " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<ArticleVO> adminSearch(@Param("status") String status,
                                @Param("keyword") String keyword,
                                @Param("offset") int offset,
                                @Param("pageSize") int pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) FROM article a " +
            "<where>" +
            "<if test='status != null and status != \"\"'>AND a.status = #{status} </if>" +
            "<if test='keyword != null and keyword != \"\"'>AND a.title LIKE CONCAT('%', #{keyword}, '%') </if>" +
            "</where>" +
            "</script>")
    int adminCount(@Param("status") String status, @Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM article")
    int totalCount();

    @Select("SELECT COUNT(*) FROM article WHERE status='published'")
    int publishedCount();

    @Select("SELECT COUNT(*) FROM article WHERE status='pending'")
    int pendingCount();
}

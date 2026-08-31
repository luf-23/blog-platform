package com.blogplatform.backend.mapper;

import com.blogplatform.backend.entity.ArticleVO;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface CommunityMapper {

    @Select("<script>" +
            "SELECT a.*, u.username AS author_username, u.nickname AS author_nickname, " +
            "u.avatar_image AS author_avatar, c.category_name " +
            "FROM article a " +
            "JOIN user u ON a.user_id = u.user_id " +
            "LEFT JOIN category c ON a.category_id = c.category_id " +
            "WHERE a.status = 'published' " +
            "<if test='followerId != null'>" +
            "AND EXISTS (SELECT 1 FROM user_follow f WHERE f.follower_id = #{followerId} AND f.following_id = a.user_id) " +
            "</if>" +
            "<if test='tagId != null'>" +
            "AND EXISTS (SELECT 1 FROM article_tag at WHERE at.article_id = a.article_id AND at.tag_id = #{tagId}) " +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (a.title LIKE CONCAT('%', #{keyword}, '%') OR a.summary LIKE CONCAT('%', #{keyword}, '%') " +
            "OR u.username LIKE CONCAT('%', #{keyword}, '%') OR u.nickname LIKE CONCAT('%', #{keyword}, '%') " +
            "OR EXISTS (SELECT 1 FROM article_tag at2 JOIN tag t2 ON t2.tag_id = at2.tag_id " +
            "WHERE at2.article_id = a.article_id AND t2.tag_name LIKE CONCAT('%', #{keyword}, '%'))) " +
            "</if>" +
            "<choose>" +
            "<when test='sort == \"hot\"'>ORDER BY (a.like_count * 4 + a.comment_count * 3 + a.view_count / 20) DESC, a.create_time DESC </when>" +
            "<otherwise>ORDER BY a.create_time DESC </otherwise>" +
            "</choose>" +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<ArticleVO> selectFeed(@Param("sort") String sort,
                               @Param("tagId") Integer tagId,
                               @Param("keyword") String keyword,
                               @Param("followerId") Integer followerId,
                               @Param("offset") int offset,
                               @Param("pageSize") int pageSize);

    @Select("<script>SELECT COUNT(*) FROM article a JOIN user u ON u.user_id = a.user_id WHERE a.status = 'published' " +
            "<if test='followerId != null'>" +
            "AND EXISTS (SELECT 1 FROM user_follow f WHERE f.follower_id = #{followerId} AND f.following_id = a.user_id) " +
            "</if>" +
            "<if test='tagId != null'>" +
            "AND EXISTS (SELECT 1 FROM article_tag at WHERE at.article_id = a.article_id AND at.tag_id = #{tagId}) " +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (a.title LIKE CONCAT('%', #{keyword}, '%') OR a.summary LIKE CONCAT('%', #{keyword}, '%') " +
            "OR u.username LIKE CONCAT('%', #{keyword}, '%') OR u.nickname LIKE CONCAT('%', #{keyword}, '%') " +
            "OR EXISTS (SELECT 1 FROM article_tag at2 JOIN tag t2 ON t2.tag_id = at2.tag_id " +
            "WHERE at2.article_id = a.article_id AND t2.tag_name LIKE CONCAT('%', #{keyword}, '%'))) " +
            "</if></script>")
    int countFeed(@Param("tagId") Integer tagId,
                  @Param("keyword") String keyword,
                  @Param("followerId") Integer followerId);

    @Select("SELECT t.tag_id AS tagId, t.tag_name AS tagName, COUNT(at.article_id) AS articleCount, " +
            "COALESCE(SUM(a.view_count + a.like_count * 8 + a.comment_count * 12), 0) AS activity " +
            "FROM tag t JOIN article_tag at ON at.tag_id = t.tag_id " +
            "JOIN article a ON a.article_id = at.article_id AND a.status = 'published' " +
            "GROUP BY t.tag_id, t.tag_name ORDER BY activity DESC, articleCount DESC LIMIT 8")
    List<Map<String, Object>> selectHotTags();

    @Select("SELECT u.user_id AS userId, u.username, u.nickname, u.avatar_image AS avatarImage, u.signature, " +
            "COUNT(a.article_id) AS articleCount, COALESCE(SUM(a.like_count), 0) AS receivedLikes, " +
            "(SELECT COUNT(*) FROM user_follow f WHERE f.following_id = u.user_id) AS followerCount " +
            "FROM user u JOIN article a ON a.user_id = u.user_id AND a.status = 'published' " +
            "WHERE (#{excludeUserId} IS NULL OR u.user_id <> #{excludeUserId}) " +
            "GROUP BY u.user_id, u.username, u.nickname, u.avatar_image, u.signature " +
            "ORDER BY (COUNT(a.article_id) * 20 + COALESCE(SUM(a.like_count), 0)) DESC, u.create_time ASC LIMIT 6")
    List<Map<String, Object>> selectRecommendedCreators(@Param("excludeUserId") Integer excludeUserId);

    @Select("SELECT COUNT(*) FROM user_follow WHERE follower_id = #{followerId} AND following_id = #{followingId}")
    int isFollowing(@Param("followerId") Integer followerId, @Param("followingId") Integer followingId);

    @Insert("INSERT IGNORE INTO user_follow(follower_id, following_id) VALUES(#{followerId}, #{followingId})")
    void insertFollow(@Param("followerId") Integer followerId, @Param("followingId") Integer followingId);

    @Delete("DELETE FROM user_follow WHERE follower_id = #{followerId} AND following_id = #{followingId}")
    void deleteFollow(@Param("followerId") Integer followerId, @Param("followingId") Integer followingId);

    @Select("SELECT u.user_id AS userId, COUNT(a.article_id) AS articleCount, " +
            "COALESCE(SUM(a.view_count), 0) AS viewCount, COALESCE(SUM(a.like_count), 0) AS likeCount, " +
            "COALESCE(SUM(a.comment_count), 0) AS commentCount, " +
            "(SELECT COUNT(*) FROM user_follow f WHERE f.following_id = u.user_id) AS followerCount, " +
            "(SELECT COUNT(*) FROM user_follow f WHERE f.follower_id = u.user_id) AS followingCount " +
            "FROM user u LEFT JOIN article a ON a.user_id = u.user_id AND a.status = 'published' " +
            "WHERE u.user_id = #{userId} GROUP BY u.user_id")
    Map<String, Object> selectProfileMetrics(Integer userId);
}

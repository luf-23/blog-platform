package com.blogplatform.backend.mapper;

import com.blogplatform.backend.entity.CommunityPollOption;
import com.blogplatform.backend.entity.CommunityPost;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface CommunityMapper {

    @Select("<script>" +
            "SELECT p.*, u.username AS author_username, u.nickname AS author_nickname, " +
            "u.avatar_image AS author_avatar, u.signature AS author_signature " +
            "FROM community_post p JOIN user u ON p.user_id = u.user_id " +
            "WHERE p.status = 'published' " +
            "<if test='followerId != null'>AND EXISTS (SELECT 1 FROM user_follow f WHERE f.follower_id=#{followerId} AND f.following_id=p.user_id) </if>" +
            "<if test='topic != null and topic != \"\" and topic != \"全部\"'>AND p.topic = #{topic} </if>" +
            "<if test='keyword != null and keyword != \"\"'>AND (p.title LIKE CONCAT('%', #{keyword}, '%') OR p.content LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "<choose>" +
            "<when test='sort == \"hot\"'>ORDER BY (p.like_count + p.comment_count * 2 + p.vote_count) DESC, p.create_time DESC </when>" +
            "<otherwise>ORDER BY p.create_time DESC </otherwise>" +
            "</choose>" +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<CommunityPost> selectFeed(@Param("sort") String sort,
                                   @Param("topic") String topic,
                                   @Param("keyword") String keyword,
                                   @Param("followerId") Integer followerId,
                                   @Param("offset") int offset,
                                   @Param("pageSize") int pageSize);

    @Select("<script>SELECT COUNT(*) FROM community_post p WHERE p.status='published' " +
            "<if test='followerId != null'>AND EXISTS (SELECT 1 FROM user_follow f WHERE f.follower_id=#{followerId} AND f.following_id=p.user_id) </if>" +
            "<if test='topic != null and topic != \"\" and topic != \"全部\"'>AND p.topic=#{topic} </if>" +
            "<if test='keyword != null and keyword != \"\"'>AND (p.title LIKE CONCAT('%', #{keyword}, '%') OR p.content LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "</script>")
    int countFeed(@Param("topic") String topic,
                  @Param("keyword") String keyword,
                  @Param("followerId") Integer followerId);

    @Select("SELECT * FROM community_poll_option WHERE post_id=#{postId} ORDER BY option_id")
    List<CommunityPollOption> selectOptions(Integer postId);

    @Insert("INSERT INTO community_post (user_id,type,title,content,topic,status) VALUES " +
            "(#{userId},#{type},#{title},#{content},#{topic},'published')")
    @Options(useGeneratedKeys = true, keyProperty = "postId")
    void insertPost(CommunityPost post);

    @Insert("INSERT INTO community_poll_option (post_id,option_text) VALUES (#{postId},#{optionText})")
    void insertOption(@Param("postId") Integer postId, @Param("optionText") String optionText);

    @Select("SELECT COUNT(*) FROM community_post_like WHERE post_id=#{postId} AND user_id=#{userId}")
    int hasLiked(@Param("postId") Integer postId, @Param("userId") Integer userId);

    @Insert("INSERT INTO community_post_like(post_id,user_id) VALUES(#{postId},#{userId})")
    void insertLike(@Param("postId") Integer postId, @Param("userId") Integer userId);

    @Delete("DELETE FROM community_post_like WHERE post_id=#{postId} AND user_id=#{userId}")
    void deleteLike(@Param("postId") Integer postId, @Param("userId") Integer userId);

    @Update("UPDATE community_post SET like_count=GREATEST(like_count + #{delta}, 0) WHERE post_id=#{postId}")
    void updateLikeCount(@Param("postId") Integer postId, @Param("delta") int delta);

    @Select("SELECT COUNT(*) FROM community_poll_vote WHERE post_id=#{postId} AND user_id=#{userId}")
    int hasVoted(@Param("postId") Integer postId, @Param("userId") Integer userId);

    @Insert("INSERT INTO community_poll_vote(post_id,option_id,user_id) VALUES(#{postId},#{optionId},#{userId})")
    void insertVote(@Param("postId") Integer postId, @Param("optionId") Integer optionId, @Param("userId") Integer userId);

    @Update("UPDATE community_poll_option SET vote_count=vote_count+1 WHERE option_id=#{optionId} AND post_id=#{postId}")
    int updateOptionVote(@Param("postId") Integer postId, @Param("optionId") Integer optionId);

    @Update("UPDATE community_post SET vote_count=vote_count+1 WHERE post_id=#{postId}")
    void updatePostVote(Integer postId);

    @Select("SELECT COUNT(*) FROM user_follow WHERE follower_id=#{followerId} AND following_id=#{followingId}")
    int isFollowing(@Param("followerId") Integer followerId, @Param("followingId") Integer followingId);

    @Insert("INSERT INTO user_follow(follower_id,following_id) VALUES(#{followerId},#{followingId})")
    void insertFollow(@Param("followerId") Integer followerId, @Param("followingId") Integer followingId);

    @Delete("DELETE FROM user_follow WHERE follower_id=#{followerId} AND following_id=#{followingId}")
    void deleteFollow(@Param("followerId") Integer followerId, @Param("followingId") Integer followingId);

    @Select("SELECT topic, COUNT(*) AS post_count, SUM(comment_count + vote_count) AS participant_count " +
            "FROM community_post WHERE status='published' GROUP BY topic " +
            "ORDER BY (COUNT(*) * 5 + SUM(comment_count + vote_count)) DESC LIMIT 5")
    List<Map<String, Object>> selectHotTopics();

    @Select("SELECT u.user_id, u.username, u.nickname, u.avatar_image, u.signature, " +
            "COUNT(p.post_id) AS post_count, COALESCE(SUM(p.like_count),0) AS received_likes " +
            "FROM user u LEFT JOIN community_post p ON p.user_id=u.user_id AND p.status='published' " +
            "WHERE u.role='user' GROUP BY u.user_id, u.username, u.nickname, u.avatar_image, u.signature " +
            "ORDER BY (COUNT(p.post_id) * 10 + COALESCE(SUM(p.like_count),0)) DESC, u.create_time ASC LIMIT 4")
    List<Map<String, Object>> selectRecommendedCreators();
}

package com.blogplatform.backend.service.impl;

import com.blogplatform.backend.entity.CommunityPost;
import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.mapper.CommunityMapper;
import com.blogplatform.backend.service.CommunityService;
import com.blogplatform.backend.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommunityServiceImpl implements CommunityService {

    @Autowired
    private CommunityMapper communityMapper;

    @Override
    public Result feed(String sort, String topic, String keyword, Integer page, Integer pageSize) {
        return feedData(sort, topic, keyword, page, pageSize, null);
    }

    @Override
    public Result followingFeed(String topic, String keyword, Integer page, Integer pageSize) {
        Integer userId = currentUserId();
        if (userId == null) return Result.error("请先登录");
        return feedData("latest", topic, keyword, page, pageSize, userId);
    }

    private Result feedData(String sort, String topic, String keyword, Integer page, Integer pageSize, Integer followerId) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = pageSize == null ? 10 : Math.min(Math.max(pageSize, 1), 30);
        String safeSort = "hot".equals(sort) ? "hot" : "latest";
        List<CommunityPost> list = communityMapper.selectFeed(
                safeSort, topic, keyword, followerId, (safePage - 1) * safeSize, safeSize);
        list.stream()
                .filter(post -> "poll".equals(post.getType()))
                .forEach(post -> post.setOptions(communityMapper.selectOptions(post.getPostId())));

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", communityMapper.countFeed(topic, keyword, followerId));
        data.put("page", safePage);
        data.put("pageSize", safeSize);
        return Result.success(data);
    }

    @Override
    public Result meta() {
        Map<String, Object> data = new HashMap<>();
        data.put("hotTopics", communityMapper.selectHotTopics());
        data.put("recommendedCreators", communityMapper.selectRecommendedCreators());
        return Result.success(data);
    }

    @Override
    @Transactional
    public Result create(CommunityPost post, List<String> options) {
        Integer userId = currentUserId();
        if (userId == null) return Result.error("请先登录");
        if (post.getTitle() == null || post.getTitle().isBlank()) return Result.error("标题不能为空");
        if (post.getContent() == null || post.getContent().isBlank()) return Result.error("内容不能为空");

        String type = post.getType();
        if (!List.of("question", "share", "poll").contains(type)) post.setType("share");
        if ("poll".equals(post.getType()) && (options == null || options.stream().filter(option -> option != null && !option.isBlank()).count() < 2)) {
            return Result.error("投票至少需要两个选项");
        }
        post.setUserId(userId);
        post.setTopic(post.getTopic() == null || post.getTopic().isBlank() ? "随想" : post.getTopic().trim());
        communityMapper.insertPost(post);

        if ("poll".equals(post.getType())) {
            options.stream().filter(option -> option != null && !option.isBlank()).limit(6)
                    .forEach(option -> communityMapper.insertOption(post.getPostId(), option.trim()));
        }
        return Result.success(post.getPostId());
    }

    @Override
    @Transactional
    public Result toggleLike(Integer postId) {
        Integer userId = currentUserId();
        if (userId == null) return Result.error("请先登录");
        boolean liked = communityMapper.hasLiked(postId, userId) > 0;
        if (liked) {
            communityMapper.deleteLike(postId, userId);
            communityMapper.updateLikeCount(postId, -1);
        } else {
            communityMapper.insertLike(postId, userId);
            communityMapper.updateLikeCount(postId, 1);
        }
        return Result.success(Map.of("liked", !liked));
    }

    @Override
    @Transactional
    public Result vote(Integer postId, Integer optionId) {
        Integer userId = currentUserId();
        if (userId == null) return Result.error("请先登录");
        if (communityMapper.hasVoted(postId, userId) > 0) return Result.error("你已经参与过该投票");
        if (communityMapper.updateOptionVote(postId, optionId) == 0) return Result.error("投票选项不存在");
        communityMapper.insertVote(postId, optionId, userId);
        communityMapper.updatePostVote(postId);
        return Result.success();
    }

    @Override
    @Transactional
    public Result toggleFollow(Integer userId) {
        Integer currentUserId = currentUserId();
        if (currentUserId == null) return Result.error("请先登录");
        if (currentUserId.equals(userId)) return Result.error("不能关注自己");
        boolean following = communityMapper.isFollowing(currentUserId, userId) > 0;
        if (following) communityMapper.deleteFollow(currentUserId, userId);
        else communityMapper.insertFollow(currentUserId, userId);
        return Result.success(Map.of("following", !following));
    }

    private Integer currentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return claims == null ? null : (Integer) claims.get("id");
    }
}

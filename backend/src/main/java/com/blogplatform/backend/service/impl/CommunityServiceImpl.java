package com.blogplatform.backend.service.impl;

import com.blogplatform.backend.entity.ArticleVO;
import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.mapper.CommunityMapper;
import com.blogplatform.backend.mapper.TagMapper;
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

    @Autowired
    private TagMapper tagMapper;

    @Override
    public Result feed(String sort, Integer tagId, String keyword, Integer page, Integer pageSize) {
        return feedData(sort, tagId, keyword, page, pageSize, null);
    }

    @Override
    public Result followingFeed(Integer tagId, String keyword, Integer page, Integer pageSize) {
        Integer userId = currentUserId();
        if (userId == null) return Result.error("请先登录");
        return feedData("latest", tagId, keyword, page, pageSize, userId);
    }

    private Result feedData(String sort, Integer tagId, String keyword, Integer page,
                            Integer pageSize, Integer followerId) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = pageSize == null ? 12 : Math.min(Math.max(pageSize, 1), 30);
        String safeSort = "hot".equals(sort) ? "hot" : "latest";
        String safeKeyword = keyword == null ? null : keyword.trim();

        List<ArticleVO> list = communityMapper.selectFeed(
                safeSort, tagId, safeKeyword, followerId, (safePage - 1) * safeSize, safeSize);
        for (ArticleVO article : list) {
            article.setTags(tagMapper.selectByArticleId(article.getArticleId()));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", communityMapper.countFeed(tagId, safeKeyword, followerId));
        data.put("page", safePage);
        data.put("pageSize", safeSize);
        return Result.success(data);
    }

    @Override
    public Result meta() {
        Integer currentUserId = currentUserId();
        List<Map<String, Object>> creators = communityMapper.selectRecommendedCreators(currentUserId);
        for (Map<String, Object> creator : creators) {
            Integer creatorId = numberValue(creator.get("userId"));
            creator.put("following", currentUserId != null && creatorId != null
                    && communityMapper.isFollowing(currentUserId, creatorId) > 0);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("hotTags", communityMapper.selectHotTags());
        data.put("recommendedCreators", creators);
        return Result.success(data);
    }

    @Override
    public Result profileMetrics(Integer userId) {
        Map<String, Object> metrics = communityMapper.selectProfileMetrics(userId);
        if (metrics == null) return Result.error("用户不存在");
        metrics.put("following", false);
        return Result.success(metrics);
    }

    @Override
    public Result followState(Integer userId) {
        Integer currentUserId = currentUserId();
        if (currentUserId == null) return Result.error("请先登录");
        Map<String, Object> metrics = communityMapper.selectProfileMetrics(userId);
        if (metrics == null) return Result.error("用户不存在");
        metrics.put("following", !currentUserId.equals(userId)
                && communityMapper.isFollowing(currentUserId, userId) > 0);
        return Result.success(metrics);
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
        Map<String, Object> metrics = communityMapper.selectProfileMetrics(userId);
        metrics.put("following", !following);
        return Result.success(metrics);
    }

    private Integer currentUserId() {
        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            return claims == null ? null : (Integer) claims.get("id");
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer numberValue(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }
}

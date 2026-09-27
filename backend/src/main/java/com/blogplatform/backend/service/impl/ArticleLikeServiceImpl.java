package com.blogplatform.backend.service.impl;

import com.blogplatform.backend.mapper.ArticleLikeMapper;
import com.blogplatform.backend.mapper.ArticleMapper;
import com.blogplatform.backend.service.ArticleService;
import com.blogplatform.backend.service.ArticleLikeService;
import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class ArticleLikeServiceImpl implements ArticleLikeService {

    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private ArticleService articleService;

    @Override
    public Result<Integer> count(Integer articleId) {
        return Result.success(articleLikeMapper.count(articleId));
    }

    @Override
    @Transactional
    public Result like(Integer articleId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        int inserted = articleLikeMapper.add(articleId, userId);
        if (inserted > 0) {
            articleMapper.incrementLikeCount(articleId);
            articleService.evictDetailCache(articleId);
        }
        return Result.success(Map.of(
                "isLiked", true,
                "likeCount", articleLikeMapper.count(articleId)
        ));
    }

    @Override
    @Transactional
    public Result unlike(Integer articleId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        int deleted = articleLikeMapper.delete(articleId, userId);
        if (deleted > 0) {
            articleMapper.decrementLikeCount(articleId);
            articleService.evictDetailCache(articleId);
        }
        return Result.success(Map.of(
                "isLiked", false,
                "likeCount", articleLikeMapper.count(articleId)
        ));
    }

    @Override
    public Result<Boolean> check(Integer articleId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return Result.success(articleLikeMapper.exists(articleId, userId) > 0);
    }
}

package com.blogplatform.backend.Service.Impl;

import com.blogplatform.backend.Mapper.ArticleLikeMapper;
import com.blogplatform.backend.Service.ArticleLikeService;
import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ArticleLikeServiceImpl implements ArticleLikeService {

    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    @Override
    public Result<Integer> count(Integer articleId) {
        return Result.success(articleLikeMapper.count(articleId));
    }

    @Override
    public Result like(Integer articleId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        if (articleLikeMapper.selectByArticleIdAndUserId(articleId, userId) != null) {
            return Result.error("已点赞");
        }
        articleLikeMapper.add(articleId, userId);
        return Result.success();
    }

    @Override
    public Result unlike(Integer articleId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        if (articleLikeMapper.selectByArticleIdAndUserId(articleId, userId) == null) {
            return Result.error("未点赞");
        }
        articleLikeMapper.delete(articleId, userId);
        return Result.success();
    }

    @Override
    public Result<Boolean> check(Integer articleId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return Result.success(articleLikeMapper.selectByArticleIdAndUserId(articleId, userId) != null);
    }
}

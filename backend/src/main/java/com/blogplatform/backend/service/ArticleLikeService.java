package com.blogplatform.backend.service;

import com.blogplatform.backend.entity.Result;

public interface ArticleLikeService {
    Result<Integer> count(Integer articleId);

    Result like(Integer articleId);

    Result unlike(Integer articleId);

    Result<Boolean> check(Integer articleId);
}

package com.blogplatform.backend.service;

import com.blogplatform.backend.entity.Result;

public interface CommentLikeService {
    Result count(Integer commentId);

    Result like(Integer commentId);

    Result unLike(Integer commentId);

    Result check(Integer commentId);
}

package com.blogplatform.backend.service;

import com.blogplatform.backend.entity.Result;

public interface CommunityService {
    Result feed(String sort, Integer tagId, String keyword, Integer page, Integer pageSize);

    Result followingFeed(Integer tagId, String keyword, Integer page, Integer pageSize);

    Result meta();

    Result profileMetrics(Integer userId);

    Result followState(Integer userId);

    Result toggleFollow(Integer userId);
}

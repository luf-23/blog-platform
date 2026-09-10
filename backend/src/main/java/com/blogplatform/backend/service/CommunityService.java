package com.blogplatform.backend.service;

import com.blogplatform.backend.entity.Result;

import java.util.List;

public interface CommunityService {
    Result feed(String sort, List<Integer> tagIds, String keyword, Integer page, Integer pageSize);

    Result followingFeed(List<Integer> tagIds, String keyword, Integer page, Integer pageSize);

    Result meta();

    Result profileMetrics(Integer userId);

    Result profileFollowers(Integer userId, Integer page, Integer pageSize);

    Result profileFollowing(Integer userId, Integer page, Integer pageSize);

    Result followState(Integer userId);

    Result toggleFollow(Integer userId);
}

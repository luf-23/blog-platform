package com.blogplatform.backend.service;

import com.blogplatform.backend.entity.CommunityPost;
import com.blogplatform.backend.entity.Result;

import java.util.List;

public interface CommunityService {
    Result feed(String sort, String topic, String keyword, Integer page, Integer pageSize);

    Result followingFeed(String topic, String keyword, Integer page, Integer pageSize);
    Result meta();
    Result create(CommunityPost post, List<String> options);
    Result toggleLike(Integer postId);
    Result vote(Integer postId, Integer optionId);
    Result toggleFollow(Integer userId);
}

package com.blogplatform.backend.service;

import com.blogplatform.backend.entity.Result;

import java.util.Map;

public interface CommentService {
    Result<Map<String, Object>> list(Integer articleId, Integer page, Integer pageSize);

    Result<Map<String, Object>> replies(Integer articleId, Integer rootId, Integer page, Integer pageSize);

    Result add(Integer articleId, String content, Integer parentId, Integer replyToUserId);

    Result delete(Integer commentId);
}

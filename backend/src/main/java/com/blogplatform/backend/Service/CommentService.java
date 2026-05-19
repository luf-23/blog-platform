package com.blogplatform.backend.Service;

import com.blogplatform.backend.entity.Result;

import java.util.Map;

public interface CommentService {
    Result<Map<String, Object>> list(Integer articleId, Integer page, Integer pageSize);

    Result add(Integer articleId, String content, Integer parentId);

    Result delete(Integer commentId);
}

package com.blogplatform.backend.Service;


import com.blogplatform.backend.entity.Comment;
import com.blogplatform.backend.entity.Result;
import org.springframework.cglib.core.TinyBitSet;

import java.util.List;
import java.util.Map;

public interface CommentService {
    Result<Map<String, Object>> list(Integer articleId, Integer page, Integer pageSize);

    Result add(Integer articleId,String  content);

    Result delete(Integer commentId);
}

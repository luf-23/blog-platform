package com.blogplatform.backend.Service;

import com.blogplatform.backend.entity.Article;
import com.blogplatform.backend.entity.Result;

import java.util.Map;

public interface CommunityService {
    Result list();

    Result getAuthor(Integer categoryId);

    Result selectedList(Map<String, Object> params);
}

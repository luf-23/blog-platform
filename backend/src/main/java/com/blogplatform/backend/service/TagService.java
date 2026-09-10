package com.blogplatform.backend.service;

import com.blogplatform.backend.entity.Result;

import java.util.List;

public interface TagService {
    Result getAllTags();
    Result getPopularTags(int limit);
    Result ensureTagsExist(List<String> tagNames);
}

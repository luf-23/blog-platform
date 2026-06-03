package com.blogplatform.backend.service.impl;

import com.blogplatform.backend.mapper.TagMapper;
import com.blogplatform.backend.service.TagService;
import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.entity.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TagServiceImpl implements TagService {

    @Autowired
    private TagMapper tagMapper;

    @Override
    public Result getAllTags() {
        return Result.success(tagMapper.selectAll());
    }

    @Override
    public Result getPopularTags(int limit) {
        return Result.success(tagMapper.selectPopular(limit));
    }

    @Override
    public Result ensureTagsExist(List<String> tagNames) {
        List<Tag> result = new ArrayList<>();
        for (String name : tagNames) {
            if (name == null || name.isBlank()) continue;
            String trimmed = name.trim();
            Tag tag = tagMapper.selectByName(trimmed);
            if (tag == null) {
                tag = new Tag();
                tag.setTagName(trimmed);
                tagMapper.insert(tag);
            }
            result.add(tag);
        }
        return Result.success(result);
    }
}

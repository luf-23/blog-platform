package com.blogplatform.backend.service.impl;

import com.blogplatform.backend.mapper.ArticleMapper;
import com.blogplatform.backend.mapper.CategoryMapper;
import com.blogplatform.backend.mapper.UserMapper;
import com.blogplatform.backend.service.CommunityService;
import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CommunityServiceImpl implements CommunityService {

    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public Result list() {
        // Deprecated: use /article/search instead
        var list = articleMapper.searchPublished(null, null, null, null, "latest", 0, 20);
        return Result.success(list);
    }

    @Override
    public Result getAuthor(Integer categoryId) {
        if (categoryId == null) return Result.error("文章分类id不能为空");
        Integer authorId = categoryMapper.selectUserIdByCategoryId(categoryId);
        if (authorId == null) return Result.error("作者不存在");
        User user = userMapper.selectById(authorId);
        return Result.success(user.getUsername());
    }

    @Override
    public Result selectedList(Map<String, Object> params) {
        String keyword = (String) params.get("title");
        var list = articleMapper.searchPublished(keyword, null, null, null, "latest", 0, 20);
        return Result.success(list);
    }
}

package com.blogplatform.backend.service.impl;

import com.blogplatform.backend.mapper.CategoryMapper;
import com.blogplatform.backend.service.CategoryService;
import com.blogplatform.backend.entity.Category;
import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public Result<List<Category>> getList() {
        Map<String,Object> claims = ThreadLocalUtil.get();
        if (claims == null) return Result.error("用户未登录");
        Integer userId = (Integer) claims.get("id");
        categoryMapper.refreshAllCounts();
        List<Category> categoryList = categoryMapper.selectByUserId(userId);
        return Result.success(categoryList);
    }

    @Override
    public Result add(Category category) {
        if (category == null) return Result.error("文章分类不能为空");
        String name = normalizeName(category.getCategoryName());
        if (name == null) return Result.error("文章分类名称不能为空且不能超过 50 个字符");
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null) return Result.error("用户未登录");
        category.setCategoryName(name);
        category.setCategoryDescription(normalizeDescription(category.getCategoryDescription()));
        category.setCoverImage(normalizeCover(category.getCoverImage()));
        category.setUserId((Integer) claims.get("id"));
        categoryMapper.add(category);
        return Result.success();
    }

    @Override
    public Result delete(Integer categoryId) {
        if (categoryId == null) return Result.error("文章分类id不能为空");
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null) return Result.error("用户未登录");
        Integer ownerId = categoryMapper.selectUserIdByCategoryId(categoryId);
        if (ownerId == null) return Result.error("文章分类不存在");
        if (!ownerId.equals((Integer) claims.get("id"))) return Result.error("权限不足");
        categoryMapper.deleteById(categoryId);
        return Result.success();
    }

    @Override
    public Result update(Category category) {
        if (category == null) return Result.error("文章分类不能为空");
        if (category.getCategoryId() == null) return Result.error("文章分类id不能为空");
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null) return Result.error("用户未登录");
        Integer ownerId = categoryMapper.selectUserIdByCategoryId(category.getCategoryId());
        if (ownerId == null) return Result.error("文章分类不存在");
        if (!ownerId.equals((Integer) claims.get("id"))) return Result.error("权限不足");
        String name = normalizeName(category.getCategoryName());
        if (name == null) return Result.error("文章分类名称不能为空且不能超过 50 个字符");
        category.setCategoryName(name);
        category.setCategoryDescription(normalizeDescription(category.getCategoryDescription()));
        category.setCoverImage(normalizeCover(category.getCoverImage()));
        categoryMapper.update(category);
        return Result.success();
    }

    @Override
    public Result setDefault() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null) return Result.error("用户未登录");
        Integer userId = (Integer) claims.get("id");
        Category existing = categoryMapper.selectByUserIdAndName(userId, "未分类");
        if (existing != null) return Result.success(existing.getCategoryId());
        Category category = new Category();
        category.setUserId(userId);
        category.setCategoryName("未分类");
        category.setCategoryDescription("暂未归入具体主题的文章");
        categoryMapper.add(category);
        return Result.success(category.getCategoryId());
    }

    @Override
    public Result<Integer> getDefaultId(Integer userId, String categoryName, String categoryDescription) {
        Integer categoryId = categoryMapper.selectCategoryId(userId, categoryName, categoryDescription);
        if (categoryId == null) return Result.error("默认分类不存在");
        return Result.success(categoryId);
    }

    private String normalizeName(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() || normalized.length() > 50 ? null : normalized;
    }

    private String normalizeDescription(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized.substring(0, Math.min(200, normalized.length()));
    }

    private String normalizeCover(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized.substring(0, Math.min(512, normalized.length()));
    }

}

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
        List<Category> categoryList = categoryMapper.selectByUserId(userId);
        return Result.success(categoryList);
    }

    @Override
    public Result add(Category category) {
        if (category == null) return Result.error("文章分类不能为空");
        if (category.getCategoryName() == null) return Result.error("文章分类名称不能为空");
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null) return Result.error("用户未登录");
        category.setUserId((Integer) claims.get("id"));
        categoryMapper.add(category);
        return Result.success();
    }

    @Override
    public Result delete(Integer categoryId) {
        if (categoryId == null) return Result.error("文章分类id不能为空");
        categoryMapper.deleteById(categoryId);
        return Result.success();
    }

    @Override
    public Result update(Category category) {
        if (category == null) return Result.error("文章分类不能为空");
        if (category.getCategoryId() == null) return Result.error("文章分类id不能为空");
        categoryMapper.update(category);
        return Result.success();
    }

    @Override
    public Result setDefault(Integer userId) {
        Category category = new Category();
        category.setUserId(userId);
        category.setCategoryName("默认分类");
        category.setCategoryDescription("用于服务安卓项目");
        categoryMapper.add(category);
        return Result.success();
    }

    @Override
    public Result<Integer> getDefaultId(Integer userId, String categoryName, String categoryDescription) {
        Integer categoryId = categoryMapper.selectCategoryId(userId, categoryName, categoryDescription);
        if (categoryId == null) return Result.error("默认分类不存在");
        return Result.success(categoryId);
    }

}

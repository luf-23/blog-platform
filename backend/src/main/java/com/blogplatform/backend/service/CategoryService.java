package com.blogplatform.backend.service;

import com.blogplatform.backend.entity.Category;
import com.blogplatform.backend.entity.Result;

import java.util.List;

public interface CategoryService {
    Result<List<Category>> getList();

    Result add(Category category);

    Result delete(Integer categoryId);

    Result update(Category category);

    Result setDefault(Integer userId);


    Result<Integer> getDefaultId(Integer userId, String categoryName, String categoryDescription);
}

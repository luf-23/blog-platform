package com.blogplatform.backend.controller;

import com.blogplatform.backend.mapper.CategoryMapper;
import com.blogplatform.backend.service.CategoryService;
import com.blogplatform.backend.entity.Category;
import com.blogplatform.backend.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryMapper categoryMapper;

    // Public: get categories by user (for profile/filter)
    @GetMapping("/byUser/{userId}")
    public Result getByUser(@PathVariable Integer userId) {
        return Result.success(categoryMapper.selectByUserId(userId));
    }

    // Auth required: my categories
    @GetMapping("/list")
    public Result<List<Category>> getList() {
        return categoryService.getList();
    }

    @PostMapping("/add")
    public Result add(@RequestBody Category category) {
        return categoryService.add(category);
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        return categoryService.delete(id);
    }

    @PutMapping("/update")
    public Result update(@RequestBody Category category) {
        return categoryService.update(category);
    }

    @PostMapping("/default")
    public Result setDefault() {
        return categoryService.setDefault();
    }
}

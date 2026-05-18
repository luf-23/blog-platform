package com.blogplatform.backend.controller;

import com.blogplatform.backend.Service.CategoryService;
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

    @GetMapping("/list")
    public Result<List<Category>> getList(){
        return categoryService.getList();
    }

    @PostMapping("/add")
    public Result add(@RequestBody Category category){
        return categoryService.add(category);
    }

    @PostMapping("/delete")
    public Result delete(@RequestParam Integer categoryId){
        return categoryService.delete(categoryId);
    }

    @PostMapping("/update")
    public Result update(@RequestBody Category category){
        return categoryService.update(category);
    }

    @PostMapping("/default")
    public Result setDefault(@RequestParam Integer userId){
        return categoryService.setDefault(userId);
    }

    @GetMapping("/defaultId")
    public Result<Integer> getDefaultId(@RequestParam Integer userId,@RequestParam String categoryName,@RequestParam String categoryDescription){
        if (categoryName == null || categoryDescription == null || userId == null) return Result.error("参数错误");
        if (categoryName.equals("") || categoryDescription.equals("")) return Result.error("参数错误");
        return categoryService.getDefaultId(userId,categoryName,categoryDescription);
    }
}

package com.blogplatform.backend.controller;

import com.blogplatform.backend.Service.ArticleService;
import com.blogplatform.backend.entity.Article;
import com.blogplatform.backend.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/article")
public class ArticleController {
    @Autowired
    private ArticleService ArticleService;

    @GetMapping("/publishedList")
    public Result getPublishedList(@RequestParam Integer categoryId){
        return ArticleService.getPublishedList(categoryId);
    }

    @GetMapping("/draftList")
    public Result getDraftList(@RequestParam Integer categoryId){
        return ArticleService.getDraftList(categoryId);
    }

    @GetMapping("/list")
    public Result getList(@RequestParam Integer categoryId){
        return ArticleService.getList(categoryId);
    }

    @GetMapping("/selectedList")
    public Result getSelectedList(@RequestParam Map<String, Object> params){
        return ArticleService.getSelectedList(params);
    }

    @PostMapping("/delete")
    public Result delete(@RequestParam Integer articleId){
        return ArticleService.delete(articleId);
    }

    @PostMapping("/add")
    public Result add(@RequestBody Article article){
        return ArticleService.add(article);
    }

    @PostMapping("/check")
    public Result check(@RequestParam Integer categoryId){
        return ArticleService.check(categoryId);
    }

    @GetMapping("/detail")
    public Result getDetail(@RequestParam Integer articleId,@RequestParam Integer categoryId){
        return ArticleService.getDetail(articleId,categoryId);
    }

    @PostMapping("/publish")
    public Result pubish(@RequestParam Integer articleId,@RequestParam Integer categoryId){
        return ArticleService.publish(articleId,categoryId);
    }

    @PostMapping("/update")
    public Result update(@RequestBody Article article){
        return ArticleService.update(article);
    }

    @PostMapping("/updateCoverImage")
    public Result updateCoverImage(@RequestParam Integer articleId,@RequestParam String coverImageUrl,@RequestParam Integer categoryId){
        return ArticleService.updateCoverImage(articleId,coverImageUrl,categoryId);
    }


}

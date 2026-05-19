package com.blogplatform.backend.controller;

import com.blogplatform.backend.Service.ArticleLikeService;
import com.blogplatform.backend.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/articleLike")
public class ArticleLikeController {

    @Autowired
    private ArticleLikeService articleLikeService;

    @GetMapping("/count")
    public Result count(@RequestParam Integer articleId) {
        if (articleId == null) return Result.error("参数错误");
        return articleLikeService.count(articleId);
    }

    @PostMapping("/like")
    public Result like(@RequestParam Integer articleId) {
        if (articleId == null) return Result.error("参数错误");
        return articleLikeService.like(articleId);
    }

    @PostMapping("/unlike")
    public Result unlike(@RequestParam Integer articleId) {
        if (articleId == null) return Result.error("参数错误");
        return articleLikeService.unlike(articleId);
    }

    @GetMapping("/check")
    public Result check(@RequestParam Integer articleId) {
        if (articleId == null) return Result.error("参数错误");
        return articleLikeService.check(articleId);
    }
}

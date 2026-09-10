package com.blogplatform.backend.controller;

import com.blogplatform.backend.service.ArticleService;
import com.blogplatform.backend.entity.Article;
import com.blogplatform.backend.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/article")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    // ── Public: compound search ───────────────────────────────────────────────

    @GetMapping("/search")
    public Result search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer tagId,
            @RequestParam(required = false) Integer authorId,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return articleService.search(keyword, categoryId, tagId, authorId, sort, page, pageSize);
    }

    @GetMapping("/detail/{id}")
    public Result getDetail(@PathVariable Integer id) {
        return articleService.getPublicDetail(id);
    }

    // ── My blog ───────────────────────────────────────────────────────────────

    @GetMapping("/my")
    public Result getMyArticles(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer categoryId) {
        return articleService.getMyArticles(title, status, categoryId);
    }

    @GetMapping("/my/detail/{id}")
    public Result getMyArticleDetail(@PathVariable Integer id) {
        return articleService.getPublicDetail(id);
    }

    @PostMapping("/add")
    public Result add(@RequestBody Map<String, Object> body) {
        Article article = buildArticle(body);
        @SuppressWarnings("unchecked")
        List<String> tagNames = (List<String>) body.get("tagNames");
        return articleService.add(article, tagNames);
    }

    @PutMapping("/update")
    public Result update(@RequestBody Map<String, Object> body) {
        Article article = buildArticle(body);
        @SuppressWarnings("unchecked")
        List<String> tagNames = (List<String>) body.get("tagNames");
        return articleService.update(article, tagNames);
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        return articleService.delete(id);
    }

    @PostMapping("/submit/{id}")
    public Result submit(@PathVariable Integer id) {
        return articleService.submitForReview(id);
    }

    @PostMapping("/cover/{id}")
    public Result updateCover(@PathVariable Integer id, @RequestParam String coverImage) {
        return articleService.updateCoverImage(id, coverImage);
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @GetMapping("/admin/list")
    public Result adminList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return articleService.adminSearch(status, keyword, page, pageSize);
    }

    @PostMapping("/admin/approve/{id}")
    public Result approve(@PathVariable Integer id) {
        return articleService.adminApprove(id);
    }

    @PostMapping("/admin/reject/{id}")
    public Result reject(@PathVariable Integer id) {
        return articleService.adminReject(id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Article buildArticle(Map<String, Object> body) {
        Article article = new Article();
        if (body.get("articleId") != null) article.setArticleId((Integer) body.get("articleId"));
        if (body.get("categoryId") != null) article.setCategoryId((Integer) body.get("categoryId"));
        if (body.get("title") != null) article.setTitle((String) body.get("title"));
        if (body.get("summary") != null) article.setSummary((String) body.get("summary"));
        if (body.get("content") != null) article.setContent((String) body.get("content"));
        if (body.get("coverImage") != null) article.setCoverImage((String) body.get("coverImage"));
        if (body.get("status") != null) article.setStatus((String) body.get("status"));
        return article;
    }
}

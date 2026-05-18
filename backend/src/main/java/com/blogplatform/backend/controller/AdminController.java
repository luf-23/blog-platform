package com.blogplatform.backend.controller;

import com.blogplatform.backend.Mapper.ArticleMapper;
import com.blogplatform.backend.Service.AdminService;
import com.blogplatform.backend.entity.Announcement;
import com.blogplatform.backend.entity.Article;
import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private AdminService adminService;
    @Autowired
    private ArticleMapper articleMapper;
    @GetMapping("/pendingList")
    public Result<List<Article>> getPendingList() {
        if (!isAdmin()) return Result.error("权限不足");
        return adminService.getPendingList();
    }

    @GetMapping("/publishedList")
    public Result<List<Article>> getPublishedList(){
        if (!isAdmin()) return Result.error("权限不足");
        return adminService.getPublishedList();
    }

    @GetMapping("/articleDetail")
    public Result<Article> getArticleDetail(Integer articleId){
        if (!isAdmin()) return Result.error("权限不足");
        if (articleMapper.selectById(articleId) == null) return Result.error("文章不存在");
        return adminService.getArticleDetail(articleId);
    }

    @PostMapping("/accept")
    public Result accept(Integer articleId){
        if (!isAdmin()) return Result.error("权限不足");
        if (articleMapper.selectById(articleId) == null) return Result.error("文章不存在");
        return adminService.accept(articleId);
    }

    @PostMapping("/reject")
    public Result reject(Integer articleId){
        if (!isAdmin()) return Result.error("权限不足");
        if (articleMapper.selectById(articleId) == null) return Result.error("文章不存在");
        return adminService.reject(articleId);
    }

    @PostMapping("/drop")
    public Result drop(Integer articleId){
        if (!isAdmin()) return Result.error("权限不足");
        if (articleMapper.selectById(articleId) == null) return Result.error("文章不存在");
        return adminService.drop(articleId);
    }

    @GetMapping("/users")
    public Result UserList(){
        if (!isAdmin()) return Result.error("权限不足");
        return adminService.getUserList();
    }

    @GetMapping("/announcement")
    public Result<List<Announcement>> getAnnouncement(){
        //if (!isAdmin()) return Result.error("权限不足");
        return adminService.getAnnouncement();
    }

    @PostMapping("/deleteAnnouncement")
    public Result deleteAnnouncement(Integer id){
        if (!isAdmin()) return Result.error("权限不足");
        if (id == null) return Result.error("参数错误");
        return adminService.deleteAnnouncement(id);
    }

    @PostMapping("/addAnnouncement")
    public Result addAnnouncement(@RequestBody Announcement announcement){
        if (!isAdmin()) return Result.error("权限不足");
        if (announcement == null|| announcement.getContent() == null || announcement.getTitle() == null || announcement.getType() == null) return Result.error("参数错误");
        return adminService.addAnnouncement(announcement);
    }

    private boolean isAdmin() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        String username = (String) claims.get("username");
        if (!username.equals("admin")) return false;
        return true;
    }
}

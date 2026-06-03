package com.blogplatform.backend.controller;

import com.blogplatform.backend.mapper.ArticleMapper;
import com.blogplatform.backend.mapper.CommentMapper;
import com.blogplatform.backend.mapper.UserMapper;
import com.blogplatform.backend.service.AdminService;
import com.blogplatform.backend.entity.Announcement;
import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CommentMapper commentMapper;

    // ── Dashboard stats ───────────────────────────────────────────────────────

    @GetMapping("/stats")
    public Result getStats() {
        if (!isAdmin()) return Result.error("权限不足");
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userMapper.totalCount());
        stats.put("totalArticles", articleMapper.totalCount());
        stats.put("publishedArticles", articleMapper.publishedCount());
        stats.put("pendingArticles", articleMapper.pendingCount());
        stats.put("totalComments", commentMapper.totalCount());
        return Result.success(stats);
    }

    // ── Article management ────────────────────────────────────────────────────

    @GetMapping("/articles")
    public Result getArticles(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        if (!isAdmin()) return Result.error("权限不足");
        int offset = (page - 1) * pageSize;
        var list = articleMapper.adminSearch(status, keyword, offset, pageSize);
        int total = articleMapper.adminCount(status, keyword);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return Result.success(result);
    }

    @PostMapping("/accept")
    public Result accept(@RequestParam Integer articleId) {
        if (!isAdmin()) return Result.error("权限不足");
        if (articleMapper.selectById(articleId) == null) return Result.error("文章不存在");
        return adminService.accept(articleId);
    }

    @PostMapping("/reject")
    public Result reject(@RequestParam Integer articleId) {
        if (!isAdmin()) return Result.error("权限不足");
        if (articleMapper.selectById(articleId) == null) return Result.error("文章不存在");
        return adminService.reject(articleId);
    }

    @PostMapping("/drop")
    public Result drop(@RequestParam Integer articleId) {
        if (!isAdmin()) return Result.error("权限不足");
        if (articleMapper.selectById(articleId) == null) return Result.error("文章不存在");
        return adminService.drop(articleId);
    }

    // ── User management ───────────────────────────────────────────────────────

    @GetMapping("/users")
    public Result getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        if (!isAdmin()) return Result.error("权限不足");
        int offset = (page - 1) * pageSize;
        var list = userMapper.selectPage(offset, pageSize);
        int total = userMapper.countAll();
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return Result.success(result);
    }

    // ── Announcements ─────────────────────────────────────────────────────────

    @GetMapping("/announcement")
    public Result<List<Announcement>> getAnnouncement() {
        return adminService.getAnnouncement();
    }

    @PostMapping("/deleteAnnouncement")
    public Result deleteAnnouncement(@RequestParam Integer id) {
        if (!isAdmin()) return Result.error("权限不足");
        if (id == null) return Result.error("参数错误");
        return adminService.deleteAnnouncement(id);
    }

    @PostMapping("/addAnnouncement")
    public Result addAnnouncement(@RequestBody Announcement announcement) {
        if (!isAdmin()) return Result.error("权限不足");
        if (announcement == null || announcement.getContent() == null
                || announcement.getTitle() == null || announcement.getType() == null)
            return Result.error("参数错误");
        return adminService.addAnnouncement(announcement);
    }

    private boolean isAdmin() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        String role = (String) claims.get("role");
        String username = (String) claims.get("username");
        return "admin".equals(role) || "admin".equals(username);
    }
}

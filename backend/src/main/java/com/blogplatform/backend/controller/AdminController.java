package com.blogplatform.backend.controller;

import com.blogplatform.backend.mapper.ArticleMapper;
import com.blogplatform.backend.mapper.CommentMapper;
import com.blogplatform.backend.mapper.TagMapper;
import com.blogplatform.backend.mapper.UserMapper;
import com.blogplatform.backend.service.AdminService;
import com.blogplatform.backend.entity.Announcement;
import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.entity.Tag;
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
    @Autowired
    private TagMapper tagMapper;

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

    // ── Tag management ───────────────────────────────────────────────────────

    @GetMapping("/tags")
    public Result<List<Tag>> getTags() {
        if (!isAdmin()) return Result.error("权限不足");
        tagMapper.refreshAllCounts();
        return Result.success(tagMapper.selectAll());
    }

    @PostMapping("/tags")
    public Result<Tag> addTag(@RequestBody Tag tag) {
        if (!isAdmin()) return Result.error("权限不足");
        String name = normalizeTagName(tag == null ? null : tag.getTagName());
        if (name == null) return Result.error("标签名不能为空且不能超过 30 个字符");
        if (tagMapper.selectByName(name) != null) return Result.error("标签已存在");
        Result parentValidation = validateTagParent(tag == null ? null : tag.getParentId(), null);
        if (parentValidation != null) return parentValidation;
        Tag created = new Tag();
        created.setTagName(name);
        created.setParentId(tag == null ? null : tag.getParentId());
        created.setSortOrder(normalizeSortOrder(tag == null ? null : tag.getSortOrder()));
        tagMapper.insert(created);
        return Result.success(tagMapper.selectById(created.getTagId()));
    }

    @PutMapping("/tags/{tagId}")
    public Result updateTag(@PathVariable Integer tagId, @RequestBody Tag tag) {
        if (!isAdmin()) return Result.error("权限不足");
        Tag existing = tagMapper.selectById(tagId);
        if (existing == null) return Result.error("标签不存在");
        String name = normalizeTagName(tag == null ? null : tag.getTagName());
        if (name == null) return Result.error("标签名不能为空且不能超过 30 个字符");
        Tag duplicate = tagMapper.selectByName(name);
        if (duplicate != null && !duplicate.getTagId().equals(tagId)) return Result.error("标签已存在");
        Integer parentId = tag == null ? null : tag.getParentId();
        Result parentValidation = validateTagParent(parentId, tagId);
        if (parentValidation != null) return parentValidation;
        if (existing.getParentId() == null && parentId != null && tagMapper.countChildren(tagId) > 0) {
            return Result.error("该一级标签下仍有二级标签，不能改为二级标签");
        }
        if (existing.getParentId() != null && parentId == null && tagMapper.countArticlesByTagId(tagId) > 0) {
            return Result.error("该标签仍被文章使用，不能改为一级标签");
        }
        tagMapper.update(tagId, name, parentId, normalizeSortOrder(tag == null ? null : tag.getSortOrder()));
        return Result.success();
    }

    @DeleteMapping("/tags/{tagId}")
    public Result deleteTag(@PathVariable Integer tagId) {
        if (!isAdmin()) return Result.error("权限不足");
        if (tagMapper.selectById(tagId) == null) return Result.error("标签不存在");
        int childCount = tagMapper.countChildren(tagId);
        if (childCount > 0) return Result.error("该一级标签下仍有 " + childCount + " 个二级标签，请先移动或删除它们");
        int articleCount = tagMapper.countArticlesByTagId(tagId);
        if (articleCount > 0) return Result.error("该标签仍被 " + articleCount + " 篇文章使用，暂不能删除");
        tagMapper.deleteById(tagId);
        return Result.success();
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

    private String normalizeTagName(String value) {
        if (value == null) return null;
        String name = value.trim();
        return name.isEmpty() || name.length() > 30 ? null : name;
    }

    private Integer normalizeSortOrder(Integer value) {
        if (value == null) return 0;
        return Math.max(0, Math.min(value, 9999));
    }

    private Result validateTagParent(Integer parentId, Integer currentTagId) {
        if (parentId == null) return null;
        if (parentId.equals(currentTagId)) return Result.error("标签不能以自己作为上级");
        Tag parent = tagMapper.selectById(parentId);
        if (parent == null) return Result.error("一级标签不存在");
        if (parent.getParentId() != null) return Result.error("最多只支持两级标签");
        return null;
    }
}

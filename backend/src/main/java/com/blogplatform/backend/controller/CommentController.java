package com.blogplatform.backend.controller;

import com.blogplatform.backend.mapper.CommentMapper;
import com.blogplatform.backend.service.CommentService;
import com.blogplatform.backend.entity.Comment;
import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentMapper commentMapper;

    @GetMapping("/list")
    public Result list(
            @RequestParam Integer articleId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return commentService.list(articleId, page, pageSize);
    }

    @GetMapping("/replies")
    public Result replies(
            @RequestParam Integer articleId,
            @RequestParam Integer rootId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return commentService.replies(articleId, rootId, page, pageSize);
    }

    @PostMapping("/publish")
    public Result add(@RequestBody Map<String, Object> body) {
        Integer articleId = (Integer) body.get("articleId");
        String content = (String) body.get("content");
        Integer parentId = (Integer) body.get("parentId");
        Integer replyToUserId = (Integer) body.get("replyToUserId");

        if (articleId == null) return Result.error("文章ID不能为空");
        if (content == null || content.isBlank()) return Result.error("评论内容不能为空");
        if (content.length() > 1000) return Result.error("评论内容不能超过1000字");

        return commentService.add(articleId, content.trim(), parentId, replyToUserId);
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) return Result.error("评论不存在");
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer currentUserId = (Integer) claims.get("id");
        String role = (String) claims.get("role");
        if (!currentUserId.equals(comment.getUserId()) && !"admin".equals(role)) {
            return Result.error("权限不足");
        }
        return commentService.delete(id);
    }
}

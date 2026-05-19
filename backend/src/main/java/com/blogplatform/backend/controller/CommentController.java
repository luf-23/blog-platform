package com.blogplatform.backend.controller;

import com.blogplatform.backend.Mapper.ArticleMapper;
import com.blogplatform.backend.Mapper.CategoryMapper;
import com.blogplatform.backend.Mapper.CommentMapper;
import com.blogplatform.backend.Service.CommentService;
import com.blogplatform.backend.entity.Article;
import com.blogplatform.backend.entity.Category;
import com.blogplatform.backend.entity.Comment;
import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private ArticleMapper articleMapper;

    @GetMapping("/list")
    public Result list(@RequestParam Integer articleId, @RequestParam Integer page, @RequestParam Integer pageSize){
        return commentService.list(articleId, page, pageSize);
    }
    @PostMapping("/publish")
    public Result add(@RequestBody Comment comment){
        Integer articleId = comment.getArticleId();
        String content = comment.getContent();
        if (articleId == null) return Result.error("文章id不能为空");
        if (content == null || content.isBlank()) return Result.error("评论内容不能为空");
        return commentService.add(articleId, content.trim(), comment.getParentId());
    }
    @PostMapping("/delete")
    public Result delete(@RequestParam Integer commentId){
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) return Result.error("评论不存在");
        if (!isok(comment.getUserId(),comment)) return Result.error("权限不足");
        return commentService.delete(commentId);
    }
    private boolean isok(Integer userId,Comment comment){
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer id = (Integer) claims.get("id");//当前使用者
        if (id.equals(userId)) return true;
        //判断当前用户是否是文章作者
        List<Category> categories = new ArrayList<>(categoryMapper.selectByUserId(id));
        for (Category category : categories){
            List<Article> articleList = new ArrayList<>(articleMapper.selectListByCategoryId(category.getCategoryId()));
            for (Article article : articleList){
                if (article.getArticleId().equals(comment.getArticleId())) return true;
            }
        }
        return false;
    }
}

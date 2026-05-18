package com.blogplatform.backend.Service.Impl;

import com.blogplatform.backend.Mapper.ArticleMapper;
import com.blogplatform.backend.Mapper.CategoryMapper;
import com.blogplatform.backend.Service.ArticleService;
import com.blogplatform.backend.entity.Article;
import com.blogplatform.backend.entity.Category;
import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public Result getPublishedList(Integer categoryId) {
        if (categoryId == null) return Result.error("文章分类id不能为空");
        //if (!isok(categoryId)) return Result.error("权限不足");
        List<Article> articleList = articleMapper.selectPublishedListByCategoryId(categoryId);
        return Result.success(articleList);
    }

    @Override
    public Result getDraftList(Integer categoryId) {
        if (categoryId == null) return Result.error("文章分类id不能为空");
        if (!isok(categoryId)) return Result.error("权限不足");
        List<Article> articleList = articleMapper.selectDraftListByCategoryId(categoryId);
        return Result.success(articleList);
    }

    @Override
    public Result getList(Integer categoryId) {
        if (categoryId == null) return Result.error("文章分类id不能为空");
        if (!isok(categoryId)) return Result.error("权限不足");
        List<Article> articleList = articleMapper.selectListByCategoryId(categoryId);
        return Result.success(articleList);
    }

    @Override
    public Result delete(Integer articleId) {
        if (articleId == null) return Result.error("文章id不能为空");
        Integer categoryId = articleMapper.selectById(articleId).getCategoryId();
        if (!isok(categoryId)) return Result.error("权限不足");
        articleMapper.deleteById(articleId);
        return Result.success();
    }

    @Override
    public Result getSelectedList(Map<String, Object> params) {
        String title = (String) params.get("title");
        String status = (String) params.get("status");
        Integer categoryId = Integer.parseInt((String) params.get("categoryId"));
        if (!isok(categoryId)) return Result.error("权限不足");
        List<String> statusList = new ArrayList<>();
        if (status != null&&!status.equals("")) statusList.add(status);
        else{
            statusList.add("published");
            statusList.add("draft");
            statusList.add("pending");
        }
        List<Article> articleList = articleMapper.selectListByCase(title, statusList, categoryId);
        return Result.success(articleList);
    }

    @Override
    public Result add(Article article) {
        if (article == null) return Result.error("文章不能为空");
        if (article.getTitle() == null) return Result.error("文章标题不能为空");
        if (article.getContent() == null) return Result.error("文章内容不能为空");
        if (!isok(article.getCategoryId())) return Result.error("权限不足");
        if (article.getStatus().equals("published")) article.setStatus("pending");
        articleMapper.add(article);
        return Result.success();
    }

    @Override
    public Result check(Integer categoryId) {
        if (categoryId == null) return Result.error("文章分类id不能为空");
        if (!isok(categoryId)) return Result.success(false);
        return Result.success(true);
    }

    @Override
    public Result getDetail(Integer articleId, Integer categoryId) {
        if (articleId == null) return Result.error("文章id不能为空");
        if (categoryId == null) return Result.error("文章分类id不能为空");
        Article article = articleMapper.selectById(articleId);
        if (!article.getStatus().equals("published")&&!isok(categoryId)) return Result.error("权限不足");
        return Result.success(article);
    }

    @Override
    public Result publish(Integer articleId, Integer categoryId) {
        if (articleId == null) return Result.error("文章id不能为空");
        if (categoryId == null) return Result.error("文章分类id不能为空");
        if (!isok(categoryId)) return Result.error("权限不足");
        articleMapper.publishById(articleId);
        return Result.success();
    }

    @Override
    public Result update(Article article) {
        if (article == null) return Result.error("文章不能为空");
        if (article.getTitle() == null) return Result.error("文章标题不能为空");
        if (article.getContent() == null) return Result.error("文章内容不能为空");
        if (!isok(article.getCategoryId())) return Result.error("权限不足");
        articleMapper.update(article);
        return Result.success();
    }

    @Override
    public Result updateCoverImage(Integer articleId, String coverImage, Integer categoryId) {
        if (articleId == null) return Result.error("文章id不能为空");
        if (coverImage == null) return Result.error("文章封面不能为空");
        if (!isok(categoryId)) return Result.error("权限不足");
        articleMapper.uploadCoverImage(articleId, coverImage);
        return Result.success();
    }


    Boolean isok(Integer categoryId){
        Map<String, Object> claims = ThreadLocalUtil.get();
        List<Category> categories = new ArrayList<>(categoryMapper.selectByUserId((Integer) claims.get("id")));
        for (Category category : categories) {
            if (category.getCategoryId().equals(categoryId)) return true;
        }
        return false;
    }
}

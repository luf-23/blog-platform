package com.blogplatform.backend.service.impl;

import com.blogplatform.backend.mapper.AdminMapper;
import com.blogplatform.backend.mapper.AnnouncementMapper;
import com.blogplatform.backend.mapper.ArticleMapper;
import com.blogplatform.backend.service.AdminService;
import com.blogplatform.backend.entity.Announcement;
import com.blogplatform.backend.entity.Article;
import com.blogplatform.backend.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {
    private static final String PUBLISHED = "published";
    private static final String DRAFT = "draft";
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private AnnouncementMapper announcementMapper;
    @Override
    public Result<List<Article>> getPendingList() {
        return Result.success(adminMapper.selectAllPendingArticles());
    }

    @Override
    public Result<List<Article>> getPublishedList() {
        return Result.success(adminMapper.selectAllPublishedArticles());
    }

    @Override
    public Result accept(Integer articleId) {
        adminMapper.updateArticleStatus(articleId, PUBLISHED);
        return Result.success();
    }

    @Override
    public Result reject(Integer articleId) {
        adminMapper.updateArticleStatus(articleId, DRAFT);
        return Result.success();
    }

    @Override
    public Result drop(Integer articleId) {
        adminMapper.updateArticleStatus(articleId, DRAFT);
        return Result.success();
    }

    @Override
    public Result<Article> getArticleDetail(Integer articleId) {
        return Result.success(articleMapper.selectById(articleId));
    }

    @Override
    public Result getUserList() {
        return Result.success(adminMapper.selectAllUser());
    }

    @Override
    public Result<List<Announcement>> getAnnouncement() {
        return Result.success(announcementMapper.selectAll());
    }

    @Override
    public Result deleteAnnouncement(Integer id) {
        announcementMapper.deleteById(id);
        return Result.success();
    }

    @Override
    public Result addAnnouncement(Announcement announcement) {
        announcementMapper.add(announcement);
        return Result.success();
    }
}

package com.blogplatform.backend.service;

import com.blogplatform.backend.entity.Announcement;
import com.blogplatform.backend.entity.Article;
import com.blogplatform.backend.entity.Result;

import java.util.List;

public interface AdminService {
    Result<List<Article>> getPendingList();

    Result<List<Article>> getPublishedList();

    Result accept(Integer articleId);

    Result reject(Integer articleId);

    Result drop(Integer articleId);

    Result<Article> getArticleDetail(Integer articleId);

    Result getUserList();

    Result<List<Announcement>> getAnnouncement();

    Result deleteAnnouncement(Integer id);

    Result addAnnouncement(Announcement announcement);
}

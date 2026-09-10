package com.blogplatform.backend.service;

import com.blogplatform.backend.entity.Article;
import com.blogplatform.backend.entity.Result;

import java.util.List;

public interface ArticleService {

    // Public discovery
    Result search(String keyword, Integer categoryId, Integer tagId,
                  Integer authorId, String sort, Integer page, Integer pageSize);

    Result getPublicDetail(Integer articleId);

    // My blog
    Result getMyArticles(String title, String status, Integer categoryId);

    Result add(Article article, List<String> tagNames);

    Result update(Article article, List<String> tagNames);

    Result delete(Integer articleId);

    Result submitForReview(Integer articleId);

    Result updateCoverImage(Integer articleId, String coverImage);

    // Admin
    Result adminSearch(String status, String keyword, Integer page, Integer pageSize);

    Result adminApprove(Integer articleId);

    Result adminReject(Integer articleId);
}

package com.blogplatform.backend.Service;

import com.blogplatform.backend.entity.Article;
import com.blogplatform.backend.entity.Result;

import java.util.Map;

public interface ArticleService {

    Result getPublishedList(Integer categoryId);

    Result getDraftList(Integer categoryId);

    Result getList(Integer categoryId);

    Result delete(Integer articleId);

    Result getSelectedList(Map<String, Object> params);

    Result add(Article article);

    Result check(Integer categoryId);

    Result getDetail(Integer articleId, Integer categoryId);

    Result publish(Integer articleId, Integer categoryId);

    Result update(Article article);

    Result updateCoverImage(Integer articleId, String coverImage,Integer categoryId);
}

package com.blogplatform.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleLike {
    private Integer id;
    private Integer articleId;
    private Integer userId;
    private LocalDateTime createTime;
}

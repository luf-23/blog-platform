package com.blogplatform.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Article {
    private Integer articleId;
    private Integer categoryId;
    private String title;
    private String content;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String coverImage;
}

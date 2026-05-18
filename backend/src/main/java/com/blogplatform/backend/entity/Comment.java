package com.blogplatform.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Comment {
    private Integer commentId;
    private Integer articleId;
    private Integer userId;//评论者id
    private String content;
    private Short isRead;
    private LocalDateTime createTime;
}

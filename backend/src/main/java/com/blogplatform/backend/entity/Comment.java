package com.blogplatform.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Comment {
    private Integer commentId;
    private Integer articleId;
    private Integer userId;
    private Integer parentId;
    private Integer rootId;
    private Integer replyToUserId;
    private String content;
    private Integer likeCount;
    private Integer status;
    private LocalDateTime createTime;
}

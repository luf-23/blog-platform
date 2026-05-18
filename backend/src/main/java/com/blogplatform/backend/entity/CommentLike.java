package com.blogplatform.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentLike {
    private Integer id;
    private Integer commentId;
    private Integer userId;
    private LocalDateTime createTime;
}

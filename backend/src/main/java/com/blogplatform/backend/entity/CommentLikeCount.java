package com.blogplatform.backend.entity;

import lombok.Data;

@Data
public class CommentLikeCount {
    private Integer commentId;
    private Integer likeCount;
}

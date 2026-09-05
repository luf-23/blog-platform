package com.blogplatform.backend.entity;

import lombok.Data;

@Data
public class CommentReplyCount {
    private Integer rootId;
    private Integer replyCount;
}

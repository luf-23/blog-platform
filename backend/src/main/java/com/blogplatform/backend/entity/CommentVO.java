package com.blogplatform.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CommentVO {
    private Integer commentId;
    private Integer articleId;
    private Integer userId;
    private Integer parentId;
    private Integer rootId;
    private String content;
    private LocalDateTime createTime;
    private String username;
    private String nickname;
    private String avatar;
    /** 被回复者（直接父评论的作者），一级评论为 null */
    private String replyToUsername;
    private String replyToNickname;
    private Integer likeCount;
    private Boolean isLiked;
    private List<CommentVO> children = new ArrayList<>();
}

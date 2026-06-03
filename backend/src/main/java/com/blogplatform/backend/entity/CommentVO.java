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
    private Integer replyToUserId;
    private String content;
    private LocalDateTime createTime;

    // Commenter info
    private String username;
    private String nickname;
    private String avatar;

    // Reply-to user info
    private String replyToUsername;
    private String replyToNickname;

    private Integer likeCount;
    private Boolean isLiked;

    private Integer replyCount;
    private List<CommentVO> children = new ArrayList<>();
}

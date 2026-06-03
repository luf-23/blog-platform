package com.blogplatform.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleVO {
    private Integer articleId;
    private Integer userId;
    private Integer categoryId;
    private String title;
    private String summary;
    private String content;
    private String coverImage;
    private String status;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // Author info
    private String authorUsername;
    private String authorNickname;
    private String authorAvatar;

    // Category info
    private String categoryName;

    // Tags
    private List<Tag> tags;

    // Current user state
    private Boolean isLiked;
}

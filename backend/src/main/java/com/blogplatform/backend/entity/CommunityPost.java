package com.blogplatform.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommunityPost {
    private Integer postId;
    private Integer userId;
    private String type;
    private String title;
    private String content;
    private String topic;
    private Boolean solved;
    private Integer likeCount;
    private Integer commentCount;
    private Integer voteCount;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private String authorUsername;
    private String authorNickname;
    private String authorAvatar;
    private String authorSignature;
    private List<CommunityPollOption> options;
}

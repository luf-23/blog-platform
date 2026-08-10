package com.blogplatform.backend.entity;

import lombok.Data;

@Data
public class CommunityPollOption {
    private Integer optionId;
    private Integer postId;
    private String optionText;
    private Integer voteCount;
}

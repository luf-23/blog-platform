package com.blogplatform.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Tag {
    private Integer tagId;
    private String tagName;
    private Integer parentId;
    private String parentName;
    private Integer sortOrder;
    private Integer articleCount;
    private LocalDateTime createTime;
}

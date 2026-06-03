package com.blogplatform.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Integer userId;
    @NotNull
    @NotEmpty
    private String username;
    @JsonIgnore
    private String password;
    private String nickname;
    private String signature;
    private String avatarImage;
    private String backgroundImage;
    private String email;
    private String role;
    private LocalDateTime lastLogin;
    private String lastLoginIp;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

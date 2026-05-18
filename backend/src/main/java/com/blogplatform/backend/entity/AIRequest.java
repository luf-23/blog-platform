package com.blogplatform.backend.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AIRequest {
    @NotNull
    private List<Message> messages;  // 历史消息列表

    private String model;           // 可选，默认模型

    private Double temperature;

    private String character;

    @Data
    public static class Message {
        @NotBlank
        private String role;       // "user" 或 "assistant" 或 "system"

        @NotBlank
        private String content;     // 消息内容

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
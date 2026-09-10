package com.blogplatform.backend.controller;

import jakarta.validation.Valid;
import com.blogplatform.backend.service.AiChatService;
import com.blogplatform.backend.entity.AIRequest;
import com.blogplatform.backend.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Set;

@RestController
@RequestMapping("/ai")
public class AiChatController {

    private static final Set<String> SUPPORTED_MODELS = Set.of(
            "deepseek-v3",
            "deepseek-r1",
            "qwq-plus",
            "qwen-max-2025-01-25"
    );

    @Autowired
    private AiChatService aiChatService;

    @PostMapping("/chat")
    public Result<String> chat(@RequestBody @Valid AIRequest request) {
        try {
            if (!SUPPORTED_MODELS.contains(request.getModel())) {
                return Result.error("不支持的模型: " + request.getModel());
            }
            String response = aiChatService.chatWithAI(request.getMessages(), request.getModel());
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("AI服务错误: " + e.getMessage());
        }
    }

    @PostMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody @Valid AIRequest request) {
        if (!SUPPORTED_MODELS.contains(request.getModel())) {
            throw new IllegalArgumentException("不支持的模型: " + request.getModel());
        }
        if (request.getTemperature()<0||request.getTemperature()>1) throw new IllegalArgumentException("temperature范围有误: " + request.getModel());
        if (request.getMessages() == null || request.getMessages().isEmpty()|| request.getMessages().get(0).getContent() == null) {
            throw new IllegalArgumentException("请提供有效的消息");
        }
        return aiChatService.handleStreamRequest(request);
    }
}

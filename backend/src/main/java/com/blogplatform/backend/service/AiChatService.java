package com.blogplatform.backend.service;

import jakarta.validation.Valid;
import com.blogplatform.backend.entity.AIRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AiChatService {

    String chatWithAI(List<AIRequest.Message> messages, String model);


    SseEmitter handleStreamRequest(@Valid AIRequest request);
}

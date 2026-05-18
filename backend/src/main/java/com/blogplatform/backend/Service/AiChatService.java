package com.blogplatform.backend.Service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.blogplatform.backend.entity.AIRequest;
import com.blogplatform.backend.entity.Result;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AiChatService {

    String chatWithAI(List<AIRequest.Message> messages, String model);


    SseEmitter handleStreamRequest(@Valid AIRequest request);
}

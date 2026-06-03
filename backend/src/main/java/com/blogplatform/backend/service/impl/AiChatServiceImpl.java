package com.blogplatform.backend.service.impl;

import com.blogplatform.backend.service.AiChatService;
import com.blogplatform.backend.entity.AIRequest;
import com.blogplatform.backend.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiChatServiceImpl implements AiChatService {

    private static final String DEFAULT_MODEL = "deepseek-v3";
    @Value("${ai.key}")
    private String API_KEY;
    @Value("${ai.url}")
    private String API_URL;
    @Value("${ai.character.type1}")
    private String CHARACTER_PATH;
    private String CHARACTER;

    //流式
    private final WebClient webClient;
    //普通
    private final RestTemplate restTemplate;

    @Autowired
    private ResourceLoader resourceLoader;


    public AiChatServiceImpl(RestTemplate restTemplate,WebClient aiWebClient) {
        this.restTemplate = restTemplate;
        this.webClient = aiWebClient;
    }

    @Override
    public String chatWithAI(List<AIRequest.Message> messages, String model) {
        // 1. 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + API_KEY);

        // 2. 构建符合DashScope要求的请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model != null ? model : DEFAULT_MODEL);

        // 转换消息格式
        List<Map<String, String>> apiMessages = messages.stream()
                .map(msg -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("role", msg.getRole());
                    m.put("content", msg.getContent());
                    return m;
                })
                .collect(Collectors.toList());

        requestBody.put("messages", apiMessages);
        requestBody.put("temperature", 0.4);

        // 3. 发送请求
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                API_URL,
                HttpMethod.POST,
                requestEntity,
                Map.class
        );

        // 4. 解析响应
        // 修改后的解析逻辑
        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                // 安全类型转换
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    return (String) message.get("content"); // 提取助手的回复内容
                }
            }
        }
        throw new RuntimeException("无法解析AI响应: " + response.getBody());
    }

    @Override
    public SseEmitter handleStreamRequest(AIRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L); // 3分钟超时
        //获取角色
        try {
            Resource resource = resourceLoader.getResource(CHARACTER_PATH); // 加载资源
            if (resource.exists()) {
                // 读取文件内容并赋值给 CHARACTER
                CHARACTER = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            } else {
                throw new RuntimeException("文件不存在: " + CHARACTER_PATH);
            }
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + CHARACTER_PATH, e);
        }
        //System.out.println(CHARACTER);
        if (isAdmin()) request.getMessages().addFirst(new AIRequest.Message("system", CHARACTER));
        else{
            if (request.getCharacter() != null && !request.getCharacter().isEmpty()){
                request.getMessages().addFirst(new AIRequest.Message("system", request.getCharacter()));
            }
        }

        // 构建AI请求体
        Map<String, Object> requestBody = Map.of(
                "model", request.getModel(),
                "messages", request.getMessages(),
                "stream", true,
                "temperature", request.getTemperature()
        );

        // 发送流式请求
        Flux<String> responseFlux = webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class);

        // 处理流式响应
        responseFlux.subscribe(
                chunk -> processChunk(chunk, emitter),
                emitter::completeWithError,
                emitter::complete
        );

        return emitter;
    }
    private void processChunk(String chunk, SseEmitter emitter) {
        try {
            // 直接通过 emitter 发送原始 chunk 数据
            emitter.send(chunk);
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private boolean isAdmin() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        String username = (String) claims.get("username");
        if (!username.equals("admin")) return false;
        return true;
    }
}
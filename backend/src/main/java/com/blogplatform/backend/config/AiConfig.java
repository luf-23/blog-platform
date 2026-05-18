package com.blogplatform.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AiConfig {

    @Value("${ai.key}")
    private String API_KEY;
    @Value("${ai.url}")
    private String API_URL;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }


    @Bean
    public WebClient aiWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(API_URL)
                .defaultHeader("Authorization", "Bearer " + API_KEY)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
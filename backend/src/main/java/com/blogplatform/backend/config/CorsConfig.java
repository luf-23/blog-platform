package com.blogplatform.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    //跨域配置
    @Bean
    public CorsFilter corsFilter() {
        // 添加CORS配置信息
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://luf.woyioii.cn/");
        config.addAllowedOrigin("http://localhost:5173/");
        config.addAllowedOrigin("http://localhost:5174/");
        config.addAllowedOrigin("http://192.168.159.105:5173/");
        config.addAllowedOrigin("http://43.142.2.253/");
        config.addAllowedOrigin("http://www.luf-23.cn/");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(1800L); // 预检请求的缓存时间（秒）
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
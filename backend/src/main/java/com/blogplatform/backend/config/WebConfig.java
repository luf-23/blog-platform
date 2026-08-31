package com.blogplatform.backend.config;

import com.blogplatform.backend.interceptors.LoginInterceptor;
import com.blogplatform.backend.interceptors.OptionalLoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Autowired
    private OptionalLoginInterceptor optionalLoginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(optionalLoginInterceptor)
                .addPathPatterns("/community/profile/**")
                .order(0);

        registry.addInterceptor(loginInterceptor)
                .order(1)
                .excludePathPatterns(
                        // Auth
                        "/user/login",
                        "/user/register",
                        "/user/refreshToken",
                        "/user/captcha",
                        "/user/verify",
                        "/user/reset-password",
                        "/user/getInfoByName",
                        "/user/getUserInfoByName",
                        // Public read APIs
                        "/article/search",
                        "/article/detail/**",
                        "/tag/all",
                        "/tag/popular",
                        "/category/byUser/**",
                        "/comment/list",
                        "/comment/replies",
                        "/community/feed",
                        "/community/meta",
                        "/community/profile/**",
                        "/announcement",
                        "/announcement/**"
                );
    }
}

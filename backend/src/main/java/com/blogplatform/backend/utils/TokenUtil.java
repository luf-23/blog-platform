package com.blogplatform.backend.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TokenUtil {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "expired_refresh_token:";

    private static final long EXPIRE_TIME = JwtUtil.REFRESH_EXPIRE_TIME/1000;

    public void add(String jti) {
        redisTemplate.opsForValue().set(KEY_PREFIX+jti,"",EXPIRE_TIME, TimeUnit.SECONDS);
    }

    public Boolean exist(String jti){
        return redisTemplate.hasKey(KEY_PREFIX+jti);
    }




}

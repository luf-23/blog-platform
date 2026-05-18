package com.blogplatform.backend.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.util.Date;
import java.util.Map;

public class JwtUtil {

    private static final String KEY = "this_is_a_key";

    public static final long ACCESS_EXPIRE_TIME = 1000 * 60 * 60;//1h
    //public static final long ACCESS_EXPIRE_TIME = 1000 * 6;//6s
    public static final long REFRESH_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 14;//14d
    //public static final long REFRESH_EXPIRE_TIME = 1000 * 60;//1m
    //接收业务数据,生成token并返回
    public static String genAccessToken(Map<String, Object> claims) {
        return JWT.create()
                .withClaim("claims", claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + ACCESS_EXPIRE_TIME))
                .sign(Algorithm.HMAC256(KEY));
    }

    public static String genRefreshToken(Map<String, Object> claims) {
        return JWT.create()
                .withClaim("claims", claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + REFRESH_EXPIRE_TIME))
                .sign(Algorithm.HMAC256(KEY));
    }

    //接收token,验证token,并返回业务数据
    public static Map<String, Object> parseToken(String token) {
        return JWT.require(Algorithm.HMAC256(KEY))
                .build()
                .verify(token)
                .getClaim("claims")
                .asMap();
    }

}

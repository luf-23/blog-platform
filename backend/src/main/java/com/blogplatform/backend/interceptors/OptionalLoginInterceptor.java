package com.blogplatform.backend.interceptors;

import com.blogplatform.backend.utils.JwtUtil;
import com.blogplatform.backend.utils.ThreadLocalUtil;
import com.blogplatform.backend.utils.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * Parses a login token for public endpoints without making login mandatory.
 */
@Component
public class OptionalLoginInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenUtil tokenUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isBlank()) return true;

        try {
            Map<String, Object> claims = JwtUtil.parseToken(token);
            String jti = (String) claims.get("jti");
            if (jti != null && !tokenUtil.exist(jti)) ThreadLocalUtil.set(claims);
        } catch (Exception ignored) {
            // Public reads remain available when the optional token is invalid.
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        ThreadLocalUtil.remove();
    }
}

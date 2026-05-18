package com.blogplatform.backend.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.blogplatform.backend.utils.JwtUtil;
import com.blogplatform.backend.utils.ThreadLocalUtil;
import com.blogplatform.backend.utils.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
// 登录拦截器
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenUtil tokenUtil;

    // 请求处理之前执行
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        try {
            Map<String,Object> claims = JwtUtil.parseToken(token);
            String jti = (String) claims.get("jti");
            // 查询redis 黑名单是否存在此jti
            if (tokenUtil.exist(jti)){
                response.setStatus(401);
                return false;
            }

            ThreadLocalUtil.set(claims);
            return true;
        }catch(Exception e){
            //System.out.println("token error");
            response.setStatus(401);
            return false;
        }
    }
    // 请求处理完毕之后执行
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        ThreadLocalUtil.remove();
    }
}

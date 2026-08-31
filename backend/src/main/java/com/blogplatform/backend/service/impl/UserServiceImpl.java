package com.blogplatform.backend.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.blogplatform.backend.mapper.UserMapper;
import com.blogplatform.backend.service.UserService;
import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.entity.User;
import com.blogplatform.backend.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private IpLocationUtil ipLocationUtil;
    @Autowired
    private IP2RegionUtil ip2RegionUtil;
    @Autowired
    private SMTPUtil smtpUtil;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private TokenUtil tokenUtil;
    @Autowired
    private PasswordUtil passwordUtil;

    @Override
    public Result login(String usernameOrEmail, String password, HttpServletRequest request, HttpServletResponse response) {
        if (usernameOrEmail == null || password == null) return Result.error("用户名或密码不能为空");
        User user;
        if (usernameOrEmail.matches("^\\S{5,16}$")) user = userMapper.selectByUsername(usernameOrEmail);
        else if (usernameOrEmail.matches("^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")) user = userMapper.selectByEmail(usernameOrEmail);
        else return Result.error("用户名或邮箱格式错误");
        if (user == null) return Result.error("用户名不存在");
        if (!passwordUtil.matches(password, user.getPassword())) return Result.error("密码错误");

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getUserId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole() != null ? user.getRole() : "user");
        claims.put("jti", UUID.randomUUID().toString().replace("-", ""));

        String accessToken = JwtUtil.genAccessToken(claims);
        String refreshToken = JwtUtil.genRefreshToken(claims);

        clearLegacyRefreshCookie(response);
        response.addHeader("Set-Cookie", cookieHeaderBuilder(refreshToken));

        System.out.println("LoginInfo:"+user);
        userMapper.updateLoginTime(user.getUserId());
        userMapper.updateLoginIp(user.getUserId(),ip2RegionUtil.getDetailedAddress(ipLocationUtil.getClientIP(request)).toString());
        return Result.success(accessToken);
    }

    @Override
    public Result register(String username, String password,String  email) {
        if (username == null || password == null) return Result.error("用户名或密码不能为空");
        User user = userMapper.selectByUsername(username);
        if (user != null) return Result.error("用户名已存在");
        if (userMapper.selectAllEmails().contains( email)) return Result.error("邮箱已存在");
        userMapper.add(username, passwordUtil.encode(password), username, email);
        redisTemplate.delete("email:captcha:"+email);
        return Result.success();
    }

    @Override
    public Result getInfo() {
        Integer id = (Integer) ((Map<String,Object>)ThreadLocalUtil.get()).get("id");
        if(id== null){
            return Result.error("用户未登录");
        }
        User user = userMapper.selectById(id);
        return Result.success(user);
    }

    @Override
    public Result update(User user) {
        if (user == null) return Result.error("用户信息不能为空");
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = claims == null ? null : (Integer) claims.get("id");
        if (userId == null) return Result.error("用户未登录");
        User existing = userMapper.selectById(userId);
        if (existing == null) return Result.error("用户不存在");
        user.setUserId(userId);
        user.setUsername(existing.getUsername());
        if (user.getNickname() == null || user.getNickname().isBlank()) user.setNickname(existing.getNickname());
        if (user.getSignature() == null) user.setSignature(existing.getSignature());
        if (user.getAvatarImage() == null || user.getAvatarImage().isBlank()) user.setAvatarImage(existing.getAvatarImage());
        if (user.getBackgroundImage() == null || user.getBackgroundImage().isBlank()) user.setBackgroundImage(existing.getBackgroundImage());
        userMapper.update(user);
        return Result.success();
    }

    @Override
    public Result getInfoByName(String username) {
        if (username == null) return Result.error("用户名不能为空");
        User user = userMapper.selectByUsername(username);
        return Result.success(user);
    }

    @Override
    public Result captcha(String email) {
        smtpUtil.sendCaptcha(email);
        return Result.success();
    }

    @Override
    public Result verifyCaptcha(String email, String captcha) {
        boolean result = smtpUtil.verifyCaptcha(email, captcha);
        return Result.success(result);
    }

    @Override
    public Result resetPassword(String email,String newPassword) {
        if (userMapper.selectByEmail( email)== null) return Result.error("该邮箱未注册");
        userMapper.resetPassword(email, passwordUtil.encode(newPassword));
        redisTemplate.delete("email:captcha:"+email);
        return Result.success();
    }

    @Override
    public Result getInfoById(Integer id) {
        User user = userMapper.selectById(id);
        if (user==null) return Result.error("用户不存在");
        return Result.success(user);
    }

    @Override
    public Result getUserInfoByName(String username) {
        if (username == null) return Result.error("用户名不能为空");
        User user = userMapper.selectByUsername(username);
        return Result.success(user);
    }

    @Override
    public Result refresh(String refreshToken, HttpServletResponse response) {
        Map<String, Object> claims;
        try {
            claims = JwtUtil.parseToken(refreshToken);
        } catch (Exception e) {
            response.setStatus(401);
            return Result.error("登录过期");
        }

        String jti = (String) claims.get("jti");
        tokenUtil.add(jti);

        String newJti = UUID.randomUUID().toString().replace("-", "");
        claims.put("jti", newJti);

        String accessToken = JwtUtil.genAccessToken(claims);
        String newRefreshToken = JwtUtil.genRefreshToken(claims);
        clearLegacyRefreshCookie(response);
        response.addHeader("Set-Cookie", cookieHeaderBuilder(newRefreshToken));
        return Result.success(accessToken);
    }

    @Override
    public Result logout(HttpServletResponse response) {
        Map<String, Object> claims = ThreadLocalUtil.get();

        // jti add to black list
        tokenUtil.add((String) claims.get("jti"));
        response.addHeader("Set-Cookie", clearCookieHeader("/"));
        clearLegacyRefreshCookie(response);
        return Result.success();
    }

    private String cookieHeaderBuilder(String refreshToken){
        Integer expire = Integer.valueOf((int) JwtUtil.REFRESH_EXPIRE_TIME / 1000);
        StringBuilder cookieBuilder = new StringBuilder();
        cookieBuilder.append("refreshToken=").append(refreshToken);
        cookieBuilder.append("; Path=/");
        cookieBuilder.append("; HttpOnly");  // 防止XSS攻击
        cookieBuilder.append("; Max-Age=").append(expire);  // 有效期
        cookieBuilder.append("; SameSite=Lax");  // 允许跨站
        return cookieBuilder.toString();
    }

    private void clearLegacyRefreshCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie", clearCookieHeader("/user/refreshToken"));
    }

    private String clearCookieHeader(String path) {
        return "refreshToken=; Path=" + path + "; HttpOnly; Max-Age=0; SameSite=Lax";
    }

}

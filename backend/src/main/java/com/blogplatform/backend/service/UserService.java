package com.blogplatform.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Pattern;
import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.entity.User;

public interface UserService {
    Result login(String usernameOrEmail, String password, HttpServletRequest request, HttpServletResponse response);

    Result register(String username, String password,String email);

    Result getInfo();

    Result update(User user);

    Result getInfoByName(String username);

    Result captcha(String email);

    Result verifyCaptcha(String email, String captcha);

    Result resetPassword(@Pattern(regexp = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$") String email,@Pattern(regexp = "^\\S{5,16}$") String newPassword);

    Result getInfoById(Integer id);

    Result getUserInfoByName(@Pattern(regexp = "^\\S{5,16}$") String username);

    Result refresh(String refreshToken,HttpServletResponse response);

    Result logout(HttpServletResponse response);
}

package com.blogplatform.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Pattern;
import com.blogplatform.backend.service.UserService;
import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/user")
@Validated
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result login(@RequestParam @Pattern(regexp = "(^\\S{5,16}$)|(^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$)") String usernameOrEmail, @RequestParam @Pattern(regexp = "^\\S{5,16}$") String password, HttpServletRequest request, HttpServletResponse  response){
        return userService.login(usernameOrEmail, password,request,response);
    }
    @PostMapping("/register")
    public Result register(@RequestParam @Pattern(regexp = "^\\S{5,16}$") String username,@RequestParam @Pattern(regexp = "^\\S{5,16}$") String password,@RequestParam @Pattern(regexp = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$") String email) {
        if (username.contains("@")) return Result.error("用户名不能包含@");
        return userService.register(username, password,email);
    }

    @PostMapping("/reset-password")
    public Result resetPassword(@RequestParam @Pattern(regexp = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$") String email,@RequestParam @Pattern(regexp = "^\\S{5,16}$") String newPassword){
        return userService.resetPassword(email, newPassword);
    }

    @GetMapping("/getInfo")
    public Result getInfo(){
        return userService.getInfo();
    }
    @PostMapping("/update")
    public Result update(@RequestBody User user){
        return userService.update(user);
    }

    @GetMapping("/getInfoByName")
    public Result getInfoByName(@RequestParam @Pattern(regexp = "^\\S{5,16}$") String username){
        return userService.getInfoByName(username);
    }
    @GetMapping("/getInfoById")
    public Result getInfoById(@RequestParam Integer id){
        return userService.getInfoById(id);
    }

    @PostMapping("/captcha")
    public Result captcha(@RequestParam String email){
        return userService.captcha(email);
    }

    @PostMapping("/verify")
    public Result verifyCaptcha(@RequestParam String email,@RequestParam String captcha){
        return userService.verifyCaptcha(email, captcha);
    }

    //这个为安卓调用的接口，没带token过滤器也不拦截
    @GetMapping("/getUserInfoByName")
    public Result getUserInfoByName(@RequestParam @Pattern(regexp = "^\\S{5,16}$") String username){
        return userService.getUserInfoByName(username);
    }

    @GetMapping("/refreshToken")
    public Result refreshToken(@CookieValue(name = "refreshToken") String refreshToken,HttpServletResponse response){
        return userService.refresh(refreshToken,response);
    }
    @PostMapping("/logout")
    public Result logout(HttpServletResponse response){
        return userService.logout(response);
    }
}

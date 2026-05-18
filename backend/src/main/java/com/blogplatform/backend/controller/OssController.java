package com.blogplatform.backend.controller;

import com.blogplatform.backend.entity.Result;
import com.blogplatform.backend.Service.OssService;
import com.blogplatform.backend.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/oss")
public class OssController {

    @Autowired
    private OssService ossService;

    // 获取上传凭证
    @GetMapping("/credentials")
    public Result<?> getCredentials() {
        // 假设从认证信息中获取当前用户ID
        Integer userId = getCurrentUserId();
        return ossService.generateCredentials(userId);
    }

    @GetMapping("/bucket")
    public Result<String> getBucket() {
        return ossService.getBucket();
    }

    @GetMapping("/region")
    public Result<String> getRegion() {
        return ossService.getRegion();
    }

    @GetMapping("/endPoint")
    public Result<String> getEndPoint() {
        return ossService.getEndPoint();
    }

    // 模拟获取当前用户ID
    private Integer getCurrentUserId() {
        Map<String, Object> claims =  ThreadLocalUtil.get();
        return (Integer) claims.get("id");
    }
}
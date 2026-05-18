package com.blogplatform.backend.Service.Impl;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.auth.sts.AssumeRoleRequest;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.blogplatform.backend.Service.OssService;
import com.blogplatform.backend.config.OssConfig;
import com.blogplatform.backend.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class OssServiceImpl  implements OssService {

    @Autowired
    private OssConfig ossConfig;

    // 生成临时凭证
    public Result<AssumeRoleResponse.Credentials> generateCredentials(Integer userId) {
        try {
            // 读取权限策略文件
            String policy = StreamUtils.copyToString(
                    new ClassPathResource(ossConfig.getPolicyFile()).getInputStream(),
                    StandardCharsets.UTF_8
            );

            // 创建 STS 客户端
            IClientProfile profile = DefaultProfile.getProfile(ossConfig.getRegion(), ossConfig.getAccessKeyId(), ossConfig.getAccessKeySecret());
            DefaultAcsClient client = new DefaultAcsClient(profile);

            // 构造请求
            AssumeRoleRequest request = new AssumeRoleRequest();
            request.setRoleArn(ossConfig.getRoleArn());
            request.setRoleSessionName("user-" + userId);
            request.setPolicy(policy);
            request.setDurationSeconds(ossConfig.getExpireTime());

            // 获取临时凭证
            AssumeRoleResponse response = client.getAcsResponse(request);
            return Result.success(response.getCredentials());
        } catch (Exception e) {
            return Result.error("生成临时凭证失败：" + e.getMessage());
        }
    }

    @Override
    public Result<String> getBucket() {
        return Result.success(ossConfig.getBucket());
    }

    @Override
    public Result<String> getRegion() {
        return Result.success(ossConfig.getRegion());
    }

    @Override
    public Result<String> getEndPoint() {
        return Result.success(ossConfig.getEndpoint());
    }


}
package com.blogplatform.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
@Data
public class OssConfig {
    private String endpoint;
    private String bucket;
    private String roleArn;
    private String policyFile;
    private String accessKeyId;
    private String accessKeySecret;
    private String region;
    private Long expireTime;
}
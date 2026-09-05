package com.blogplatform.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class OssConfigTest {

    @Test
    void bindsUploadConfigurationToGeneratedAccessors() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestConfiguration.class)
                .withPropertyValues(
                        "aliyun.oss.endpoint=https://oss-cn-test.aliyuncs.com",
                        "aliyun.oss.bucket=test-bucket",
                        "aliyun.oss.role-arn=test-role",
                        "aliyun.oss.policy-file=oss-policy.json",
                        "aliyun.oss.access-key-id=test-key",
                        "aliyun.oss.access-key-secret=test-secret",
                        "aliyun.oss.region=cn-test",
                        "aliyun.oss.expire-time=900")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    OssConfig config = context.getBean(OssConfig.class);
                    assertThat(config.getPolicyFile()).isEqualTo("oss-policy.json");
                    assertThat(config.getEndpoint()).isEqualTo("https://oss-cn-test.aliyuncs.com");
                    assertThat(config.getBucket()).isEqualTo("test-bucket");
                    assertThat(config.getRoleArn()).isEqualTo("test-role");
                    assertThat(config.getAccessKeyId()).isEqualTo("test-key");
                    assertThat(config.getAccessKeySecret()).isEqualTo("test-secret");
                    assertThat(config.getRegion()).isEqualTo("cn-test");
                    assertThat(config.getExpireTime()).isEqualTo(900L);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(OssConfig.class)
    static class TestConfiguration {
    }
}

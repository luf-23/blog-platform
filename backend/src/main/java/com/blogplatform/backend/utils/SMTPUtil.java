package com.blogplatform.backend.utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
public class SMTPUtil {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // 验证码过期时间（分钟）
    private static final long EXPIRE_MINUTES = 5;
    // Redis key 前缀
    private static final String CAPTCHA_KEY_PREFIX = "email:captcha:";

    /**
     * 生成6位随机验证码
     */
    private String generateCaptcha() {
        Random random = new Random();
        StringBuilder captcha = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            captcha.append(random.nextInt(10));
        }
        return captcha.toString();
    }

    /**
     * 发送验证码邮件
     * @param toEmail 目标邮箱
     * @return 生成的验证码
     */
    public boolean sendCaptcha(String toEmail) {
        try {
            String captcha = generateCaptcha();

            // 存储验证码到Redis
            String key = CAPTCHA_KEY_PREFIX + toEmail;
            redisTemplate.opsForValue().set(key, captcha, EXPIRE_MINUTES, TimeUnit.MINUTES);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("验证码");
            message.setText("您的验证码是：" + captcha + "，" + EXPIRE_MINUTES + "分钟内有效。请勿泄露给他人。");

            mailSender.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 验证验证码
     * @param email 邮箱
     * @param captcha 验证码
     * @return 是否验证通过
     */
    public boolean verifyCaptcha(String email, String captcha) {
        String key = CAPTCHA_KEY_PREFIX + email;
        String savedCaptcha = redisTemplate.opsForValue().get(key);
        if (savedCaptcha != null && savedCaptcha.equals(captcha)) {
            // 不能验证通过后删除验证码，因为如果验证是通过的但是前端调用的register接口是失败的（如邮箱已存在或数据库插入失败），那么验证码就无法再次使用
            return true;
        }
        return false;
    }
}

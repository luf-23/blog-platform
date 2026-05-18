package com.blogplatform.backend.utils;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

  /** 与 init.sql 演示账号密码哈希一致 */
  private static final String DEMO_HASH =
      "$2b$10$I/Me9zozCbEwd0Tlkd9SQuLOqDGpQ6yJWc5pcCIxXG8P/F222D3H2";

  @Test
  void demoPasswordMatchesBcryptHash() {
    assertTrue(new BCryptPasswordEncoder().matches("123456", DEMO_HASH));
  }

  @Test
  void passwordUtilMatchesBcryptOnly() {
    PasswordUtil util = new PasswordUtil();
    assertTrue(util.matches("123456", DEMO_HASH));
    assertFalse(util.matches("wrong", DEMO_HASH));
    assertFalse(util.matches("123456", "e10adc3949ba59abbe56e057f20f883e"));
  }
}

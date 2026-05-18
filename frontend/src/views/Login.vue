<script setup>
import { reactive, ref, computed, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import {
  User,
  Lock,
  Message,
  Key,
  ChatDotSquare,
  MagicStick,
  Document,
  Promotion
} from "@element-plus/icons-vue";

import {
  loginService,
  registerService,
  getUserInfoService,
  sendEmailCaptchaService,
  checkEmailCaptchaService,
  resetPasswordService
} from "../api/user.js";
import { useTokenStore } from "../store/token.js";
import { useUserInfoStore } from "../store/userInfo.js";

const router = useRouter();
const tokenStore = useTokenStore();
const userInfoStore = useUserInfoStore();

const mode = ref("login");
const loading = ref(false);
const captchaLoading = ref(false);
const countdown = ref(0);
let countdownTimer = null;

const formRef = ref(null);
const formData = reactive({
  username: "",
  password: "",
  confirmPassword: "",
  email: "",
  captcha: "",
  newPassword: ""
});

const rules = reactive({
  username: [
    { required: true, message: "请输入用户名/邮箱", trigger: "blur" },
    {
      validator: (_, value, callback) => {
        const emailPattern = /^[\w.+-]+@[\w-]+\.[a-zA-Z]{2,}$/;
        const usernamePattern = /^.{5,16}$/;
        if (mode.value === "login") {
          return emailPattern.test(value) || usernamePattern.test(value)
            ? callback()
            : callback(new Error("请输入合法的用户名(5-16位)或邮箱"));
        }
        if (value.includes("@")) return callback(new Error("用户名不能包含 @"));
        return usernamePattern.test(value)
          ? callback()
          : callback(new Error("用户名长度需在 5-16 字符"));
      },
      trigger: "blur"
    }
  ],
  email: [
    { required: true, message: "请输入邮箱", trigger: "blur" },
    {
      pattern: /^[\w.+-]+@[\w-]+\.[a-zA-Z]{2,}$/,
      message: "邮箱格式不正确",
      trigger: "blur"
    }
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 5, max: 16, message: "长度为 5-16 个字符", trigger: "blur" }
  ],
  confirmPassword: [
    { required: true, message: "请再次输入密码", trigger: "blur" },
    {
      validator: (_, value, callback) => {
        if (value !== formData.password) {
          callback(new Error("两次密码不一致"));
        } else {
          callback();
        }
      },
      trigger: "blur"
    }
  ],
  newPassword: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    { min: 5, max: 16, message: "长度为 5-16 个字符", trigger: "blur" }
  ],
  captcha: [{ required: true, message: "请输入验证码", trigger: "blur" }]
});

const title = computed(() => {
  if (mode.value === "login") return "登录到你的账号";
  if (mode.value === "register") return "创建新账户";
  return "重置账户密码";
});

const subtitle = computed(() => {
  if (mode.value === "login") return "继续探索分享和创造的乐趣";
  if (mode.value === "register") return "加入这片小小的写作社区";
  return "通过邮箱验证码重新设置密码";
});

function switchMode(target) {
  mode.value = target;
  Object.assign(formData, {
    username: "",
    password: "",
    confirmPassword: "",
    email: "",
    captcha: "",
    newPassword: ""
  });
  formRef.value?.clearValidate?.();
}

function startCountdown() {
  countdown.value = 60;
  countdownTimer = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value -= 1;
    } else {
      clearInterval(countdownTimer);
    }
  }, 1000);
}

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer);
});

async function sendCaptcha() {
  try {
    await formRef.value.validateField("email");
  } catch {
    ElMessage.error("请先填写正确的邮箱");
    return;
  }
  if (countdown.value > 0) return;
  captchaLoading.value = true;
  try {
    await sendEmailCaptchaService({ email: formData.email });
    ElMessage.success("验证码已发送，请查收邮箱");
    startCountdown();
  } catch {
    ElMessage.error("验证码发送失败");
  } finally {
    captchaLoading.value = false;
  }
}

async function handleLogin() {
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  loading.value = true;
  try {
    const result = await loginService({
      usernameOrEmail: formData.username,
      password: formData.password
    });
    tokenStore.setToken(result.data);
    const res = await getUserInfoService();
    userInfoStore.setUserInfo(res.data);
    ElMessage.success("登录成功");
    const redirect = router.currentRoute.value.query.redirect || "/home";
    router.push(redirect);
  } finally {
    loading.value = false;
  }
}

async function handleRegister() {
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  loading.value = true;
  try {
    const verify = await checkEmailCaptchaService({
      email: formData.email,
      captcha: formData.captcha
    });
    if (!verify.data) {
      ElMessage.error("验证码错误或已过期");
      return;
    }
    const result = await registerService({
      username: formData.username,
      password: formData.password,
      email: formData.email
    });
    if (result.code === 0) {
      ElMessage.success("注册成功，请登录");
      switchMode("login");
    } else {
      ElMessage.error(result.msg || "注册失败");
    }
  } finally {
    loading.value = false;
  }
}

async function handleResetPassword() {
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  loading.value = true;
  try {
    const verify = await checkEmailCaptchaService({
      email: formData.email,
      captcha: formData.captcha
    });
    if (!verify.data) {
      ElMessage.error("验证码错误或已过期");
      return;
    }
    await resetPasswordService({
      email: formData.email,
      newPassword: formData.newPassword
    });
    ElMessage.success("密码重置成功，请登录");
    switchMode("login");
  } finally {
    loading.value = false;
  }
}

function submit() {
  if (mode.value === "login") return handleLogin();
  if (mode.value === "register") return handleRegister();
  return handleResetPassword();
}

const features = [
  { icon: Document, title: "随心写作", desc: "Markdown 编辑、自动保存草稿" },
  { icon: ChatDotSquare, title: "社区互动", desc: "评论、点赞、关注感兴趣的作者" },
  { icon: MagicStick, title: "AI 助手", desc: "内置多模型对话，写作灵感不断" }
];
</script>

<template>
  <div class="login-wrap">
    <section class="login-side login-side--hero">
      <div class="hero-badge">
        <el-icon><Promotion /></el-icon>
        <span>欢迎来到 Blog Platform</span>
      </div>
      <h1 class="hero-title">
        让灵感<span class="bp-gradient-text">流动</span>，<br />让思想被看见。
      </h1>
      <p class="hero-subtitle">
        一个轻盈、现代、专注内容创作的全栈博客平台。
        支持 Markdown、社区评论、AI 助手与管理后台。
      </p>
      <ul class="hero-features">
        <li v-for="f in features" :key="f.title">
          <span class="hero-features__icon">
            <el-icon><component :is="f.icon" /></el-icon>
          </span>
          <div>
            <strong>{{ f.title }}</strong>
            <p>{{ f.desc }}</p>
          </div>
        </li>
      </ul>
    </section>

    <section class="login-side login-side--form">
      <div class="login-card bp-card">
        <header class="login-card__header">
          <h2>{{ title }}</h2>
          <p>{{ subtitle }}</p>
        </header>

        <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-position="top"
          class="login-form"
          @keyup.enter="submit"
        >
          <el-form-item
            v-if="mode !== 'forgot'"
            prop="username"
            :label="mode === 'login' ? '用户名 / 邮箱' : '用户名'"
          >
            <el-input
              v-model="formData.username"
              size="large"
              :prefix-icon="User"
              :placeholder="mode === 'login' ? '账号或邮箱' : '5-16 位用户名'"
            />
          </el-form-item>

          <el-form-item v-if="mode !== 'login'" prop="email" label="邮箱">
            <el-input
              v-model="formData.email"
              size="large"
              :prefix-icon="Message"
              placeholder="请输入邮箱"
            />
          </el-form-item>

          <el-form-item v-if="mode !== 'login'" prop="captcha" label="验证码">
            <div class="captcha-row">
              <el-input
                v-model="formData.captcha"
                size="large"
                :prefix-icon="Key"
                placeholder="邮箱验证码"
              />
              <el-button
                size="large"
                :loading="captchaLoading"
                :disabled="countdown > 0"
                @click="sendCaptcha"
              >
                {{ countdown > 0 ? `${countdown}s` : "获取" }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item v-if="mode !== 'forgot'" prop="password" label="密码">
            <el-input
              v-model="formData.password"
              size="large"
              :prefix-icon="Lock"
              type="password"
              show-password
              placeholder="请输入密码"
            />
          </el-form-item>

          <el-form-item
            v-if="mode === 'register'"
            prop="confirmPassword"
            label="确认密码"
          >
            <el-input
              v-model="formData.confirmPassword"
              size="large"
              :prefix-icon="Lock"
              type="password"
              show-password
              placeholder="请再次输入密码"
            />
          </el-form-item>

          <el-form-item v-if="mode === 'forgot'" prop="newPassword" label="新密码">
            <el-input
              v-model="formData.newPassword"
              size="large"
              :prefix-icon="Lock"
              type="password"
              show-password
              placeholder="请输入新密码"
            />
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            class="login-submit"
            :loading="loading"
            @click="submit"
          >
            <template v-if="mode === 'login'">登录</template>
            <template v-else-if="mode === 'register'">立即注册</template>
            <template v-else>重置密码</template>
          </el-button>
        </el-form>

        <footer class="login-card__footer">
          <template v-if="mode === 'login'">
            <span>还没有账号？</span>
            <el-button link type="primary" @click="switchMode('register')">
              立即注册
            </el-button>
            <span class="dot">·</span>
            <el-button link @click="switchMode('forgot')">忘记密码？</el-button>
          </template>
          <template v-else>
            <span>已有账号？</span>
            <el-button link type="primary" @click="switchMode('login')">
              返回登录
            </el-button>
          </template>
        </footer>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-wrap {
  width: min(1080px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
  align-items: stretch;
}

.login-side {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-side--hero {
  padding: 32px 8px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--bp-color-primary-soft);
  color: var(--bp-color-primary);
  font-size: 12px;
  margin-bottom: 16px;
  width: fit-content;
}

.hero-title {
  font-size: clamp(28px, 4vw, 40px);
  line-height: 1.2;
  margin: 0 0 16px 0;
}

.hero-subtitle {
  color: var(--bp-color-text-secondary);
  font-size: 15px;
  line-height: 1.7;
  margin: 0 0 28px 0;
  max-width: 460px;
}

.hero-features {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hero-features li {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 16px;
  background: var(--bp-color-bg-elevated);
  border: 1px solid var(--bp-color-border);
}

.hero-features__icon {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bp-color-primary-soft);
  color: var(--bp-color-primary);
  flex-shrink: 0;
}

.hero-features strong {
  display: block;
  font-size: 14px;
  color: var(--bp-color-text-primary);
}

.hero-features p {
  margin: 2px 0 0 0;
  font-size: 12.5px;
  color: var(--bp-color-text-tertiary);
}

.login-card {
  padding: 36px;
  width: 100%;
  max-width: 460px;
  margin: 0 auto;
}

.login-card__header h2 {
  font-size: 22px;
}

.login-card__header p {
  margin-top: 6px;
  font-size: 13px;
  color: var(--bp-color-text-tertiary);
}

.login-form {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.captcha-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.captcha-row .el-button {
  flex-shrink: 0;
  min-width: 92px;
}

.login-submit {
  width: 100%;
  margin-top: 8px;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
}

.login-card__footer {
  margin-top: 20px;
  text-align: center;
  font-size: 13px;
  color: var(--bp-color-text-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 4px;
}

.dot {
  opacity: 0.5;
}

@media (max-width: 900px) {
  .login-wrap {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  .login-side--hero {
    text-align: center;
    padding: 8px;
  }
  .hero-badge {
    margin-inline: auto;
  }
  .hero-features {
    text-align: left;
  }
}
</style>

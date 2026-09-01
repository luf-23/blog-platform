<template>
  <div class="auth-view">
    <!-- Tabs -->
    <div class="auth-tabs">
      <button class="auth-tab" :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
      <button class="auth-tab" :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
    </div>

    <!-- Login Form -->
    <transition name="fade" mode="out-in">
      <div v-if="mode === 'login'" key="login">
        <h2 class="auth-title">账户登录</h2>
        <p class="auth-subtitle">使用用户名或邮箱登录</p>
        <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" @submit.prevent="handleLogin">
          <el-form-item prop="usernameOrEmail">
            <el-input v-model="loginForm.usernameOrEmail" placeholder="用户名或邮箱" size="large" clearable>
              <template #prefix>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="loginForm.password" type="password" placeholder="密码" size="large" show-password @keyup.enter="handleLogin">
              <template #prefix>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
              </template>
            </el-input>
          </el-form-item>
          <div class="auth-options">
            <a class="link-text" @click="mode = 'reset'">忘记密码？</a>
          </div>
          <el-button type="primary" size="large" class="auth-btn" :loading="loading" @click="handleLogin">
            登录
          </el-button>
        </el-form>
        <p class="auth-switch">
          还没有账户？<a class="link-text" @click="mode = 'register'">立即注册</a>
        </p>
      </div>

      <!-- Register Form -->
      <div v-else-if="mode === 'register'" key="register">
        <h2 class="auth-title">创建账户</h2>
        <p class="auth-subtitle">注册后可以发布文章、评论和关注作者</p>
        <el-form ref="regFormRef" :model="regForm" :rules="regRules" @submit.prevent="handleRegister">
          <el-form-item prop="username">
            <el-input v-model="regForm.username" placeholder="用户名（5-16位字母数字）" size="large" clearable>
              <template #prefix>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="email">
            <el-input v-model="regForm.email" placeholder="邮箱" size="large" clearable>
              <template #prefix>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="captcha">
            <div class="captcha-row">
              <el-input v-model="regForm.captcha" placeholder="邮箱验证码" size="large">
                <template #prefix>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                </template>
              </el-input>
              <el-button size="large" :disabled="captchaCountdown > 0 || sendingCaptcha" :loading="sendingCaptcha" @click="sendCaptcha" class="captcha-btn">
                {{ captchaCountdown > 0 ? `${captchaCountdown}s` : '发送验证码' }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="regForm.password" type="password" placeholder="密码（6-20位）" size="large" show-password>
              <template #prefix>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
              </template>
            </el-input>
          </el-form-item>
          <el-button type="primary" size="large" class="auth-btn" :loading="loading" @click="handleRegister">
            注册
          </el-button>
        </el-form>
        <p class="auth-switch">
          已有账户？<a class="link-text" @click="mode = 'login'">立即登录</a>
        </p>
      </div>

      <!-- Reset Password -->
      <div v-else key="reset">
        <h2 class="auth-title">重置密码</h2>
        <p class="auth-subtitle">通过邮箱验证重置你的密码</p>
        <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules">
          <el-form-item prop="email">
            <el-input v-model="resetForm.email" placeholder="注册邮箱" size="large">
              <template #prefix>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="captcha">
            <div class="captcha-row">
              <el-input v-model="resetForm.captcha" placeholder="邮箱验证码" size="large"/>
              <el-button size="large" :disabled="resetCountdown > 0 || sendingReset" :loading="sendingReset" @click="sendResetCaptcha" class="captcha-btn">
                {{ resetCountdown > 0 ? `${resetCountdown}s` : '发送' }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item prop="newPassword">
            <el-input v-model="resetForm.newPassword" type="password" placeholder="新密码" size="large" show-password/>
          </el-form-item>
          <el-button type="primary" size="large" class="auth-btn" :loading="loading" @click="handleReset">
            重置密码
          </el-button>
        </el-form>
        <p class="auth-switch">
          <a class="link-text" @click="mode = 'login'">← 返回登录</a>
        </p>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useTokenStore } from '../store/token.js'
import { useUserInfoStore } from '../store/userInfo.js'
import {
  loginService, registerService, getUserInfoService,
  sendEmailCaptchaService, checkEmailCaptchaService, resetPasswordService
} from '../api/user.js'

const router = useRouter()
const route = useRoute()
const tokenStore = useTokenStore()
const userInfoStore = useUserInfoStore()

const mode = ref('login')
const loading = ref(false)
const sendingCaptcha = ref(false)
const captchaCountdown = ref(0)
const sendingReset = ref(false)
const resetCountdown = ref(0)

const loginFormRef = ref()
const regFormRef = ref()
const resetFormRef = ref()

const loginForm = reactive({ usernameOrEmail: '', password: '' })
const regForm = reactive({ username: '', email: '', captcha: '', password: '' })
const resetForm = reactive({ email: '', captcha: '', newPassword: '' })

const loginRules = {
  usernameOrEmail: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const regRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^\S{5,16}$/, message: '用户名为5-16位非空字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ]
}

const resetRules = {
  email: [{ required: true, type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }]
}

async function handleLogin() {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await loginService({
        usernameOrEmail: loginForm.usernameOrEmail,
        password: loginForm.password
      })
      tokenStore.setToken(res.data)
      const info = await getUserInfoService()
      userInfoStore.setUserInfo(info.data)
      ElMessage.success('登录成功')
      const redirect = route.query.redirect || '/home'
      router.push(redirect)
    } catch (e) {
      // error handled by interceptor
    } finally {
      loading.value = false
    }
  })
}

async function sendCaptcha() {
  if (!regForm.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(regForm.email)) {
    ElMessage.warning('请先填写正确的邮箱')
    return
  }
  sendingCaptcha.value = true
  try {
    await sendEmailCaptchaService({ email: regForm.email })
    ElMessage.success('验证码已发送，请查收邮件')
    captchaCountdown.value = 60
    const timer = setInterval(() => {
      captchaCountdown.value--
      if (captchaCountdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch {}
  finally { sendingCaptcha.value = false }
}

async function handleRegister() {
  if (!regFormRef.value) return
  await regFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const verify = await checkEmailCaptchaService({ email: regForm.email, captcha: regForm.captcha })
      if (!verify.data) { ElMessage.error('验证码错误'); return }
      await registerService({ username: regForm.username, password: regForm.password, email: regForm.email })
      ElMessage.success('注册成功，请登录')
      mode.value = 'login'
      loginForm.usernameOrEmail = regForm.username
    } catch {}
    finally { loading.value = false }
  })
}

async function sendResetCaptcha() {
  if (!resetForm.email) { ElMessage.warning('请先填写邮箱'); return }
  sendingReset.value = true
  try {
    await sendEmailCaptchaService({ email: resetForm.email })
    ElMessage.success('验证码已发送')
    resetCountdown.value = 60
    const timer = setInterval(() => {
      resetCountdown.value--
      if (resetCountdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch {}
  finally { sendingReset.value = false }
}

async function handleReset() {
  if (!resetFormRef.value) return
  await resetFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const verify = await checkEmailCaptchaService({ email: resetForm.email, captcha: resetForm.captcha })
      if (!verify.data) { ElMessage.error('验证码错误'); return }
      await resetPasswordService({ email: resetForm.email, newPassword: resetForm.newPassword })
      ElMessage.success('密码重置成功，请登录')
      mode.value = 'login'
    } catch {}
    finally { loading.value = false }
  })
}
</script>

<style scoped>
.auth-view {}

.auth-tabs {
  display: flex;
  gap: 0;
  border-bottom: 1px solid var(--c-border);
  margin-bottom: 30px;
}

.auth-tab {
  flex: 1;
  position: relative;
  padding: 10px 8px 12px;
  font-size: 14px;
  font-weight: 500;
  border: none;
  background: transparent;
  color: var(--c-text-3);
  border-radius: 0;
  cursor: pointer;
  transition: all var(--transition);
}
.auth-tab.active {
  color: var(--c-primary);
  box-shadow: inset 0 -2px 0 var(--c-primary);
}

.auth-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--c-text);
  margin-bottom: 6px;
}

.auth-subtitle {
  font-size: 14px;
  color: var(--c-text-3);
  margin-bottom: 24px;
}

.auth-btn {
  width: 100%;
  margin-top: 8px;
  font-size: 15px;
  min-height: 46px;
  border-radius: 9px;
  font-weight: 700;
}

.auth-options {
  display: flex;
  justify-content: flex-end;
  margin: -4px 0 12px;
}

.auth-switch {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: var(--c-text-3);
}

.link-text {
  color: var(--c-primary);
  cursor: pointer;
  font-weight: 500;
}
.link-text:hover { text-decoration: underline; }

.captcha-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.captcha-row .el-input { flex: 1; }
.captcha-btn { flex-shrink: 0; white-space: nowrap; }

:deep(.el-form-item) { margin-bottom: 16px; }
</style>

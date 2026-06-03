<template>
  <div class="app-layout">
    <!-- Nav -->
    <header class="nav-bar">
      <div class="nav-inner page-container">
        <router-link to="/home" class="nav-logo">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="8" fill="var(--c-primary)"/>
            <path d="M7 9h14M7 14h10M7 19h12" stroke="white" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <span class="nav-logo-text">Blog Platform</span>
        </router-link>

        <nav class="nav-links">
          <router-link to="/home" class="nav-link" :class="{ active: $route.path === '/home' }">发现</router-link>
          <router-link to="/article/my" class="nav-link" :class="{ active: $route.path.startsWith('/article') }">我的博客</router-link>
          <router-link v-if="isAdmin" to="/admin/home" class="nav-link" :class="{ active: $route.path.startsWith('/admin') }">管理后台</router-link>
        </nav>

        <div class="nav-actions">
          <button class="btn btn-ghost btn-icon theme-toggle" @click="toggleTheme" :title="isDark ? '切换亮色' : '切换深色'">
            <svg v-if="!isDark" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
            </svg>
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z"/>
            </svg>
          </button>

          <router-link to="/announcement" class="btn btn-ghost btn-icon" title="公告">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/>
            </svg>
          </router-link>

          <el-dropdown trigger="click" placement="bottom-end" @command="handleCommand">
            <div class="nav-user-avatar">
              <img :src="userInfo?.avatarImage || defaultAvatar" :alt="userInfo?.nickname" class="avatar" style="width:32px;height:32px"/>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <div class="nav-user-info">
                  <img :src="userInfo?.avatarImage || defaultAvatar" class="avatar" style="width:40px;height:40px"/>
                  <div>
                    <div style="font-weight:600;font-size:14px">{{ userInfo?.nickname || userInfo?.username }}</div>
                    <div style="font-size:12px;color:var(--c-text-3)">@{{ userInfo?.username }}</div>
                  </div>
                </div>
                <el-dropdown-item command="profile">个人主页</el-dropdown-item>
                <el-dropdown-item command="ai">AI 助手</el-dropdown-item>
                <el-dropdown-item divided command="logout" style="color:var(--c-danger)">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useTokenStore } from '../store/token.js'
import { useUserInfoStore } from '../store/userInfo.js'
import { useTheme } from '../composables/useTheme.js'
import request from '../utils/request.js'

const router = useRouter()
const tokenStore = useTokenStore()
const userInfoStore = useUserInfoStore()

const { isDark, toggleTheme } = useTheme()

const userInfo = computed(() => userInfoStore.userInfo)
const isAdmin = computed(() => userInfo.value?.username === 'admin' || userInfo.value?.role === 'admin')
const defaultAvatar = 'https://luf-23.oss-cn-wuhan-lr.aliyuncs.com/avatar/default.png'

async function handleCommand(cmd) {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'ai') {
    router.push('/ai/chat')
  } else if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await request({ url: '/user/logout', method: 'post' }).catch(() => {})
      tokenStore.removeToken()
      userInfoStore.clearUserInfo()
      ElMessage.success('已退出登录')
      router.push('/login')
    } catch {}
  }
}
</script>

<style scoped>
.app-layout {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.nav-bar {
  flex: 0 0 var(--nav-height);
  z-index: 100;
  height: var(--nav-height);
  background: rgba(var(--c-surface, 255,255,255), 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--c-border);
}

[data-theme="dark"] .nav-bar {
  background: rgba(26, 26, 31, 0.85);
}

.nav-inner {
  height: 100%;
  display: flex;
  align-items: center;
  gap: 32px;
}

.nav-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 18px;
  color: var(--c-text);
  text-decoration: none;
  flex-shrink: 0;
}

.nav-logo-text { color: var(--c-text); }

.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.nav-link {
  padding: 6px 12px;
  border-radius: var(--radius);
  font-size: 14px;
  font-weight: 500;
  color: var(--c-text-3);
  transition: all var(--transition);
  text-decoration: none;
}
.nav-link:hover { color: var(--c-text); background: var(--c-surface-2); }
.nav-link.active { color: var(--c-primary); background: var(--c-primary-light); }

.nav-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.nav-user-avatar {
  cursor: pointer;
  border-radius: 50%;
  transition: opacity var(--transition);
}
.nav-user-avatar:hover { opacity: 0.8; }

.nav-user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--c-border);
  margin-bottom: 4px;
}

.main-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding-top: 24px;
  padding-bottom: 0;
  box-sizing: border-box;
  overscroll-behavior: none;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.main-content::-webkit-scrollbar {
  display: none;
}

.theme-toggle { color: var(--c-text-3); }
</style>

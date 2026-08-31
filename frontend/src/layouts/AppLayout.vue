<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import BrandMark from '../components/common/BrandMark.vue'
import { useTokenStore } from '../store/token.js'
import { useUserInfoStore } from '../store/userInfo.js'
import { useTheme } from '../composables/useTheme.js'
import request from '../utils/request.js'
import { DEFAULT_AVATAR_URL as defaultAvatar } from '../constants/assets.js'

const router = useRouter()
const route = useRoute()
const tokenStore = useTokenStore()
const userInfoStore = useUserInfoStore()
const { isDark, toggleTheme } = useTheme()

const searchOpen = ref(false)
const mobileOpen = ref(false)
const globalKeyword = ref('')
const userInfo = computed(() => userInfoStore.userInfo)
const loggedIn = computed(() => Boolean(tokenStore.token))
const isAdmin = computed(() => userInfo.value?.username === 'admin' || userInfo.value?.role === 'admin')
const adminRoute = computed(() => route.path.startsWith('/admin'))
const workspaceRoute = computed(() =>
  route.path === '/home' || route.path === '/community' ||
  route.path.startsWith('/article/write') || route.path.startsWith('/article/edit/') ||
  /^\/article\/\d+/.test(route.path) || route.path.startsWith('/ai/chat')
)

const navItems = computed(() => [
  { label: '首页', path: '/home', active: route.path === '/home' || route.path.startsWith('/community') },
  { label: '我的创作', path: '/article/my', active: route.path.startsWith('/article/my') || route.path.startsWith('/article/categories') },
  ...(isAdmin.value ? [{ label: '管理后台', path: '/admin/home', active: route.path.startsWith('/admin') }] : [])
])

function submitSearch() {
  const keyword = globalKeyword.value.trim()
  router.push({ path: '/home', query: keyword ? { keyword } : {} })
  searchOpen.value = false
  mobileOpen.value = false
}

function goWrite() {
  if (!loggedIn.value) {
    router.push({ path: '/login', query: { redirect: '/article/write' } })
    return
  }
  router.push('/article/write')
}

async function handleCommand(cmd) {
  if (cmd === 'profile') router.push('/profile')
  if (cmd === 'articles') router.push('/article/my')
  if (cmd === 'ai') router.push('/ai/chat')
  if (cmd === 'admin') router.push('/admin/home')
  if (cmd === 'theme') toggleTheme()
  if (cmd !== 'logout') return

  try {
    await ElMessageBox.confirm('确定要退出当前账号吗？', '退出登录', {
      confirmButtonText: '退出登录',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request({ url: '/user/logout', method: 'post' }).catch(() => {})
    tokenStore.removeToken()
    userInfoStore.clearUserInfo()
    ElMessage.success('已安全退出')
    router.push('/login')
  } catch {}
}
</script>

<template>
  <div class="app-layout">
    <header v-if="!adminRoute" class="topbar">
      <div class="topbar__inner page-container">
        <router-link to="/home" class="topbar__brand" aria-label="BYTE 首页">
          <BrandMark />
        </router-link>

        <nav class="topbar__nav" aria-label="主导航">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="topbar__link"
            :class="{ active: item.active }"
          >{{ item.label }}</router-link>
        </nav>

        <div class="topbar__actions">
          <form class="global-search" :class="{ 'is-open': searchOpen }" @submit.prevent="submitSearch">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></svg>
            <input id="global-search" v-model="globalKeyword" name="global-search" placeholder="搜索文章、作者或标签" />
            <button v-if="globalKeyword || searchOpen" type="button" aria-label="关闭搜索" @click="globalKeyword = ''; searchOpen = false">×</button>
          </form>
          <button class="topbar__icon mobile-search" aria-label="搜索" @click="searchOpen = true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></svg>
          </button>

          <button class="write-button" @click="goWrite">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z"/></svg>
            <span>写文章</span>
          </button>

          <router-link to="/announcement" class="topbar__icon notification" aria-label="系统公告">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 8-3 8h18s-3-1-3-8"/><path d="M10 20h4"/></svg>
            <span class="notification__dot"></span>
          </router-link>

          <router-link v-if="isAdmin" to="/admin/home" class="admin-entry">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 3 4 7v5c0 5 3.4 8 8 9 4.6-1 8-4 8-9V7Z"/><path d="m9 12 2 2 4-4"/></svg>
            <span>管理控制台</span>
          </router-link>

          <el-dropdown v-if="loggedIn" trigger="click" placement="bottom-end" @command="handleCommand">
            <button class="user-trigger">
              <img :src="userInfo?.avatarImage || defaultAvatar" :alt="userInfo?.nickname || '用户头像'" />
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m8 10 4 4 4-4"/></svg>
            </button>
            <template #dropdown>
              <el-dropdown-menu class="user-menu">
                <div class="user-menu__head">
                  <img :src="userInfo?.avatarImage || defaultAvatar" alt="" />
                  <div><strong>{{ userInfo?.nickname || userInfo?.username }}</strong><span>@{{ userInfo?.username }}</span></div>
                </div>
                <el-dropdown-item command="profile">个人主页</el-dropdown-item>
                <el-dropdown-item command="articles">内容管理</el-dropdown-item>
                <el-dropdown-item command="ai">AI 写作助手</el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" divided command="admin">管理控制台</el-dropdown-item>
                <el-dropdown-item command="theme">{{ isDark ? '切换浅色模式' : '切换深色模式' }}</el-dropdown-item>
                <el-dropdown-item divided command="logout" class="danger-item">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <router-link v-else to="/login" class="login-link">登录</router-link>

          <button class="mobile-toggle" aria-label="展开导航" @click="mobileOpen = !mobileOpen">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 7h16M4 12h16M4 17h16"/></svg>
          </button>
        </div>
      </div>

      <transition name="fade">
        <nav v-if="mobileOpen" class="mobile-nav">
          <router-link v-for="item in navItems" :key="item.path" :to="item.path" @click="mobileOpen = false">{{ item.label }}</router-link>
        </nav>
      </transition>
    </header>

    <main class="main-content" :class="{ 'main-content--admin': adminRoute, 'main-content--workspace': workspaceRoute }">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in"><component :is="Component" /></transition>
      </router-view>
    </main>
  </div>
</template>

<style scoped>
.app-layout { display: flex; height: 100%; flex-direction: column; overflow: hidden; }
.topbar { position: relative; z-index: 100; flex: 0 0 var(--nav-height); border-bottom: 1px solid var(--c-border-strong); background: color-mix(in srgb, var(--c-surface) 94%, transparent); backdrop-filter: blur(14px); }
.topbar__inner { display: flex; height: var(--nav-height); align-items: center; gap: 38px; }
.topbar__brand { display: flex; flex: 0 0 auto; }
.topbar__nav { display: none; height: 100%; align-items: stretch; gap: 4px; }
.topbar__link { position: relative; display: flex; align-items: center; padding: 0 15px; color: var(--c-text-2); font-size: 13px; font-weight: 700; letter-spacing: .02em; transition: color var(--transition); }
.topbar__link::after { position: absolute; right: 15px; bottom: -1px; left: 15px; height: 3px; border-radius: 3px 3px 0 0; background: var(--c-primary); content: ''; opacity: 0; transform: scaleX(.35); transition: all var(--transition); }
.topbar__link:hover, .topbar__link.active { color: var(--c-primary); }
.topbar__link.active::after { opacity: 1; transform: scaleX(1); }
.topbar__actions { display: flex; flex: 1; align-items: center; justify-content: flex-end; gap: 8px; min-width: 0; }
.topbar__icon, .mobile-toggle { display: grid; width: 38px; height: 38px; place-items: center; border: 1px solid transparent; border-radius: var(--radius-sm); background: transparent; color: var(--c-text-2); transition: all var(--transition); }
.topbar__icon:hover, .mobile-toggle:hover { border-color: var(--c-border); background: var(--c-surface-2); color: var(--c-primary); }
.topbar__icon svg, .mobile-toggle svg, .write-button svg { width: 19px; height: 19px; }
.write-button { display: inline-flex; height: 40px; align-items: center; gap: 8px; padding: 0 18px; border: 1px solid var(--c-primary); border-radius: var(--radius-sm); background: var(--c-primary); color: #fff; font-weight: 700; transition: all var(--transition); }
.write-button:hover { border-color: var(--c-primary-hover); background: var(--c-primary-hover); box-shadow: 0 8px 18px rgba(var(--c-primary-rgb), .16); }
.notification { position: relative; }
.notification__dot { position: absolute; top: 7px; right: 7px; width: 7px; height: 7px; border: 2px solid var(--c-surface); border-radius: 50%; background: var(--c-danger); }
.admin-entry { display: inline-flex; height: 38px; align-items: center; gap: 7px; padding: 0 12px; border: 1px solid color-mix(in srgb, var(--c-primary) 35%, var(--c-border)); border-radius: var(--radius-sm); background: var(--c-primary-soft); color: var(--c-primary); font-size: 11px; font-weight: 800; }.admin-entry:hover { border-color: var(--c-primary); background: var(--c-primary); color: #fff; }.admin-entry svg { width: 16px; height: 16px; }
.user-trigger { display: flex; height: 40px; align-items: center; gap: 4px; padding: 2px 4px 2px 2px; border: 0; border-radius: var(--radius-full); background: transparent; color: var(--c-text-3); }
.user-trigger img { width: 36px; height: 36px; border: 2px solid var(--c-surface); border-radius: 50%; box-shadow: 0 0 0 1px var(--c-border); object-fit: cover; }
.user-trigger svg { width: 15px; height: 15px; }
.global-search { display: flex; width: min(680px, 52vw); height: 42px; align-items: center; gap: 9px; padding: 0 10px 0 13px; border: 1px solid var(--c-border-strong); border-radius: var(--radius-sm); background: var(--c-surface); box-shadow: none; }
.global-search:focus-within { border-color: var(--c-primary); box-shadow: 0 0 0 2px rgba(var(--c-primary-rgb), .08); }
.global-search svg { width: 17px; height: 17px; color: var(--c-text-4); }
.global-search input { min-width: 0; flex: 1; border: 0; outline: 0; background: transparent; color: var(--c-text); }
.global-search button { border: 0; background: transparent; color: var(--c-text-4); font-size: 20px; }
.login-link { padding: 8px 10px; color: var(--c-primary); font-weight: 700; }
.user-menu__head { display: flex; min-width: 220px; align-items: center; gap: 10px; padding: 12px 16px 14px; border-bottom: 1px solid var(--c-border); margin-bottom: 5px; }
.user-menu__head img { width: 42px; height: 42px; border-radius: 50%; object-fit: cover; }
.user-menu__head div { display: flex; min-width: 0; flex-direction: column; }
.user-menu__head strong { color: var(--c-text); }
.user-menu__head span { color: var(--c-text-3); font-size: 12px; }
.main-content { min-height: 0; flex: 1; overflow: auto; overscroll-behavior: contain; }
.main-content--admin { overflow: hidden; }
.main-content--workspace { overflow: hidden; }
.mobile-toggle { display: none; }
.mobile-search { display: none; }
.mobile-nav { position: absolute; top: var(--nav-height); right: 14px; left: 14px; display: none; padding: 8px; border: 1px solid var(--c-border); border-radius: var(--radius-lg); background: var(--c-surface); box-shadow: var(--shadow-md); }
.mobile-nav a { padding: 10px 12px; border-radius: var(--radius-sm); font-weight: 600; }
.mobile-nav a.router-link-active { background: var(--c-primary-soft); color: var(--c-primary); }

@media (max-width: 900px) {
  .topbar__nav { display: none; }
  .mobile-toggle, .mobile-nav { display: flex; }
  .mobile-nav { flex-direction: column; }
  .global-search { width: min(520px, 52vw); }
}

@media (max-width: 620px) {
  .topbar__inner { gap: 12px; }
  .write-button { width: 40px; padding: 0; justify-content: center; }
  .write-button span { display: none; }
  .mobile-search { display: grid; }
  .global-search { display: none; }
  .global-search.is-open { position: absolute; right: 14px; bottom: -52px; left: 14px; display: flex; width: auto; box-shadow: var(--shadow-md); }
  .admin-entry { width: 38px; padding: 0; justify-content: center; }
  .admin-entry span { display: none; }
}
</style>

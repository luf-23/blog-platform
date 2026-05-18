<script setup>
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import {
  User,
  Setting,
  SwitchButton,
  Sunny,
  Moon,
  Menu,
  EditPen,
  Collection,
  Compass,
  MagicStick,
  Bell
} from "@element-plus/icons-vue";
import { useBreakpoints, breakpointsTailwind } from "@vueuse/core";

import { useTokenStore } from "../store/token.js";
import { useUserInfoStore } from "../store/userInfo.js";
import { useTheme } from "../composables/useTheme.js";

const route = useRoute();
const router = useRouter();
const tokenStore = useTokenStore();
const userInfoStore = useUserInfoStore();
const { userInfo } = storeToRefs(userInfoStore);
const { isDark, toggle: toggleTheme } = useTheme();

const breakpoints = useBreakpoints(breakpointsTailwind);
const isDesktop = breakpoints.greater("md");
const mobileMenuOpen = ref(false);

watch(
  () => route.fullPath,
  () => {
    mobileMenuOpen.value = false;
  }
);

const isAdmin = computed(() => userInfo.value?.username === "admin");
const isAdminRoute = computed(() => route.path.startsWith("/admin"));

const mainNav = [
  { path: "/home", label: "发现", icon: Compass },
  { path: "/article/category", label: "我的博客", icon: Collection }
];

const adminNav = [
  { path: "/admin/home", label: "控制台" },
  { path: "/admin/list", label: "文章审核" },
  { path: "/admin/user", label: "用户管理" }
];

const activeMainPath = computed(() => {
  const path = route.path;
  if (path.startsWith("/article")) return "/article/category";
  if (path.startsWith("/admin")) return "";
  if (path === "/community") return "/home";
  return path;
});

function navigate(path) {
  router.push(path);
}

function handleUserCommand(command) {
  if (command === "profile") router.push("/profile");
  else if (command === "ai") router.push("/ai/chat");
  else if (command === "announcement") router.push("/announcement");
  else if (command === "admin") router.push("/admin/home");
  else if (command === "logout") handleLogout();
}

function handleLogout() {
  tokenStore.removeToken();
  userInfoStore.removeUserInfo();
  router.push("/login");
}

function goWrite() {
  router.push("/article/category");
}
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="app-header__inner">
        <div class="app-header__brand" @click="navigate('/home')">
          <span class="app-header__logo" aria-hidden="true" />
          <span class="app-header__brand-text">
            <span class="app-header__brand-word app-header__brand-word--main">Blog</span>
            <span class="app-header__brand-word app-header__brand-word--sub">Platform</span>
          </span>
        </div>

        <nav v-if="!isAdminRoute" class="app-header__nav app-header__nav--desktop">
          <button
            v-for="item in mainNav"
            :key="item.path"
            class="nav-link"
            :class="{ 'nav-link--active': activeMainPath === item.path }"
            @click="navigate(item.path)"
          >
            <el-icon :size="16"><component :is="item.icon" /></el-icon>
            {{ item.label }}
          </button>
        </nav>

        <nav v-else class="app-header__nav app-header__nav--desktop">
          <button
            v-for="item in adminNav"
            :key="item.path"
            class="nav-link"
            :class="{ 'nav-link--active': route.path === item.path }"
            @click="navigate(item.path)"
          >
            {{ item.label }}
          </button>
        </nav>

        <div class="app-header__actions">
          <el-button
            v-if="!isAdminRoute"
            type="primary"
            class="write-btn write-btn--desktop"
            :icon="EditPen"
            @click="goWrite"
          >
            写文章
          </el-button>

          <button
            class="icon-btn"
            :title="isDark ? '切换浅色主题' : '切换深色主题'"
            @click="toggleTheme"
          >
            <el-icon>
              <component :is="isDark ? Sunny : Moon" />
            </el-icon>
          </button>

          <el-dropdown
            trigger="click"
            placement="bottom-end"
            @command="handleUserCommand"
          >
            <button class="user-trigger" type="button">
              <el-avatar
                :size="32"
                :src="userInfo?.avatarImage || '/avatar/avatar1.png'"
              />
              <span v-if="isDesktop" class="user-trigger__name">{{
                userInfo?.nickname || userInfo?.username || "用户"
              }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人主页
                </el-dropdown-item>
                <el-dropdown-item command="ai">
                  <el-icon><MagicStick /></el-icon>AI 助手
                </el-dropdown-item>
                <el-dropdown-item command="announcement">
                  <el-icon><Bell /></el-icon>系统公告
                </el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" divided command="admin">
                  <el-icon><Setting /></el-icon>管理后台
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <button
            class="icon-btn app-header__menu-btn"
            aria-label="打开菜单"
            @click="mobileMenuOpen = true"
          >
            <el-icon><Menu /></el-icon>
          </button>
        </div>
      </div>
    </header>

    <main class="app-main">
      <div class="app-main__container">
        <router-view v-slot="{ Component }">
          <transition name="route-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>

    <el-drawer
      v-model="mobileMenuOpen"
      direction="rtl"
      size="min(320px, 88vw)"
      :with-header="false"
    >
      <div class="mobile-drawer__head">
        <el-avatar
          :size="44"
          :src="userInfo?.avatarImage || '/avatar/avatar1.png'"
        />
        <div>
          <div class="mobile-drawer__name">
            {{ userInfo?.nickname || userInfo?.username || "用户" }}
          </div>
          <div class="mobile-drawer__role">
            {{ isAdmin ? "管理员" : "博主" }}
          </div>
        </div>
      </div>

      <template v-if="!isAdminRoute">
        <button
          v-for="item in mainNav"
          :key="item.path"
          class="mobile-nav-item"
          :class="{ 'mobile-nav-item--active': activeMainPath === item.path }"
          @click="navigate(item.path)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          {{ item.label }}
        </button>
        <el-button
          type="primary"
          class="mobile-write-btn"
          :icon="EditPen"
          @click="goWrite"
        >
          写文章
        </el-button>
      </template>
      <template v-else>
        <button
          v-for="item in adminNav"
          :key="item.path"
          class="mobile-nav-item"
          :class="{ 'mobile-nav-item--active': route.path === item.path }"
          @click="navigate(item.path)"
        >
          {{ item.label }}
        </button>
      </template>

      <div class="mobile-drawer__divider" />

      <button class="mobile-nav-item" @click="handleUserCommand('profile')">
        <el-icon><User /></el-icon>个人主页
      </button>
      <button class="mobile-nav-item" @click="handleUserCommand('logout')">
        <el-icon><SwitchButton /></el-icon>退出登录
      </button>
    </el-drawer>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bp-color-bg);
  color: var(--bp-color-text-primary);
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: color-mix(in srgb, var(--bp-color-bg-elevated) 92%, transparent);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--bp-color-border);
}

.app-header__inner {
  max-width: var(--bp-content-max-width);
  margin: 0 auto;
  padding: 0 clamp(16px, 3vw, 24px);
  height: var(--bp-header-height);
  display: flex;
  align-items: center;
  gap: 20px;
}

.app-header__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  flex-shrink: 0;
}

.app-header__logo {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: var(--bp-gradient-hero);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
  flex-shrink: 0;
}

.app-header__brand-text {
  display: inline-flex;
  flex-direction: row;
  align-items: baseline;
  gap: 10px;
  flex-shrink: 0;
  white-space: nowrap;
}

.app-header__brand-word--main {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: var(--bp-color-text-primary);
  line-height: 1;
}

.app-header__brand-word--sub {
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.04em;
  color: var(--bp-color-text-secondary);
  line-height: 1;
}

.app-header__nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.app-header__nav--desktop {
  display: none;
}

@media (min-width: 768px) {
  .app-header__nav--desktop {
    display: flex;
  }
}

.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--bp-color-text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.nav-link:hover {
  background: var(--bp-color-bg-hover);
  color: var(--bp-color-primary);
}

.nav-link--active {
  background: var(--bp-color-primary-soft);
  color: var(--bp-color-primary);
}

.app-header__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.write-btn--desktop {
  display: none;
}

@media (min-width: 768px) {
  .write-btn--desktop {
    display: inline-flex;
  }
}

.icon-btn {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  border: 1px solid var(--bp-color-border);
  background: var(--bp-color-bg-elevated);
  color: var(--bp-color-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease, border-color 0.2s ease;
}

.icon-btn:hover {
  background: var(--bp-color-primary-soft);
  color: var(--bp-color-primary);
  border-color: var(--bp-color-primary-soft-strong);
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 4px;
  border-radius: 999px;
  border: 1px solid var(--bp-color-border);
  background: var(--bp-color-bg-elevated);
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.user-trigger:hover {
  background: var(--bp-color-primary-soft);
  border-color: var(--bp-color-primary-soft-strong);
}

.user-trigger__name {
  font-size: 13px;
  font-weight: 500;
  max-width: 100px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-header__menu-btn {
  display: flex;
}

@media (min-width: 768px) {
  .app-header__menu-btn {
    display: none;
  }
}

.app-main {
  flex: 1;
  min-height: 0;
}

.app-main__container {
  max-width: var(--bp-content-max-width);
  margin: 0 auto;
  width: 100%;
  min-height: calc(100vh - var(--bp-header-height));
}

.mobile-drawer__head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 4px 20px;
}

.mobile-drawer__name {
  font-size: 16px;
  font-weight: 600;
}

.mobile-drawer__role {
  font-size: 12px;
  color: var(--bp-color-text-tertiary);
  margin-top: 2px;
}

.mobile-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 14px;
  margin-bottom: 4px;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: var(--bp-color-text-secondary);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  text-align: left;
  transition: background 0.2s ease, color 0.2s ease;
}

.mobile-nav-item:hover,
.mobile-nav-item--active {
  background: var(--bp-color-primary-soft);
  color: var(--bp-color-primary);
}

.mobile-write-btn {
  width: 100%;
  margin: 8px 0 16px;
}

.mobile-drawer__divider {
  height: 1px;
  background: var(--bp-color-divider);
  margin: 12px 0;
}
</style>

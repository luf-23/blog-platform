<script setup>
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import {
  HomeFilled,
  ChatDotSquare,
  MagicStick,
  Document,
  Bell,
  User,
  Setting,
  SwitchButton,
  Fold,
  Expand,
  Sunny,
  Moon,
  Search
} from "@element-plus/icons-vue";
import { useBreakpoints, breakpointsTailwind } from "@vueuse/core";

import { useAppStore } from "../store/app.js";
import { useTokenStore } from "../store/token.js";
import { useUserInfoStore } from "../store/userInfo.js";
import { useTheme } from "../composables/useTheme.js";

const route = useRoute();
const router = useRouter();
const appStore = useAppStore();
const tokenStore = useTokenStore();
const userInfoStore = useUserInfoStore();
const { userInfo } = storeToRefs(userInfoStore);
const { sidebarCollapsed } = storeToRefs(appStore);
const { isDark, toggle: toggleTheme } = useTheme();

const breakpoints = useBreakpoints(breakpointsTailwind);
const isDesktop = breakpoints.greater("md");
const mobileDrawer = ref(false);

watch(
  () => route.fullPath,
  () => {
    mobileDrawer.value = false;
  }
);

const isAdmin = computed(() => userInfo.value?.username === "admin");

const navigation = computed(() => {
  const items = [
    { path: "/home", icon: HomeFilled, label: "首页" },
    { path: "/community", icon: ChatDotSquare, label: "社区" },
    { path: "/ai/chat", icon: MagicStick, label: "AI 助手" },
    { path: "/article/category", icon: Document, label: "我的文章" },
    { path: "/announcement", icon: Bell, label: "系统公告" },
    { path: "/profile", icon: User, label: "个人信息" }
  ];
  if (isAdmin.value) {
    items.push({ path: "/admin/home", icon: Setting, label: "管理后台", admin: true });
  }
  return items;
});

const activePath = computed(() => {
  const path = route.path;
  if (path.startsWith("/article")) return "/article/category";
  if (path.startsWith("/admin")) return "/admin/home";
  if (path.startsWith("/ai")) return "/ai/chat";
  return path;
});

const pageTitleMap = {
  "/home": "首页",
  "/community": "社区",
  "/ai/chat": "AI 助手",
  "/announcement": "系统公告",
  "/profile": "个人主页",
  "/article/category": "我的分类",
  "/article/list": "文章列表",
  "/article/detail": "文章详情",
  "/article/add": "新建文章",
  "/article/edit": "编辑文章",
  "/admin/home": "管理后台",
  "/admin/list": "文章审核",
  "/admin/detail": "文章详情",
  "/admin/user": "用户管理"
};

const pageTitle = computed(() => {
  return (
    pageTitleMap[route.path] ||
    route.meta?.title ||
    "Blog Platform"
  );
});

function navigate(path) {
  router.push(path);
}

function toggleSidebar() {
  if (isDesktop.value) {
    appStore.toggleSidebar();
  } else {
    mobileDrawer.value = !mobileDrawer.value;
  }
}

function handleUserCommand(command) {
  if (command === "profile") router.push("/profile");
  else if (command === "logout") handleLogout();
  else if (command === "admin") router.push("/admin/home");
}

function handleLogout() {
  tokenStore.removeToken();
  userInfoStore.removeUserInfo();
  router.push("/login");
}

const showSidebar = computed(() => isDesktop.value || mobileDrawer.value);
</script>

<template>
  <div class="app-shell" :class="{ collapsed: sidebarCollapsed && isDesktop }">
    <transition name="fade">
      <div
        v-if="!isDesktop && mobileDrawer"
        class="app-shell__mask"
        @click="mobileDrawer = false"
      />
    </transition>

    <aside
      class="app-sidebar"
      :class="{
        'app-sidebar--collapsed': sidebarCollapsed && isDesktop,
        'app-sidebar--mobile': !isDesktop,
        'app-sidebar--open': showSidebar
      }"
    >
      <div class="app-sidebar__brand" @click="navigate('/home')">
        <div class="app-sidebar__logo">
          <span class="app-sidebar__logo-dot" />
          <span class="app-sidebar__logo-dot app-sidebar__logo-dot--alt" />
        </div>
        <transition name="fade">
          <div
            v-if="!(sidebarCollapsed && isDesktop)"
            class="app-sidebar__brand-text"
          >
            <strong>Blog</strong>
            <span>Platform</span>
          </div>
        </transition>
      </div>

      <nav class="app-sidebar__nav">
        <button
          v-for="item in navigation"
          :key="item.path"
          class="nav-item"
          :class="{
            'nav-item--active': activePath === item.path,
            'nav-item--admin': item.admin
          }"
          @click="navigate(item.path)"
        >
          <el-icon class="nav-item__icon" :size="18">
            <component :is="item.icon" />
          </el-icon>
          <span
            v-if="!(sidebarCollapsed && isDesktop)"
            class="nav-item__label"
          >{{ item.label }}</span>
        </button>
      </nav>

      <div class="app-sidebar__footer">
        <div class="user-card" v-if="!(sidebarCollapsed && isDesktop)">
          <el-avatar
            :size="36"
            :src="userInfo?.avatarImage || '/avatar/avatar1.png'"
          />
          <div class="user-card__text">
            <div class="user-card__name">
              {{ userInfo?.nickname || userInfo?.username || "未登录" }}
            </div>
            <div class="user-card__role">
              {{ isAdmin ? "管理员" : "普通用户" }}
            </div>
          </div>
        </div>
        <button class="footer-btn" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          <span v-if="!(sidebarCollapsed && isDesktop)">退出登录</span>
        </button>
      </div>
    </aside>

    <div class="app-main">
      <header class="app-topbar">
        <div class="app-topbar__left">
          <button
            class="topbar-icon-btn"
            @click="toggleSidebar"
            :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
          >
            <el-icon>
              <component :is="sidebarCollapsed || !isDesktop ? Expand : Fold" />
            </el-icon>
          </button>

          <div class="app-topbar__title">
            <span class="app-topbar__title-text">{{ pageTitle }}</span>
          </div>
        </div>

        <div class="app-topbar__right">
          <button
            class="topbar-icon-btn"
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
            <div class="topbar-user">
              <el-avatar
                :size="32"
                :src="userInfo?.avatarImage || '/avatar/avatar1.png'"
              />
              <span class="topbar-user__name">{{
                userInfo?.nickname || userInfo?.username || "用户"
              }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人主页
                </el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" command="admin">
                  <el-icon><Setting /></el-icon>管理后台
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="app-content">
        <router-view v-slot="{ Component }">
          <transition name="route-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  height: 100vh;
  width: 100vw;
  background: var(--bp-color-bg);
  color: var(--bp-color-text-primary);
  overflow: hidden;
}

.app-shell__mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  z-index: 90;
  backdrop-filter: blur(2px);
}

.app-sidebar {
  flex-shrink: 0;
  width: var(--bp-sidebar-width);
  height: 100vh;
  background: var(--bp-color-bg-elevated);
  border-right: 1px solid var(--bp-color-border);
  display: flex;
  flex-direction: column;
  transition: width 0.25s ease, transform 0.25s ease;
  z-index: 100;
}

.app-sidebar--collapsed {
  width: var(--bp-sidebar-width-collapsed);
}

.app-sidebar--mobile {
  position: fixed;
  top: 0;
  left: 0;
  transform: translateX(-100%);
  width: var(--bp-sidebar-width);
}

.app-sidebar--mobile.app-sidebar--open {
  transform: translateX(0);
  box-shadow: var(--bp-shadow-lg);
}

.app-sidebar__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 18px;
  cursor: pointer;
  border-bottom: 1px solid var(--bp-color-divider);
}

.app-sidebar__logo {
  position: relative;
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: var(--bp-gradient-hero);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
}

.app-sidebar__logo-dot {
  position: absolute;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: white;
  top: 9px;
  left: 9px;
  opacity: 0.95;
}

.app-sidebar__logo-dot--alt {
  top: auto;
  left: auto;
  bottom: 9px;
  right: 9px;
  background: rgba(255, 255, 255, 0.6);
  width: 6px;
  height: 6px;
}

.app-sidebar__brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.app-sidebar__brand-text strong {
  font-size: 16px;
  letter-spacing: -0.01em;
  color: var(--bp-color-text-primary);
}

.app-sidebar__brand-text span {
  font-size: 11px;
  color: var(--bp-color-text-tertiary);
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.app-sidebar__nav {
  flex: 1;
  padding: 12px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 10px 12px;
  background: transparent;
  border: none;
  border-radius: 12px;
  color: var(--bp-color-text-secondary);
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease, transform 0.2s ease;
  font-size: 14px;
  font-weight: 500;
  text-align: left;
}

.nav-item:hover {
  background: var(--bp-color-bg-hover);
  color: var(--bp-color-primary);
}

.nav-item--active {
  background: var(--bp-color-primary-soft);
  color: var(--bp-color-primary);
}

.nav-item--active::before {
  content: "";
  width: 4px;
  height: 18px;
  background: var(--bp-color-primary);
  border-radius: 999px;
  margin-left: -16px;
  margin-right: 8px;
}

.app-sidebar--collapsed .nav-item--active::before {
  display: none;
}

.nav-item--admin {
  color: var(--bp-color-warning);
}

.nav-item--admin:hover {
  background: rgba(245, 158, 11, 0.12);
  color: var(--bp-color-warning);
}

.app-sidebar--collapsed .nav-item {
  justify-content: center;
  padding: 12px;
}

.app-sidebar__footer {
  padding: 12px;
  border-top: 1px solid var(--bp-color-divider);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 12px;
  background: var(--bp-color-bg-soft);
}

.user-card__text {
  min-width: 0;
}

.user-card__name {
  font-size: 13px;
  font-weight: 600;
  color: var(--bp-color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-card__role {
  font-size: 11px;
  color: var(--bp-color-text-tertiary);
}

.footer-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 12px;
  color: var(--bp-color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
  font-weight: 500;
}

.footer-btn:hover {
  background: rgba(239, 68, 68, 0.1);
  color: var(--bp-color-danger);
}

.app-sidebar--collapsed .footer-btn {
  justify-content: center;
}

.app-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.app-topbar {
  height: var(--bp-topbar-height);
  padding: 0 clamp(16px, 2.5vw, 24px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: var(--bp-color-bg-elevated);
  border-bottom: 1px solid var(--bp-color-border);
  position: sticky;
  top: 0;
  z-index: 50;
}

.app-topbar__left,
.app-topbar__right {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.topbar-icon-btn {
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

.topbar-icon-btn:hover {
  background: var(--bp-color-primary-soft);
  color: var(--bp-color-primary);
  border-color: var(--bp-color-primary-soft-strong);
}

.app-topbar__title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.app-topbar__title-text {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.01em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.topbar-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 4px;
  border-radius: 999px;
  border: 1px solid var(--bp-color-border);
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;
  outline: none;
}

.topbar-user:hover {
  background: var(--bp-color-primary-soft);
  border-color: var(--bp-color-primary-soft-strong);
}

.topbar-user__name {
  font-size: 13px;
  font-weight: 500;
  max-width: 96px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  background: var(--bp-color-bg);
}

@media (max-width: 767px) {
  .topbar-user__name {
    display: none;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

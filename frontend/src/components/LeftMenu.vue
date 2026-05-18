<template>
  <div class="layout-container">
    <!-- 移动端遮罩层 -->
    <div
      v-if="showMobileMenu"
      class="mobile-overlay"
      @click="closeMobileMenu"
    ></div>

    <!-- 顶部导航栏（移动端显示） -->
    <TopBar
      @toggle-mobile-menu="toggleMobileMenu"
      @toggle-ai-chat-settings="toggleAiChatSettings"
      @clear-ai-chat="clearAiChat"
    />

    <!-- 侧边栏 -->
    <el-aside
      :width="isCollapse ? '64px' : '220px'"
      class="aside"
      :class="{ 'mobile-show': showMobileMenu }"
    >
      <div class="logo" @click="toggleCollapse">
        <el-icon class="logo-icon" :size="24">
          <Avatar />
        </el-icon>
        <span v-if="!isCollapse" class="logo-text">Blog Platform</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        class="el-menu-vertical"
        :collapse="isCollapse"
        :collapse-transition="false"
        background-color="transparent"
        text-color="#64748b"
        active-text-color="#3b82f6"
        router
        @select="onMenuSelect"
      >
        <el-menu-item index="/home" class="menu-item">
          <el-icon><home-filled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>

        <el-menu-item index="/community" class="menu-item">
          <el-icon><ChatDotSquare /></el-icon>
          <template #title>社区</template>
        </el-menu-item>

        <el-menu-item index="/ai/chat" class="menu-item">
          <el-icon><MagicStick /></el-icon>
          <template #title>AI助手</template>
        </el-menu-item>

        <el-menu-item index="/article/category" class="menu-item">
          <el-icon><Document /></el-icon>
          <template #title>文章管理</template>
        </el-menu-item>

        <el-menu-item index="/announcement" class="menu-item">
          <el-icon><Bell /></el-icon>
          <template #title>系统公告</template>
        </el-menu-item>

        <el-sub-menu index="user" class="menu-item">
          <template #title>
            <el-icon><user /></el-icon>
            <span>个人中心</span>
          </template>
          <el-menu-item index="/profile">个人信息</el-menu-item>
          <el-menu-item @click="handleLogout" class="logout-item">
            <span>退出登录</span>
          </el-menu-item>
        </el-sub-menu>

        <el-menu-item
          v-if="userInfo.username === 'admin'"
          index="/admin/home"
          class="menu-item admin-item"
        >
          <el-icon><Setting /></el-icon>
          <template #title>管理后台</template>
        </el-menu-item>
      </el-menu>

      <!-- 底部用户信息 -->
      <div v-if="!isCollapse" class="user-info">
        <el-avatar
          :size="32"
          :src="userInfo.avatarImage || '/avatar/avatar1.png'"
        />
        <div class="user-text">
          <div class="username">{{ userInfo.nickname || "用户" }}</div>
          <div class="user-role">
            {{ userInfo.username === "admin" ? "管理员" : "普通用户" }}
          </div>
        </div>
      </div>
    </el-aside>

    <!-- 主内容区 -->
    <el-main class="main">
      <div class="main-content">
        <router-view />
      </div>
    </el-main>
  </div>
</template>

<script setup>
import { ref, computed, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  HomeFilled,
  User,
  Setting,
  ChatDotSquare,
  Document,
  MagicStick,
  Avatar,
  SwitchButton,
  Bell
} from "@element-plus/icons-vue";
import { useTokenStore } from "../store/token";
import { useUserInfoStore } from "../store/userInfo";
import { storeToRefs } from "pinia";
import { ElMessage } from "element-plus";
import TopBar from "./TopBar.vue";

const route = useRoute();
const isCollapse = ref(false);
const showMobileMenu = ref(false);
const tokenStore = useTokenStore();
const userInfoStore = useUserInfoStore();
const { userInfo } = storeToRefs(userInfoStore);
const router = useRouter();

// 计算当前激活的菜单
const activeMenu = computed(() => {
  const { path } = route;
  return path;
});

// 监听折叠状态变化，更新CSS变量
watch(
  isCollapse,
  (newValue) => {
    if (typeof document !== "undefined") {
      const root = document.documentElement;
      if (newValue) {
        root.style.setProperty("--sidebar-width", "64px");
      } else {
        root.style.setProperty("--sidebar-width", "220px");
      }
    }
  },
  { immediate: true }
);

const handleLogout = () => {
  tokenStore.removeToken();
  router.push({
    path: "/login"
  });
};

// 切换折叠状态
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value;
};

// 移动端菜单控制
const toggleMobileMenu = () => {
  showMobileMenu.value = !showMobileMenu.value;
};

const closeMobileMenu = () => {
  showMobileMenu.value = false;
};

// 菜单选择时关闭移动端菜单
const onMenuSelect = () => {
  if (window.innerWidth <= 768) {
    showMobileMenu.value = false;
  }
};

// AI聊天设置面板控制（传递给Chat组件）
const toggleAiChatSettings = () => {
  // 通过事件总线或其他方式通知Chat组件
  window.dispatchEvent(new CustomEvent("toggle-ai-chat-settings"));
};

// 清空AI聊天记录（传递给Chat组件）
const clearAiChat = () => {
  // 通过事件总线或其他方式通知Chat组件
  window.dispatchEvent(new CustomEvent("clear-ai-chat"));
};
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
  background: #f8fafc;
  /* 设置CSS变量供其他组件使用 */
  --sidebar-width: 220px;
  --sidebar-collapsed-width: 64px;
}

.aside {
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 2px 0 20px rgba(0, 0, 0, 0.08);
  border-right: 1px solid rgba(226, 232, 240, 0.6);
  position: relative;
  z-index: 1000;
  flex-shrink: 0; /* 防止侧边栏被压缩 */
  overflow: visible; /* 确保内容不被裁剪 */
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #3b82f6;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
  background: linear-gradient(135deg, #f1f5f9, #e2e8f0);
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  transition: all 0.3s ease;
  margin-bottom: 8px;
}

.logo:hover {
  color: #2563eb;
  background: linear-gradient(135deg, #e2e8f0, #cbd5e1);
}

.logo-icon {
  color: #3b82f6;
}

.logo-text {
  font-family: "Inter", -apple-system, BlinkMacSystemFont, sans-serif;
  letter-spacing: -0.5px;
}

.el-menu-vertical {
  border-right: none;
  padding: 0 12px;
  overflow: visible; /* 防止内容被裁剪 */
}

.el-menu-vertical:not(.el-menu--collapse) {
  width: 196px;
}

:deep(.el-menu-item) {
  margin: 6px 0;
  border-radius: 8px;
  height: 48px;
  line-height: 48px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;

  &:hover {
    background: linear-gradient(135deg, #f1f5f9, #e2e8f0) !important;
    color: #3b82f6 !important;
    border-color: rgba(59, 130, 246, 0.2);
    transform: translateX(4px);
  }

  &.is-active {
    background: linear-gradient(135deg, #dbeafe, #bfdbfe) !important;
    color: #1d4ed8 !important;
    border-color: rgba(59, 130, 246, 0.3);
    box-shadow: 0 2px 8px rgba(59, 130, 246, 0.15);
    font-weight: 600;
  }
}

:deep(.el-sub-menu__title) {
  margin: 6px 0;
  border-radius: 8px;
  height: 48px;
  line-height: 48px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;

  &:hover {
    background: linear-gradient(135deg, #f1f5f9, #e2e8f0) !important;
    color: #3b82f6 !important;
    border-color: rgba(59, 130, 246, 0.2);
    transform: translateX(4px);
  }
}

:deep(.el-menu--collapse) {
  .el-sub-menu__title {
    padding: 0 20px !important;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .el-menu-item {
    padding: 0 20px !important;
    display: flex;
    align-items: center;
    justify-content: center;
    text-align: center;
  }

  .el-menu-item .el-icon {
    margin-right: 0 !important;
    font-size: 18px;
  }

  .el-sub-menu__title .el-icon {
    margin-right: 0 !important;
    font-size: 18px;
  }

  /* 确保折叠状态下的宽度足够 */
  width: 64px !important;
  padding: 0 6px; /* 减少菜单容器的内边距 */
}

:deep(.el-sub-menu .el-menu-item) {
  margin: 2px 0;
  margin-left: 16px;
  border-radius: 6px;
  height: 40px;
  line-height: 40px;
  font-size: 14px;
}

.admin-item {
  border: 1px solid #fbbf24 !important;
  background: linear-gradient(135deg, #fef3c7, #fed7aa) !important;
  color: #d97706 !important;
}

.admin-item:hover {
  background: linear-gradient(135deg, #fde68a, #fcd34d) !important;
  color: #b45309 !important;
}

.logout-item {
  color: #ef4444 !important;

  &:hover {
    background: linear-gradient(135deg, #fee2e2, #fecaca) !important;
    color: #dc2626 !important;
    border-color: rgba(239, 68, 68, 0.2);
  }
}

.user-info {
  position: absolute;
  bottom: 16px;
  left: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: linear-gradient(135deg, #f8fafc, #f1f5f9);
  border-radius: 12px;
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.user-text {
  flex: 1;
  min-width: 0;
}

.username {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.main {
  flex: 1;
  background: #ffffff;
  padding: 0;
  overflow: hidden;
  min-width: 0; /* 允许主内容区缩小 */
}

.main-content {
  height: 100%;
  padding: 24px;
  overflow-y: auto;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.mobile-overlay {
  display: none;
}

/* 响应式设计 */
@media screen and (max-width: 768px) {
  .layout-container {
    position: relative;
    display: block; /* 移动端不使用flex布局 */
  }

  .mobile-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 1500;
    backdrop-filter: blur(4px);
    display: block;
  }

  .aside {
    position: fixed;
    left: 0;
    top: 60px; /* 确保与TopBar对齐 */
    height: calc(100vh - 60px);
    width: 220px !important; /* 固定宽度，忽略动态宽度 */
    z-index: 2000;
    transform: translateX(-100%);
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 4px 0 20px rgba(0, 0, 0, 0.15);
  }

  .aside.mobile-show {
    transform: translateX(0);
  }

  .main {
    width: 100% !important;
    height: 100vh !important; /* 使用100vh而不是计算值 */
    margin-left: 0 !important;
    margin-top: 0; /* 移除margin-top */
    padding-top: 60px; /* 恢复使用padding-top，但确保无额外空白 */
    flex: none;
    position: relative;
    box-sizing: border-box; /* 确保padding包含在高度计算中 */
  }

  .main-content {
    padding: 8px; /* 进一步减少移动端内边距 */
    height: calc(100% - 60px); /* 减去顶部padding */
    overflow-y: auto;
  }

  .user-info {
    bottom: 20px;
  }

  .logo {
    height: 50px;
    margin-bottom: 6px;
  }

  /* 确保移动端侧边栏不折叠 */
  .el-menu-vertical {
    width: 196px !important;
  }
}

/* 桌面端确保侧边栏正常显示 */
@media screen and (min-width: 769px) {
  .aside {
    position: relative;
    transform: translateX(0) !important;
    z-index: auto;
    box-shadow: 2px 0 20px rgba(0, 0, 0, 0.08);
  }

  .main {
    padding-top: 0;
  }
}

/* 滚动条样式 */
.main-content::-webkit-scrollbar {
  width: 6px;
}

.main-content::-webkit-scrollbar-track {
  background: #f1f5f9;
}

.main-content::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.main-content::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>

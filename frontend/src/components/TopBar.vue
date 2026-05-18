<template>
  <div class="top-bar">
    <div class="top-bar-content">
      <!-- 移动端菜单按钮 -->
      <el-button
        class="mobile-menu-btn"
        @click="toggleMobileMenu"
        :icon="Menu"
        circle
        size="large"
        type="primary"
      />

      <!-- AI聊天页面的汉堡菜单按钮 -->
      <el-button
        v-if="isAiChatPage"
        class="ai-chat-settings-btn"
        @click="toggleAiChatSettings"
        circle
        size="large"
        type="primary"
      >
        <span>三</span>
      </el-button>

      <!-- 页面标题 -->
      <div class="page-title">
        <h3>{{ pageTitle }}</h3>
      </div>

      <!-- AI聊天页面的清空按钮 -->
      <el-button
        v-if="isAiChatPage"
        class="ai-clear-btn"
        @click="clearAiChat"
        size="default"
        type="danger"
      >
        清空当前会话记录
      </el-button>

      <!-- 用户信息和操作 -->
      <div class="user-actions">
        <el-dropdown @command="handleCommand" trigger="click">
          <span class="user-info-trigger">
            <el-avatar
              :size="36"
              :src="userInfo.avatarImage || '/avatar/avatar1.png'"
              class="user-avatar"
            />
            <span class="username">{{ userInfo.nickname || "用户" }}</span>
            <el-icon><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>
                个人信息
              </el-dropdown-item>
              <el-dropdown-item command="settings">
                <el-icon><Setting /></el-icon>
                设置
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useUserInfoStore } from "../store/userInfo";
import { useTokenStore } from "../store/token";
import { storeToRefs } from "pinia";
import {
  Menu,
  ArrowDown,
  User,
  Setting,
  SwitchButton
} from "@element-plus/icons-vue";

// 定义 emits
const emit = defineEmits([
  "toggle-mobile-menu",
  "toggle-ai-chat-settings",
  "clear-ai-chat"
]);

const route = useRoute();
const router = useRouter();
const userInfoStore = useUserInfoStore();
const tokenStore = useTokenStore();
const { userInfo } = storeToRefs(userInfoStore);

// 页面标题映射
const pageTitleMap = {
  "/home": "首页",
  "/community": "社区讨论",
  "/ai/chat": "AI助手",
  "/profile": "个人信息",
  "/article/category": "文章分类",
  "/article/list": "文章列表",
  "/article/save": "写文章",
  "/announcement": "系统公告",
  "/admin/home": "管理后台"
};

// 判断是否是AI聊天页面
const isAiChatPage = computed(() => {
  return route.path === "/ai/chat";
});

// 计算当前页面标题
const pageTitle = computed(() => {
  return pageTitleMap[route.path] || route.meta?.title || "系统";
});

// 切换移动端菜单
const toggleMobileMenu = () => {
  emit("toggle-mobile-menu");
};

// 切换AI聊天设置面板
const toggleAiChatSettings = () => {
  emit("toggle-ai-chat-settings");
};

// 清空AI聊天记录
const clearAiChat = () => {
  emit("clear-ai-chat");
};

// 处理下拉菜单命令
const handleCommand = (command) => {
  switch (command) {
    case "profile":
      router.push("/profile");
      break;
    case "settings":
      // 跳转到设置页面（如果有的话）
      break;
    case "logout":
      tokenStore.removeToken();
      router.push("/login");
      break;
  }
};
</script>

<style scoped>
.top-bar {
  height: 60px;
  background: linear-gradient(135deg, #ffffff, #f8fafc);
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: fixed; /* 使用fixed定位确保始终在顶部 */
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  display: none; /* 默认隐藏 */
}

/* 只在移动端显示 */
@media screen and (max-width: 768px) {
  .top-bar {
    display: block;
  }
}

.top-bar-content {
  height: 100%;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.mobile-menu-btn {
  width: 44px;
  height: 44px;
  margin-right: 16px;
}

.ai-chat-settings-btn {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  font-size: 18px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.ai-chat-settings-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.ai-clear-btn {
  height: 36px;
  padding: 0 12px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
  border: none;
  color: white;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  transition: all 0.3s ease;
  min-width: auto;
}

.ai-clear-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.page-title {
  flex: 1;
  min-width: 0;
  text-align: center;
}

.page-title h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-actions {
  display: flex;
  align-items: center;
}

.user-info-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #64748b;
}

.user-info-trigger:hover {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.user-avatar {
  border: 2px solid rgba(59, 130, 246, 0.2);
}

.username {
  font-size: 14px;
  font-weight: 500;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media screen and (max-width: 480px) {
  .top-bar-content {
    padding: 0 8px;
    gap: 6px;
  }

  .mobile-menu-btn {
    width: 36px;
    height: 36px;
    margin-right: 6px;
    flex-shrink: 0;
  }

  .ai-chat-settings-btn {
    width: 36px;
    height: 36px;
    font-size: 14px;
    flex-shrink: 0;
  }

  .ai-clear-btn {
    height: 28px;
    padding: 0 6px;
    font-size: 10px;
    flex-shrink: 0;
    min-width: 60px;
  }

  .page-title {
    flex: 1;
    min-width: 60px;
  }

  .page-title h3 {
    font-size: 14px;
  }

  .username {
    display: none;
  }

  .user-info-trigger {
    padding: 6px;
    gap: 4px;
  }

  .user-avatar {
    width: 32px !important;
    height: 32px !important;
  }
}

/* 中等屏幕尺寸优化 */
@media screen and (max-width: 640px) and (min-width: 481px) {
  .top-bar-content {
    padding: 0 10px;
    gap: 8px;
  }

  .mobile-menu-btn {
    width: 40px;
    height: 40px;
    margin-right: 8px;
  }

  .ai-chat-settings-btn {
    width: 40px;
    height: 40px;
    font-size: 16px;
  }

  .ai-clear-btn {
    height: 30px;
    padding: 0 10px;
    font-size: 11px;
  }

  .page-title h3 {
    font-size: 16px;
  }

  .username {
    max-width: 60px;
  }
}
</style>

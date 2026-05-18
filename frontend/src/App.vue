<script setup>
import { useTokenStore } from "./store/token.js";
import { useRoute } from "vue-router";
import { computed } from "vue";
import LeftMenu from "./components/LeftMenu.vue";

const tokenStore = useTokenStore();
const route = useRoute();
const token = tokenStore.token;

// 定义不需要显示侧边栏的页面
const noLayoutPages = ["/login"];

// 计算是否显示布局
const showLayout = computed(() => {
  return token && !noLayoutPages.includes(route.path);
});
</script>

<template>
  <!-- 有token且不在登录页面时显示完整布局 -->
  <LeftMenu v-if="showLayout" />
  <!-- 无token或在登录页面时直接显示路由内容 -->
  <router-view v-else />
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
    sans-serif;
  background-color: #f8fafc;
  color: #1e293b;
  line-height: 1.6;
  margin: 0; /* 确保body没有默认边距 */
  padding: 0; /* 确保body没有默认内边距 */
}

#app {
  height: 100vh;
  overflow: hidden;
  margin: 0; /* 确保app容器没有边距 */
  padding: 0; /* 确保app容器没有内边距 */
}

/* 全局适配侧边栏的布局 */
.main-content-wrapper {
  margin-left: var(--sidebar-width, 220px);
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 移动端重置 */
@media screen and (max-width: 768px) {
  .main-content-wrapper {
    margin-left: 0;
  }
}

/* Element Plus 全局样式覆盖 */
:root {
  --el-color-primary: #3b82f6;
  --el-color-primary-light-3: #93c5fd;
  --el-color-primary-light-5: #bfdbfe;
  --el-color-primary-light-7: #dbeafe;
  --el-color-primary-light-8: #eff6ff;
  --el-color-primary-light-9: #f8fafc;
  --el-color-primary-dark-2: #1d4ed8;
  --el-border-radius-base: 8px;
  --el-border-radius-small: 6px;
}

.el-card {
  border-radius: 12px !important;
  border: 1px solid rgba(226, 232, 240, 0.6) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05) !important;
}

.el-button {
  border-radius: 8px !important;
  font-weight: 500 !important;
}

.el-input__wrapper {
  border-radius: 8px !important;
}

.el-dialog {
  border-radius: 16px !important;
}

/* 滚动条全局样式 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>

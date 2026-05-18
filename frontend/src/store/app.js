import { defineStore } from "pinia";
import { ref } from "vue";

/**
 * 全局 UI/偏好设置 Store
 * 持久化在 localStorage 中，跨页面保留主题与侧边栏状态
 */
export const useAppStore = defineStore(
  "app",
  () => {
    const theme = ref("light");
    const sidebarCollapsed = ref(false);

    function setTheme(value) {
      theme.value = value === "dark" ? "dark" : "light";
    }

    function toggleSidebar() {
      sidebarCollapsed.value = !sidebarCollapsed.value;
    }

    function setSidebarCollapsed(value) {
      sidebarCollapsed.value = Boolean(value);
    }

    return {
      theme,
      sidebarCollapsed,
      setTheme,
      toggleSidebar,
      setSidebarCollapsed
    };
  },
  {
    persist: {
      key: "bp-app",
      pick: ["theme", "sidebarCollapsed"]
    }
  }
);

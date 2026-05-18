import { defineStore } from "pinia";
import { ref } from "vue";

/**
 * 全局 UI/偏好设置 Store
 * 持久化在 localStorage 中，跨页面保留主题
 */
export const useAppStore = defineStore(
  "app",
  () => {
    const theme = ref("light");

    function setTheme(value) {
      theme.value = value === "dark" ? "dark" : "light";
    }

    return {
      theme,
      setTheme
    };
  },
  {
    persist: {
      key: "bp-app",
      pick: ["theme"]
    }
  }
);

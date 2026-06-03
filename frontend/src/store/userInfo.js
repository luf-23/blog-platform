import { defineStore } from "pinia";
import { ref } from "vue";

export const useUserInfoStore = defineStore(
  "userInfo",
  () => {
    const userInfo = ref(null);

    const setUserInfo = (info) => {
      userInfo.value = info || null;
    };

    const clearUserInfo = () => {
      userInfo.value = null;
    };

    // backward compat
    const removeUserInfo = clearUserInfo;

    return { userInfo, setUserInfo, clearUserInfo, removeUserInfo };
  },
  { persist: { key: "bp-user-info" } }
);

import { defineStore } from "pinia";
import { ref } from "vue";
export const useUserInfoStore = defineStore(
  "userInfo",
  () => {
    const userInfo = ref(null);
    const setUserInfo = (newUserInfo) => {
      userInfo.value = newUserInfo;
    };
    const removeUserInfo = () => {
      userInfo.value = null;
    };
    return {
      userInfo,
      setUserInfo,
      removeUserInfo
    };
  },
  {
    persist: true
  }
);

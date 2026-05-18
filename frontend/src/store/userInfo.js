import { defineStore } from "pinia";
import { ref } from "vue";

export const useUserInfoStore = defineStore(
  "userInfo",
  () => {
    const userInfo = ref({
      username: "",
      nickname: "",
      signature: "",
      avatarImage: "",
      backgroundImage: ""
    });

    const setUserInfo = (newUserInfo) => {
      userInfo.value = newUserInfo || {};
    };

    const removeUserInfo = () => {
      userInfo.value = {};
    };

    return {
      userInfo,
      setUserInfo,
      removeUserInfo
    };
  },
  {
    persist: { key: "bp-user-info" }
  }
);

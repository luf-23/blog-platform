import { defineStore } from "pinia";
import { ref } from "vue";
export const useTokenStore = defineStore(
  "token",
  () => {
    const token = ref(null); // 存储accessToken
    const setToken = (newToken) => {
      token.value = newToken;
    };
    const removeToken = () => {
      token.value = null;
    };
    return {
      token,
      setToken,
      removeToken
    };
  },
  {
    persist: true
  }
);

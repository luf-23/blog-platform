import { defineStore } from "pinia";
import { ref } from "vue";

export const useChatStore = defineStore(
  "chat",
  () => {
    const chatList = ref([]);
    const currentChatId = ref(null);

    const createNewChat = () => {
      const newChat = {
        id: Date.now(),
        title: "新对话",
        messages: []
      };
      chatList.value.push(newChat);
      currentChatId.value = newChat.id;
      return newChat.id;
    };

    const addMessage = (role, content) => {
      if (!currentChatId.value) createNewChat();
      const chat = chatList.value.find((c) => c.id === currentChatId.value);
      if (!chat) return;
      chat.messages.push({
        id: Date.now() + Math.random(),
        role,
        content,
        timestamp: new Date().toISOString()
      });
      if (role === "user" && chat.messages.filter((m) => m.role === "user").length === 1) {
        chat.title = content.slice(0, 24) || "新对话";
      }
    };

    const deleteChat = (id) => {
      chatList.value = chatList.value.filter((chat) => chat.id !== id);
      if (currentChatId.value === id) {
        currentChatId.value = chatList.value[0]?.id || null;
      }
    };

    const setCurrentChat = (id) => {
      currentChatId.value = id;
    };

    const clearCurrentChat = () => {
      const chat = chatList.value.find((c) => c.id === currentChatId.value);
      if (chat) chat.messages = [];
    };

    const getCurrentChatHistory = () => {
      const chat = chatList.value.find((c) => c.id === currentChatId.value);
      return chat ? chat.messages : [];
    };

    const addChatToCurrent = (chunk) => {
      const current = chatList.value.find((c) => c.id === currentChatId.value);
      if (current && current.messages.length > 0) {
        current.messages[current.messages.length - 1].content += chunk;
      }
    };

    return {
      chatList,
      currentChatId,
      createNewChat,
      addMessage,
      deleteChat,
      setCurrentChat,
      clearCurrentChat,
      getCurrentChatHistory,
      addChatToCurrent
    };
  },
  {
    persist: { key: "bp-chat" }
  }
);

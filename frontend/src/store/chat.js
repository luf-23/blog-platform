import { defineStore } from "pinia";
import { ref } from "vue";

export const useChatStore = defineStore(
  "chat",
  () => {
    // 聊天记录列表，每个元素包含对话id和消息列表
    const chatList = ref([]);

    // 当前选中的对话id
    const currentChatId = ref(null);

    // 添加新对话
    const createNewChat = () => {
      const newChat = {
        id: Date.now(),
        messages: []
      };
      chatList.value.push(newChat);
      currentChatId.value = newChat.id;
      return newChat.id;
    };

    // 添加消息到当前对话
    const addMessage = (role, content) => {
      if (!currentChatId.value) {
        createNewChat();
      }
      const chat = chatList.value.find((c) => c.id === currentChatId.value);
      if (chat) {
        chat.messages.push({
          id: Date.now(),
          role,
          content,
          timestamp: new Date().toISOString()
        });
      }
    };

    // 删除对话
    const deleteChat = (id) => {
      chatList.value = chatList.value.filter((chat) => chat.id !== id);
      if (currentChatId.value === id) {
        currentChatId.value = chatList.value[0]?.id || null;
      }
    };

    // 切换当前对话
    const setCurrentChat = (id) => {
      currentChatId.value = id;
    };

    // 清空当前对话消息
    const clearCurrentChat = () => {
      const chat = chatList.value.find((c) => c.id === currentChatId.value);
      if (chat) {
        chat.messages = [];
      }
    };

    //获取当前对话的历史对话
    const getCurrentChatHistory = () => {
      const chat = chatList.value.find((c) => c.id === currentChatId.value);
      return chat ? chat.messages : [];
    };

    //添加对话到当前消息
    const addChatToCurrent = (x) => {
      const currentChat = chatList.value.find(
        (c) => c.id === currentChatId.value
      );
      if (currentChat && currentChat.messages.length > 0) {
        currentChat.messages[currentChat.messages.length - 1].content += x;
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
    persist: true
  }
);

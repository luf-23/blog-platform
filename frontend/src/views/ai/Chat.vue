<script setup>
import { ref, onMounted, nextTick, onUnmounted } from "vue";
import { useChatStore } from "../../store/chat";
import { storeToRefs } from "pinia";
import { ElMessage, ElSelect, ElOption } from "element-plus";
import { ElMessageBox } from "element-plus";
import "github-markdown-css";
import { renderAssistantMessage } from "../../utils/markdown/chat/assistant.js";
import { renderUserMessage } from "../../utils/markdown/chat/user.js";
import { useUserInfoStore } from "../../store/userInfo.js";
import { chatStreamService } from "../../api/ai.js";
import { getUserInfoService } from "../../api/user.js";
const userInfoStore = useUserInfoStore();
const { userInfo } = storeToRefs(userInfoStore);
const avatarUrl = ref(userInfo.value?.avatarImage || "/avatar/avatar1.png");
const chatStore = useChatStore();
const { chatList, currentChatId } = storeToRefs(chatStore);
const inputMessage = ref("");
const chatContainer = ref(null);

// AI模型列表
const models = [
  { value: "deepseek-v3", label: "deepseek-v3" },
  { value: "deepseek-r1", label: "deepseek-r1" },
  { value: "qwq-plus", label: "qwq-plus" },
  { value: "qwen-max-2025-01-25", label: "qwen-max-2025-01-25" }
];
const selectedModel = ref("deepseek-v3");

// temperature相关配置
const temperature = ref(0.4);
const temperatureOptions = Array.from({ length: 11 }, (_, i) => i / 10); // 生成0.0到1.0的选项

// 移动端侧边栏控制
const isMobileSettingsVisible = ref(false);

const toggleMobileSettings = () => {
  isMobileSettingsVisible.value = !isMobileSettingsVisible.value;
};

const closeMobileSettings = () => {
  isMobileSettingsVisible.value = false;
};

// 监听来自TopBar的事件
const handleAiChatSettingsToggle = () => {
  toggleMobileSettings();
};

const handleAiChatClear = () => {
  clearChat();
};

// 在组件加载时，如果没有对话，创建一个新的
onMounted(() => {
  if (!currentChatId.value) {
    chatStore.createNewChat();
  }

  // 添加事件监听器
  window.addEventListener(
    "toggle-ai-chat-settings",
    handleAiChatSettingsToggle
  );
  window.addEventListener("clear-ai-chat", handleAiChatClear);
});

onUnmounted(() => {
  // 移除事件监听器
  window.removeEventListener(
    "toggle-ai-chat-settings",
    handleAiChatSettingsToggle
  );
  window.removeEventListener("clear-ai-chat", handleAiChatClear);
});

const isLoading = ref(false);
const isthinking = ref(true); // 是否在思考中
const handleStreamChat = async () => {
  if (!inputMessage.value.trim()) {
    ElMessage.error("消息不能为空");
    return;
  }

  // 添加用户消息
  chatStore.addMessage("user", inputMessage.value);

  // 清空输入框
  inputMessage.value = "";

  // 等待DOM更新后滚动到底部
  await nextTick();
  scrollToBottom();

  isLoading.value = true;
  isthinking.value = true;

  try {
    const history = chatStore.getCurrentChatHistory();
    const messages = history.map((msg) => ({
      role: msg.role,
      content: msg.content
    }));

    chatStore.addMessage("assistant", ""); // 添加空的AI消息占位符

    // 在发起AI流式请求之前，先调用一个常规API确保token有效
    await getUserInfoService();

    // token确保有效后，使用AI API发起流式请求
    const response = await chatStreamService({
      messages: messages,
      model: selectedModel.value,
      temperature: temperature.value
    });

    if (!response.ok) {
      throw new Error("网络错误");
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();

    while (true) {
      const { value, done } = await reader.read();
      if (done) break;

      const chunk = decoder.decode(value, { stream: true });
      // 更新最后一条消息的内容
      const temp = parseChunk(chunk);
      chatStore.addChatToCurrent(temp);
    }
  } catch (error) {
    console.error("对话出错:", error);
    ElMessage.error(error.message || "对话出错");
  } finally {
    isLoading.value = false;
  }
};

let buffer = ""; // 存储未解析完的数据

function parseChunk(chunk) {
  buffer += chunk;
  const lines = buffer.split("\n");
  buffer = lines.pop(); // 最后一行可能不完整，放回 buffer

  let content = "";

  for (const line of lines) {
    if (!line.startsWith("data:")) continue;

    const data = line.slice(5).trim();
    if (data === "[DONE]") continue;

    try {
      const json = JSON.parse(data);
      const delta = json.choices?.[0]?.delta;

      if (!delta) continue;

      // 处理思考和回答
      if (delta.reasoning_content === null && isthinking.value) {
        content += "\n\n**======以上是思考过程======**\n\n";
        isthinking.value = false;
      }

      // 优先取 content，再取 reasoning_content
      const text = delta.content ?? delta.reasoning_content;
      if (text) content += text;
    } catch (error) {
      console.error("解析失败:", error, "数据:", data);
    }
  }

  return content;
}

// 滚动到底部
const scrollToBottom = () => {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight;
  }
};

// 创建新对话
const createNew = () => {
  chatStore.createNewChat();
};

// 清空当前对话
const clearChat = () => {
  ElMessageBox.confirm("确定要清空当前会话吗", "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  }).then(() => {
    chatStore.clearCurrentChat();
    ElMessage.success("当前会话已清空");
  });
};
// 删除对话
const handleChatDelete = (chatId) => {
  ElMessageBox.confirm("确定要删除此对话吗", "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  }).then(() => {
    chatStore.deleteChat(chatId);
    ElMessage.success("对话已删除");
  });
};

// 移动端选择对话后关闭设置面板
const handleMobileChatSelect = (chatId) => {
  chatStore.setCurrentChat(chatId);
  if (window.innerWidth <= 768) {
    isMobileSettingsVisible.value = false;
  }
};

// Markdown渲染方法已移至 utils/markdown.js
</script>

<template>
  <div class="chat-container">
    <!-- 移动端遮罩层 -->
    <div
      v-if="isMobileSettingsVisible"
      class="mobile-overlay"
      @click="closeMobileSettings"
    ></div>

    <!-- 移动端设置侧边栏 -->
    <div
      class="mobile-settings-sidebar"
      :class="{ show: isMobileSettingsVisible }"
    >
      <div class="mobile-settings-header">
        <h3>对话设置</h3>
        <button class="close-btn" @click="closeMobileSettings">×</button>
      </div>

      <div class="mobile-settings-content">
        <div class="setting-item">
          <label>模型选择:</label>
          <el-select v-model="selectedModel" style="width: 100%">
            <el-option
              v-for="model in models"
              :key="model.value"
              :label="model.label"
              :value="model.value"
            />
          </el-select>
        </div>

        <div class="setting-item">
          <label>采样温度:</label>
          <el-select v-model="temperature" style="width: 100%">
            <el-option
              v-for="temp in temperatureOptions"
              :key="temp"
              :label="temp.toFixed(1)"
              :value="temp"
            />
          </el-select>
        </div>

        <div class="setting-actions">
          <el-button type="primary" @click="createNew" style="width: 100%"
            >新对话</el-button
          >
        </div>

        <!-- 移动端对话历史 -->
        <div class="mobile-chat-history" v-if="chatList.length > 1">
          <h4>对话历史</h4>
          <div
            v-for="(chat, index) in chatList"
            :key="chat.id"
            class="mobile-chat-item"
            :class="{ active: chat.id === currentChatId }"
            @click="handleMobileChatSelect(chat.id)"
          >
            <span>对话 {{ index + 1 }}</span>
            <button
              v-if="chatList.length > 1"
              class="mobile-delete-btn"
              @click.stop="handleChatDelete(chat.id)"
            >
              删除
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 顶部控制面板（桌面端） -->
    <div class="chat-header desktop-only">
      <div class="header-left">
        <h1>AI 助手</h1>
      </div>

      <!-- 对话设置区域 -->
      <div class="chat-controls">
        <div class="control-group">
          <label>模型:</label>
          <el-select v-model="selectedModel" size="small" style="width: 140px">
            <el-option
              v-for="model in models"
              :key="model.value"
              :label="model.label"
              :value="model.value"
            />
          </el-select>
        </div>

        <div class="control-group">
          <label>温度:</label>
          <el-select v-model="temperature" size="small" style="width: 80px">
            <el-option
              v-for="temp in temperatureOptions"
              :key="temp"
              :label="temp.toFixed(1)"
              :value="temp"
            />
          </el-select>
        </div>

        <div class="control-group">
          <el-button size="small" @click="createNew">新对话</el-button>
          <el-button size="small" type="danger" @click="clearChat"
            >清空</el-button
          >
        </div>
      </div>
    </div>

    <!-- 对话历史标签页（桌面端） -->
    <div class="chat-tabs desktop-only" v-if="chatList.length > 1">
      <div class="tabs-container">
        <div
          v-for="(chat, index) in chatList"
          :key="chat.id"
          class="chat-tab"
          :class="{ active: chat.id === currentChatId }"
          @click="chatStore.setCurrentChat(chat.id)"
        >
          <span>对话 {{ index + 1 }}</span>
          <el-button
            v-if="chatList.length > 1"
            size="small"
            type="danger"
            link
            @click.stop="handleChatDelete(chat.id)"
            class="delete-tab-btn"
          >
            ×
          </el-button>
        </div>
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="chat-messages" ref="chatContainer">
      <template v-if="currentChatId">
        <div
          v-for="msg in chatList.find((c) => c.id === currentChatId)?.messages"
          :key="msg.id"
          :class="['message-container', msg.role]"
        >
          <div class="avatar">
            <img
              :src="
                msg.role === 'assistant' ? '/avatar/avatar2.png' : avatarUrl
              "
              :alt="msg.role"
            />
          </div>
          <div class="message-wrapper">
            <div class="message">
              <div
                v-if="msg.role === 'assistant'"
                class="message-content markdown-body"
                v-html="renderAssistantMessage(msg.content)"
              ></div>
              <div
                v-else-if="msg.role === 'user'"
                class="message-content"
                v-html="renderUserMessage(msg.content)"
              ></div>
              <div class="message-time">
                {{ new Date(msg.timestamp).toLocaleTimeString() }}
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 输入框 -->
    <div class="chat-input">
      <textarea
        v-model="inputMessage"
        placeholder="请输入文本..."
        @keyup.enter.ctrl="handleStreamChat"
        rows="3"
      ></textarea>
      <button @click="handleStreamChat" :disabled="isLoading">
        {{ isLoading ? "发送中..." : "发送" }}
      </button>
    </div>
  </div>
</template>
<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #f7fbfe;
  position: relative;
}

/* 移动端遮罩层 */
.mobile-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1500;
  backdrop-filter: blur(4px);
}

/* 移动端设置侧边栏 */
.mobile-settings-sidebar {
  position: fixed;
  top: 60px; /* 为TopBar留出空间 */
  left: 0;
  width: 280px;
  height: calc(100vh - 60px);
  background: #fff;
  z-index: 2000;
  transform: translateX(-100%);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.15);
  display: none; /* 默认在桌面端隐藏 */
}

.mobile-settings-sidebar.show {
  transform: translateX(0);
}

.mobile-settings-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e0e0e0;
  background: #f8f9fa;
}

.mobile-settings-header h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #666;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.2s;
}

.close-btn:hover {
  background: #e9ecef;
}

.mobile-settings-content {
  padding: 20px;
  overflow-y: auto;
  height: calc(100% - 70px);
}

.setting-item {
  margin-bottom: 20px;
}

.setting-item label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #555;
}

.setting-actions {
  margin: 24px 0;
}

.mobile-chat-history {
  margin-top: 24px;
  border-top: 1px solid #e0e0e0;
  padding-top: 20px;
}

.mobile-chat-history h4 {
  margin: 0 0 16px 0;
  color: #333;
  font-size: 16px;
}

.mobile-chat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 8px;
  background: #f8f9fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.mobile-chat-item:hover {
  background: #e9ecef;
}

.mobile-chat-item.active {
  background: #e3f2fd;
  color: #1976d2;
  font-weight: 500;
}

.mobile-delete-btn {
  background: #dc3545;
  color: white;
  border: none;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.chat-header {
  background-color: #fff;
  border-bottom: 1px solid #e0e0e0;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.header-left h1 {
  margin: 0;
  font-size: 1.5rem;
  color: #333;
  font-weight: 600;
}

.chat-controls {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.control-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.control-group label {
  font-size: 14px;
  color: #666;
  white-space: nowrap;
}

.chat-tabs {
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
  padding: 0 24px;
  overflow-x: auto;
}

.tabs-container {
  display: flex;
  gap: 4px;
  min-height: 48px;
  align-items: flex-end;
}

.chat-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-bottom: none;
  border-radius: 8px 8px 0 0;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  min-height: 36px;
}

.chat-tab:hover {
  background: #e8f4fd;
}

.chat-tab.active {
  background: #fff;
  border-color: #3498db;
  color: #3498db;
  font-weight: 500;
}

.delete-tab-btn {
  font-size: 16px !important;
  padding: 0 !important;
  width: 16px !important;
  height: 16px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-container {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
}

.message-container.user {
  flex-direction: row-reverse;
}

.avatar {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-wrapper {
  flex: 1;
  max-width: 85%;
  display: flex;
  flex-direction: column;
}

.message-container.user .message-wrapper {
  align-items: flex-end;
}

.message-container.assistant .message-wrapper {
  align-items: flex-start;
}

.message {
  padding: 12px 16px;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  max-width: 100%;
  word-wrap: break-word;
}

.message-container.user .message {
  background-color: #3498db;
  color: white;
  border-top-right-radius: 4px;
}

.message-container.assistant .message {
  background-color: #fff;
  border: 1px solid #e0e0e0;
  border-top-left-radius: 4px;
}

.message-content {
  line-height: 1.6;
}

.message-container.user .message-content {
  color: #fff;
}

.message-time {
  margin-top: 4px;
  font-size: 0.75rem;
  color: rgba(0, 0, 0, 0.4);
}

.message-container.user .message-time {
  color: rgba(255, 255, 255, 0.7);
}

/* 优化markdown内容的显示 */
.message.assistant .markdown-body {
  background: transparent;
  font-size: 14px;
  line-height: 1.6;
  color: #24292e;
}

.message.assistant .markdown-body pre {
  background-color: #f6f8fa;
  border-radius: 6px;
  margin: 8px 0;
}

.message.assistant .markdown-body code {
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
  padding: 2px 4px;
}

.chat-input {
  background-color: #fff;
  border-top: 1px solid #e0e0e0;
  padding: 12px 20px;
  display: flex;
  gap: 10px;
  align-items: flex-end;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
}

.chat-input textarea {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  height: 50px;
  max-height: 120px;
  transition: border-color 0.3s ease;
  font-family: inherit;
}

.chat-input textarea:focus {
  outline: none;
  border-color: #3498db;
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.15);
}

.chat-input button {
  padding: 10px 20px;
  background-color: #3498db;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s ease;
  white-space: nowrap;
  min-height: 50px;
}

.chat-input button:hover:not(:disabled) {
  background-color: #2980b9;
  transform: translateY(-1px);
}

.chat-input button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
  transform: none;
}

/* 响应式设计 */
@media screen and (max-width: 768px) {
  /* 显示移动端元素 */
  .mobile-settings-sidebar {
    display: block;
  }

  /* 隐藏桌面端元素 */
  .desktop-only {
    display: none !important;
  }

  .chat-messages {
    padding: 12px 16px;
    gap: 12px;
  }

  .avatar {
    width: 32px;
    height: 32px;
  }

  .message {
    padding: 10px 12px;
    font-size: 14px;
  }

  .chat-input {
    padding: 12px 16px;
    gap: 8px;
  }

  .chat-input textarea {
    min-height: 44px;
    max-height: 88px;
    padding: 8px 12px;
    font-size: 14px;
  }

  .chat-input button {
    padding: 8px 16px;
    min-height: 44px;
    font-size: 14px;
  }
}

/* 桌面端确保移动端元素隐藏 */
@media screen and (min-width: 769px) {
  .mobile-settings-sidebar {
    display: none !important;
  }

  .mobile-overlay {
    display: none !important;
  }
}

/* 滚动条样式 */
.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track {
  background: #f1f5f9;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

.tabs-container::-webkit-scrollbar {
  height: 4px;
}

.tabs-container::-webkit-scrollbar-track {
  background: #f1f5f9;
}

.tabs-container::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 2px;
}

.mobile-settings-content::-webkit-scrollbar {
  width: 4px;
}

.mobile-settings-content::-webkit-scrollbar-track {
  background: #f1f5f9;
}

.mobile-settings-content::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 2px;
}
</style>

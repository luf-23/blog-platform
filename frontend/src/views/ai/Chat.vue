<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from "vue";
import { storeToRefs } from "pinia";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Plus,
  Delete,
  ChatLineRound,
  Position,
  Setting,
  CircleClose
} from "@element-plus/icons-vue";
import "github-markdown-css";

import { useChatStore } from "../../store/chat.js";
import { useUserInfoStore } from "../../store/userInfo.js";
import { chatStreamService } from "../../api/ai.js";
import { getUserInfoService } from "../../api/user.js";
import { renderAssistantMessage } from "../../utils/markdown/chat/assistant.js";
import { renderUserMessage } from "../../utils/markdown/chat/user.js";
import EmptyState from "../../components/common/EmptyState.vue";
import { DEFAULT_AVATAR_URL as defaultAvatar } from "../../constants/assets.js";

const chatStore = useChatStore();
const userInfoStore = useUserInfoStore();
const { chatList, currentChatId } = storeToRefs(chatStore);
const { userInfo } = storeToRefs(userInfoStore);

const MODELS = [
  { value: "deepseek-v3", label: "DeepSeek V3" },
  { value: "deepseek-r1", label: "DeepSeek R1" },
  { value: "qwq-plus", label: "QwQ Plus" },
  { value: "qwen-max-2025-01-25", label: "Qwen Max" }
];
const TEMPERATURES = Array.from({ length: 11 }, (_, i) => i / 10);

const selectedModel = ref("deepseek-v3");
const temperature = ref(0.4);
const inputMessage = ref("");
const isLoading = ref(false);
const isThinking = ref(false);

const settingsOpen = ref(false);
const sessionsOpen = ref(false);
const chatContainer = ref(null);

const currentChat = computed(() =>
  chatList.value.find((c) => c.id === currentChatId.value)
);
const currentMessages = computed(() => currentChat.value?.messages || []);

const isEmpty = computed(() => !currentMessages.value.length);

onMounted(() => {
  if (!currentChatId.value) chatStore.createNewChat();
  nextTick(scrollToBottom);
});

function scrollToBottom() {
  const el = chatContainer.value;
  if (el) el.scrollTop = el.scrollHeight;
}

let buffer = "";

function parseChunk(chunk) {
  buffer += chunk;
  const lines = buffer.split("\n");
  buffer = lines.pop();
  let content = "";
  for (const line of lines) {
    if (!line.startsWith("data:")) continue;
    const data = line.slice(5).trim();
    if (data === "[DONE]") continue;
    try {
      const json = JSON.parse(data);
      const delta = json.choices?.[0]?.delta;
      if (!delta) continue;
      if (delta.reasoning_content === null && isThinking.value) {
        content += "\n\n**= = = 以上为思考过程 = = =**\n\n";
        isThinking.value = false;
      }
      const text = delta.content ?? delta.reasoning_content;
      if (text) content += text;
    } catch (e) {
      console.warn("AI 流式响应解析失败", e);
    }
  }
  return content;
}

async function send() {
  const text = inputMessage.value.trim();
  if (!text) {
    ElMessage.warning("请输入内容");
    return;
  }
  chatStore.addMessage("user", text);
  inputMessage.value = "";
  await nextTick();
  scrollToBottom();

  isLoading.value = true;
  isThinking.value = true;
  buffer = "";

  try {
    const history = chatStore
      .getCurrentChatHistory()
      .map((msg) => ({ role: msg.role, content: msg.content }));

    chatStore.addMessage("assistant", "");
    await getUserInfoService();

    const response = await chatStreamService({
      messages: history,
      model: selectedModel.value,
      temperature: temperature.value
    });

    if (!response.ok) throw new Error("网络错误");

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      const chunk = decoder.decode(value, { stream: true });
      const parsed = parseChunk(chunk);
      if (parsed) {
        chatStore.addChatToCurrent(parsed);
        nextTick(scrollToBottom);
      }
    }
  } catch (error) {
    ElMessage.error(error.message || "对话出错");
  } finally {
    isLoading.value = false;
  }
}

function createNew() {
  chatStore.createNewChat();
  sessionsOpen.value = false;
}

function selectChat(id) {
  chatStore.setCurrentChat(id);
  sessionsOpen.value = false;
}

function removeChat(id) {
  ElMessageBox.confirm("确定删除此对话吗？", "删除对话", {
    confirmButtonText: "删除",
    cancelButtonText: "取消",
    type: "warning"
  }).then(() => {
    chatStore.deleteChat(id);
    if (!currentChatId.value) chatStore.createNewChat();
  });
}

function clearChat() {
  ElMessageBox.confirm("清空当前对话所有消息？", "清空对话", {
    confirmButtonText: "清空",
    cancelButtonText: "取消",
    type: "warning"
  }).then(() => {
    chatStore.clearCurrentChat();
    ElMessage.success("已清空当前对话");
  });
}

function timeOf(value) {
  return new Date(value).toLocaleTimeString("zh-CN", { hour12: false });
}

onUnmounted(() => {
  buffer = "";
});

const SUGGESTIONS = [
  "为一篇 Vue 3 最佳实践文章生成清晰的大纲",
  "把这段技术说明改写得更适合初学者阅读",
  "根据文章内容生成 8 个标题",
  "从文章草稿中提炼摘要、关键词和结尾观点"
];

const QUICK_ACTIONS = [
  { label: "生成大纲", prompt: "请根据我的博客主题生成一份结构清晰、层次完整的文章大纲。" },
  { label: "修改文字", prompt: "请修改我接下来提供的文字，保留原意，修正语病并精简重复表达。" },
  { label: "提炼摘要", prompt: "请从我接下来提供的文章中提炼一段不超过 150 字的摘要。" },
  { label: "生成标题", prompt: "请根据我接下来提供的文章生成 8 个准确概括内容、避免夸张的标题。" }
];
</script>

<template>
  <div class="chat-shell">
    <aside
      class="chat-sessions"
      :class="{ 'chat-sessions--open': sessionsOpen }"
    >
      <header class="chat-sessions__head">
        <strong>对话历史</strong>
        <el-button size="small" type="primary" :icon="Plus" @click="createNew">
          新建
        </el-button>
      </header>
      <div class="chat-sessions__list">
        <button
          v-for="(chat, index) in chatList"
          :key="chat.id"
          class="session-item"
          :class="{ 'session-item--active': chat.id === currentChatId }"
          @click="selectChat(chat.id)"
        >
          <div class="session-item__main">
            <span class="session-item__title">
              {{ chat.title || `对话 ${index + 1}` }}
            </span>
            <span class="session-item__meta">
              {{ chat.messages.length }} 条消息
            </span>
          </div>
          <el-button
            link
            type="danger"
            :icon="Delete"
            @click.stop="removeChat(chat.id)"
          />
        </button>
      </div>
    </aside>

    <section class="chat-main">
      <header class="chat-toolbar">
        <div class="chat-toolbar__left">
          <el-button
            class="chat-toolbar__menu-btn"
            :icon="ChatLineRound"
            @click="sessionsOpen = !sessionsOpen"
          >
            对话列表
          </el-button>
          <div class="chat-toolbar__model">
            <span>模型</span>
            <el-select v-model="selectedModel" size="small" style="width: 160px">
              <el-option
                v-for="m in MODELS"
                :key="m.value"
                :label="m.label"
                :value="m.value"
              />
            </el-select>
          </div>
        </div>
        <div class="chat-toolbar__right">
          <el-button
            size="small"
            :icon="Setting"
            @click="settingsOpen = !settingsOpen"
            text
          >
            设置
          </el-button>
          <el-button size="small" type="danger" :icon="Delete" plain @click="clearChat">
            清空
          </el-button>
        </div>
      </header>

      <transition name="settings">
        <div v-if="settingsOpen" class="chat-settings bp-card">
          <div class="setting-row">
            <label>采样温度</label>
            <el-select v-model="temperature" size="small">
              <el-option
                v-for="t in TEMPERATURES"
                :key="t"
                :label="t.toFixed(1)"
                :value="t"
              />
            </el-select>
          </div>
          <div class="setting-row">
            <label>当前模型</label>
            <el-select v-model="selectedModel" size="small">
              <el-option
                v-for="m in MODELS"
                :key="m.value"
                :label="m.label"
                :value="m.value"
              />
            </el-select>
          </div>
        </div>
      </transition>

      <div class="chat-messages" ref="chatContainer">
        <div v-if="isEmpty" class="chat-welcome">
          <div class="chat-welcome__hero">
            <span class="chat-welcome__icon">
              <el-icon size="32"><ChatLineRound /></el-icon>
            </span>
            <h2>写作助手</h2>
            <p>可以生成大纲、修改文字、提取摘要和拟定标题。</p>
          </div>
          <div class="chat-welcome__suggestions">
            <button
              v-for="(s, i) in SUGGESTIONS"
              :key="i"
              class="suggestion bp-card bp-card-hover"
              @click="inputMessage = s"
            >
              {{ s }}
            </button>
          </div>
        </div>

        <template v-else>
          <div
            v-for="msg in currentMessages"
            :key="msg.id"
            class="message-row"
            :class="`message-row--${msg.role}`"
          >
            <el-avatar
              :size="36"
              :src="
                msg.role === 'assistant'
                  ? defaultAvatar
                  : userInfo?.avatarImage || defaultAvatar
              "
              class="message-row__avatar"
            />
            <div class="message-bubble" :class="`message-bubble--${msg.role}`">
              <div
                v-if="msg.role === 'assistant'"
                class="markdown-body"
                v-html="renderAssistantMessage(msg.content || '正在思考...')"
              />
              <div
                v-else
                class="message-text"
                v-html="renderUserMessage(msg.content)"
              />
              <div class="message-time">{{ timeOf(msg.timestamp) }}</div>
            </div>
          </div>
        </template>
      </div>

      <footer class="chat-input">
        <textarea
          v-model="inputMessage"
          rows="2"
          placeholder="输入消息后按 Ctrl + Enter 发送..."
          @keyup.ctrl.enter="send"
        />
        <el-button
          type="primary"
          :loading="isLoading"
          :icon="Position"
          @click="send"
        >
          发送
        </el-button>
      </footer>
    </section>

    <aside class="chat-context">
      <header class="context-head">
        <div><h3>快捷操作</h3></div>
      </header>

      <section class="context-section">
        <div class="context-actions">
          <button
            v-for="action in QUICK_ACTIONS"
            :key="action.label"
            @click="inputMessage = action.prompt"
          >
            <span>✦</span>{{ action.label }}
          </button>
        </div>
      </section>

      <div class="context-tip">
        <strong>不会自动读取文章</strong>
        <p>需要处理的文章或段落，请直接粘贴到对话中。</p>
      </div>
    </aside>

    <transition name="fade">
      <div
        v-if="sessionsOpen"
        class="chat-mobile-mask"
        @click="sessionsOpen = false"
      />
    </transition>
  </div>
</template>

<style scoped>
.chat-shell {
  --bp-color-bg: var(--c-bg);
  --bp-color-bg-elevated: var(--c-surface);
  --bp-color-bg-soft: var(--c-surface-muted);
  --bp-color-bg-hover: #f0efff;
  --bp-color-border: var(--c-border);
  --bp-color-divider: var(--c-border-light);
  --bp-color-text-primary: var(--c-text);
  --bp-color-text-secondary: var(--c-text-2);
  --bp-color-text-tertiary: var(--c-text-3);
  --bp-color-primary: var(--c-primary);
  --bp-color-primary-soft: rgba(81, 70, 229, 0.09);
  --bp-color-primary-soft-strong: rgba(81, 70, 229, 0.28);
  --bp-gradient-hero: linear-gradient(135deg, #5146e5, #7768ee);
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr) 286px;
  height: 100%;
  min-height: 0;
  background: var(--bp-color-bg);
  border: 1px solid var(--bp-color-border);
  border-radius: 18px;
  overflow: hidden;
}

.chat-sessions {
  border-right: 1px solid var(--bp-color-border);
  background: var(--bp-color-bg-elevated);
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-sessions__head {
  padding: 18px 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--bp-color-divider);
}

.chat-sessions__list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 12px;
  background: transparent;
  border: 1px solid transparent;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;
  width: 100%;
  text-align: left;
}

.session-item:hover {
  background: var(--bp-color-bg-soft);
}

.session-item--active {
  background: var(--bp-color-primary-soft);
  border-color: var(--bp-color-primary-soft-strong);
}

.session-item__main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.session-item__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--bp-color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 180px;
}

.session-item__meta {
  font-size: 11px;
  color: var(--bp-color-text-tertiary);
}

.chat-main {
  display: flex;
  flex-direction: column;
  min-height: 0;
  position: relative;
}

.chat-toolbar {
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  background: var(--bp-color-bg-elevated);
  border-bottom: 1px solid var(--bp-color-border);
  flex-wrap: wrap;
}

.chat-toolbar__left,
.chat-toolbar__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-toolbar__menu-btn {
  display: none;
}

.chat-toolbar__model {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--bp-color-text-tertiary);
}

.chat-settings {
  position: absolute;
  top: 64px;
  right: 16px;
  z-index: 5;
  width: 270px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid var(--bp-color-border);
  box-shadow: var(--c-shadow-lg);
}

.setting-row {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
  font-size: 12px;
  color: var(--bp-color-text-tertiary);
}

.chat-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 24px clamp(16px, 4vw, 36px);
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.chat-welcome {
  margin: auto;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 720px;
}

.chat-welcome__hero h2 {
  font-size: 24px;
}

.chat-welcome__hero p {
  margin-top: 6px;
  color: var(--bp-color-text-tertiary);
}

.chat-welcome__icon {
  display: inline-flex;
  width: 64px;
  height: 64px;
  border-radius: 22px;
  align-items: center;
  justify-content: center;
  background: var(--bp-gradient-hero);
  color: white;
  margin: 0 auto 16px;
  box-shadow: 0 10px 24px rgba(99, 102, 241, 0.35);
}

.chat-welcome__suggestions {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
  text-align: left;
}

.suggestion {
  padding: 14px 16px;
  border-radius: 14px;
  background: var(--bp-color-bg-elevated);
  border: 1px solid var(--bp-color-border);
  cursor: pointer;
  font-size: 13.5px;
  color: var(--bp-color-text-secondary);
  text-align: left;
}

.message-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.message-row--user {
  flex-direction: row-reverse;
}

.message-bubble {
  max-width: 78%;
  padding: 14px 18px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.7;
  background: var(--bp-color-bg-elevated);
  border: 1px solid var(--bp-color-border);
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.message-bubble--user {
  background: var(--bp-gradient-hero);
  color: white;
  border-color: transparent;
}

.chat-context {
  min-width: 0;
  padding: 22px 18px;
  border-left: 1px solid var(--bp-color-border);
  background: var(--bp-color-bg-elevated);
  overflow-y: auto;
}

.context-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 20px;
}

.context-head h3 {
  margin: 3px 0 0;
  font-size: 18px;
}

.context-tip p {
  margin: 0;
  color: var(--bp-color-text-tertiary);
  font-size: 12px;
  line-height: 1.65;
}

.context-section {
  padding: 19px 2px;
  border-bottom: 1px solid var(--bp-color-divider);
}

.context-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 12px;
}

.context-actions button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px;
  border: 1px solid var(--bp-color-border);
  border-radius: 10px;
  color: var(--bp-color-text-secondary);
  background: var(--bp-color-bg-elevated);
  font-size: 12px;
  cursor: pointer;
  transition: 0.2s ease;
}

.context-actions button:hover {
  color: var(--bp-color-primary);
  border-color: var(--bp-color-primary-soft-strong);
  background: var(--bp-color-primary-soft);
}

.context-actions span { color: var(--bp-color-primary); }

.context-tip {
  margin-top: 18px;
  padding: 13px 14px;
  border-radius: 12px;
  background: #fff8e8;
}

.context-tip strong {
  display: block;
  margin-bottom: 4px;
  color: #9a6200;
  font-size: 12px;
}

.message-bubble--assistant {
  background: var(--bp-color-bg-elevated);
}

.message-time {
  margin-top: 6px;
  font-size: 11px;
  color: color-mix(in srgb, currentColor 40%, transparent);
}

.markdown-body {
  background: transparent !important;
  color: inherit;
  font-size: 14px;
}

.message-text {
  white-space: pre-wrap;
}

.chat-input {
  padding: 14px clamp(16px, 4vw, 36px);
  display: flex;
  gap: 10px;
  align-items: flex-end;
  background: var(--bp-color-bg-elevated);
  border-top: 1px solid var(--bp-color-border);
}

.chat-input textarea {
  flex: 1;
  resize: none;
  border-radius: 12px;
  border: 1px solid var(--bp-color-border);
  background: var(--bp-color-bg);
  color: var(--bp-color-text-primary);
  padding: 10px 14px;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
  outline: none;
  transition: border-color 0.2s ease;
  min-height: 48px;
  max-height: 160px;
}

.chat-input textarea:focus {
  border-color: var(--bp-color-primary);
  box-shadow: 0 0 0 3px var(--bp-color-primary-soft);
}

.chat-mobile-mask {
  display: none;
}

.bp-card {
  background: var(--bp-color-bg-elevated);
  border: 1px solid var(--bp-color-border);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18);
}

.bp-card-hover {
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}
.bp-card-hover:hover {
  transform: translateY(-1px);
  border-color: var(--bp-color-primary-soft-strong);
  background: var(--bp-color-bg-soft);
}

.settings-enter-active,
.settings-leave-active {
  transition: max-height 0.2s ease, opacity 0.2s ease, padding 0.2s ease;
  overflow: hidden;
}

.settings-enter-from,
.settings-leave-to {
  max-height: 0;
  opacity: 0;
  padding-top: 0;
  padding-bottom: 0;
}

.settings-enter-to,
.settings-leave-from {
  max-height: 200px;
  opacity: 1;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 900px) {
  .chat-shell {
    grid-template-columns: 1fr;
  }
  .chat-toolbar__menu-btn {
    display: inline-flex;
  }
  .chat-sessions {
    position: fixed;
    top: var(--bp-header-height);
    left: 0;
    bottom: 0;
    width: 280px;
    z-index: 200;
    transform: translateX(-100%);
    transition: transform 0.25s ease;
    box-shadow: var(--bp-shadow-lg);
  }
  .chat-sessions--open {
    transform: translateX(0);
  }
  .chat-mobile-mask {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.4);
    z-index: 150;
  }
  .message-bubble {
    max-width: 88%;
  }
}

@media (max-width: 1180px) {
  .chat-shell { grid-template-columns: 250px minmax(0, 1fr); }
  .chat-context { display: none; }
}

@media (max-width: 900px) {
  .chat-shell { grid-template-columns: 1fr; }
}
</style>

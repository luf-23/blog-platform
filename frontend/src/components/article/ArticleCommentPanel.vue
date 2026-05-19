<script setup>
import { ChatLineRound, Close } from "@element-plus/icons-vue";
import CommentItem from "../comment/CommentItem.vue";
import EmptyState from "../common/EmptyState.vue";

defineProps({
  commentList: { type: Array, default: () => [] },
  commentContent: { type: String, default: "" },
  commentLoading: { type: Boolean, default: false },
  total: { type: Number, default: 0 },
  totalAll: { type: Number, default: 0 },
  currentPage: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  currentUsername: { type: String, default: "" },
  isArticleAuthor: { type: Boolean, default: false },
  showClose: { type: Boolean, default: false }
});

const emit = defineEmits([
  "update:commentContent",
  "publish",
  "page-change",
  "delete",
  "toggle-like",
  "view-author",
  "close",
  "cancel-reply"
]);

function onInput(val) {
  emit("update:commentContent", val);
}
</script>

<template>
  <div class="comment-panel">
    <header class="comment-panel__head">
      <div class="comment-panel__head-left">
        <el-icon class="comment-panel__icon"><ChatLineRound /></el-icon>
        <h2 class="comment-panel__title">评论</h2>
        <span class="comment-panel__badge">{{ totalAll }}</span>
      </div>
      <button
        v-if="showClose"
        type="button"
        class="comment-panel__close"
        aria-label="收起评论"
        @click="emit('close')"
      >
        <el-icon><Close /></el-icon>
      </button>
    </header>

    <div class="comment-panel__composer">
      <el-input
        :model-value="commentContent"
        type="textarea"
        :rows="2"
        placeholder="说点什么..."
        resize="none"
        @update:model-value="onInput"
        @focus="emit('cancel-reply')"
      />
      <el-button
        type="primary"
        class="comment-panel__send"
        :loading="commentLoading"
        @click="emit('publish')"
      >
        发送
      </el-button>
    </div>

    <div class="comment-panel__list">
      <CommentItem
        v-for="comment in commentList"
        :key="comment.commentId"
        variant="panel"
        :comment="comment"
        :current-username="currentUsername"
        :is-article-author="isArticleAuthor"
        @delete="emit('delete', $event)"
        @toggle-like="emit('toggle-like', $event)"
        @view-author="emit('view-author', $event)"
      />
      <EmptyState
        v-if="!commentList.length"
        title="暂无评论"
        description="来发第一条吧"
      />
    </div>

    <footer v-if="total > pageSize" class="comment-panel__foot">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        small
        background
        @current-change="emit('page-change', $event)"
      />
    </footer>
  </div>
</template>

<style scoped>
.comment-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: var(--bp-color-bg-elevated);
}

.comment-panel__head {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--bp-color-border);
}

.comment-panel__head-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.comment-panel__icon {
  font-size: 18px;
  color: var(--bp-color-primary);
}

.comment-panel__title {
  font-size: 15px;
  font-weight: 600;
  margin: 0;
}

.comment-panel__badge {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--bp-color-bg-soft);
  color: var(--bp-color-text-secondary);
}

.comment-panel__close {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: var(--bp-color-bg-soft);
  color: var(--bp-color-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s ease, color 0.2s ease;
}

.comment-panel__close:hover {
  background: var(--bp-color-bg-hover);
  color: var(--bp-color-text-primary);
}

.comment-panel__composer {
  flex-shrink: 0;
  padding: 12px 16px;
  border-bottom: 1px solid var(--bp-color-border);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.comment-panel__send {
  align-self: flex-end;
  border-radius: 8px;
}

.comment-panel__list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 12px 16px;
  scrollbar-width: thin;
}

.comment-panel__foot {
  flex-shrink: 0;
  padding: 10px 16px 14px;
  border-top: 1px solid var(--bp-color-border);
  display: flex;
  justify-content: center;
}
</style>

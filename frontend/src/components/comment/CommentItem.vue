<script setup>
import { inject, computed, ref } from "vue";
import {
  StarFilled,
  Star,
  Delete,
  ChatDotRound,
  ArrowDown,
  ArrowUp
} from "@element-plus/icons-vue";

defineOptions({ name: "CommentItem" });

const props = defineProps({
  comment: { type: Object, required: true },
  depth: { type: Number, default: 0 },
  variant: { type: String, default: "default" },
  currentUsername: { type: String, default: "" },
  isArticleAuthor: { type: Boolean, default: false }
});

const isPanel = computed(() => props.variant === "panel");
const isRoot = computed(() => props.depth === 0);

const emit = defineEmits(["delete", "toggle-like", "view-author"]);

const commentActions = inject("commentActions", null);

const isReplying = computed(
  () => commentActions?.replyingId?.value === props.comment.commentId
);

const replyDraft = computed({
  get: () => commentActions?.replyDraft.value ?? "",
  set: (v) => {
    if (commentActions?.replyDraft) commentActions.replyDraft.value = v;
  }
});

const replyCount = computed(() => countDescendants(props.comment.children));

const repliesOpen = ref(true);

const likeCount = computed(() => Number(props.comment.likeCount) || 0);

function countDescendants(children) {
  if (!children?.length) return 0;
  return children.reduce(
    (n, c) => n + 1 + countDescendants(c.children),
    0
  );
}

function formatDate(value) {
  if (!value) return "";
  return new Date(value).toLocaleString("zh-CN", { hour12: false });
}

function canDelete() {
  return (
    props.currentUsername === props.comment.username || props.isArticleAuthor
  );
}

function onReplyClick() {
  commentActions?.startReply(props.comment);
}

function onSubmitReply() {
  commentActions?.submitReply();
}

function onCancelReply() {
  commentActions?.cancelReply();
}

function toggleReplies() {
  repliesOpen.value = !repliesOpen.value;
}
</script>

<template>
  <article
    class="comment"
    :class="{
      'comment--nested': depth > 0,
      'comment--panel': isPanel,
      'comment--panel-nested': isPanel && depth > 0,
      'comment--root': isRoot
    }"
  >
    <el-avatar
      :size="isPanel ? (depth > 0 ? 28 : 32) : depth > 0 ? 32 : 40"
      :src="comment.avatar || '/avatar/avatar1.png'"
      class="comment__avatar"
      @click="emit('view-author', comment.username)"
    />
    <div class="comment__body">
      <header class="comment__head">
        <div class="comment__meta">
          <strong
            class="comment__name"
            @click="emit('view-author', comment.username)"
          >
            {{ comment.nickname || comment.username }}
          </strong>
          <template v-if="comment.parentId && comment.replyToUsername">
            <span class="comment__reply-label">回复</span>
            <strong
              class="comment__name comment__name--target"
              @click="emit('view-author', comment.replyToUsername)"
            >
              {{ comment.replyToNickname || comment.replyToUsername }}
            </strong>
          </template>
        </div>
        <span class="comment__time">{{ formatDate(comment.createTime) }}</span>
      </header>
      <p class="comment__content">{{ comment.content }}</p>
      <div class="comment__actions">
        <button
          type="button"
          class="action-btn action-btn--like"
          :class="{ 'action-btn--liked': comment.isLiked }"
          @click="emit('toggle-like', comment)"
        >
          <el-icon>
            <StarFilled v-if="comment.isLiked" />
            <Star v-else />
          </el-icon>
          <span class="action-btn__text">赞</span>
          <span class="action-btn__count">{{ likeCount }}</span>
        </button>
        <button type="button" class="action-btn" @click="onReplyClick">
          <el-icon><ChatDotRound /></el-icon>
          回复
        </button>
        <button
          v-if="canDelete()"
          type="button"
          class="action-btn action-btn--danger"
          @click="emit('delete', comment.commentId)"
        >
          <el-icon><Delete /></el-icon>
          删除
        </button>
      </div>

      <div v-if="isReplying" class="comment__reply-box">
        <el-input
          v-model="replyDraft"
          type="textarea"
          :rows="2"
          :placeholder="`回复 ${comment.nickname || comment.username}...`"
          resize="none"
        />
        <div class="comment__reply-actions">
          <el-button size="small" @click="onCancelReply">取消</el-button>
          <el-button
            type="primary"
            size="small"
            :loading="commentActions?.submitting?.value"
            @click="onSubmitReply"
          >
            发表回复
          </el-button>
        </div>
      </div>

      <button
        v-if="isRoot && replyCount > 0"
        type="button"
        class="comment__replies-toggle"
        @click="toggleReplies"
      >
        <el-icon>
          <ArrowUp v-if="repliesOpen" />
          <ArrowDown v-else />
        </el-icon>
        {{ repliesOpen ? "收起" : "展开" }} {{ replyCount }} 条回复
      </button>
    </div>
  </article>

  <div
    v-if="comment.children?.length && (!isRoot || repliesOpen)"
    class="comment__children"
  >
    <CommentItem
      v-for="child in comment.children"
      :key="child.commentId"
      :comment="child"
      :depth="depth + 1"
      :variant="variant"
      :current-username="currentUsername"
      :is-article-author="isArticleAuthor"
      @delete="emit('delete', $event)"
      @toggle-like="emit('toggle-like', $event)"
      @view-author="emit('view-author', $event)"
    />
  </div>
</template>

<style scoped>
.comment {
  display: flex;
  gap: 12px;
  padding: 14px;
  border-radius: 14px;
  background: var(--bp-color-bg-soft);
  border: 1px solid var(--bp-color-border);
}

.comment--panel {
  padding: 12px 4px;
  margin: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  border-bottom: 1px solid var(--bp-color-divider);
}

.comment--panel-nested {
  padding: 8px 4px 8px 10px;
  border-bottom: none;
  border-left: 2px solid var(--bp-color-border);
  margin-left: 6px;
}

.comment__avatar {
  cursor: pointer;
  flex-shrink: 0;
}

.comment__body {
  flex: 1;
  min-width: 0;
}

.comment__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.comment__meta {
  display: inline-flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 6px;
}

.comment__reply-label {
  font-size: 12px;
  color: var(--bp-color-text-tertiary);
}

.comment__name {
  font-size: 14px;
  cursor: pointer;
  color: var(--bp-color-text-primary);
}

.comment__name:hover,
.comment__name--target:hover {
  color: var(--bp-color-primary);
}

.comment__name--target {
  font-weight: 600;
}

.comment__time {
  font-size: 12px;
  color: var(--bp-color-text-tertiary);
  flex-shrink: 0;
}

.comment__content {
  margin-top: 6px;
  font-size: 14px;
  color: var(--bp-color-text-secondary);
  line-height: 1.7;
  white-space: pre-wrap;
}

.comment--panel .comment__content {
  font-size: 13px;
}

.comment__actions {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}

.comment__replies-toggle {
  margin-top: 8px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: none;
  border-radius: 6px;
  background: var(--bp-color-bg-soft);
  color: var(--bp-color-text-secondary);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.comment__replies-toggle:hover {
  background: var(--bp-color-bg-hover);
  color: var(--bp-color-primary);
}

.comment__reply-box {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.comment__reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.comment__children {
  display: flex;
  flex-direction: column;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: transparent;
  border: none;
  color: var(--bp-color-text-tertiary);
  cursor: pointer;
  font-size: 13px;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s ease, color 0.2s ease;
}

.action-btn:hover {
  background: var(--bp-color-bg-hover);
  color: var(--bp-color-primary);
}

.action-btn--like {
  min-width: 52px;
}

.action-btn--like .action-btn__count {
  min-width: 1.2em;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  color: var(--bp-color-text-secondary);
}

.action-btn--liked {
  color: var(--bp-color-warning);
}

.action-btn--liked .action-btn__count {
  color: var(--bp-color-warning);
}

.action-btn--danger:hover {
  background: rgba(239, 68, 68, 0.1);
  color: var(--bp-color-danger);
}
</style>

<template>
  <article class="comment-item">
    <img :src="comment.avatar || defaultAvatar" class="comment-avatar avatar" />

    <div class="comment-main">
      <div class="comment-meta">
        <div class="comment-author-line">
          <span class="comment-author">{{ displayName(comment) }}</span>
          <span v-if="isAuthor(comment)" class="author-badge">作者</span>
        </div>
        <time class="comment-time">{{ formatDate(comment.createTime) }}</time>
      </div>

      <p class="comment-content">{{ comment.content }}</p>

      <CommentActions
        :comment="comment"
        :can-delete="canDelete(comment)"
        @like="toggleLike(comment)"
        @reply="openReply(comment)"
        @delete="deleteComment(comment)"
      />

      <Transition name="reply-panel">
        <div v-if="replyTarget" class="reply-panel">
          <img :src="currentUser?.avatarImage || defaultAvatar" class="reply-avatar avatar" />
          <div class="reply-editor">
            <el-input
              ref="replyInputRef"
              v-model="replyContent"
              type="textarea"
              :placeholder="`回复 ${displayName(replyTarget)}...`"
              :autosize="{ minRows: 2, maxRows: 5 }"
              resize="none"
            />
            <div class="reply-footer">
              <span class="char-count" :class="{ over: replyContent.length > 1000 }">
                {{ replyContent.length }}/1000
              </span>
              <button class="reply-cancel" @click="cancelReply">取消</button>
              <button class="reply-submit" :disabled="!canSubmitReply" @click="submitReply">
                <el-icon v-if="submitting" class="is-loading"><Loading /></el-icon>
                回复
              </button>
            </div>
          </div>
        </div>
      </Transition>

      <div v-if="visibleReplies.length" class="reply-list">
        <div v-for="reply in visibleReplies" :key="reply.commentId" class="reply-item">
          <img :src="reply.avatar || defaultAvatar" class="reply-avatar avatar" />
          <div class="reply-main">
            <div class="reply-meta">
              <span class="reply-author">{{ displayName(reply) }}</span>
              <span v-if="isAuthor(reply)" class="author-badge compact">作者</span>
              <time>{{ formatDate(reply.createTime) }}</time>
            </div>
            <p class="reply-content">
              <template v-if="shouldShowReplyTo(reply)">
                <span class="reply-to-text">回复</span>
                <span class="reply-to-name">@{{ reply.replyToNickname || reply.replyToUsername }}</span>
                <span class="reply-colon">：</span>
              </template>
              {{ reply.content }}
            </p>
            <CommentActions
              :comment="reply"
              :can-delete="canDelete(reply)"
              compact
              @like="toggleLike(reply)"
              @reply="openReply(reply)"
              @delete="deleteComment(reply)"
            />
          </div>
        </div>
      </div>

      <div v-if="comment.replyCount > visibleReplies.length || loadedFromServer" class="reply-more">
        <button v-if="hasMoreReplies" class="reply-more-btn" :disabled="loadingReplies" @click="loadMoreReplies">
          <el-icon v-if="loadingReplies" class="is-loading"><Loading /></el-icon>
          {{ loadingReplies ? '加载中...' : `展开更多回复（${remainingReplyCount}）` }}
        </button>
        <button v-else-if="visibleReplies.length > previewReplies.length" class="reply-more-btn muted" @click="collapseReplies">
          收起回复
        </button>
      </div>
    </div>
  </article>
</template>

<script setup>
import { ref, computed, nextTick, h } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { publishCommentService, deleteCommentService, getCommentRepliesService } from '../../api/comment.js'
import { likeCommentService, unlikeCommentService } from '../../api/commentLike.js'
import { useUserInfoStore } from '../../store/userInfo.js'
import { DEFAULT_AVATAR_URL as defaultAvatar } from '../../constants/assets.js'

const props = defineProps({
  comment: { type: Object, required: true },
  articleId: { type: Number, required: true },
  authorId: { type: Number, default: null }
})
const emit = defineEmits(['reply-submitted', 'deleted'])

const userInfoStore = useUserInfoStore()
const currentUser = computed(() => userInfoStore.userInfo)
const REPLY_PAGE_SIZE = 10

const replyTarget = ref(null)
const replyContent = ref('')
const submitting = ref(false)
const replyInputRef = ref()
const replies = ref([...(props.comment.children || [])])
const repliesPage = ref(1)
const loadingReplies = ref(false)
const loadedFromServer = ref(false)

const previewReplies = computed(() => props.comment.children || [])
const visibleReplies = computed(() => replies.value)
const hasMoreReplies = computed(() => (props.comment.replyCount || 0) > visibleReplies.value.length)
const remainingReplyCount = computed(() => Math.max(0, (props.comment.replyCount || 0) - visibleReplies.value.length))
const canSubmitReply = computed(() => replyContent.value.trim() && replyContent.value.length <= 1000 && !submitting.value)

const CommentActions = {
  props: {
    comment: { type: Object, required: true },
    canDelete: { type: Boolean, default: false },
    compact: { type: Boolean, default: false }
  },
  emits: ['like', 'reply', 'delete'],
  setup(actionProps, { emit: actionEmit }) {
    return () => h('div', { class: ['comment-actions', { compact: actionProps.compact }] }, [
      h('button', {
        class: ['comment-action like-action', { liked: actionProps.comment.isLiked }],
        onClick: () => actionEmit('like')
      }, [
        h('span', { class: 'heart-icon' }, actionProps.comment.isLiked ? '♥' : '♡'),
        actionProps.comment.likeCount ? h('span', actionProps.comment.likeCount) : null
      ]),
      currentUser.value ? h('button', {
        class: 'comment-action',
        onClick: () => actionEmit('reply')
      }, '回复') : null,
      actionProps.canDelete ? h('button', {
        class: 'comment-action delete-action',
        onClick: () => actionEmit('delete')
      }, '删除') : null
    ])
  }
}

function displayName(comment) {
  return comment.nickname || comment.username || '匿名用户'
}

function isAuthor(comment) {
  return props.authorId != null && comment.userId === props.authorId
}

function canDelete(comment) {
  if (!currentUser.value) return false
  return (
    currentUser.value.userId === comment.userId ||
    currentUser.value.username === 'admin' ||
    currentUser.value.role === 'admin'
  )
}

function shouldShowReplyTo(reply) {
  return reply.replyToUserId && reply.replyToUserId !== props.comment.userId
}

function openReply(comment) {
  if (!currentUser.value) {
    ElMessage.warning('请先登录')
    return
  }
  replyTarget.value = comment
  nextTick(() => replyInputRef.value?.focus())
}

function cancelReply() {
  replyTarget.value = null
  replyContent.value = ''
}

async function submitReply() {
  if (!canSubmitReply.value || !replyTarget.value) return
  submitting.value = true
  try {
    await publishCommentService({
      articleId: props.articleId,
      content: replyContent.value.trim(),
      parentId: props.comment.commentId,
      replyToUserId: replyTarget.value.userId
    })
    ElMessage.success('回复成功')
    replyContent.value = ''
    replyTarget.value = null
    emit('reply-submitted')
  } catch {}
  finally { submitting.value = false }
}

async function deleteComment(comment) {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
    await deleteCommentService(comment.commentId)
    ElMessage.success('已删除')
    emit('deleted')
  } catch {}
}

async function toggleLike(comment) {
  if (!currentUser.value) { ElMessage.warning('请先登录'); return }
  try {
    if (comment.isLiked) {
      await unlikeCommentService(comment.commentId)
      comment.isLiked = false
      comment.likeCount = Math.max(0, (comment.likeCount || 1) - 1)
    } else {
      await likeCommentService(comment.commentId)
      comment.isLiked = true
      comment.likeCount = (comment.likeCount || 0) + 1
    }
  } catch {}
}

async function loadMoreReplies() {
  loadingReplies.value = true
  try {
    const nextPage = loadedFromServer.value ? repliesPage.value + 1 : 1
    const res = await getCommentRepliesService({
      articleId: props.articleId,
      rootId: props.comment.commentId,
      page: nextPage,
      pageSize: REPLY_PAGE_SIZE
    })
    const list = res.data.list || []
    if (nextPage === 1) {
      replies.value = list
    } else {
      const existingIds = new Set(replies.value.map(item => item.commentId))
      replies.value.push(...list.filter(item => !existingIds.has(item.commentId)))
    }
    repliesPage.value = nextPage
    loadedFromServer.value = true
  } finally {
    loadingReplies.value = false
  }
}

function collapseReplies() {
  replies.value = [...previewReplies.value]
  repliesPage.value = 1
  loadedFromServer.value = false
}

function formatDate(time) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 2592000000) return `${Math.floor(diff / 86400000)} 天前`
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
</script>

<style scoped>
.comment-item {
  display: flex;
  gap: 14px;
  padding: 22px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
}
.comment-item:last-child { border-bottom: none; }

.comment-avatar {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.08);
}

.comment-main,
.reply-main {
  flex: 1;
  min-width: 0;
}

.comment-meta,
.reply-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.comment-author-line,
.reply-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.comment-author,
.reply-author {
  font-weight: 600;
  font-size: 14px;
  color: var(--c-text);
}

.author-badge {
  padding: 1px 6px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, #eef2ff, #e0e7ff);
  color: var(--c-primary);
  font-size: 11px;
  font-weight: 700;
  line-height: 18px;
}
.author-badge.compact {
  font-size: 10px;
  line-height: 16px;
}

.comment-time,
.reply-meta time {
  font-size: 12px;
  color: var(--c-text-4);
  white-space: nowrap;
  flex-shrink: 0;
}

.comment-content {
  font-size: 15px;
  color: var(--c-text);
  line-height: 1.75;
  word-break: break-word;
  margin-bottom: 10px;
}

:deep(.comment-actions) {
  display: flex;
  align-items: center;
  gap: 14px;
}

:deep(.comment-actions.compact) {
  gap: 12px;
  margin-top: 5px;
}

:deep(.comment-action) {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 0;
  border: none;
  background: none;
  font-size: 13px;
  font-weight: 500;
  color: var(--c-text-3);
  cursor: pointer;
  transition: all var(--transition);
}
:deep(.comment-action:hover) { color: var(--c-primary); }
:deep(.like-action.liked),
:deep(.like-action:hover) { color: var(--c-danger); }
:deep(.heart-icon) { font-size: 16px; line-height: 1; }
:deep(.delete-action:hover) { color: var(--c-danger); }

.reply-panel {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  margin-top: 14px;
  padding: 14px;
  background: linear-gradient(180deg, var(--c-surface-2), rgba(249, 250, 251, 0.72));
  border: 1px solid var(--c-border-light);
  border-radius: 14px;
}

.reply-avatar {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
}

.reply-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.reply-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.char-count {
  flex: 1;
  font-size: 12px;
  color: var(--c-text-4);
}
.char-count.over { color: var(--c-danger); }

.reply-cancel,
.reply-submit {
  border: none;
  border-radius: var(--radius-full);
  padding: 7px 15px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition);
}
.reply-cancel {
  color: var(--c-text-3);
  background: transparent;
}
.reply-cancel:hover {
  color: var(--c-text);
  background: var(--c-border-light);
}
.reply-submit {
  color: #fff;
  background: linear-gradient(135deg, var(--c-primary), #6366f1);
  box-shadow: 0 8px 18px rgba(var(--c-primary-rgb), 0.22);
}
.reply-submit:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

.reply-list {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.reply-item {
  display: flex;
  gap: 10px;
  padding: 12px 14px;
  background: rgba(248, 250, 252, 0.82);
  border: 1px solid rgba(226, 232, 240, 0.82);
  border-radius: 14px;
}

.reply-content {
  margin-bottom: 0;
  color: var(--c-text-2);
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.reply-to-text,
.reply-colon {
  color: var(--c-text-3);
}

.reply-to-name {
  margin: 0 2px;
  color: var(--c-primary);
  font-weight: 600;
}

.reply-more {
  margin-top: 12px;
  padding-left: 40px;
}

.reply-more-btn {
  border: none;
  background: transparent;
  color: var(--c-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  padding: 4px 0;
  transition: color var(--transition);
}
.reply-more-btn:hover { color: var(--c-primary-hover); }
.reply-more-btn.muted { color: var(--c-text-4); }

.reply-panel-enter-active,
.reply-panel-leave-active {
  transition: all 0.18s ease;
}
.reply-panel-enter-from,
.reply-panel-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

[data-theme="dark"] .reply-panel {
  background: rgba(34, 34, 40, 0.82);
}
[data-theme="dark"] .reply-item {
  background: rgba(34, 34, 40, 0.66);
  border-color: var(--c-border);
}

@media (max-width: 640px) {
  .comment-item { gap: 10px; padding: 18px 0; }
  .comment-avatar { width: 38px; height: 38px; }
  .comment-meta { align-items: flex-start; flex-direction: column; gap: 2px; }
  .reply-more { padding-left: 0; }
}
</style>

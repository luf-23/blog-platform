<template>
  <article class="comment-item">
    <img :src="comment.avatar || defaultAvatar" class="comment-avatar avatar" alt="" />
    <div class="comment-main">
      <div class="comment-author-line">
        <span class="comment-author">{{ displayName(comment) }}</span>
        <span v-if="isAuthor(comment)" class="author-badge">作者</span>
      </div>
      <p class="comment-content">{{ comment.content }}</p>
      <div class="comment-footer">
        <time :datetime="comment.createTime" :title="formatExactDate(comment.createTime)">{{ formatRelativeTime(comment.createTime) }}</time>
        <CommentActions
          :comment="comment"
          :can-delete="canDelete(comment)"
          @like="toggleLike(comment)"
          @reply="openReply(comment)"
          @delete="deleteComment(comment)"
        />
      </div>

      <button v-if="!expanded && totalReplies > 0" type="button" class="reply-toggle" :disabled="loadingReplies" @click="expandReplies">
        <i></i>
        <el-icon v-if="loadingReplies" class="is-loading"><Loading /></el-icon>
        <span>{{ loadingReplies ? '正在加载回复' : `展开 ${totalReplies} 条回复` }}</span>
        <svg v-if="!loadingReplies" viewBox="0 0 24 24"><path d="m7 10 5 5 5-5" /></svg>
      </button>

      <div v-if="expanded" class="reply-group">
        <div v-if="loadingReplies && replies.length === 0" class="reply-loading">
          <el-icon class="is-loading"><Loading /></el-icon> 正在加载回复
        </div>
        <div v-for="reply in replies" :key="reply.commentId" class="reply-item">
          <img :src="reply.avatar || defaultAvatar" class="reply-avatar avatar" alt="" />
          <div class="reply-main">
            <div class="reply-heading">
              <span class="reply-author">{{ displayName(reply) }}</span>
              <span v-if="isAuthor(reply)" class="author-badge compact">作者</span>
              <template v-if="shouldShowReplyTarget(reply)">
                <span class="reply-word">回复</span>
                <strong class="reply-target">@{{ replyToName(reply) }}</strong>
              </template>
            </div>
            <p class="reply-content">{{ reply.content }}</p>
            <div class="comment-footer compact-footer">
              <time :datetime="reply.createTime" :title="formatExactDate(reply.createTime)">{{ formatRelativeTime(reply.createTime) }}</time>
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
        <div class="reply-pagination">
          <button v-if="hasMoreReplies" type="button" :disabled="loadingReplies" @click="loadReplies()">
            <el-icon v-if="loadingReplies" class="is-loading"><Loading /></el-icon>
            {{ loadingReplies ? '加载中' : `展开更多（剩余 ${remainingReplyCount} 条）` }}
          </button>
          <button type="button" class="collapse" @click="collapseReplies">
            收起 <svg viewBox="0 0 24 24"><path d="m7 14 5-5 5 5" /></svg>
          </button>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed, h, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { deleteCommentService, getCommentRepliesService } from '../../api/comment.js'
import { likeCommentService, unlikeCommentService } from '../../api/commentLike.js'
import { useUserInfoStore } from '../../store/userInfo.js'
import { DEFAULT_AVATAR_URL as defaultAvatar } from '../../constants/assets.js'

const props = defineProps({
  comment: { type: Object, required: true },
  articleId: { type: Number, required: true },
  authorId: { type: Number, default: null },
  refreshKey: { type: Number, default: 0 }
})
const emit = defineEmits(['reply', 'deleted'])
const userInfoStore = useUserInfoStore()
const currentUser = computed(() => userInfoStore.userInfo)
const REPLY_PAGE_SIZE = 5

const replies = ref([])
const totalReplies = ref(Number(props.comment.replyCount || 0))
const repliesPage = ref(0)
const expanded = ref(false)
const loadingReplies = ref(false)
const serverHasMore = ref(totalReplies.value > 0)
const hasMoreReplies = computed(() => serverHasMore.value && replies.value.length < totalReplies.value)
const remainingReplyCount = computed(() => Math.max(0, totalReplies.value - replies.value.length))

watch(() => props.comment.replyCount, value => { if (value != null) totalReplies.value = Number(value) })
watch(() => props.refreshKey, () => {
  serverHasMore.value = true
  if (expanded.value) loadReplies(true)
})

const CommentActions = {
  props: {
    comment: { type: Object, required: true },
    canDelete: { type: Boolean, default: false },
    compact: { type: Boolean, default: false }
  },
  emits: ['like', 'reply', 'delete'],
  setup(actionProps, { emit: actionEmit }) {
    return () => h('div', { class: ['comment-actions', { compact: actionProps.compact }] }, [
      currentUser.value ? h('button', { type: 'button', class: 'comment-action', onClick: () => actionEmit('reply') }, '回复') : null,
      actionProps.canDelete ? h('button', { type: 'button', class: 'comment-action delete-action', onClick: () => actionEmit('delete') }, '删除') : null,
      h('button', {
        type: 'button',
        class: ['comment-action', 'like-action', { liked: actionProps.comment.isLiked }],
        'aria-label': actionProps.comment.isLiked ? '取消点赞' : '点赞',
        onClick: () => actionEmit('like')
      }, [
        h('span', { class: 'heart-icon' }, actionProps.comment.isLiked ? '♥' : '♡'),
        h('span', actionProps.comment.likeCount || '赞')
      ])
    ])
  }
}

function displayName(item) { return item.nickname || item.username || '匿名用户' }
function replyToName(reply) { return reply.replyToNickname || reply.replyToUsername || '该用户' }
function shouldShowReplyTarget(reply) {
  const rootId = reply.rootId ?? props.comment.commentId
  return Number(reply.parentId) !== Number(rootId)
}
function isAuthor(item) { return props.authorId != null && Number(item.userId) === Number(props.authorId) }
function canDelete(item) {
  if (!currentUser.value) return false
  return Number(currentUser.value.userId) === Number(item.userId)
    || currentUser.value.username === 'admin'
    || currentUser.value.role === 'admin'
}

function openReply(item) {
  if (!currentUser.value) { ElMessage.warning('请先登录'); return }
  emit('reply', { target: item, rootId: props.comment.commentId })
}

async function deleteComment(item) {
  try {
    await ElMessageBox.confirm('删除后将无法恢复，确定继续吗？', '删除评论', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning', confirmButtonClass: 'el-button--danger'
    })
    await deleteCommentService(item.commentId)
    const isRoot = Number(item.commentId) === Number(props.comment.commentId)
    if (!isRoot) {
      replies.value = replies.value.filter(reply => reply.commentId !== item.commentId)
      totalReplies.value = Math.max(0, totalReplies.value - 1)
    }
    emit('deleted', { commentId: item.commentId, isRoot, rootId: props.comment.commentId })
    ElMessage.success('评论已删除')
  } catch (_) {}
}

async function toggleLike(item) {
  if (!currentUser.value) { ElMessage.warning('请先登录'); return }
  try {
    if (item.isLiked) {
      await unlikeCommentService(item.commentId)
      item.isLiked = false
      item.likeCount = Math.max(0, Number(item.likeCount || 1) - 1)
    } else {
      await likeCommentService(item.commentId)
      item.isLiked = true
      item.likeCount = Number(item.likeCount || 0) + 1
    }
  } catch (_) {}
}

function expandReplies() { expanded.value = true; loadReplies(true) }
async function loadReplies(reset = false) {
  if (loadingReplies.value || (!reset && !hasMoreReplies.value)) return
  loadingReplies.value = true
  const page = reset ? 1 : repliesPage.value + 1
  try {
    const res = await getCommentRepliesService({ articleId: props.articleId, rootId: props.comment.commentId, page, pageSize: REPLY_PAGE_SIZE })
    const data = res.data || {}
    const incoming = data.list || []
    if (reset) replies.value = incoming
    else {
      const existingIds = new Set(replies.value.map(item => item.commentId))
      replies.value.push(...incoming.filter(item => !existingIds.has(item.commentId)))
    }
    repliesPage.value = page
    totalReplies.value = Number(data.total ?? totalReplies.value)
    serverHasMore.value = Boolean(data.hasMore)
  } catch (_) {
    if (reset) expanded.value = false
    ElMessage.error('回复加载失败，请稍后重试')
  } finally { loadingReplies.value = false }
}
function collapseReplies() {
  expanded.value = false
  replies.value = []
  repliesPage.value = 0
  serverHasMore.value = totalReplies.value > 0
}

function parseTime(time) {
  if (!time) return null
  const date = new Date(time)
  return Number.isNaN(date.getTime()) ? null : date
}
function formatRelativeTime(time) {
  const date = parseTime(time)
  if (!date) return ''
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const thatDay = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const dayDiff = Math.floor((today - thatDay) / 86400000)
  if (dayDiff === 1) return '昨天'
  if (dayDiff === 2) return '前天'
  if (dayDiff > 2 && dayDiff < 7) return `${dayDiff}天前`
  if (dayDiff === 0) {
    const diff = Math.max(0, now - date)
    if (diff < 60000) return '刚刚'
    if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
    return `${Math.floor(diff / 3600000)}小时前`
  }
  if (date.getFullYear() === now.getFullYear()) return `${date.getMonth() + 1}月${date.getDate()}日`
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}
function formatExactDate(time) {
  const date = parseTime(time)
  if (!date) return ''
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
  }).format(date)
}
</script>

<style scoped>
.comment-item { display: flex; gap: 9px; padding: 12px 0; border-bottom: 1px solid var(--c-border); }
.comment-avatar { width: 32px; height: 32px; flex: 0 0 auto; }
.comment-main, .reply-main { min-width: 0; flex: 1; }
.comment-author-line, .reply-heading { display: flex; min-width: 0; align-items: center; flex-wrap: wrap; gap: 6px; }
.comment-author, .reply-author { overflow: hidden; color: var(--c-text-3); font-size: 13px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.author-badge { display: inline-flex; height: 18px; align-items: center; padding: 0 5px; border-radius: 4px; background: var(--c-primary-soft); color: var(--c-primary); font-size: 9px; font-weight: 700; }
.author-badge.compact { height: 16px; }
.comment-content { margin: 3px 0 2px; color: var(--c-text); font-size: 15px; line-height: 1.55; white-space: pre-wrap; word-break: break-word; }
.comment-footer { display: flex; min-height: 24px; align-items: center; gap: 10px; }
.comment-footer time { color: var(--c-text-4); font-size: 12px; white-space: nowrap; }
:deep(.comment-actions) { display: flex; min-width: 0; flex: 1; align-items: center; gap: 10px; }
:deep(.comment-action) { display: inline-flex; min-height: 24px; align-items: center; gap: 4px; padding: 1px 0; border: 0; background: transparent; color: var(--c-text-3); font-size: 12px; line-height: 1.4; transition: color var(--transition); }
:deep(.comment-action:hover) { color: var(--c-primary); }
:deep(.like-action) { margin-left: auto; }
:deep(.like-action.liked), :deep(.like-action:hover), :deep(.delete-action:hover) { color: var(--c-danger); }
:deep(.heart-icon) { font-size: 16px; line-height: 1; }
.reply-avatar { width: 24px; height: 24px; flex: 0 0 auto; }
.reply-toggle { display: inline-flex; min-height: 24px; align-items: center; gap: 6px; margin-top: 3px; padding: 1px 0; border: 0; background: transparent; color: var(--c-text-3); font-size: 12px; font-weight: 650; }
.reply-toggle i { width: 22px; height: 1px; background: var(--c-border-strong); }
.reply-toggle svg, .reply-pagination svg { width: 15px; height: 15px; fill: none; stroke: currentColor; stroke-width: 2; }
.reply-toggle:hover { color: var(--c-primary); }
.reply-group { margin-top: 5px; padding: 0 0 0 8px; border-left: 2px solid var(--c-border); }
.reply-item { display: flex; gap: 7px; padding: 6px 0; }
.reply-item + .reply-item { border-top: 1px solid color-mix(in srgb, var(--c-border) 72%, transparent); }
.reply-word { color: var(--c-text-4); font-size: 11px; }.reply-target { max-width: 150px; overflow: hidden; color: var(--c-primary); font-size: 11px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.reply-content { margin: 2px 0 1px; color: var(--c-text-2); font-size: 13px; line-height: 1.5; white-space: pre-wrap; word-break: break-word; }
.compact-footer { gap: 10px; }.reply-loading { display: flex; align-items: center; gap: 7px; padding: 8px 0; color: var(--c-text-4); font-size: 12px; }
.reply-pagination { display: flex; align-items: center; gap: 12px; padding: 2px 0 0 31px; }
.reply-pagination button { display: inline-flex; min-height: 24px; align-items: center; gap: 4px; padding: 1px 0; border: 0; background: transparent; color: var(--c-primary); font-size: 12px; font-weight: 650; }
.reply-pagination button.collapse { color: var(--c-text-4); }.reply-pagination button:disabled { cursor: wait; opacity: .6; }
@media (max-width: 640px) { .comment-item { gap: 8px; padding: 10px 0; }.comment-avatar { width: 28px; height: 28px; }.reply-group { margin-left: -4px; padding-left: 7px; } }
</style>

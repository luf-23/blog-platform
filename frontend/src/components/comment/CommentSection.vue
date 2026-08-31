<template>
  <section class="comment-section card" :class="{ 'is-panel': panel }">
    <div class="cs-header">
      <div class="cs-heading">
        <h3 class="cs-title">评论</h3>
        <span class="cs-count">{{ totalAll }}</span>
      </div>
      <button v-if="panel" class="cs-sort">最新</button>
      <p v-else class="cs-subtitle">{{ totalAll }} 条讨论</p>
    </div>

    <div class="cs-composer">
      <img :src="currentUser?.avatarImage || defaultAvatar" class="composer-avatar avatar" />
      <div class="cs-input-wrap">
        <el-input
          v-model="newComment"
          type="textarea"
          :placeholder="currentUser ? '写下你的评论...' : '登录后参与评论'"
          :autosize="{ minRows: 2, maxRows: 6 }"
          :disabled="!currentUser"
          resize="none"
        />
        <div class="cs-input-footer" v-if="currentUser">
          <span class="cs-char-count" :class="{ over: newComment.length > 1000 }">{{ newComment.length }}/1000</span>
          <button class="cs-submit" :disabled="!newComment.trim() || newComment.length > 1000 || submitting" @click="submitComment">
            <el-icon v-if="submitting" class="is-loading"><Loading /></el-icon>
            发布评论
          </button>
        </div>
        <router-link v-else to="/login" class="cs-submit login-link">登录后评论</router-link>
      </div>
    </div>

    <div v-if="loading && comments.length === 0" class="loading-spinner">
      <el-icon class="is-loading" :size="20"><Loading /></el-icon>
      <span style="margin-left:8px">加载评论...</span>
    </div>

    <div v-else-if="!loading && comments.length === 0" class="cs-empty">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--c-text-4)" stroke-width="1.5">
        <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/>
      </svg>
      <p>还没有评论，来发表第一条吧！</p>
    </div>

    <div v-else class="cs-list">
      <CommentItem
        v-for="comment in comments"
        :key="comment.commentId"
        :comment="comment"
        :article-id="articleId"
        :author-id="authorId"
        @reply-submitted="onReplySubmitted"
        @deleted="onCommentDeleted"
      />
    </div>

    <div v-if="hasMoreRoots" class="cs-pagination">
      <button class="cs-load-more" :disabled="loadingMore" @click="loadMoreComments">
        <el-icon v-if="loadingMore" class="is-loading"><Loading /></el-icon>
        <span>{{ loadingMore ? '加载中...' : '加载更多' }}</span>
        <svg v-if="!loadingMore" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M6 9l6 6 6-6"/>
        </svg>
      </button>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getCommentListService, publishCommentService } from '../../api/comment.js'
import { useUserInfoStore } from '../../store/userInfo.js'
import CommentItem from './CommentItem.vue'
import { DEFAULT_AVATAR_URL as defaultAvatar } from '../../constants/assets.js'

const props = defineProps({
  articleId: { type: Number, required: true },
  totalCount: { type: Number, default: 0 },
  authorId: { type: Number, default: null },
  panel: { type: Boolean, default: false }
})
const emit = defineEmits(['count-change'])

const userInfoStore = useUserInfoStore()
const currentUser = computed(() => userInfoStore.userInfo)

const PAGE_SIZE = 10

const comments = ref([])
const totalRoots = ref(0)
const totalAll = ref(props.totalCount)
const currentPage = ref(1)
const loading = ref(false)
const loadingMore = ref(false)
const newComment = ref('')
const submitting = ref(false)
const hasMoreRoots = computed(() => totalRoots.value > comments.value.length)

async function fetchComments(append = false) {
  if (append) loadingMore.value = true
  else loading.value = true
  try {
    const res = await getCommentListService({
      articleId: props.articleId,
      page: currentPage.value,
      pageSize: PAGE_SIZE
    })
    const data = res.data
    totalRoots.value = data.total
    totalAll.value = data.totalAll
    if (append) comments.value.push(...(data.list || []))
    else comments.value = data.list || []
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadMoreComments() {
  if (!hasMoreRoots.value || loadingMore.value) return
  currentPage.value++
  await fetchComments(true)
}

async function submitComment() {
  if (!newComment.value.trim()) return
  submitting.value = true
  try {
    await publishCommentService({
      articleId: props.articleId,
      content: newComment.value.trim(),
      parentId: null,
      replyToUserId: null
    })
    ElMessage.success('评论发布成功')
    newComment.value = ''
    totalAll.value++
    emit('count-change', totalAll.value)
    currentPage.value = 1
    await fetchComments()
  } catch {}
  finally { submitting.value = false }
}

function onReplySubmitted() {
  totalAll.value++
  emit('count-change', totalAll.value)
  currentPage.value = 1
  fetchComments()
}

function onCommentDeleted() {
  totalAll.value = Math.max(0, totalAll.value - 1)
  emit('count-change', totalAll.value)
  currentPage.value = 1
  fetchComments()
}

onMounted(() => fetchComments())
</script>

<style scoped>
.comment-section {
  padding: 24px 28px 28px;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.comment-section.is-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0;
  border: none;
  border-radius: 0;
  box-shadow: none;
  background: #fff;
}

.cs-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.is-panel .cs-header {
  align-items: center;
  margin-bottom: 0;
  padding: 18px 18px 14px;
  border-bottom: 1px solid #eef0f5;
}

.cs-heading {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.cs-title {
  font-size: 18px;
  font-weight: 800;
  color: var(--c-text);
  letter-spacing: -0.02em;
}

.is-panel .cs-title {
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0;
}

.cs-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: var(--c-text-4);
}

.is-panel .cs-subtitle {
  display: none;
}

.cs-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 30px;
  height: 23px;
  background: var(--c-primary-soft);
  color: var(--c-primary);
  font-size: 13px;
  font-weight: 800;
  padding: 0 8px;
  border-radius: var(--radius-full);
}

.is-panel .cs-count {
  min-width: 28px;
  height: 22px;
  padding: 0 8px;
  font-size: 12px;
  background: #f3f4f6;
  color: #6b7280;
}

.cs-sort {
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 999px;
  padding: 0 12px;
  background: #f8fafc;
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
}
.cs-sort:hover {
  color: #111827;
  background: #f1f5f9;
}

.cs-composer {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  margin-bottom: 20px;
  padding: 14px 15px;
  border: 1px solid var(--c-border);
  border-radius: 10px;
  background: var(--c-surface-2);
}

.is-panel .cs-composer {
  margin: 0;
  padding: 16px 18px 18px;
  gap: 12px;
  border: none;
  border-bottom: 1px solid #eef0f5;
  border-radius: 0;
  background: #fff;
}

.composer-avatar {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
}

.is-panel .composer-avatar {
  width: 30px;
  height: 30px;
}

.cs-input-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cs-input-wrap :deep(.el-textarea__inner) {
  min-height: 62px !important;
  border: none;
  box-shadow: none;
  background: transparent;
  padding: 5px 2px;
  font-size: 13px;
  line-height: 1.7;
}

.is-panel .cs-input-wrap :deep(.el-textarea__inner) {
  min-height: 72px !important;
  padding: 12px 14px;
  border-radius: 9px !important;
  background: #fff;
  box-shadow: 0 0 0 1px #e3e7ef inset !important;
}

.cs-input-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.cs-char-count {
  font-size: 12px;
  color: var(--c-text-4);
}
.cs-char-count.over { color: var(--c-danger); }

.cs-submit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 86px;
  border: none;
  border-radius: 6px;
  padding: 8px 15px;
  color: #fff;
  background: var(--c-primary);
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
  cursor: pointer;
  box-shadow: none;
  transition: all var(--transition);
}
.cs-submit:hover {
  background: var(--c-primary-hover);
}
.cs-submit:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}
.login-link { align-self: flex-end; }

.is-panel .cs-submit {
  min-width: 82px;
  padding: 8px 14px;
  border-radius: 8px;
  box-shadow: none;
}

.cs-empty {
  text-align: center;
  padding: 40px 20px;
  color: var(--c-text-4);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.cs-list {
  display: flex;
  flex-direction: column;
  border-top: 1px solid var(--c-border);
}

.is-panel .cs-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 18px;
  border-top: none;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.is-panel .cs-list::-webkit-scrollbar {
  display: none;
}

.is-panel :deep(.comment-item) {
  gap: 11px;
  padding: 18px 0;
}

.is-panel :deep(.comment-avatar) {
  width: 32px;
  height: 32px;
  box-shadow: none;
}

.is-panel :deep(.comment-content) {
  margin-bottom: 8px;
  font-size: 13px;
  line-height: 1.65;
}

.is-panel :deep(.comment-meta),
.is-panel :deep(.reply-meta) {
  margin-bottom: 5px;
}

.is-panel :deep(.reply-list) {
  margin-top: 10px;
  gap: 10px;
}

.is-panel :deep(.reply-item) {
  padding: 0;
  border: none;
  background: transparent;
}

.is-panel :deep(.reply-avatar) {
  width: 24px;
  height: 24px;
}

.cs-pagination {
  display: flex;
  justify-content: center;
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid rgba(148, 163, 184, 0.16);
}

.is-panel .cs-pagination {
  flex-shrink: 0;
  margin-top: 0;
  padding: 12px 18px 18px;
  border-top: 1px solid #eef0f5;
  background: #fff;
}

.cs-load-more {
  width: 100%;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px solid #e3e7ef;
  border-radius: 10px;
  background: #fff;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  transition: all var(--transition);
}
.cs-load-more:hover {
  color: #4f46e5;
  border-color: #c7d2fe;
  background: #f8faff;
}
.cs-load-more:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

:deep(.el-pagination.is-background .el-pager li.is-active) {
  background: linear-gradient(135deg, var(--c-primary), #6366f1);
  box-shadow: 0 8px 18px rgba(var(--c-primary-rgb), 0.18);
}

[data-theme="dark"] .comment-section {
  box-shadow: none;
}
[data-theme="dark"] .cs-composer {
  background: var(--c-surface-2);
  border-color: var(--c-border);
}

@media (max-width: 640px) {
  .comment-section { padding: 22px 18px; }
  .cs-composer { padding: 14px; gap: 10px; }
  .composer-avatar { width: 36px; height: 36px; }
}
</style>

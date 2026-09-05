<template>
  <section class="comment-section card" :class="{ 'is-panel': panel }">
    <header class="cs-header">
      <div class="cs-heading">
        <h3 class="cs-title">评论</h3>
        <span class="cs-count">{{ totalAll }}</span>
      </div>
      <p class="cs-subtitle">分享你的想法，参与这场讨论</p>
      <button v-if="panel" type="button" class="cs-close" aria-label="关闭评论" @click="emit('close')">
        <svg viewBox="0 0 24 24"><path d="m6 6 12 12M18 6 6 18" /></svg>
      </button>
    </header>

    <div class="cs-composer" :class="{ 'is-replying': replyTarget, 'is-focused': composerFocused }">
      <Transition name="reply-context">
        <div v-if="replyTarget" class="cs-reply-context" aria-live="polite">
          <span class="cs-reply-icon">
            <svg viewBox="0 0 24 24"><path d="m9 7-5 5 5 5M5 12h8a6 6 0 0 1 6 6" /></svg>
          </span>
          <p><span>正在回复</span><strong>@{{ replyTargetName }}</strong></p>
          <button type="button" aria-label="取消回复" title="取消回复" @click="cancelReply">
            <svg viewBox="0 0 24 24"><path d="m7 7 10 10M17 7 7 17" /></svg>
          </button>
        </div>
      </Transition>

      <div class="cs-composer-row">
        <img :src="currentUser?.avatarImage || defaultAvatar" class="composer-avatar avatar" alt="" />
        <div class="cs-input-wrap">
          <el-input
            ref="composerInput"
            v-model="newComment"
            type="textarea"
            :placeholder="composerPlaceholder"
            :autosize="{ minRows: 1, maxRows: 5 }"
            :disabled="!currentUser"
            resize="none"
            maxlength="1000"
            @focus="composerFocused = true"
            @blur="composerFocused = false"
            @keydown.ctrl.enter="submitComment"
            @keydown.meta.enter="submitComment"
            @keydown.esc="cancelReply"
          />
          <div v-if="currentUser" class="cs-input-footer">
            <span class="cs-input-hint">Ctrl + Enter 发送</span>
            <span class="cs-char-count" :class="{ visible: newComment.length }">{{ newComment.length }}/1000</span>
            <button type="button" class="cs-submit" :disabled="!canSubmit" @click="submitComment">
              <el-icon v-if="submitting" class="is-loading"><Loading /></el-icon>
              {{ submitting ? '发送中' : submitLabel }}
            </button>
          </div>
          <router-link v-else to="/login" class="cs-submit login-link">登录后评论</router-link>
        </div>
      </div>
    </div>

    <div v-if="loading && comments.length === 0" class="cs-initial-loading" aria-live="polite">
      <div v-for="index in 3" :key="index" class="comment-skeleton">
        <i></i><div><b></b><span></span><span></span></div>
      </div>
    </div>

    <div v-else-if="comments.length === 0" class="cs-empty">
      <span class="empty-bubble">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2Z" />
        </svg>
      </span>
      <strong>还没有评论</strong>
      <p>来做第一个发言的人吧</p>
    </div>

    <div v-else class="cs-list">
      <CommentItem
        v-for="comment in comments"
        :key="comment.commentId"
        :comment="comment"
        :article-id="articleId"
        :author-id="authorId"
        :refresh-key="replyRefreshKeys[comment.commentId] || 0"
        @reply="openComposerForReply"
        @deleted="onCommentDeleted"
      />

      <div ref="loadMoreTrigger" class="cs-scroll-sentinel" aria-hidden="true"></div>
      <div v-if="loadingMore" class="cs-list-status" aria-live="polite">
        <el-icon class="is-loading"><Loading /></el-icon>
        正在加载更多评论
      </div>
      <div v-else-if="!hasMoreRoots" class="cs-list-end"><span>已经到底了</span></div>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
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
const emit = defineEmits(['count-change', 'close'])
const userInfoStore = useUserInfoStore()
const currentUser = computed(() => userInfoStore.userInfo)
const PAGE_SIZE = 10

const comments = ref([])
const totalRoots = ref(0)
const totalAll = ref(props.totalCount)
const currentPage = ref(0)
const hasMoreRoots = ref(true)
const loading = ref(false)
const loadingMore = ref(false)
const newComment = ref('')
const submitting = ref(false)
const composerFocused = ref(false)
const composerInput = ref(null)
const replyTarget = ref(null)
const replyRootId = ref(null)
const replyRefreshKeys = ref({})
const loadMoreTrigger = ref(null)
let loadObserver = null

const replyTargetName = computed(() => replyTarget.value?.nickname || replyTarget.value?.username || '该用户')
const composerPlaceholder = computed(() => {
  if (!currentUser.value) return '登录后参与评论'
  return replyTarget.value ? `回复 @${replyTargetName.value}` : '友善表达你的观点…'
})
const submitLabel = computed(() => replyTarget.value ? '回复' : '发布')
const canSubmit = computed(() => Boolean(newComment.value.trim()) && !submitting.value && Boolean(currentUser.value))

async function fetchComments(page = 1, append = false) {
  if (loading.value || loadingMore.value) return
  append ? (loadingMore.value = true) : (loading.value = true)
  try {
    const res = await getCommentListService({ articleId: props.articleId, page, pageSize: PAGE_SIZE })
    const data = res.data || {}
    const incoming = data.list || []
    if (append) {
      const existingIds = new Set(comments.value.map(item => item.commentId))
      comments.value.push(...incoming.filter(item => !existingIds.has(item.commentId)))
    } else comments.value = incoming

    currentPage.value = page
    totalRoots.value = Number(data.total || 0)
    totalAll.value = Number(data.totalAll ?? props.totalCount)
    hasMoreRoots.value = data.hasMore ?? comments.value.length < totalRoots.value
  } catch (_) {
    if (append) ElMessage.error('更多评论加载失败，请稍后重试')
  } finally {
    loading.value = false
    loadingMore.value = false
    nextTick(setupLoadObserver)
  }
}

function loadMoreComments() {
  if (!hasMoreRoots.value || loading.value || loadingMore.value) return
  fetchComments(currentPage.value + 1, true)
}

function setupLoadObserver() {
  loadObserver?.disconnect()
  if (!loadMoreTrigger.value || !hasMoreRoots.value) return
  loadObserver = new IntersectionObserver(entries => {
    if (entries.some(entry => entry.isIntersecting)) loadMoreComments()
  }, { root: null, rootMargin: '0px 0px 180px 0px', threshold: 0.01 })
  loadObserver.observe(loadMoreTrigger.value)
}

async function submitComment() {
  const content = newComment.value.trim()
  if (!content || submitting.value) return
  const target = replyTarget.value
  const rootId = replyRootId.value
  submitting.value = true
  try {
    await publishCommentService({
      articleId: props.articleId,
      content,
      parentId: target?.commentId ?? null,
      replyToUserId: target?.userId ?? null
    })
    newComment.value = ''
    if (target && rootId != null) {
      const root = comments.value.find(item => Number(item.commentId) === Number(rootId))
      if (root) root.replyCount = Number(root.replyCount || 0) + 1
      totalAll.value += 1
      replyRefreshKeys.value = {
        ...replyRefreshKeys.value,
        [rootId]: Number(replyRefreshKeys.value[rootId] || 0) + 1
      }
      cancelReply(false)
      emit('count-change', totalAll.value)
      ElMessage.success('回复成功')
    } else {
      await fetchComments(1)
      emit('count-change', totalAll.value)
      ElMessage.success('评论发布成功')
    }
  } finally { submitting.value = false }
}

function openComposerForReply({ target, rootId } = {}) {
  if (!target) return
  replyTarget.value = target
  replyRootId.value = rootId
  nextTick(() => composerInput.value?.focus())
}

function cancelReply(preserveContent = true) {
  replyTarget.value = null
  replyRootId.value = null
  if (!preserveContent) newComment.value = ''
}

function onCommentDeleted({ commentId, isRoot, rootId } = {}) {
  totalAll.value = Math.max(0, totalAll.value - 1)
  if (isRoot) {
    comments.value = comments.value.filter(item => item.commentId !== commentId)
    totalRoots.value = Math.max(0, totalRoots.value - 1)
    hasMoreRoots.value = comments.value.length < totalRoots.value
  } else {
    const root = comments.value.find(item => Number(item.commentId) === Number(rootId))
    if (root) root.replyCount = Math.max(0, Number(root.replyCount || 1) - 1)
  }
  emit('count-change', totalAll.value)
}

watch(loadMoreTrigger, () => nextTick(setupLoadObserver))
watch(hasMoreRoots, () => nextTick(setupLoadObserver))
watch(() => props.totalCount, value => { if (!loading.value) totalAll.value = value })
watch(() => props.articleId, () => {
  comments.value = []
  currentPage.value = 0
  hasMoreRoots.value = true
  newComment.value = ''
  cancelReply(false)
  replyRefreshKeys.value = {}
  fetchComments(1)
})
onMounted(() => fetchComments(1))
onBeforeUnmount(() => loadObserver?.disconnect())
</script>

<style scoped>
.comment-section { padding: 20px 24px; border: 0; border-radius: 0; background: transparent; box-shadow: none; }
.comment-section.is-panel { display: flex; height: 100%; flex-direction: column; padding: 0; background: var(--c-surface); }
.cs-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.is-panel .cs-header { z-index: 2; order: 1; flex: 0 0 auto; flex-direction: row; align-items: center; margin: 0; padding: 8px 16px; border-bottom: 1px solid var(--c-border); background: var(--c-surface); }
.cs-heading { display: flex; align-items: baseline; gap: 9px; }
.cs-title { color: var(--c-text); font-size: 20px; font-weight: 800; letter-spacing: -.02em; }
.is-panel .cs-title { font-family: inherit; font-size: 18px; font-weight: 760; letter-spacing: -.01em; }
.is-panel .cs-count { font-size: 12px; }
.cs-count { color: var(--c-text-4); font-size: 13px; font-weight: 600; }
.cs-subtitle { color: var(--c-text-4); font-size: 12px; }
.is-panel .cs-subtitle { display: none; }
.cs-close { display: grid; width: 32px; height: 32px; margin-left: auto; place-items: center; border: 0; border-radius: 7px; background: transparent; color: var(--c-text-3); transition: background var(--transition), color var(--transition); }
.cs-close:hover { background: var(--c-surface-2); color: var(--c-text); }
.cs-close svg { width: 19px; height: 19px; fill: none; stroke: currentColor; stroke-linecap: round; stroke-width: 1.8; }
.cs-composer { display: flex; flex-direction: column; gap: 7px; margin-bottom: 12px; padding: 10px 12px; border: 1px solid var(--c-border); border-radius: 12px; background: var(--c-surface); transition: border-color var(--transition), box-shadow var(--transition); }
.cs-composer.is-focused { border-color: color-mix(in srgb, var(--c-primary) 38%, var(--c-border)); box-shadow: 0 0 0 3px color-mix(in srgb, var(--c-primary) 7%, transparent); }
.is-panel .cs-composer { z-index: 3; order: 3; flex: 0 0 auto; gap: 6px; margin: 0; padding: 8px 14px; border-width: 1px 0 0; border-radius: 0; background: color-mix(in srgb, var(--c-surface) 96%, transparent); box-shadow: 0 -6px 18px rgba(15, 23, 42, .045); backdrop-filter: blur(14px); }
.is-panel .cs-composer.is-focused { border-color: var(--c-border); box-shadow: 0 -12px 32px rgba(15, 23, 42, .075); }
.cs-composer-row { display: flex; min-width: 0; align-items: flex-start; gap: 10px; }
.composer-avatar { width: 36px; height: 36px; flex: 0 0 auto; margin-top: 3px; }
.is-panel .composer-avatar { width: 28px; height: 28px; }
.cs-input-wrap { display: flex; min-width: 0; flex: 1; flex-direction: column; overflow: hidden; border: 1px solid var(--c-border); border-radius: 11px; background: var(--c-surface-2); transition: border-color var(--transition), background var(--transition), box-shadow var(--transition); }
.is-focused .cs-input-wrap { border-color: color-mix(in srgb, var(--c-primary) 42%, var(--c-border)); background: var(--c-surface); box-shadow: 0 0 0 2px color-mix(in srgb, var(--c-primary) 6%, transparent); }
.cs-input-wrap :deep(.el-textarea__inner) { min-height: 34px !important; padding: 7px 10px 4px; border: 0; border-radius: 0; background: transparent; box-shadow: none; color: var(--c-text); font-size: 13px; line-height: 1.6; }
.cs-input-wrap :deep(.el-textarea__inner::placeholder) { color: var(--c-text-4); }
.cs-input-footer { display: flex; min-height: 32px; align-items: center; gap: 8px; padding: 0 5px 4px 10px; }
.cs-input-hint { color: var(--c-text-4); font-size: 10px; opacity: 0; transition: opacity var(--transition); }
.is-focused .cs-input-hint { opacity: .82; }
.cs-char-count { margin-left: auto; color: var(--c-text-4); font-size: 10px; opacity: 0; transition: opacity var(--transition); }
.cs-char-count.visible { opacity: 1; }
.cs-submit { display: inline-flex; min-width: 56px; height: 28px; align-items: center; justify-content: center; gap: 5px; padding: 0 12px; border: 0; border-radius: 7px; background: var(--c-primary); box-shadow: 0 4px 10px rgba(var(--c-primary-rgb), .14); color: #fff; font-size: 12px; font-weight: 700; text-decoration: none; transition: background var(--transition), opacity var(--transition), transform var(--transition); }
.cs-submit:hover:not(:disabled) { background: var(--c-primary-hover); transform: translateY(-1px); }
.cs-submit:active:not(:disabled) { transform: translateY(0); }
.cs-submit:disabled { cursor: not-allowed; opacity: .35; box-shadow: none; }
.login-link { align-self: flex-end; margin: 0 6px 7px; }
.cs-reply-context { display: flex; min-width: 0; align-items: center; gap: 8px; margin-left: 46px; padding: 0 3px; color: var(--c-text-4); font-size: 11px; }
.is-panel .cs-reply-context { margin-left: 38px; }
.cs-reply-icon { display: grid; width: 22px; height: 22px; flex: 0 0 auto; place-items: center; border-radius: 7px; background: var(--c-primary-soft); color: var(--c-primary); }
.cs-reply-icon svg { width: 13px; height: 13px; fill: none; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; }
.cs-reply-context p { display: flex; min-width: 0; align-items: center; gap: 5px; }
.cs-reply-context strong { overflow: hidden; color: var(--c-primary); font-size: 11px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.cs-reply-context > button { display: grid; width: 24px; height: 24px; margin-left: auto; flex: 0 0 auto; place-items: center; border: 0; border-radius: 7px; background: transparent; color: var(--c-text-4); transition: background var(--transition), color var(--transition); }
.cs-reply-context > button:hover { background: var(--c-surface-3); color: var(--c-text); }
.cs-reply-context > button svg { width: 14px; height: 14px; fill: none; stroke: currentColor; stroke-linecap: round; stroke-width: 1.8; }
.reply-context-enter-active, .reply-context-leave-active { transition: opacity .16s ease, transform .16s ease; }
.reply-context-enter-from, .reply-context-leave-to { opacity: 0; transform: translateY(4px); }
.cs-list { display: flex; min-height: 0; flex-direction: column; border-top: 1px solid var(--c-border); }
.is-panel .cs-list { order: 2; flex: 1; overflow-y: auto; padding: 0 16px 10px; border-top: 0; scrollbar-width: thin; overscroll-behavior: contain; }
.is-panel .cs-initial-loading, .is-panel .cs-empty { order: 2; min-height: 0; flex: 1; overflow-y: auto; }
.is-panel :deep(.comment-item) { gap: 8px; padding: 9px 0; }
.is-panel :deep(.comment-avatar) { width: 28px; height: 28px; }
.is-panel :deep(.comment-content) { margin: 2px 0 1px; font-size: 14px; line-height: 1.5; }
.is-panel :deep(.reply-group) { margin-top: 4px; }
.is-panel :deep(.reply-item) { padding-block: 5px; }
.cs-scroll-sentinel { height: 1px; }
.cs-list-status, .cs-list-end { display: flex; align-items: center; justify-content: center; gap: 7px; padding: 12px 0 4px; color: var(--c-text-4); font-size: 12px; }
.cs-list-end::before, .cs-list-end::after { width: 42px; height: 1px; background: var(--c-border); content: ''; }
.cs-initial-loading { padding-top: 4px; border-top: 1px solid var(--c-border); }
.comment-skeleton { display: flex; gap: 13px; padding: 22px 0; border-bottom: 1px solid var(--c-border); }
.comment-skeleton > i { width: 42px; height: 42px; flex: 0 0 auto; border-radius: 50%; background: var(--c-surface-3); }
.comment-skeleton > div { display: flex; flex: 1; flex-direction: column; gap: 10px; }
.comment-skeleton b, .comment-skeleton span { height: 10px; border-radius: 5px; background: var(--c-surface-3); }
.comment-skeleton b { width: 92px; }.comment-skeleton span { width: 72%; }.comment-skeleton span:last-child { width: 42%; }
.comment-skeleton > i, .comment-skeleton b, .comment-skeleton span { animation: skeleton-pulse 1.2s ease-in-out infinite alternate; }
@keyframes skeleton-pulse { to { opacity: .48; } }
.cs-empty { display: flex; align-items: center; flex-direction: column; padding: 54px 20px; color: var(--c-text-4); text-align: center; }
.empty-bubble { display: grid; width: 52px; height: 52px; margin-bottom: 13px; place-items: center; border-radius: 50%; background: var(--c-surface-2); }
.empty-bubble svg { width: 25px; height: 25px; }.cs-empty strong { color: var(--c-text-2); font-size: 14px; }.cs-empty p { margin-top: 5px; font-size: 12px; }
@media (max-width: 640px) { .comment-section { padding: 16px 14px; }.cs-header { align-items: flex-start; flex-direction: column; gap: 4px; }.cs-composer { padding: 10px; }.composer-avatar { width: 30px; height: 30px; } }
</style>

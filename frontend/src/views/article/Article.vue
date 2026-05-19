<script setup>
import { ref, onMounted, computed, provide, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { useBreakpoints, breakpointsTailwind } from "@vueuse/core";
import {
  ArrowLeft,
  Clock,
  EditPen,
  Promotion,
  ChatLineRound,
  DArrowLeft,
  DArrowRight,
  Star,
  StarFilled
} from "@element-plus/icons-vue";
import { MdPreview } from "md-editor-v3";
import "md-editor-v3/lib/preview.css";

import ArticleCommentPanel from "../../components/article/ArticleCommentPanel.vue";
import {
  getArticleDetailService,
  publishArticleService,
  checkService
} from "../../api/article.js";
import {
  getCommentListService,
  publishCommentService,
  deleteCommentService
} from "../../api/comment.js";
import {
  likeCommentService,
  unlikeCommentService
} from "../../api/commentLike.js";
import {
  getArticleLikeCountService,
  likeArticleService,
  unlikeArticleService,
  checkArticleLikeService
} from "../../api/articleLike.js";
import { useUserInfoStore } from "../../store/userInfo.js";
import { useTokenStore } from "../../store/token.js";
import { useTheme } from "../../composables/useTheme.js";

const route = useRoute();
const router = useRouter();
const userInfoStore = useUserInfoStore();
const tokenStore = useTokenStore();
const { isDark } = useTheme();
const breakpoints = useBreakpoints(breakpointsTailwind);
const isWide = breakpoints.greater("lg");

const articleData = ref({});
const isAuthor = ref(false);
const author = ref("");
const commentsOpen = ref(true);
const mobileCommentsVisible = ref(false);

watch(
  isWide,
  (wide) => {
    commentsOpen.value = wide;
    if (wide) mobileCommentsVisible.value = false;
  },
  { immediate: true }
);

async function fetchDetail() {
  const { articleId, categoryId } = route.query;
  const result = await getArticleDetailService({ articleId, categoryId });
  articleData.value = result.data || {};
}

async function checkAuthor() {
  const { categoryId } = route.query;
  const result = await checkService({ categoryId });
  isAuthor.value = result.data;
  author.value = isAuthor.value
    ? userInfoStore.userInfo.username
    : route.query.author;
}

const articleLikeCount = ref(0);
const articleLiked = ref(false);

async function loadArticleLike() {
  const articleId = Number(route.query.articleId);
  if (!articleId) return;
  try {
    const [countRes, checkRes] = await Promise.all([
      getArticleLikeCountService({ articleId }),
      tokenStore.token
        ? checkArticleLikeService({ articleId }).catch(() => ({ data: false }))
        : Promise.resolve({ data: false })
    ]);
    articleLikeCount.value = countRes.data ?? 0;
    articleLiked.value = Boolean(checkRes.data);
  } catch {
    articleLikeCount.value = 0;
  }
}

async function toggleArticleLike() {
  if (!ensureLoggedIn()) return;
  const articleId = Number(route.query.articleId);
  try {
    if (articleLiked.value) {
      await unlikeArticleService({ articleId });
      articleLikeCount.value = Math.max(0, articleLikeCount.value - 1);
    } else {
      await likeArticleService({ articleId });
      articleLikeCount.value += 1;
    }
    articleLiked.value = !articleLiked.value;
  } catch {
    ElMessage.error("操作失败");
  }
}

onMounted(() => {
  fetchDetail();
  checkAuthor();
  loadComments();
  loadArticleLike();
});

const statusInfo = computed(() => {
  switch (articleData.value.status) {
    case "published":
      return { label: "已发布", type: "success" };
    case "pending":
      return { label: "审核中", type: "warning" };
    case "draft":
      return { label: "草稿", type: "info" };
    default:
      return { label: "未知", type: "" };
  }
});

function formatDate(value) {
  if (!value) return "";
  return new Date(value).toLocaleString("zh-CN", { hour12: false });
}

function goBack() {
  if (window.history.length > 1) router.back();
  else router.push("/home");
}

async function publish() {
  try {
    await publishArticleService({
      articleId: articleData.value.articleId,
      categoryId: articleData.value.categoryId
    });
    ElMessage.success("已提交审核");
    fetchDetail();
  } catch {
    ElMessage.error("发布失败");
  }
}

function edit() {
  router.push({
    name: "ArticleEdit",
    query: {
      articleId: articleData.value.articleId,
      categoryId: articleData.value.categoryId,
      redirect: route.fullPath,
      type: "edit"
    }
  });
}

function viewAuthor(username) {
  if (!username) return;
  router.push({ name: "Profile", query: { author: username } });
}

const commentList = ref([]);
const commentContent = ref("");
const commentLoading = ref(false);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const totalAll = ref(0);
const replyingTo = ref(null);

async function loadComments() {
  try {
    const { articleId } = route.query;
    const result = await getCommentListService({
      articleId,
      page: currentPage.value,
      pageSize: pageSize.value
    });
    commentList.value = result.data?.list || [];
    total.value = result.data?.total || 0;
    totalAll.value = result.data?.totalAll ?? result.data?.total ?? 0;
  } catch {
    ElMessage.error("加载评论失败");
  }
}

function ensureLoggedIn() {
  if (tokenStore.token) return true;
  ElMessage.warning("请先登录后再操作");
  router.push({ path: "/login", query: { redirect: route.fullPath } });
  return false;
}

function startReply(comment) {
  if (!ensureLoggedIn()) return;
  if (!commentsOpen.value && isWide.value) {
    commentsOpen.value = true;
  }
  if (!isWide.value) {
    mobileCommentsVisible.value = true;
  }
  replyingTo.value = {
    commentId: comment.commentId,
    nickname: comment.nickname || comment.username
  };
  commentContent.value = "";
}

function cancelReply() {
  replyingTo.value = null;
}

function toggleCommentsPanel() {
  if (isWide.value) {
    commentsOpen.value = !commentsOpen.value;
  } else {
    mobileCommentsVisible.value = !mobileCommentsVisible.value;
  }
}

function closeCommentsPanel() {
  if (isWide.value) commentsOpen.value = false;
  else mobileCommentsVisible.value = false;
}

async function toggleLike(comment) {
  if (!ensureLoggedIn()) return;
  try {
    if (comment.isLiked) {
      await unlikeCommentService({ commentId: comment.commentId });
      comment.likeCount -= 1;
    } else {
      await likeCommentService({ commentId: comment.commentId });
      comment.likeCount += 1;
    }
    comment.isLiked = !comment.isLiked;
  } catch {
    ElMessage.error("操作失败");
  }
}

function publishRootComment() {
  replyingTo.value = null;
  publishComment();
}

async function publishComment() {
  if (!ensureLoggedIn()) return;
  if (!commentContent.value.trim()) {
    ElMessage.warning("评论不能为空");
    return;
  }
  const isReply = Boolean(replyingTo.value?.commentId);
  commentLoading.value = true;
  try {
    const payload = {
      articleId: Number(route.query.articleId),
      content: commentContent.value.trim()
    };
    if (isReply) {
      payload.parentId = replyingTo.value.commentId;
    }
    await publishCommentService(payload);
    commentContent.value = "";
    replyingTo.value = null;
    ElMessage.success(isReply ? "回复已发布" : "评论已发布");
    loadComments();
  } catch {
    ElMessage.error("发布失败");
  } finally {
    commentLoading.value = false;
  }
}

function removeComment(commentId) {
  ElMessageBox.confirm("确定删除这条评论吗？", "删除评论", {
    confirmButtonText: "删除",
    cancelButtonText: "取消",
    type: "warning"
  })
    .then(async () => {
      await deleteCommentService({ commentId });
      ElMessage.success("删除成功");
      loadComments();
    })
    .catch(() => {});
}

function pageChange(p) {
  currentPage.value = p;
  loadComments();
}

provide("commentActions", {
  replyingId: computed(() => replyingTo.value?.commentId ?? null),
  replyDraft: commentContent,
  submitting: commentLoading,
  startReply,
  cancelReply,
  submitReply: publishComment
});

const previewTheme = computed(() => (isDark.value ? "github-dark" : "github"));
const codeTheme = computed(() => (isDark.value ? "github-dark" : "github"));

const showDesktopPanel = computed(() => isWide.value && commentsOpen.value);
const showCommentsFab = computed(
  () => (isWide.value && !commentsOpen.value) || (!isWide.value && !mobileCommentsVisible.value)
);
</script>

<template>
  <div class="article-view">
    <header class="article-view__bar">
      <button type="button" class="article-view__back" aria-label="返回" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
      </button>

      <h1 class="article-view__title">{{ articleData.title || "文章详情" }}</h1>

      <div class="article-view__bar-actions">
        <button
          type="button"
          class="article-view__comment-toggle"
          :class="{ 'is-active': showDesktopPanel || mobileCommentsVisible }"
          @click="toggleCommentsPanel"
        >
          <el-icon><ChatLineRound /></el-icon>
          <span class="article-view__comment-count">{{ totalAll }}</span>
        </button>

        <template v-if="isAuthor">
          <el-button
            v-if="articleData.status === 'draft'"
            type="primary"
            size="small"
            :icon="Promotion"
            @click="publish"
          >
            发布
          </el-button>
          <el-button
            v-if="articleData.status !== 'pending'"
            size="small"
            :icon="EditPen"
            @click="edit"
          >
            编辑
          </el-button>
        </template>
      </div>
    </header>

    <div
      class="article-view__stage"
      :class="{ 'article-view__stage--panel-open': showDesktopPanel }"
    >
      <div class="article-view__main">
        <article class="article-sheet">
          <header class="article-sheet__hero">
            <h2 class="article-sheet__heading">{{ articleData.title }}</h2>
            <div class="article-sheet__meta">
              <button
                type="button"
                class="article-sheet__author"
                @click="viewAuthor(author)"
              >
                <el-avatar :size="36">{{ (author || "?")[0] }}</el-avatar>
                <span>
                  <strong>{{ author || "未知作者" }}</strong>
                  <em>
                    <el-icon><Clock /></el-icon>
                    {{ formatDate(articleData.createTime) }}
                  </em>
                </span>
              </button>
              <el-tag :type="statusInfo.type" effect="plain" round size="small">
                {{ statusInfo.label }}
              </el-tag>
            </div>
            <p v-if="articleData.updateTime" class="article-sheet__updated">
              更新于 {{ formatDate(articleData.updateTime) }}
            </p>
            <div class="article-sheet__stats">
              <button
                type="button"
                class="stat-pill"
                :class="{ 'stat-pill--active': articleLiked }"
                @click="toggleArticleLike"
              >
                <el-icon>
                  <StarFilled v-if="articleLiked" />
                  <Star v-else />
                </el-icon>
                <span>{{ articleLikeCount }}</span>
                <em>赞</em>
              </button>
              <button
                type="button"
                class="stat-pill"
                @click="toggleCommentsPanel"
              >
                <el-icon><ChatLineRound /></el-icon>
                <span>{{ totalAll }}</span>
                <em>评论</em>
              </button>
            </div>
          </header>

          <div class="article-sheet__body bp-article-preview">
            <md-preview
              :model-value="articleData.content || ''"
              :preview-theme="previewTheme"
              :code-theme="codeTheme"
              :theme="isDark ? 'dark' : 'light'"
            />
          </div>
        </article>
      </div>

      <aside
        v-if="isWide"
        class="article-view__panel"
        :class="{ 'article-view__panel--open': commentsOpen }"
      >
        <button
          type="button"
          class="article-view__panel-edge"
          :aria-label="commentsOpen ? '收起评论' : '展开评论'"
          @click="toggleCommentsPanel"
        >
          <el-icon>
            <DArrowRight v-if="commentsOpen" />
            <DArrowLeft v-else />
          </el-icon>
        </button>
        <ArticleCommentPanel
          v-show="commentsOpen"
          v-model:comment-content="commentContent"
          :comment-list="commentList"
          :comment-loading="commentLoading"
          :total="total"
          :total-all="totalAll"
          :current-page="currentPage"
          :page-size="pageSize"
          :current-username="userInfoStore.userInfo?.username || ''"
          :is-article-author="isAuthor"
          show-close
          @publish="publishRootComment"
          @cancel-reply="cancelReply"
          @page-change="pageChange"
          @delete="removeComment"
          @toggle-like="toggleLike"
          @view-author="viewAuthor"
          @close="closeCommentsPanel"
        />
      </aside>
    </div>

    <button
      v-if="showCommentsFab"
      type="button"
      class="article-view__fab"
      @click="toggleCommentsPanel"
    >
      <el-icon><ChatLineRound /></el-icon>
      <span>{{ totalAll }}</span>
    </button>

    <el-drawer
      v-model="mobileCommentsVisible"
      direction="btt"
      size="min(78vh, 640px)"
      :with-header="false"
      class="article-comments-drawer"
    >
      <ArticleCommentPanel
        v-model:comment-content="commentContent"
        :comment-list="commentList"
        :comment-loading="commentLoading"
        :total="total"
        :total-all="totalAll"
        :current-page="currentPage"
        :page-size="pageSize"
        :current-username="userInfoStore.userInfo?.username || ''"
        :is-article-author="isAuthor"
        show-close
        @publish="publishRootComment"
        @cancel-reply="cancelReply"
        @page-change="pageChange"
        @delete="removeComment"
        @toggle-like="toggleLike"
        @view-author="viewAuthor"
        @close="closeCommentsPanel"
      />
    </el-drawer>
  </div>
</template>

<style scoped>
.article-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: var(--bp-color-bg);
}

.article-view__bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--bp-color-border);
  background: color-mix(in srgb, var(--bp-color-bg-elevated) 94%, transparent);
  backdrop-filter: blur(10px);
}

.article-view__back {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 10px;
  background: var(--bp-color-bg-soft);
  color: var(--bp-color-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.2s ease, color 0.2s ease;
}

.article-view__back:hover {
  background: var(--bp-color-bg-hover);
  color: var(--bp-color-primary);
}

.article-view__title {
  flex: 1;
  min-width: 0;
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.article-view__bar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.article-view__comment-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 12px;
  border: 1px solid var(--bp-color-border);
  border-radius: 999px;
  background: var(--bp-color-bg-elevated);
  color: var(--bp-color-text-secondary);
  cursor: pointer;
  font-size: 13px;
  transition: border-color 0.2s ease, color 0.2s ease, background 0.2s ease;
}

.article-view__comment-toggle:hover,
.article-view__comment-toggle.is-active {
  border-color: var(--bp-color-primary-soft-strong);
  background: var(--bp-color-primary-soft);
  color: var(--bp-color-primary);
}

.article-view__comment-count {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.article-view__stage {
  flex: 1;
  min-height: 0;
  display: flex;
  position: relative;
}

.article-view__main {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  padding: 20px clamp(16px, 3vw, 32px) 32px;
  scrollbar-width: thin;
}

.article-sheet {
  max-width: var(--bp-article-content-max);
  margin: 0 auto;
}

.article-sheet__hero {
  margin-bottom: 28px;
}

.article-sheet__heading {
  font-size: clamp(22px, 2.8vw, 30px);
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: -0.02em;
  margin: 0 0 16px;
  color: var(--bp-color-text-primary);
}

.article-sheet__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.article-sheet__author {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  text-align: left;
}

.article-sheet__author span {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.article-sheet__author strong {
  font-size: 14px;
  color: var(--bp-color-text-primary);
}

.article-sheet__author em {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-style: normal;
  color: var(--bp-color-text-tertiary);
}

.article-sheet__updated {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--bp-color-text-tertiary);
}

.article-sheet__stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.stat-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 1px solid var(--bp-color-border);
  border-radius: 999px;
  background: var(--bp-color-bg-elevated);
  color: var(--bp-color-text-secondary);
  cursor: pointer;
  font-size: 14px;
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.stat-pill span {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--bp-color-text-primary);
}

.stat-pill em {
  font-style: normal;
  font-size: 13px;
  color: var(--bp-color-text-tertiary);
}

.stat-pill:hover,
.stat-pill--active {
  border-color: var(--bp-color-primary-soft-strong);
  background: var(--bp-color-primary-soft);
  color: var(--bp-color-primary);
}

.stat-pill--active span {
  color: var(--bp-color-warning);
}

.article-sheet__body {
  margin-top: 8px;
}

.article-view__panel {
  position: relative;
  flex-shrink: 0;
  width: 0;
  overflow: hidden;
  border-left: 1px solid transparent;
  transition: width 0.28s cubic-bezier(0.4, 0, 0.2, 1),
    border-color 0.28s ease;
}

.article-view__panel--open {
  width: var(--bp-article-panel-width);
  border-left-color: var(--bp-color-border);
}

.article-view__panel-edge {
  position: absolute;
  left: 0;
  top: 50%;
  z-index: 2;
  transform: translate(-50%, -50%);
  width: 22px;
  height: 52px;
  padding: 0;
  border: 1px solid var(--bp-color-border);
  border-radius: 999px;
  background: var(--bp-color-bg-elevated);
  color: var(--bp-color-text-tertiary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--bp-shadow-sm);
  transition: color 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.article-view__panel-edge:hover {
  color: var(--bp-color-primary);
  border-color: var(--bp-color-primary-soft-strong);
  background: var(--bp-color-primary-soft);
}

.article-view__fab {
  position: fixed;
  right: 20px;
  bottom: 24px;
  z-index: 50;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  min-width: 52px;
  padding: 10px 12px;
  border: 1px solid var(--bp-color-border);
  border-radius: 16px;
  background: var(--bp-color-bg-elevated);
  color: var(--bp-color-text-primary);
  cursor: pointer;
  box-shadow: var(--bp-shadow-md);
  font-size: 12px;
  font-weight: 600;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.article-view__fab:hover {
  transform: translateY(-2px);
  box-shadow: var(--bp-shadow-lg);
}

@media (min-width: 1024px) {
  .article-view__comment-toggle {
    display: none;
  }
}

@media (max-width: 1023px) {
  .article-view__panel {
    display: none;
  }
}
</style>

<style>
.article-comments-drawer .el-drawer__body {
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>

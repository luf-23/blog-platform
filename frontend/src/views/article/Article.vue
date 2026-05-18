<script setup>
import { ref, onMounted, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Clock,
  EditPen,
  Promotion,
  StarFilled,
  Star,
  Delete,
  ChatLineRound
} from "@element-plus/icons-vue";
import { MdPreview } from "md-editor-v3";
import "md-editor-v3/lib/preview.css";

import PageHeader from "../../components/common/PageHeader.vue";
import EmptyState from "../../components/common/EmptyState.vue";
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
  getCommentLikesService,
  likeCommentService,
  unlikeCommentService,
  checkUserLikeService
} from "../../api/commentLike.js";
import { getUserInfoByIdService } from "../../api/user.js";
import { useUserInfoStore } from "../../store/userInfo.js";
import { useTheme } from "../../composables/useTheme.js";

const route = useRoute();
const router = useRouter();
const userInfoStore = useUserInfoStore();
const { isDark } = useTheme();

const articleData = ref({});
const isAuthor = ref(false);
const author = ref("");

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

onMounted(() => {
  fetchDetail();
  checkAuthor();
  loadComments();
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

async function loadComments() {
  try {
    const { articleId } = route.query;
    const result = await getCommentListService({
      articleId,
      page: currentPage.value,
      pageSize: pageSize.value
    });
    const items = result.data?.list || [];
    const enriched = await Promise.all(
      items.map(async (comment) => {
        const [userInfo, likeCount, likeStatus] = await Promise.all([
          getCommenterInfo(comment.userId),
          getCommentLikesService({ commentId: comment.commentId }),
          checkUserLikeService({ commentId: comment.commentId })
        ]);
        return {
          ...comment,
          username: userInfo?.username || "已注销用户",
          nickname: userInfo?.nickname,
          avatar: userInfo?.avatarImage || "",
          likeCount: likeCount.data || 0,
          isLiked: likeStatus.data || false
        };
      })
    );
    enriched.sort((a, b) => new Date(b.createTime) - new Date(a.createTime));
    commentList.value = enriched;
    total.value = result.data?.total || 0;
  } catch {
    ElMessage.error("加载评论失败");
  }
}

async function getCommenterInfo(userId) {
  if (!userId) return null;
  try {
    const result = await getUserInfoByIdService({ id: userId });
    return result.data;
  } catch {
    return null;
  }
}

async function toggleLike(comment) {
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

async function publishComment() {
  if (!commentContent.value.trim()) {
    ElMessage.warning("评论不能为空");
    return;
  }
  commentLoading.value = true;
  try {
    await publishCommentService({
      articleId: route.query.articleId,
      content: commentContent.value
    });
    commentContent.value = "";
    ElMessage.success("评论已发布");
    loadComments();
  } catch {
    ElMessage.error("发布评论失败");
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

const previewTheme = computed(() => (isDark.value ? "vuepress-dark" : "vuepress"));
const codeTheme = computed(() => (isDark.value ? "github-dark" : "github"));
</script>

<template>
  <div class="bp-page article-page">
    <PageHeader :title="articleData.title || '文章详情'" :show-back="true">
      <template #actions>
        <template v-if="isAuthor">
          <el-button
            v-if="articleData.status === 'draft'"
            type="primary"
            :icon="Promotion"
            @click="publish"
          >
            发布
          </el-button>
          <el-button
            v-if="articleData.status !== 'pending'"
            :icon="EditPen"
            @click="edit"
          >
            编辑
          </el-button>
        </template>
      </template>
    </PageHeader>

    <section class="article-meta bp-card">
      <div class="article-meta__row">
        <div
          class="author-chip"
          :class="{ 'author-chip--clickable': author }"
          @click="viewAuthor(author)"
        >
          <el-avatar :size="32" :src="userInfoStore.userInfo?.avatarImage || '/avatar/avatar1.png'" />
          <div>
            <strong>{{ author || "未知作者" }}</strong>
            <span>
              <el-icon><Clock /></el-icon>
              {{ formatDate(articleData.createTime) }}
            </span>
          </div>
        </div>
        <el-tag :type="statusInfo.type" effect="light" round>
          {{ statusInfo.label }}
        </el-tag>
      </div>

      <div v-if="articleData.updateTime" class="article-meta__update">
        最后更新于 {{ formatDate(articleData.updateTime) }}
      </div>
    </section>

    <section class="article-content bp-card bp-md-preview">
      <md-preview
        :model-value="articleData.content || ''"
        :preview-theme="previewTheme"
        :code-theme="codeTheme"
        :theme="isDark ? 'dark' : 'light'"
      />
    </section>

    <section class="comments bp-card">
      <h3 class="comments__title">
        <el-icon><ChatLineRound /></el-icon>
        评论区
        <span class="comments__total">{{ total }}</span>
      </h3>

      <div class="comments__editor">
        <el-input
          v-model="commentContent"
          type="textarea"
          :rows="3"
          placeholder="留下你的想法..."
          resize="none"
        />
        <div class="comments__editor-actions">
          <el-button
            type="primary"
            :loading="commentLoading"
            @click="publishComment"
          >
            发表评论
          </el-button>
        </div>
      </div>

      <div v-if="commentList.length" class="comments__list">
        <article
          v-for="comment in commentList"
          :key="comment.commentId"
          class="comment"
        >
          <el-avatar
            :size="40"
            :src="comment.avatar || '/avatar/avatar1.png'"
            class="comment__avatar"
            @click="viewAuthor(comment.username)"
          />
          <div class="comment__body">
            <header class="comment__head">
              <strong @click="viewAuthor(comment.username)" class="comment__name">
                {{ comment.nickname || comment.username }}
              </strong>
              <span class="comment__time">
                {{ formatDate(comment.createTime) }}
              </span>
            </header>
            <p class="comment__content">{{ comment.content }}</p>
            <div class="comment__actions">
              <button
                class="action-btn"
                :class="{ 'action-btn--liked': comment.isLiked }"
                @click="toggleLike(comment)"
              >
                <el-icon>
                  <StarFilled v-if="comment.isLiked" />
                  <Star v-else />
                </el-icon>
                {{ comment.likeCount }}
              </button>
              <button
                v-if="
                  userInfoStore.userInfo?.username === comment.username ||
                  isAuthor
                "
                class="action-btn action-btn--danger"
                @click="removeComment(comment.commentId)"
              >
                <el-icon><Delete /></el-icon>
                删除
              </button>
            </div>
          </div>
        </article>
      </div>

      <EmptyState v-else title="还没有评论" description="抢先抛出第一条想法吧" />

      <div v-if="total > pageSize" class="comments__pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="pageChange"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.article-page {
  max-width: 920px;
  margin: 0 auto;
  width: 100%;
}

.article-meta {
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.article-meta__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.author-chip {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-chip--clickable {
  cursor: pointer;
}

.author-chip strong {
  display: block;
  font-size: 14px;
  color: var(--bp-color-text-primary);
}

.author-chip span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--bp-color-text-tertiary);
}

.article-meta__update {
  font-size: 12px;
  color: var(--bp-color-text-tertiary);
}

.article-content {
  padding: 24px clamp(20px, 3vw, 36px);
}

:deep(.md-editor-preview-wrapper) {
  background: transparent !important;
  padding: 0 !important;
}

:deep(.md-editor-preview) {
  background: transparent !important;
}

.comments {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comments__title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
}

.comments__total {
  font-size: 12px;
  padding: 2px 8px;
  background: var(--bp-color-primary-soft);
  color: var(--bp-color-primary);
  border-radius: 999px;
  font-weight: 600;
}

.comments__editor {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.comments__editor-actions {
  display: flex;
  justify-content: flex-end;
}

.comments__list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment {
  display: flex;
  gap: 12px;
  padding: 14px;
  border-radius: 14px;
  background: var(--bp-color-bg-soft);
  border: 1px solid var(--bp-color-border);
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
  gap: 10px;
}

.comment__name {
  font-size: 14px;
  cursor: pointer;
  color: var(--bp-color-text-primary);
}

.comment__name:hover {
  color: var(--bp-color-primary);
}

.comment__time {
  font-size: 12px;
  color: var(--bp-color-text-tertiary);
}

.comment__content {
  margin-top: 6px;
  font-size: 14px;
  color: var(--bp-color-text-secondary);
  line-height: 1.7;
  white-space: pre-wrap;
}

.comment__actions {
  margin-top: 10px;
  display: flex;
  gap: 12px;
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

.action-btn--liked {
  color: var(--bp-color-warning);
}

.action-btn--danger:hover {
  background: rgba(239, 68, 68, 0.1);
  color: var(--bp-color-danger);
}

.comments__pagination {
  display: flex;
  justify-content: center;
}
</style>

<script setup>
import { ref, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Clock, Check, Close, CircleClose } from "@element-plus/icons-vue";
import { MdPreview } from "md-editor-v3";
import "md-editor-v3/lib/preview.css";

import PageHeader from "../../components/common/PageHeader.vue";
import {
  getArticleDetailService,
  acceptArticleService,
  rejectArticleService,
  dropArticleService
} from "../../api/admin.js";
import { useTheme } from "../../composables/useTheme.js";

const route = useRoute();
const router = useRouter();
const { isDark } = useTheme();

const articleData = ref({});

async function fetchDetail() {
  const result = await getArticleDetailService({
    articleId: route.query.articleId
  });
  articleData.value = result.data || {};
  articleData.value.author = route.query.author;
}

fetchDetail();

const statusInfo = computed(() => {
  switch (articleData.value.status) {
    case "published":
      return { label: "已发布", type: "success" };
    case "pending":
      return { label: "待审核", type: "warning" };
    case "draft":
      return { label: "草稿", type: "info" };
    default:
      return { label: "未知", type: "" };
  }
});

const isPublished = computed(() => articleData.value.status === "published");

function formatDate(value) {
  if (!value) return "";
  return new Date(value).toLocaleString("zh-CN", { hour12: false });
}

async function accept() {
  await acceptArticleService({ articleId: articleData.value.articleId });
  ElMessage.success("已通过审核");
  fetchDetail();
}

function reject() {
  ElMessageBox.confirm(
    `确定拒绝《${articleData.value.title || ""}》吗？`,
    "拒绝文章",
    { type: "warning", confirmButtonText: "确定", cancelButtonText: "取消" }
  ).then(async () => {
    await rejectArticleService({ articleId: articleData.value.articleId });
    ElMessage.success("已拒绝该文章");
    fetchDetail();
  });
}

function drop() {
  ElMessageBox.confirm(
    `确定下架《${articleData.value.title || ""}》吗？`,
    "下架文章",
    { type: "warning", confirmButtonText: "确定", cancelButtonText: "取消" }
  ).then(async () => {
    await dropArticleService({ articleId: articleData.value.articleId });
    ElMessage.success("文章已下架");
    fetchDetail();
  });
}

function viewAuthor() {
  if (!articleData.value.author) return;
  router.push({
    name: "Profile",
    query: { author: articleData.value.author }
  });
}

const previewTheme = computed(() => (isDark.value ? "vuepress-dark" : "vuepress"));
const codeTheme = computed(() => (isDark.value ? "github-dark" : "github"));
</script>

<template>
  <div class="bp-page bp-page--compact article-manager-page">
    <PageHeader
      :title="articleData.title || '文章详情'"
      compact
      :show-back="true"
    >
      <template #actions>
        <template v-if="articleData.status === 'pending'">
          <el-button type="primary" :icon="Check" @click="accept">
            通过
          </el-button>
          <el-button type="danger" :icon="Close" plain @click="reject">
            拒绝
          </el-button>
        </template>
        <el-button
          v-if="isPublished"
          type="danger"
          :icon="CircleClose"
          plain
          @click="drop"
        >
          下架
        </el-button>
      </template>
    </PageHeader>

    <section class="article-meta bp-card">
      <div class="author-chip" @click="viewAuthor">
        <el-avatar :size="32" />
        <div>
          <strong>{{ articleData.author || "未知作者" }}</strong>
          <span>
            <el-icon><Clock /></el-icon>
            {{ formatDate(articleData.createTime) }}
          </span>
        </div>
      </div>
      <el-tag :type="statusInfo.type" effect="light" round>
        {{ statusInfo.label }}
      </el-tag>
    </section>

    <section class="article-content bp-card bp-md-preview">
      <md-preview
        :model-value="articleData.content || ''"
        :preview-theme="previewTheme"
        :code-theme="codeTheme"
        :theme="isDark ? 'dark' : 'light'"
      />
    </section>
  </div>
</template>

<style scoped>
.article-manager-page {
  max-width: 920px;
  margin: 0 auto;
  width: 100%;
}

.article-meta {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.author-chip {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.author-chip strong {
  display: block;
  font-size: 14px;
}

.author-chip span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
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
</style>

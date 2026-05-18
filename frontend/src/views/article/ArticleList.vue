<script setup>
import { reactive, ref, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Search, RefreshLeft } from "@element-plus/icons-vue";

import PageHeader from "../../components/common/PageHeader.vue";
import EmptyState from "../../components/common/EmptyState.vue";
import ArticleCard from "../../components/article/ArticleCard.vue";
import UploadImageDialog from "../../components/common/UploadImageDialog.vue";
import {
  deleteArticleService,
  getArticleListService,
  getSelectedArticleListService,
  updateCoverImageService
} from "../../api/article.js";
import { ossClient } from "../../utils/oss/index.js";

const route = useRoute();
const router = useRouter();
const categoryId = route.query.categoryId;

const articles = ref([]);
const loading = ref(false);

const filters = reactive({
  title: "",
  status: ""
});

const statusOptions = [
  { label: "全部", value: "" },
  { label: "已发布", value: "published" },
  { label: "待审核", value: "pending" },
  { label: "草稿", value: "draft" }
];

function normalize(item) {
  return {
    articleId: item.articleId,
    title: item.title,
    content: item.content,
    status: item.status,
    createTime: item.createTime,
    updateTime: item.updateTime,
    coverImage: item.coverImage,
    categoryId
  };
}

async function fetchList() {
  loading.value = true;
  try {
    const result = await getArticleListService({ categoryId });
    articles.value = (result.data || []).map(normalize);
  } catch {
    ElMessage.error("获取文章列表失败");
  } finally {
    loading.value = false;
  }
}

fetchList();

async function search() {
  loading.value = true;
  try {
    const result = await getSelectedArticleListService({
      title: filters.title,
      status: filters.status,
      categoryId
    });
    articles.value = (result.data || []).map(normalize);
  } catch {
    ElMessage.error("搜索失败");
  } finally {
    loading.value = false;
  }
}

function reset() {
  filters.title = "";
  filters.status = "";
  fetchList();
}

function goDetail(article) {
  router.push({
    name: "ArticleDetail",
    query: { articleId: article.articleId, categoryId }
  });
}

function goAdd() {
  router.push({
    name: "ArticleAdd",
    query: {
      categoryId,
      redirect: route.fullPath,
      type: "add"
    }
  });
}

function remove(article) {
  ElMessageBox.confirm(`确定要删除「${article.title}」吗？`, "删除文章", {
    confirmButtonText: "删除",
    cancelButtonText: "取消",
    type: "warning"
  })
    .then(async () => {
      await deleteArticleService({ articleId: article.articleId });
      ElMessage.success("删除成功");
      articles.value = articles.value.filter(
        (a) => a.articleId !== article.articleId
      );
    })
    .catch(() => {});
}

const uploadDialogVisible = ref(false);
const uploadingArticle = ref(null);
const uploadLoading = ref(false);

function changeCover(article) {
  uploadingArticle.value = article;
  uploadDialogVisible.value = true;
}

async function handleUpload(file) {
  if (!uploadingArticle.value) return;
  uploadLoading.value = true;
  try {
    await ossClient.init();
    const extension = file.name.split(".").pop();
    const fileName = ossClient.generateFileName(
      uploadingArticle.value.articleId,
      ossClient.constructor.IMAGE_TYPE.ARTICLE_BACKGROUND,
      extension
    );
    const fileUrl = ossClient.generateFileUrl(fileName);
    await ossClient.uploadFile(fileName, file);
    await updateCoverImageService({
      articleId: uploadingArticle.value.articleId,
      categoryId,
      coverImageUrl: fileUrl
    });
    const target = articles.value.find(
      (a) => a.articleId === uploadingArticle.value.articleId
    );
    if (target) target.coverImage = fileUrl;
    ElMessage.success("封面已更新");
    uploadDialogVisible.value = false;
  } catch (error) {
    console.error(error);
    ElMessage.error("上传失败");
  } finally {
    uploadLoading.value = false;
  }
}

const hasFilter = computed(() => Boolean(filters.title || filters.status));
</script>

<template>
  <div class="bp-page">
    <PageHeader
      title="文章列表"
      subtitle="当前分类下的全部文章"
      :show-back="true"
    >
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="goAdd">
          新建文章
        </el-button>
      </template>
    </PageHeader>

    <div class="filter-bar bp-card">
      <el-input
        v-model="filters.title"
        :prefix-icon="Search"
        placeholder="搜索文章标题..."
        clearable
        class="filter-bar__input"
        @keyup.enter="search"
      />
      <el-select
        v-model="filters.status"
        placeholder="筛选状态"
        class="filter-bar__select"
      >
        <el-option
          v-for="opt in statusOptions"
          :key="opt.value || 'all'"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <div class="filter-bar__actions">
        <el-button type="primary" :icon="Search" @click="search">搜索</el-button>
        <el-button v-if="hasFilter" :icon="RefreshLeft" @click="reset">
          重置
        </el-button>
      </div>
    </div>

    <div v-loading="loading">
      <div v-if="articles.length" class="grid">
        <ArticleCard
          v-for="article in articles"
          :key="article.articleId"
          :article="article"
          :show-author="false"
          :show-delete="true"
          :show-change-cover-image="true"
          @click="goDetail"
          @delete="remove"
          @change-cover-image="changeCover"
        />
      </div>
      <EmptyState
        v-else-if="!loading"
        title="该分类下还没有文章"
        description="开始写下你的第一篇内容吧"
      >
        <el-button type="primary" :icon="Plus" @click="goAdd">
          新建文章
        </el-button>
      </EmptyState>
    </div>

    <UploadImageDialog
      v-model:visible="uploadDialogVisible"
      title="更换文章封面"
      :loading="uploadLoading"
      @confirm="handleUpload"
    />
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex;
  gap: 12px;
  padding: 14px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-bar__input {
  flex: 1;
  min-width: 200px;
}

.filter-bar__select {
  width: 160px;
}

.filter-bar__actions {
  display: flex;
  gap: 8px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

@media (max-width: 640px) {
  .filter-bar__input,
  .filter-bar__select,
  .filter-bar__actions {
    width: 100%;
  }
  .filter-bar__select {
    flex: none;
  }
  .filter-bar__actions {
    justify-content: flex-end;
  }
}
</style>

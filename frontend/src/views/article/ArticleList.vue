<script setup>
import { reactive, ref, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Search, RefreshLeft } from "@element-plus/icons-vue";

import PageHeader from "../../components/common/PageHeader.vue";
import FilterBar from "../../components/common/FilterBar.vue";
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
  <div class="bp-page bp-page--compact">
    <div class="article-list-toolbar">
      <PageHeader
        title="文章列表"
        compact
        :show-back="true"
        class="article-list-toolbar__header"
      />

      <FilterBar class="article-list-toolbar__filter">
        <el-input
          v-model="filters.title"
          :prefix-icon="Search"
          placeholder="搜索标题"
          clearable
          @keyup.enter="search"
        />
        <el-select
          v-model="filters.status"
          placeholder="状态"
          class="article-list-toolbar__status"
        >
          <el-option
            v-for="opt in statusOptions"
            :key="opt.value || 'all'"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <template #actions>
          <el-button type="primary" :icon="Search" @click="search">搜索</el-button>
          <el-button v-if="hasFilter" :icon="RefreshLeft" @click="reset">
            重置
          </el-button>
        </template>
      </FilterBar>

      <el-button
        type="primary"
        :icon="Plus"
        class="article-list-toolbar__add"
        @click="goAdd"
      >
        新建文章
      </el-button>
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
.article-list-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: var(--bp-radius-sm);
  background: var(--bp-color-bg-elevated);
  border: 1px solid var(--bp-color-border);
}

.article-list-toolbar__header {
  flex-shrink: 0;
  flex-wrap: nowrap;
}

.article-list-toolbar__header :deep(.page-header__left) {
  flex: none;
}

.article-list-toolbar__filter {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  padding: 0;
}

.article-list-toolbar__status {
  width: 108px;
  flex: 0 0 108px;
}

.article-list-toolbar__add {
  flex-shrink: 0;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

@media (max-width: 900px) {
  .article-list-toolbar {
    flex-wrap: wrap;
    align-items: stretch;
  }

  .article-list-toolbar__filter {
    flex: 1 1 100%;
    order: 2;
  }

  .article-list-toolbar__add {
    margin-left: auto;
    order: 1;
  }
}

@media (max-width: 640px) {
  .article-list-toolbar__add span {
    display: none;
  }
}
</style>

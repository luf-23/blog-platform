<script setup>
import { reactive, ref, computed } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Search, RefreshLeft } from "@element-plus/icons-vue";

import EmptyState from "../components/common/EmptyState.vue";
import ArticleCard from "../components/article/ArticleCard.vue";
import {
  getAuthorNameService,
  getCommunityListService,
  getSelectedCommunityListService
} from "../api/community.js";

const router = useRouter();
const articles = ref([]);
const loading = ref(false);
const filters = reactive({ title: "", content: "" });

function normalize(item) {
  return {
    categoryId: item.categoryId,
    articleId: item.articleId,
    title: item.title,
    content: item.content,
    author: "",
    createTime: item.createTime,
    updateTime: item.updateTime,
    coverImage: item.coverImage
  };
}

async function attachAuthors(list) {
  for (const item of list) {
    try {
      const result = await getAuthorNameService({ categoryId: item.categoryId });
      item.author = result.data;
    } catch {
      item.author = "未知";
    }
  }
}

async function fetchList() {
  loading.value = true;
  try {
    const result = await getCommunityListService();
    const list = (result.data || []).map(normalize);
    await attachAuthors(list);
    list.sort((a, b) => new Date(b.updateTime) - new Date(a.updateTime));
    articles.value = list;
  } finally {
    loading.value = false;
  }
}

fetchList();

async function search() {
  loading.value = true;
  try {
    const result = await getSelectedCommunityListService({
      title: filters.title,
      content: filters.content
    });
    const list = (result.data || []).map(normalize);
    await attachAuthors(list);
    list.sort((a, b) => new Date(b.updateTime) - new Date(a.updateTime));
    articles.value = list;
  } catch {
    ElMessage.error("搜索失败");
  } finally {
    loading.value = false;
  }
}

function reset() {
  filters.title = "";
  filters.content = "";
  fetchList();
}

function open(item) {
  router.push({
    name: "ArticleDetail",
    query: {
      articleId: item.articleId,
      categoryId: item.categoryId,
      author: item.author
    }
  });
}

const hasFilter = computed(() => Boolean(filters.title || filters.content));
</script>

<template>
  <div class="bp-page discover-page">
    <header class="discover-hero">
      <div class="discover-hero__text">
        <h1>发现好文</h1>
        <p>浏览社区中已发布的博客，点击卡片阅读全文</p>
      </div>
    </header>

    <div class="filter-bar bp-card">
      <el-input
        v-model="filters.title"
        :prefix-icon="Search"
        placeholder="搜索标题"
        clearable
        @keyup.enter="search"
      />
      <el-input
        v-model="filters.content"
        placeholder="搜索正文关键词"
        clearable
        @keyup.enter="search"
      />
      <div class="filter-bar__actions">
        <el-button type="primary" :icon="Search" @click="search">搜索</el-button>
        <el-button v-if="hasFilter" :icon="RefreshLeft" @click="reset">
          重置
        </el-button>
      </div>
    </div>

    <div v-loading="loading">
      <div v-if="articles.length" class="article-grid">
        <ArticleCard
          v-for="article in articles"
          :key="article.articleId"
          :article="article"
          :show-status="false"
          @click="open"
        />
      </div>
      <EmptyState
        v-else-if="!loading"
        title="还没有公开文章"
        description="成为第一个分享观点的人，去「我的博客」发布一篇吧"
      />
    </div>
  </div>
</template>

<style scoped>
.discover-page {
  gap: 20px;
}

.discover-hero {
  padding: 8px 0 4px;
}

.discover-hero__text h1 {
  font-size: clamp(24px, 3vw, 32px);
  font-weight: 700;
  letter-spacing: -0.02em;
}

.discover-hero__text p {
  margin-top: 8px;
  font-size: 14px;
  color: var(--bp-color-text-secondary);
}

.filter-bar {
  display: grid;
  grid-template-columns: 1.4fr 1.4fr auto;
  gap: 12px;
  padding: 14px;
  align-items: center;
}

.filter-bar__actions {
  display: flex;
  gap: 8px;
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

@media (max-width: 720px) {
  .filter-bar {
    grid-template-columns: 1fr;
  }
  .filter-bar__actions {
    justify-content: flex-end;
  }
}
</style>

<script setup>
import { ref, computed } from "vue";
import { useRouter } from "vue-router";
import PageHeader from "../../components/common/PageHeader.vue";
import EmptyState from "../../components/common/EmptyState.vue";
import ArticleCard from "../../components/article/ArticleCard.vue";
import {
  getPendingArticleListService,
  getPublishedArticleListService
} from "../../api/admin.js";
import { getAuthorNameService } from "../../api/community.js";

const router = useRouter();

const allList = ref([]);
const filter = ref("all");
const loading = ref(false);

function normalize(item) {
  return {
    articleId: item.articleId,
    categoryId: item.categoryId,
    author: "",
    title: item.title,
    content: item.content,
    status: item.status,
    createTime: item.createTime,
    updateTime: item.updateTime,
    coverImage: item.coverImage
  };
}

async function attachAuthors(list) {
  for (const item of list) {
    try {
      const res = await getAuthorNameService({ categoryId: item.categoryId });
      item.author = res.data;
    } catch {
      item.author = "未知";
    }
  }
}

async function fetchAll() {
  loading.value = true;
  try {
    const [pending, published] = await Promise.all([
      getPendingArticleListService(),
      getPublishedArticleListService()
    ]);
    const merged = [
      ...(pending.data || []).map(normalize),
      ...(published.data || []).map(normalize)
    ];
    await attachAuthors(merged);
    merged.sort((a, b) => new Date(b.updateTime) - new Date(a.updateTime));
    allList.value = merged;
  } finally {
    loading.value = false;
  }
}

fetchAll();

const filteredList = computed(() => {
  if (filter.value === "all") return allList.value;
  return allList.value.filter((item) => item.status === filter.value);
});

function open(item) {
  router.push({
    name: "ArticleDetailManager",
    query: {
      articleId: item.articleId,
      categoryId: item.categoryId,
      author: item.author
    }
  });
}
</script>

<template>
  <div class="bp-page bp-page--compact">
    <PageHeader title="文章审核" compact>
      <template #actions>
        <el-radio-group v-model="filter" size="small">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="pending">待审核</el-radio-button>
          <el-radio-button value="published">已发布</el-radio-button>
        </el-radio-group>
      </template>
    </PageHeader>

    <div v-loading="loading">
      <div v-if="filteredList.length" class="grid">
        <ArticleCard
          v-for="item in filteredList"
          :key="item.articleId"
          :article="item"
          @click="open"
        />
      </div>
      <EmptyState
        v-else-if="!loading"
        title="暂无文章"
        description="当前筛选下没有匹配的内容"
      />
    </div>
  </div>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}
</style>

<script setup>
import {
  getPendingArticleListService,
  getPublishedArticleListService
} from "../../api/admin";
import { getAuthorNameService } from "../../api/community";
import { ref } from "vue";
import ArticleCard from "../../components/ArticleCard.vue";
import { check } from "../../utils/admin/check";
import { useRouter } from "vue-router";
check();
const router = useRouter();
const articleList = ref([
  {
    articleId: "",
    categoryId: "",
    author: "",
    title: "",
    content: "",
    status: "", //pending or published
    createTime: "",
    updateTime: "",
    coverImage: ""
  }
]);
const getAuthorName = async (categoryId) => {
  try {
    const result = await getAuthorNameService(categoryId);
    return result.data;
  } catch (error) {
    console.error("获取作者名称失败:", error);
    return "error";
  }
};
const loading = ref(false);
const getArticleList = async () => {
  loading.value = true;
  const publishedList = ref();
  const pendingList = ref();
  const result1 = await getPendingArticleListService();
  pendingList.value = result1.data.map((item) => {
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
  });
  for (const item of pendingList.value) {
    const authorName = await getAuthorName({
      categoryId: item.categoryId
    });
    item.author = authorName;
  }
  const result2 = await getPublishedArticleListService();
  publishedList.value = result2.data.map((item) => {
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
  });
  for (const item of publishedList.value) {
    const authorName = await getAuthorName({
      categoryId: item.categoryId
    });
    item.author = authorName;
  }
  articleList.value = [
    ...JSON.parse(JSON.stringify(publishedList.value)),
    ...JSON.parse(JSON.stringify(pendingList.value))
  ];
  articleList.value.sort((a, b) => {
    return new Date(b.updateTime) - new Date(a.updateTime);
  });
  loading.value = false;
};
getArticleList();

const handleRowClick = (item) => {
  router.push({
    name: "ArticleDetailManager",
    query: {
      articleId: item.articleId,
      categoryId: item.categoryId,
      author: item.author
    }
  });
};
</script>

<template>
  <div class="article-list-container">
    <ArticleCard
      v-for="item in articleList"
      :article="item"
      :key="item.articleId"
      :onClick="() => handleRowClick(item)"
    />
  </div>
</template>

<style scoped>
.article-list {
  display: grid;
  gap: 20px;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  padding: 20px;
  max-width: 100%;
}
</style>

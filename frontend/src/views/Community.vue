<script setup>
import { ref, reactive } from "vue";
import ArticleCard from "../components/ArticleCard.vue";
import {
  getAuthorNameService,
  getCommunityListService,
  getSelectedCommunityListService
} from "../api/community";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { ArrowLeft, ArrowDown, Search } from "@element-plus/icons-vue";
const router = useRouter();
const communityList = ref([]);

const loading = ref(false);
const isSearchVisible = ref(false);

// 根据屏幕宽度设置搜索框的初始显示状态
const setInitialSearchVisibility = () => {
  //isSearchVisible.value = window.innerWidth > 768;
  isSearchVisible.value = false;
};

// 在组件挂载时设置初始状态
setInitialSearchVisibility();

const toggleSearch = () => {
  isSearchVisible.value = !isSearchVisible.value;
};

const getAuthorName = async (categoryId) => {
  try {
    const result = await getAuthorNameService(categoryId);
    return result.data;
  } catch (error) {
    console.error("获取作者名称失败:", error);
    return "error";
  }
};
const getCommunityList = async () => {
  try {
    loading.value = true;
    const result = await getCommunityListService();
    communityList.value = result.data.map((item) => {
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
    });
    for (const item of communityList.value) {
      const authorName = await getAuthorName({
        categoryId: item.categoryId
      });
      item.author = authorName;
    }
    communityList.value.sort((a, b) => {
      return new Date(b.updateTime) - new Date(a.updateTime);
    });
  } catch (error) {
    console.error("获取社区列表失败:", error);
  } finally {
    loading.value = false;
  }
};
getCommunityList();

const handleRowClick = (item) => {
  router.push({
    name: "ArticleDetail",
    query: {
      articleId: item.articleId,
      categoryId: item.categoryId,
      author: item.author
    }
  });
};

//搜索框
const searchForm = reactive({
  title: "",
  content: ""
});

const handleSearch = async function () {
  try {
    loading.value = true;
    const result = await getSelectedCommunityListService({
      title: searchForm.title,
      content: searchForm.content
    });
    communityList.value = result.data.map((item) => {
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
    });
    for (const item of communityList.value) {
      const authorName = await getAuthorName({
        categoryId: item.categoryId
      });
      item.author = authorName;
    }
    communityList.value.sort((a, b) => {
      return new Date(b.updateTime) - new Date(a.updateTime);
    });
  } catch {
    console.log("搜索失败");
    ElMessage.error("搜索失败");
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div
    class="community-container"
    :class="{ 'search-visible': isSearchVisible }"
  >
    <div class="fixed-header">
      <div class="page-header">
        <div class="header-left">
          <el-button @click="router.back()" link>
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
        </div>
        <h2 class="page-title">社区文章</h2>
        <div class="search-toggle">
          <el-button @click="toggleSearch" text class="toggle-button">
            <el-icon>
              <template v-if="isSearchVisible">
                <ArrowDown :class="{ 'rotate-180': !isSearchVisible }" />
              </template>
              <template v-else>
                <Search />
              </template>
            </el-icon>
            {{ isSearchVisible ? "收起搜索" : "展开搜索" }}
          </el-button>
        </div>
      </div>
      <transition name="slide-fade">
        <div v-show="isSearchVisible" class="search-section">
          <el-form :inline="true" :model="searchForm" class="search-form">
            <el-form-item label="标题关键字">
              <el-input
                v-model="searchForm.title"
                placeholder="请输入文章标题关键字"
                clearable
                style="width: 300px"
              />
            </el-form-item>
            <el-form-item label="内容关键字">
              <el-input
                v-model="searchForm.content"
                placeholder="请输入文章内容关键字"
                clearable
                style="width: 300px"
              />
            </el-form-item>
            <div class="button-group">
              <el-button type="primary" @click="handleSearch">搜索</el-button>
              <el-button
                @click="
                  searchForm.title = '';
                  searchForm.content = '';
                "
                >重置</el-button
              >
            </div>
          </el-form>
        </div>
      </transition>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else class="article-list">
      <ArticleCard
        v-for="item in communityList"
        :key="item.articleId"
        :article="item"
        :onClick="() => handleRowClick(item)"
        :showCategoryId="false"
        :showStatus="false"
      />
    </div>
  </div>
</template>

<style scoped>
.community-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
  padding-top: 100px; /* 为固定头部留出空间 */
}

.fixed-header {
  position: fixed;
  top: 0;
  left: 220px; /* 为侧边栏留出空间 */
  right: 0;
  background-color: #f5f7fa;
  z-index: 100;
  padding: 8px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 响应式设计和侧边栏状态适配 */
@media screen and (max-width: 768px) {
  .fixed-header {
    left: 0; /* 移动端侧边栏隐藏，头部占满宽度 */
    top: 60px; /* 为顶部导航栏留出空间 */
  }

  .community-container {
    padding-top: 160px; /* 为固定头部和顶部导航栏留出空间 */
  }

  .community-container.search-visible {
    padding-top: 260px; /* 搜索框展开时增加上边距 */
  }
}

/* 桌面端适配侧边栏折叠状态 */
@media screen and (min-width: 769px) {
  .fixed-header {
    left: var(--sidebar-width, 220px); /* 使用CSS变量动态调整 */
  }
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-bottom: 1px solid #ebeef5;
  background-color: white;
  position: relative;
}

.header-left {
  display: flex;
  align-items: center;
}

.page-title {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  margin: 0;
  font-size: 18px;
  color: #303133;
  font-weight: 500;
}

.search-toggle {
  margin-left: auto;
}

.toggle-button {
  display: flex;
  align-items: center;
  gap: 4px;
}

.rotate-180 {
  transform: rotate(180deg);
  transition: transform 0.3s;
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateY(-20px);
  opacity: 0;
}

.loading {
  text-align: center;
  padding: 20px;
  font-size: 16px;
}

.article-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
  padding: 10px;
}

.search-section {
  background: white;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.search-form {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  max-width: 1000px;
  margin: 0 auto;
}

@media screen and (max-width: 768px) {
  .community-container {
    padding: 70px 8px 20px 8px; /* 减小左右边距，保持上边距 */
    transition: padding-top 0.3s ease;
  }

  .page-title {
    left: 45%; /* 在移动端将标题稍微左移 */
  }

  .community-container.search-visible {
    padding-top: 220px; /* 增加上边距防止遮挡 */
  }

  .fixed-header {
    position: fixed;
    width: 100%;
  }

  .article-list {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 0; /* 移除文章列表的内边距 */
  }

  .article-list :deep(.article-card) {
    border: 5px solid #e1e4ec;
    border-bottom: 2px solid #e6e6e6;
    padding: 12px;
    margin-bottom: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    border-radius: 8px;
    background-color: #ffffff;
  }

  .article-list :deep(.article-card:last-child) {
    border-bottom: none;
    margin-bottom: 0;
  }

  .search-section {
    padding: 8px;
    background: white;
  }

  .search-form {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  .button-group {
    display: flex;
    gap: 8px;
    margin-top: 8px;
    width: 100%;
  }

  .button-group .el-button {
    flex: 1;
  }

  .search-form :deep(.el-form-item) {
    margin-bottom: 8px;
    margin-right: 0;
    width: 100%;
  }

  .toggle-button {
    font-size: 14px;
    padding: 6px 12px;
    transition: all 0.3s ease;
    border-radius: 20px;
  }

  .toggle-button:hover {
    background-color: #f2f6fc;
  }

  .toggle-button .el-icon {
    font-size: 18px;
    margin-right: 2px;
  }

  /* 调整输入框在移动端的宽度 */
  .search-form :deep(.el-input) {
    width: 100% !important;
  }

  /* 优化移动端搜索框过渡动画 */
  .slide-fade-enter-active,
  .slide-fade-leave-active {
    transition: all 0.2s ease;
  }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .article-list {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>

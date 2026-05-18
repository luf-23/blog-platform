<script setup>
import { ref } from "vue";
import { getArticleDetailService } from "../../api/admin";
import { useRoute, useRouter } from "vue-router";
import { ArrowLeft, Clock } from "@element-plus/icons-vue";
import { MdPreview } from "md-editor-v3";
import "md-editor-v3/lib/preview.css";
import {
  acceptArticleService,
  rejectArticleService,
  dropArticleService
} from "../../api/admin";
import { ElMessage, ElMessageBox } from "element-plus";
import { check } from "../../utils/admin/check";
check();
const route = useRoute();
const router = useRouter();
const articleData = ref({
  articleId: "",
  categoryId: "",
  author: "",
  title: "",
  content: "",
  status: "",
  createTime: "",
  updateTime: ""
});
const isPublished = ref(false); // 是否已发布
const getArticleDetail = async () => {
  const articleId = route.query.articleId;
  const result = await getArticleDetailService({
    articleId: articleId
  });
  articleData.value = result.data;
  articleData.value.author = route.query.author;
  isPublished.value = articleData.value.status === "published";
};
getArticleDetail();

const getStatusType = (status) => {
  switch (status) {
    case "published":
      return "success";
    case "draft":
      return "info";
    case "pending":
      return "warning";
    default:
      return "info";
  }
};

const getStatusText = (status) => {
  switch (status) {
    case "published":
      return "已发布";
    case "pending":
      return "待审核";
    case "draft":
      return "草稿";
    default:
      return "未知状态";
  }
};

const goBack = () => {
  router.go(-1);
};

const handleAccept = async () => {
  // 接受文章逻辑
  await acceptArticleService({
    articleId: articleData.value.articleId
  });
  ElMessage.success("文章已接受");
  await getArticleDetail(); // 刷新文章详情
};
const handleReject = async () => {
  ElMessageBox.confirm(
    `确定要拒绝${articleData.value.author}发布的 ${
      articleData.value.title.length > 10
        ? articleData.value.title.slice(0, 10) + "..."
        : articleData.value.title
    } 吗`,
    "警告",
    {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning"
    }
  ).then(async () => {
    // 拒绝文章逻辑
    await rejectArticleService({
      articleId: articleData.value.articleId
    });
    ElMessage.success("文章已拒绝");
    await getArticleDetail(); // 刷新文章详情
  });
};
const handleDrop = async () => {
  ElMessageBox.confirm(
    `下架${articleData.value.author}发布的 ${
      articleData.value.title.length > 10
        ? articleData.value.title.slice(0, 10) + "..."
        : articleData.value.title
    } 吗`,
    "警告",
    {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning"
    }
  ).then(async () => {
    // 下架文章逻辑
    await dropArticleService({
      articleId: articleData.value.articleId
    });
    ElMessage.success("文章已下架");
    await getArticleDetail(); // 刷新文章详情
  });
};

const handleAuthorClick = (authorName) => {
  router.push({
    name: "Profile",
    query: {
      author: authorName
    }
  });
};

const formatDate = (dateString) => {
  if (!dateString) return "";
  const date = new Date(dateString);
  return date.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false
  });
};
</script>

<template>
  <div class="article-container">
    <div class="article-header">
      <el-button @click="goBack" class="back-button">
        <el-icon><arrow-left /></el-icon>
        返回
      </el-button>
      <h1>文章详情</h1>
    </div>

    <el-card class="article-card">
      <div class="article-meta">
        <div class="left-meta">
          <div class="meta-item">
            <span class="label">文章ID：</span>
            <span>{{ articleData.id }}</span>
          </div>
          <div class="meta-item">
            <span class="label">状态：</span>
            <el-tag :type="getStatusType(articleData.status)">
              {{ getStatusText(articleData.status) }}
            </el-tag>
          </div>
          <div
            class="meta-item author-info"
            @click="handleAuthorClick(articleData.author)"
          >
            <span class="label">作者：</span>
            <span>{{ articleData.author }}</span>
          </div>
        </div>
        <div class="right-meta" v-if="articleData.status !== 'draft'">
          <el-button type="primary" v-if="!isPublished" @click="handleAccept"
            >接受</el-button
          >
          <el-button type="danger" v-if="!isPublished" @click="handleReject"
            >拒绝</el-button
          >
          <el-button type="danger" v-if="isPublished" @click="handleDrop"
            >下架</el-button
          >
        </div>
      </div>

      <div class="article-content">
        <h2 class="article-title">{{ articleData.title }}</h2>

        <div class="time-info">
          <span class="time-item">
            <el-icon><clock /></el-icon>
            创建时间：{{ formatDate(articleData.createTime) }}
          </span>
          <span class="time-item">
            <el-icon><clock /></el-icon>
            更新时间：{{ formatDate(articleData.updateTime) }}
          </span>
        </div>

        <div class="content-box">
          <div class="content-label">文章内容：</div>
          <md-preview
            :modelValue="articleData.content"
            preview-theme="vuepress"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.article-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.article-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  gap: 20px;
  background: white;
  padding: 16px 24px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.article-header h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
  font-weight: 500;
}

.back-button {
  display: flex;
  align-items: center;
  gap: 5px;
  transition: all 0.3s ease;
}

.back-button:hover {
  transform: translateX(-3px);
}

.article-card {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.article-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.article-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px 24px;
  border-bottom: 1px solid #ebeef5;
  background: #fafafa;
  border-radius: 8px 8px 0 0;
}

.left-meta {
  display: flex;
  gap: 24px;
}

.right-meta {
  display: flex;
  gap: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-info {
  padding: 8px 12px;
  border: 1px solid #e1e4e8;
  border-radius: 6px;
  background-color: #f8f9fa;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  cursor: pointer;
}

.label {
  color: #606266;
  font-weight: 500;
}

.article-content {
  padding: 24px;
}

.article-title {
  font-size: 28px;
  color: #303133;
  margin: 0 0 24px 0;
  font-weight: 600;
  line-height: 1.4;
}

.time-info {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
  color: #909399;
  font-size: 14px;
  padding: 16px;
  background: #f8f9fb;
  border-radius: 6px;
}

.time-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.content-box {
  margin-top: 24px;
}

.content-label {
  font-weight: 500;
  color: #606266;
  margin-bottom: 16px;
  font-size: 16px;
}

:deep(.md-preview-wrapper) {
  background-color: transparent !important;
  padding: 0 !important;
}

:deep(.md-preview-wrapper pre) {
  background-color: #f6f8fa;
  border-radius: 6px;
  margin: 16px 0;
}

:deep(.md-preview-wrapper code) {
  background-color: rgba(175, 184, 193, 0.2);
  padding: 0.2em 0.4em;
  border-radius: 4px;
  font-size: 85%;
  font-family: "Courier New", Courier, monospace;
}

:deep(.md-preview-wrapper pre code) {
  background-color: transparent;
  padding: 1em;
  display: block;
  overflow-x: auto;
}

:deep(.el-button) {
  transition: all 0.3s ease;
}

:deep(.el-button:hover) {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

:deep(.el-tag) {
  font-weight: 500;
  padding: 6px 12px;
  border-radius: 4px;
}

@media (max-width: 768px) {
  .article-container {
    padding: 10px;
  }

  .article-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
    padding: 12px;
  }

  .article-header h1 {
    font-size: 20px;
  }

  .article-meta {
    flex-direction: column;
    padding: 15px;
  }

  .left-meta,
  .right-meta {
    width: 100%;
  }

  .left-meta {
    flex-direction: column;
    gap: 10px;
  }

  .right-meta {
    margin-top: 15px;
    justify-content: flex-start;
    gap: 8px;
  }

  .article-title {
    font-size: 22px;
    margin-bottom: 16px;
  }

  .time-info {
    flex-direction: column;
    gap: 8px;
    padding: 12px;
    font-size: 12px;
  }

  :deep(.md-preview-wrapper) {
    font-size: 14px;
  }

  :deep(.md-preview-wrapper pre) {
    margin: 12px 0;
    padding: 8px;
    font-size: 12px;
    overflow-x: auto;
  }

  :deep(.el-button) {
    padding: 8px 12px;
    font-size: 14px;
  }
}
</style>

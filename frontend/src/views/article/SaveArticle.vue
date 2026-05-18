<script setup>
import { ref, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { MdEditor } from "md-editor-v3";
import { ElMessage, ElLoading, ElMessageBox } from "element-plus";
import { OSSClient } from "../../utils/oss";
import {
  addArticleService,
  getArticleDetailService,
  updateArticleService
} from "../../api/article";
import "md-editor-v3/lib/style.css";

const route = useRoute();
const router = useRouter();
const ossClient = new OSSClient();

// 根据屏幕宽度设置预览状态
const previewShow = ref(window.innerWidth > 768);
const updatePreviewState = () => {
  previewShow.value = window.innerWidth > 768;
};

// 监听窗口大小变化
window.addEventListener("resize", updatePreviewState);
onUnmounted(() => {
  window.removeEventListener("resize", updatePreviewState);
});

// 图片验证配置
const IMAGE_CONFIG = {
  maxSize: 5 * 1024 * 1024, // 5MB
  allowedTypes: ["image/jpeg", "image/png", "image/gif", "image/webp"]
};

// 验证图片
const validateImage = (file) => {
  // 验证文件类型
  if (!IMAGE_CONFIG.allowedTypes.includes(file.type)) {
    ElMessage.error("只支持 JPG、PNG、GIF、WEBP 格式的图片");
    return false;
  }

  // 验证文件大小
  if (file.size > IMAGE_CONFIG.maxSize) {
    ElMessage.error("图片大小不能超过 5MB");
    return false;
  }

  return true;
};

const isEdit = ref(route.query.type === "edit");
const categoryId = ref(route.query.categoryId);
const title = ref("");
const content = ref("");
const getArticleDetail = async () => {
  const result = await getArticleDetailService({
    articleId: route.query.articleId,
    categoryId: categoryId.value
  });
  title.value = result.data.title;
  content.value = result.data.content;
};
if (isEdit.value) {
  getArticleDetail();
}
const status = ref("draft"); // 默认为草稿状态

const handleSave = async () => {
  if (!title.value.trim()) {
    ElMessage.warning("请输入文章标题");
    return;
  }
  if (!content.value.trim()) {
    ElMessage.warning("请输入文章内容");
    return;
  }
  if (!isEdit.value) {
    await addArticleService({
      title: title.value,
      content: content.value,
      status: status.value === "published" ? "pending" : "draft",
      categoryId: categoryId.value
    });
    if (status.value === "published") {
      ElMessage.success("发布成功");
    } else {
      ElMessage.success("文章已保存为草稿");
    }
  } else {
    await updateArticleService({
      articleId: route.query.articleId,
      title: title.value,
      content: content.value,
      status: status.value === "published" ? "pending" : "draft",
      categoryId: categoryId.value
    });
    ElMessage.success("文章已更新");
  }
  const redirect = router.currentRoute.value.query.redirect || "/home";
  router.push(redirect);
};

const handleBack = () => {
  router.back();
};

// 上传状态管理
const uploadingCount = ref(0);
const cancelTokens = new Map();

// 创建加载实例
const createLoadingInstance = (text) => {
  return ElLoading.service({
    lock: true,
    text: text,
    background: "rgba(0, 0, 0, 0.5)"
  });
};

const handleImageUpload = async (files, callback) => {
  try {
    // 验证所有图片
    const validFiles = files.filter(validateImage);
    if (validFiles.length === 0) return;

    // 显示总体上传进度
    const loading = createLoadingInstance(
      `正在上传 ${validFiles.length} 张图片...`
    );
    uploadingCount.value = validFiles.length;

    await ossClient.init();
    const promises = validFiles.map(async (file, index) => {
      try {
        const extension = file.name.split(".").pop();
        const fileName = ossClient.generateFileName(
          route.query.articleId || "temp",
          OSSClient.IMAGE_TYPE.ARTICLE_CONTENT,
          extension
        );

        // 创建取消令牌
        const cancelToken = { cancel: false };
        cancelTokens.set(fileName, cancelToken);

        // 定期更新加载提示
        const updateProgress = () => {
          if (cancelToken.cancel) return;
          loading.text = `正在上传第 ${index + 1}/${
            validFiles.length
          } 张图片 (${file.name})`;
        };
        const progressInterval = setInterval(updateProgress, 500);

        try {
          await ossClient.uploadFile(fileName, file);
          clearInterval(progressInterval);
          uploadingCount.value--;
          return ossClient.generateFileUrl(fileName);
        } catch (error) {
          clearInterval(progressInterval);
          if (cancelToken.cancel) {
            throw new Error("已取消上传");
          }
          throw error;
        } finally {
          cancelTokens.delete(fileName);
        }
      } catch (error) {
        ElMessage.error(`图片 ${file.name} 上传失败: ${error.message}`);
        return null;
      }
    });

    const urls = (await Promise.all(promises)).filter((url) => url !== null);
    if (urls.length > 0) {
      callback(urls);
      ElMessage.success(`成功上传 ${urls.length} 张图片`);
    }

    loading.close();
  } catch (error) {
    console.error("图片上传失败:", error);
    ElMessage.error("图片上传失败，请重试");
  } finally {
    uploadingCount.value = 0;
    cancelTokens.clear();
  }
};

const handlePasteImage = async (event, callback) => {
  const items = event.clipboardData.items;
  const imageItems = Array.from(items).filter((item) =>
    item.type.startsWith("image")
  );

  if (imageItems.length === 0) return;

  try {
    const loading = createLoadingInstance("正在处理粘贴的图片...");
    uploadingCount.value = imageItems.length;

    await ossClient.init();
    const promises = imageItems.map(async (item) => {
      const file = item.getAsFile();
      if (!validateImage(file)) return null;

      try {
        const extension = file.type.split("/")[1];
        const fileName = ossClient.generateFileName(
          route.query.articleId || "temp",
          OSSClient.IMAGE_TYPE.ARTICLE_CONTENT,
          extension
        );

        // 创建取消令牌
        const cancelToken = { cancel: false };
        cancelTokens.set(fileName, cancelToken);

        try {
          await ossClient.uploadFile(fileName, file);
          uploadingCount.value--;
          return ossClient.generateFileUrl(fileName);
        } catch (error) {
          if (cancelToken.cancel) {
            throw new Error("已取消上传");
          }
          throw error;
        } finally {
          cancelTokens.delete(fileName);
        }
      } catch (error) {
        ElMessage.error(`粘贴的图片上传失败: ${error.message}`);
        return null;
      }
    });

    const urls = (await Promise.all(promises)).filter((url) => url !== null);
    if (urls.length > 0) {
      callback(urls);
      ElMessage.success(`成功上传 ${urls.length} 张图片`);
    }

    loading.close();
  } catch (error) {
    console.error("图片上传失败:", error);
    ElMessage.error("图片上传失败，请重试");
  } finally {
    uploadingCount.value = 0;
    cancelTokens.clear();
  }
};

// 取消所有正在进行的上传
const cancelAllUploads = () => {
  if (uploadingCount.value === 0) return;

  cancelTokens.forEach((token) => {
    token.cancel = true;
  });
  cancelTokens.clear();
  uploadingCount.value = 0;
  ElMessage.info("已取消所有上传任务");
};

// 在组件卸载时检查是否需要取消上传
onUnmounted(() => {
  // 只有在还有图片正在上传时才取消并提示
  if (uploadingCount.value > 0) {
    cancelAllUploads();
  }
});
</script>

<template>
  <div class="add-article">
    <div class="article-header">
      <div class="left-section">
        <el-button @click="handleBack">返回</el-button>
        <input
          v-model="title"
          type="text"
          class="title-input"
          placeholder="请输入文章标题"
        />
      </div>
      <div class="right-section">
        <el-select v-model="status" class="status-select">
          <el-option label="草稿" value="draft" />
          <el-option label="发布" value="published" />
        </el-select>
        <el-button type="primary" @click="handleSave">保存文章</el-button>
      </div>
    </div>
    <div class="editor-container">
      <md-editor
        v-model="content"
        preview-theme="vuepress"
        style="height: calc(100vh - 150px)"
        :preview="previewShow"
        :toolbars="[
          'preview',
          'bold',
          'underline',
          'italic',
          'strikeThrough',
          'title',
          'sub',
          'sup',
          'quote',
          'unorderedList',
          'orderedList',
          'codeRow',
          'code',
          'link',
          'image',
          'table',
          'revoke',
          'next',
          'pageFullscreen',
          'fullscreen',
          'htmlPreview',
          'catalog'
        ]"
        @upload-img="handleImageUpload"
        @paste-image="handlePasteImage"
      />
      <div v-if="uploadingCount > 0" class="upload-progress">
        <p>正在上传图片 ({{ uploadingCount }} 张待处理)</p>
        <el-button size="small" type="danger" @click="cancelAllUploads">
          取消上传
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.add-article {
  padding: 24px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.article-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
  padding: 16px 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.left-section {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.right-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title-input {
  flex: 1;
  padding: 12px 16px;
  font-size: 16px;
  border: 2px solid #dcdfe6;
  border-radius: 8px;
  outline: none;
  transition: all 0.3s ease;
  background-color: #f8fafc;
  color: #2c3e50;
}

.title-input:hover {
  border-color: #c0c4cc;
}

.title-input:focus {
  border-color: #409eff;
  background-color: #fff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.title-input::placeholder {
  color: #909399;
}

.status-select {
  width: 120px;
}

:deep(.el-select) {
  .el-input__wrapper {
    border-radius: 8px;
  }
}

.editor-container {
  flex: 1;
  border-radius: 8px;
  overflow: hidden;
  background: white;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

:deep(.md-editor) {
  height: 100% !important;
  border: none !important;
}

:deep(.md-editor-toolbar) {
  border-bottom: 1px solid #e4e7ed !important;
  background-color: #f8fafc !important;
  padding: 8px 16px !important;

  .md-toolbar-item {
    margin: 0 4px !important;
    padding: 6px !important;
    border-radius: 6px !important;
    transition: all 0.2s ease !important;

    &:hover {
      background-color: #ecf5ff !important;
      transform: translateY(-1px) !important;
    }

    &.active {
      background-color: #409eff !important;
      color: white !important;
    }

    svg {
      width: 18px !important;
      height: 18px !important;
      stroke-width: 2px !important;
    }
  }

  .md-toolbar-item + .md-toolbar-item {
    margin-left: 2px !important;
  }

  .md-toolbar-divider {
    margin: 0 8px !important;
    background-color: #e4e7ed !important;
  }
}

:deep(.md-editor-content) {
  background-color: #fff !important;
}

:deep(.el-button) {
  border-radius: 8px;
  padding: 12px 20px;
  transition: all 0.3s ease;
  font-weight: 500;
}

:deep(.el-button:hover) {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #409eff 0%, #007fff 100%);
}

:deep(.el-select-dropdown__item) {
  padding: 8px 16px;
}

:deep(.el-select-dropdown__item.selected) {
  font-weight: 600;
  background-color: #ecf5ff;
}

.upload-progress {
  position: fixed;
  bottom: 20px;
  right: 20px;
  background: white;
  padding: 12px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 1000;
}

.upload-progress p {
  margin: 0;
  color: #606266;
}

@media screen and (max-width: 768px) {
  .article-header {
    flex-direction: column;
    align-items: stretch;
  }

  .left-section {
    flex-direction: column;
  }

  .right-section {
    justify-content: flex-end;
    margin-top: 16px;
  }

  .title-input {
    width: 100%;
  }
}
</style>

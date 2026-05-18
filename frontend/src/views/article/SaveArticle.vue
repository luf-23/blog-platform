<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElLoading } from "element-plus";
import { ArrowLeft } from "@element-plus/icons-vue";
import { MdEditor } from "md-editor-v3";
import "md-editor-v3/lib/style.css";

import { useTheme } from "../../composables/useTheme.js";
import { OSSClient } from "../../utils/oss/index.js";
import {
  addArticleService,
  getArticleDetailService,
  updateArticleService
} from "../../api/article.js";

const route = useRoute();
const router = useRouter();
const { isDark } = useTheme();
const ossClient = new OSSClient();

const isEdit = computed(() => route.query.type === "edit");
const categoryId = ref(route.query.categoryId);
const title = ref("");
const content = ref("");
const status = ref("draft");
const saving = ref(false);

const editorTheme = computed(() => (isDark.value ? "dark" : "light"));
const previewTheme = computed(() => (isDark.value ? "vuepress-dark" : "vuepress"));
const codeTheme = computed(() => (isDark.value ? "github-dark" : "github"));

async function loadDetail() {
  const result = await getArticleDetailService({
    articleId: route.query.articleId,
    categoryId: categoryId.value
  });
  title.value = result.data?.title || "";
  content.value = result.data?.content || "";
  status.value = result.data?.status === "draft" ? "draft" : "published";
}

if (isEdit.value) loadDetail();

async function save() {
  if (!title.value.trim()) {
    ElMessage.warning("请填写文章标题");
    return;
  }
  if (!content.value.trim()) {
    ElMessage.warning("请输入文章内容");
    return;
  }
  saving.value = true;
  try {
    const payload = {
      title: title.value,
      content: content.value,
      status: status.value === "published" ? "pending" : "draft",
      categoryId: categoryId.value
    };
    if (isEdit.value) {
      await updateArticleService({
        articleId: route.query.articleId,
        ...payload
      });
      ElMessage.success("文章已更新");
    } else {
      await addArticleService(payload);
      ElMessage.success(
        status.value === "published" ? "已提交审核" : "已保存为草稿"
      );
    }
    const redirect = route.query.redirect || "/article/category";
    router.push(redirect);
  } finally {
    saving.value = false;
  }
}

function back() {
  router.back();
}

const IMAGE_TYPES = ["image/jpeg", "image/png", "image/gif", "image/webp"];
const MAX_SIZE = 5 * 1024 * 1024;

function validate(file) {
  if (!IMAGE_TYPES.includes(file.type)) {
    ElMessage.error("只支持 JPG / PNG / GIF / WEBP 格式");
    return false;
  }
  if (file.size > MAX_SIZE) {
    ElMessage.error("图片不能超过 5MB");
    return false;
  }
  return true;
}

const uploadingCount = ref(0);

async function uploadImages(files, callback) {
  const valid = files.filter(validate);
  if (!valid.length) return;
  const loading = ElLoading.service({
    lock: true,
    text: `正在上传 ${valid.length} 张图片...`,
    background: "rgba(15, 23, 42, 0.45)"
  });
  uploadingCount.value = valid.length;
  try {
    await ossClient.init();
    const urls = await Promise.all(
      valid.map(async (file) => {
        try {
          const ext = file.name.split(".").pop();
          const fileName = ossClient.generateFileName(
            route.query.articleId || "temp",
            OSSClient.IMAGE_TYPE.ARTICLE_CONTENT,
            ext
          );
          await ossClient.uploadFile(fileName, file);
          uploadingCount.value -= 1;
          return ossClient.generateFileUrl(fileName);
        } catch {
          uploadingCount.value -= 1;
          return null;
        }
      })
    );
    const filtered = urls.filter(Boolean);
    if (filtered.length) {
      callback(filtered);
      ElMessage.success(`成功上传 ${filtered.length} 张图片`);
    }
  } finally {
    loading.close();
    uploadingCount.value = 0;
  }
}

async function pasteImage(event, callback) {
  const items = event.clipboardData?.items || [];
  const files = Array.from(items)
    .filter((item) => item.type.startsWith("image"))
    .map((item) => item.getAsFile())
    .filter(Boolean);
  if (!files.length) return;
  await uploadImages(files, callback);
}

onUnmounted(() => {
  // 卸载时如有正在上传图片，提示
  if (uploadingCount.value > 0) {
    ElMessage.info("已离开编辑器，未完成的上传将中止");
  }
});

const toolbars = [
  "bold",
  "underline",
  "italic",
  "strikeThrough",
  "-",
  "title",
  "sub",
  "sup",
  "quote",
  "unorderedList",
  "orderedList",
  "-",
  "codeRow",
  "code",
  "link",
  "image",
  "table",
  "-",
  "revoke",
  "next",
  "=",
  "preview",
  "pageFullscreen",
  "fullscreen",
  "catalog"
];
</script>

<template>
  <div class="save-article">
    <header class="save-bar">
      <div class="save-bar__left">
        <el-button :icon="ArrowLeft" plain @click="back">返回</el-button>
        <input
          v-model="title"
          class="save-bar__title"
          placeholder="写一个吸引人的标题..."
        />
      </div>
      <div class="save-bar__right">
        <el-select v-model="status" class="save-bar__select">
          <el-option label="保存为草稿" value="draft" />
          <el-option label="提交发布" value="published" />
        </el-select>
        <el-button type="primary" :loading="saving" @click="save">
          {{ isEdit ? "保存修改" : "保存文章" }}
        </el-button>
      </div>
    </header>

    <div class="save-editor">
      <md-editor
        v-model="content"
        :theme="editorTheme"
        :preview-theme="previewTheme"
        :code-theme="codeTheme"
        :toolbars="toolbars"
        @upload-img="uploadImages"
        @paste-image="pasteImage"
      />
    </div>
  </div>
</template>

<style scoped>
.save-article {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
}

.save-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--bp-color-bg-elevated);
  border: 1px solid var(--bp-color-border);
  border-radius: var(--bp-radius-md);
  flex-wrap: wrap;
}

.save-bar__left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.save-bar__title {
  flex: 1;
  min-width: 200px;
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid var(--bp-color-border);
  background: var(--bp-color-bg-soft);
  font-size: 15px;
  font-weight: 600;
  color: var(--bp-color-text-primary);
  outline: none;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.save-bar__title:focus {
  border-color: var(--bp-color-primary);
  background: var(--bp-color-bg-elevated);
}

.save-bar__right {
  display: flex;
  gap: 10px;
  align-items: center;
}

.save-bar__select {
  width: 140px;
}

.save-editor {
  flex: 1;
  min-height: 0;
  border-radius: var(--bp-radius-md);
  overflow: hidden;
  border: 1px solid var(--bp-color-border);
}

:deep(.md-editor) {
  height: 100% !important;
  border-radius: var(--bp-radius-md);
}

@media (max-width: 720px) {
  .save-bar__right {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>

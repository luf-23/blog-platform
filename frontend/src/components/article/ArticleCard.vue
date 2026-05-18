<script setup>
import { computed } from "vue";
import { Picture, Calendar, User, Delete, EditPen } from "@element-plus/icons-vue";

const props = defineProps({
  article: { type: Object, required: true },
  showAuthor: { type: Boolean, default: true },
  showStatus: { type: Boolean, default: true },
  showCategoryId: { type: Boolean, default: false },
  showDelete: { type: Boolean, default: false },
  showChangeCoverImage: { type: Boolean, default: false }
});

const emit = defineEmits(["click", "delete", "changeCoverImage"]);

function formatDate(value) {
  if (!value) return "";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleDateString("zh-CN");
}

const statusConfig = computed(() => {
  switch (props.article.status) {
    case "published":
      return { label: "已发布", type: "success" };
    case "pending":
      return { label: "待审核", type: "warning" };
    case "draft":
      return { label: "草稿", type: "info" };
    default:
      return { label: props.article.status || "未知", type: "" };
  }
});

const summary = computed(() => {
  const content = props.article.content || "";
  const text = content.replace(/[#>*`_~\-!\[\]\(\)]/g, " ").replace(/\s+/g, " ").trim();
  return text.length > 120 ? `${text.slice(0, 120)}…` : text;
});

function onClick() {
  emit("click", props.article);
}

function onDelete(e) {
  e.stopPropagation();
  emit("delete", props.article);
}

function onChangeCover(e) {
  e.stopPropagation();
  emit("changeCoverImage", props.article);
}
</script>

<template>
  <article class="article-card bp-card bp-card-hover" @click="onClick">
    <div class="article-card__cover">
      <img
        v-if="article.coverImage"
        :src="article.coverImage"
        :alt="article.title"
      />
      <div v-else class="article-card__cover-fallback">
        <el-icon size="32"><Picture /></el-icon>
      </div>
      <div v-if="showStatus" class="article-card__status">
        <el-tag :type="statusConfig.type" size="small" effect="light" round>
          {{ statusConfig.label }}
        </el-tag>
      </div>
    </div>

    <div class="article-card__body">
      <h3 class="article-card__title">{{ article.title || "无标题" }}</h3>
      <p class="article-card__summary">{{ summary || "暂无内容" }}</p>

      <div class="article-card__meta">
        <span v-if="showAuthor && article.author" class="meta-pill">
          <el-icon><User /></el-icon>{{ article.author }}
        </span>
        <span class="meta-pill">
          <el-icon><Calendar /></el-icon>
          {{ formatDate(article.updateTime || article.createTime) }}
        </span>
        <span v-if="showCategoryId && article.categoryId" class="meta-pill">
          #{{ article.categoryId }}
        </span>
      </div>

      <div
        v-if="showDelete || showChangeCoverImage"
        class="article-card__actions"
      >
        <el-button
          v-if="showChangeCoverImage"
          size="small"
          plain
          @click="onChangeCover"
        >
          <el-icon><EditPen /></el-icon> 更换封面
        </el-button>
        <el-button
          v-if="showDelete"
          size="small"
          type="danger"
          plain
          @click="onDelete"
        >
          <el-icon><Delete /></el-icon> 删除
        </el-button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.article-card {
  display: flex;
  flex-direction: column;
  cursor: pointer;
  overflow: hidden;
}

.article-card__cover {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: var(--bp-gradient-soft);
}

.article-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.article-card:hover .article-card__cover img {
  transform: scale(1.05);
}

.article-card__cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--bp-color-primary);
}

.article-card__status {
  position: absolute;
  top: 12px;
  left: 12px;
}

.article-card__body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
}

.article-card__title {
  font-size: 16px;
  font-weight: 600;
  line-height: 1.35;
  color: var(--bp-color-text-primary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-card__summary {
  font-size: 13px;
  color: var(--bp-color-text-secondary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-card__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: auto;
}

.meta-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--bp-color-bg-soft);
  color: var(--bp-color-text-tertiary);
  font-size: 12px;
}

.meta-pill .el-icon {
  font-size: 12px;
}

.article-card__actions {
  display: flex;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px dashed var(--bp-color-divider);
}
</style>

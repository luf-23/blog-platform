<script setup>
import { computed } from "vue";
import { Folder, EditPen, Delete } from "@element-plus/icons-vue";

const props = defineProps({
  category: { type: Object, required: true }
});

const emit = defineEmits(["click", "edit", "delete"]);

const initial = computed(() => (props.category.name || "?").trim().slice(0, 2));

function formatDate(value) {
  if (!value) return "";
  return new Date(value).toLocaleDateString("zh-CN");
}

function onEdit(e) {
  e.stopPropagation();
  emit("edit", props.category);
}

function onDelete(e) {
  e.stopPropagation();
  emit("delete", props.category);
}
</script>

<template>
  <div class="category-card bp-card bp-card-hover" @click="emit('click', category)">
    <div class="category-card__avatar">
      <span>{{ initial }}</span>
    </div>
    <div class="category-card__body">
      <h3 class="category-card__title">
        <el-icon><Folder /></el-icon>
        {{ category.name }}
      </h3>
      <p class="category-card__desc">
        {{ category.description || "暂无描述" }}
      </p>
      <div class="category-card__meta">
        <span>创建于 {{ formatDate(category.createTime) }}</span>
      </div>
    </div>
    <div class="category-card__actions">
      <el-button size="small" circle plain @click="onEdit">
        <el-icon><EditPen /></el-icon>
      </el-button>
      <el-button size="small" type="danger" circle plain @click="onDelete">
        <el-icon><Delete /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.category-card {
  position: relative;
  padding: 18px 18px 18px 20px;
  padding-right: 88px;
  cursor: pointer;
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.category-card__avatar {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: var(--bp-gradient-hero);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
  flex-shrink: 0;
  box-shadow: 0 6px 18px rgba(99, 102, 241, 0.3);
}

.category-card__body {
  flex: 1;
  min-width: 0;
}

.category-card__title {
  font-size: 16px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--bp-color-text-primary);
  margin-bottom: 6px;
}

.category-card__desc {
  font-size: 13px;
  color: var(--bp-color-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}

.category-card__meta {
  margin-top: 10px;
  font-size: 12px;
  color: var(--bp-color-text-tertiary);
}

.category-card__actions {
  position: absolute;
  top: 14px;
  right: 14px;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
}

.category-card__actions :deep(.el-button) {
  margin: 0;
  width: 32px;
  height: 32px;
}

.category-card__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
</style>

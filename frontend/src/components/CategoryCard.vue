<script setup>
const props = defineProps({
  category: {
    type: Object,
    required: true,
    default: () => ({
      name: "",
      description: "",
      createTime: "",
      updateTime: ""
    })
  },
  onEdit: {
    type: Function,
    default: () => {}
  },
  onDelete: {
    type: Function,
    default: () => {}
  },
  onClick: {
    type: Function,
    default: () => {}
  }
});

const formatDate = (dateString) => {
  return new Date(dateString).toLocaleDateString();
};
</script>

<template>
  <div class="category-card" @click="onClick">
    <h2 class="category-title">{{ category.name }}</h2>
    <p class="category-description">{{ category.description }}</p>
    <div class="category-info">
      <span class="time">创建时间：{{ formatDate(category.createTime) }}</span>
      <span class="time">更新时间：{{ formatDate(category.updateTime) }}</span>
    </div>
    <div class="category-actions" @click.stop>
      <el-button size="small" @click="onEdit">编辑</el-button>
      <el-button size="small" type="danger" @click="onDelete">删除</el-button>
    </div>
  </div>
</template>

<style scoped>
.category-card {
  padding: 20px;
  border-radius: 8px;
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.category-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.category-title {
  margin: 0 0 10px 0;
  font-size: 18px;
  color: #333;
  word-break: break-all;
  overflow-wrap: break-word;
}

.category-description {
  margin: 0 0 15px 0;
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  word-break: break-all;
  overflow-wrap: break-word;
}

.category-info {
  display: flex;
  gap: 15px;
  margin-bottom: 15px;
  font-size: 14px;
  color: #666;
  flex-wrap: wrap;
}

.category-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  border-top: 1px solid #ebeef5;
  padding-top: 15px;
  margin-top: 10px;
}

@media screen and (max-width: 768px) {
  .category-card {
    padding: 15px;
  }

  .category-title {
    font-size: 16px;
  }

  .category-description {
    font-size: 14px;
  }

  .category-info {
    flex-direction: column;
    gap: 5px;
  }

  .category-actions {
    padding-top: 12px;
  }

  :deep(.el-button) {
    padding: 6px 12px;
    font-size: 12px;
  }
}
</style>

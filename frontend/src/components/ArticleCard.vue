<script setup>
const props = defineProps({
  article: {
    type: Object,
    required: true,
    default: () => ({
      title: "",
      author: "",
      createTime: "",
      updateTime: "",
      content: "",
      articleId: "",
      categoryId: "",
      status: "",
      coverImage: "" // 添加封面图属性
    })
  },
  onClick: {
    type: Function,
    default: () => {}
  },
  onDelete: {
    type: Function,
    default: () => {}
  },
  onChangeCoverImage: {
    type: Function,
    default: () => {}
  },
  showCategoryId: {
    type: Boolean,
    default: true
  },
  showStatus: {
    type: Boolean,
    default: true
  },
  showDelete: {
    type: Boolean,
    default: false
  },
  showChangeCoverImage: {
    type: Boolean,
    default: false
  },
  showAuthor: {
    type: Boolean,
    default: true
  }
});

const formatDate = (dateString) => {
  return new Date(dateString).toLocaleDateString();
};

const statusColor = (status) => {
  switch (status) {
    case "draft":
      return "gray";
    case "pending":
      return "orange";
    case "published":
      return "green";
    default:
      return "gray";
  }
};

const handleDelete = (e) => {
  e.stopPropagation(); // 阻止事件冒泡，避免触发card的click事件
  props.onDelete();
};

const handleChangeCoverImage = (e) => {
  e.stopPropagation(); // 阻止事件冒泡，避免触发card的click事件
  props.onChangeCoverImage();
};
</script>

<template>
  <div class="article-card" @click="onClick">
    <div v-if="article.coverImage" class="article-cover">
      <img :src="article.coverImage" :alt="article.title" />
    </div>
    <div class="article-header">
      <h2 class="article-title">{{ article.title }}</h2>
      <div class="header-actions">
        <span
          v-if="showStatus"
          class="status-tag"
          :class="statusColor(article.status)"
        >
          {{ article.status }}
        </span>
        <button v-if="showDelete" class="delete-btn" @click="handleDelete">
          删除
        </button>
        <button
          v-if="showChangeCoverImage"
          class="change-cover-btn"
          @click="handleChangeCoverImage"
        >
          更改封面
        </button>
      </div>
    </div>

    <div class="article-meta">
      <div class="meta-item">
        <span class="meta-label">ID:</span>
        <span class="meta-value">{{ article.articleId }}</span>
      </div>
      <div class="meta-item" v-if="showCategoryId">
        <span class="meta-label">分类ID:</span>
        <span class="meta-value">{{ article.categoryId }}</span>
      </div>
    </div>

    <div class="article-info">
      <span class="author" v-if="showAuthor">作者：{{ article.author }}</span>
      <span class="time">发布时间：{{ formatDate(article.createTime) }}</span>
      <span class="time">更新时间：{{ formatDate(article.updateTime) }}</span>
    </div>

    <p class="article-content">
      {{
        article.content?.length > 100
          ? article.content.substring(0, 100) + "..."
          : article.content
      }}
    </p>
  </div>
</template>

<style scoped>
.article-card {
  padding: 20px;
  border-radius: 8px;
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.article-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.article-cover {
  margin: -20px -20px 15px -20px;
  height: 200px;
  overflow: hidden;
  border-radius: 8px 8px 0 0;
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.article-card:hover .article-cover img {
  transform: scale(1.05);
}

.article-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  gap: 10px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.article-title {
  margin: 0;
  font-size: 18px;
  color: #333;
  word-break: break-all;
  overflow-wrap: break-word;
  flex: 1;
}

.status-tag {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  text-transform: capitalize;
}

.status-tag.gray {
  background-color: #e0e0e0;
  color: #616161;
}

.status-tag.orange {
  background-color: #ffe0b2;
  color: #e65100;
}

.status-tag.green {
  background-color: #c8e6c9;
  color: #2e7d32;
}

.delete-btn {
  padding: 4px 10px;
  border: none;
  border-radius: 4px;
  background-color: #ffebee;
  color: #c62828;
  font-size: 12px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s ease;
}

.delete-btn:hover {
  background-color: #ffcdd2;
}

.change-cover-btn {
  padding: 4px 10px;
  border: none;
  border-radius: 4px;
  background-color: #e3f2fd;
  color: #1565c0;
  font-size: 12px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s ease;
}

.change-cover-btn:hover {
  background-color: #bbdefb;
}

.article-meta {
  display: flex;
  gap: 15px;
  margin-bottom: 10px;
  font-size: 13px;
  color: #666;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-label {
  font-weight: bold;
}

.meta-value {
  font-family: monospace;
}

.article-info {
  display: flex;
  gap: 15px;
  margin-bottom: 10px;
  font-size: 14px;
  color: #666;
  flex-wrap: wrap;
}

.article-content {
  margin: 0;
  line-height: 1.6;
  color: #555;
  word-break: break-all;
  overflow-wrap: break-word;
}

@media screen and (max-width: 768px) {
  .article-card {
    padding: 15px;
  }

  .article-cover {
    margin: -15px -15px 12px -15px;
    height: 150px;
  }

  .article-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .header-actions {
    width: 100%;
    justify-content: space-between;
  }

  .status-tag {
    align-self: flex-start;
  }

  .article-info {
    flex-direction: column;
    gap: 5px;
  }

  .article-title {
    font-size: 16px;
  }

  .article-content {
    font-size: 14px;
  }
}
</style>

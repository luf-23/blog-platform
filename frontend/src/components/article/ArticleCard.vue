<template>
  <article class="article-card card" @click="gotoDetail">
    <div class="card-cover" v-if="article.coverImage">
      <img :src="article.coverImage" :alt="article.title" loading="lazy" />
    </div>
    <div class="card-body">
      <div class="card-meta">
        <div class="author" @click.stop="$emit('author-click', { userId: article.userId, username: article.authorUsername })">
          <img
            :src="article.authorAvatar || defaultAvatar"
            class="avatar"
            style="width:24px;height:24px"
            :alt="article.authorNickname"
          />
          <span class="author-name">{{ article.authorNickname || article.authorUsername }}</span>
        </div>
        <span class="sep">·</span>
        <time class="date">{{ formatDate(article.createTime) }}</time>
        <span v-if="article.categoryName" class="sep">·</span>
        <span v-if="article.categoryName" class="category">{{ article.categoryName }}</span>
      </div>

      <h2 class="card-title">{{ article.title }}</h2>
      <p v-if="article.summary" class="card-summary">{{ article.summary }}</p>

      <div class="card-footer">
        <div class="card-tags" @click.stop>
          <span
            v-for="tag in (article.tags || []).slice(0, 3)"
            :key="tag.tagId"
            class="tag-pill"
            style="font-size:12px"
            @click="$emit('tag-click', tag)"
          >
            # {{ tag.tagName }}
          </span>
        </div>
        <div class="card-stats">
          <span class="stat">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
            {{ formatCount(article.viewCount) }}
          </span>
          <span class="stat">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/></svg>
            {{ formatCount(article.likeCount) }}
          </span>
          <span class="stat">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
            {{ formatCount(article.commentCount) }}
          </span>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  article: { type: Object, required: true }
})
defineEmits(['tag-click', 'author-click'])

const router = useRouter()
const defaultAvatar = 'https://luf-23.oss-cn-wuhan-lr.aliyuncs.com/avatar/default.png'

function gotoDetail() {
  router.push(`/article/${props.article.articleId}`)
}

function formatDate(time) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function formatCount(n) {
  if (!n) return '0'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}
</script>

<style scoped>
.article-card {
  display: flex;
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition);
  border-radius: var(--radius-lg);
}

.article-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.card-cover {
  width: 200px;
  flex-shrink: 0;
  overflow: hidden;
}
.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}
.article-card:hover .card-cover img {
  transform: scale(1.04);
}

.card-body {
  flex: 1;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.author {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}
.author:hover .author-name { color: var(--c-primary); }

.author-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--c-text-3);
  transition: color var(--transition);
}

.sep { color: var(--c-text-4); font-size: 12px; }
.date { font-size: 12px; color: var(--c-text-4); }
.category {
  font-size: 12px;
  color: var(--c-primary);
  background: var(--c-primary-light);
  padding: 1px 8px;
  border-radius: var(--radius-full);
  font-weight: 500;
}

.card-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--c-text);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color var(--transition);
}
.article-card:hover .card-title { color: var(--c-primary); }

.card-summary {
  font-size: 14px;
  color: var(--c-text-3);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 4px;
}

.card-tags { display: flex; flex-wrap: wrap; gap: 4px; }

.card-stats {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--c-text-4);
}

@media (max-width: 640px) {
  .article-card { flex-direction: column; }
  .card-cover { width: 100%; height: 160px; }
}
</style>

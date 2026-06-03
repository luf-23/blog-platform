<template>
  <div class="my-articles page-container">
    <div class="page-top">
      <div class="page-heading">
        <h1 class="page-title">我的博客</h1>
        <p class="page-subtitle">管理你的文章，创作属于你的内容</p>
      </div>
      <div class="page-actions">
        <router-link to="/article/categories" class="btn btn-secondary">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.59 13.41l-7.17 7.17a2 2 0 01-2.83 0L2 12V2h10l8.59 8.59a2 2 0 010 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>
          分类管理
        </router-link>
        <router-link to="/article/write" class="btn btn-primary">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          写文章
        </router-link>
      </div>
    </div>

    <!-- Filters -->
    <div class="filters-bar card">
      <div class="filters-inner">
        <el-input v-model="filters.title" placeholder="搜索文章标题" clearable style="width:240px" @input="debouncedFetch">
          <template #prefix>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
          </template>
        </el-input>

        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:140px" @change="fetchArticles">
          <el-option label="已发布" value="published"/>
          <el-option label="审核中" value="pending"/>
          <el-option label="草稿" value="draft"/>
        </el-select>

        <el-select v-model="filters.categoryId" placeholder="全部分类" clearable style="width:160px" @change="fetchArticles">
          <el-option v-for="c in categories" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId"/>
        </el-select>
      </div>

      <div class="filters-stats">
        共 <strong>{{ articles.length }}</strong> 篇文章
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-spinner" style="height:200px">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
    </div>

    <!-- Empty -->
    <div v-else-if="articles.length === 0" class="empty-state">
      <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="var(--c-text-4)" stroke-width="1.2">
        <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/>
      </svg>
      <p>还没有文章，快去创作吧！</p>
      <router-link to="/article/write" class="btn btn-primary">写第一篇文章</router-link>
    </div>

    <!-- Article table -->
    <div v-else class="article-table card">
      <div v-for="article in articles" :key="article.articleId" class="article-row">
        <div class="article-row-cover" v-if="article.coverImage">
          <img :src="article.coverImage" :alt="article.title"/>
        </div>
        <div class="article-row-body">
          <div class="article-row-top">
            <h3 class="article-row-title" @click="viewArticle(article)">{{ article.title }}</h3>
            <span class="badge" :class="`badge-${article.status}`">{{ statusLabel(article.status) }}</span>
          </div>
          <p class="article-row-summary">{{ article.summary || '暂无摘要' }}</p>
          <div class="article-row-meta">
            <span v-if="categoryName(article.categoryId)" class="meta-item">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.59 13.41l-7.17 7.17a2 2 0 01-2.83 0L2 12V2h10l8.59 8.59a2 2 0 010 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>
              {{ categoryName(article.categoryId) }}
            </span>
            <span class="meta-item">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              {{ formatDate(article.updateTime || article.createTime) }}
            </span>
            <span class="meta-item">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/></svg>
              {{ article.viewCount || 0 }}
            </span>
          </div>
        </div>
        <div class="article-row-actions">
          <button v-if="article.status === 'draft'" class="btn btn-secondary btn-sm" @click="submitForReview(article)">提交审核</button>
          <button class="btn btn-secondary btn-sm" @click="editArticle(article)">编辑</button>
          <button class="btn btn-ghost btn-sm" @click="deleteArticle(article)" style="color:var(--c-danger)">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getMyArticlesService, deleteArticleService, submitArticleForReviewService } from '../../api/article.js'
import { getCategoryListService } from '../../api/category.js'

const router = useRouter()
const articles = ref([])
const categories = ref([])
const loading = ref(false)

const filters = reactive({ title: '', status: '', categoryId: null })

let fetchTimer = null
function debouncedFetch() {
  clearTimeout(fetchTimer)
  fetchTimer = setTimeout(fetchArticles, 300)
}

async function fetchArticles() {
  loading.value = true
  try {
    const params = {}
    if (filters.title) params.title = filters.title
    if (filters.status) params.status = filters.status
    if (filters.categoryId) params.categoryId = filters.categoryId
    const res = await getMyArticlesService(params)
    articles.value = res.data || []
  } finally { loading.value = false }
}

async function fetchCategories() {
  try {
    const res = await getCategoryListService()
    categories.value = res.data || []
  } catch {}
}

function categoryName(id) {
  const cat = categories.value.find(c => c.categoryId === id)
  return cat?.categoryName
}

function statusLabel(s) {
  return { published: '已发布', pending: '审核中', draft: '草稿' }[s] || s
}

function formatDate(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}

function viewArticle(a) { router.push(`/article/${a.articleId}`) }
function editArticle(a) { router.push(`/article/edit/${a.articleId}`) }

async function deleteArticle(a) {
  try {
    await ElMessageBox.confirm(`确定删除《${a.title}》吗？此操作不可恢复。`, '删除文章', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteArticleService(a.articleId)
    ElMessage.success('已删除')
    fetchArticles()
  } catch {}
}

async function submitForReview(a) {
  try {
    await submitArticleForReviewService(a.articleId)
    ElMessage.success('已提交审核')
    fetchArticles()
  } catch {}
}

onMounted(() => {
  fetchCategories()
  fetchArticles()
})
</script>

<style scoped>
.my-articles {}

.page-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}
.page-heading {}
.page-title { font-size: 24px; font-weight: 800; color: var(--c-text); }
.page-subtitle { font-size: 14px; color: var(--c-text-3); margin-top: 4px; }
.page-actions { display: flex; gap: 8px; flex-shrink: 0; }

.filters-bar {
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.filters-inner { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.filters-stats { font-size: 13px; color: var(--c-text-3); white-space: nowrap; }

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--c-text-4);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  font-size: 15px;
}

.article-table { overflow: hidden; }

.article-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--c-border-light);
  transition: background var(--transition);
}
.article-row:last-child { border-bottom: none; }
.article-row:hover { background: var(--c-surface-2); }

.article-row-cover {
  width: 80px;
  height: 60px;
  border-radius: var(--radius);
  overflow: hidden;
  flex-shrink: 0;
}
.article-row-cover img { width: 100%; height: 100%; object-fit: cover; }

.article-row-body { flex: 1; min-width: 0; }

.article-row-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.article-row-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--c-text);
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 0;
  transition: color var(--transition);
}
.article-row-title:hover { color: var(--c-primary); }

.article-row-summary {
  font-size: 13px;
  color: var(--c-text-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 6px;
}

.article-row-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--c-text-4);
}

.article-row-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .article-row { flex-wrap: wrap; }
  .article-row-actions { width: 100%; justify-content: flex-end; }
}
</style>

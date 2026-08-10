<template>
  <div class="admin-articles page-container">
    <div class="page-top">
      <div class="page-heading">
        <h1 class="page-title">文章管理</h1>
        <p class="page-subtitle">审核、管理平台所有文章</p>
      </div>
    </div>

    <!-- Filter bar -->
    <div class="filters-bar card">
      <div class="filters-inner">
        <div class="status-tabs">
          <button
            v-for="tab in statusTabs"
            :key="tab.value"
            class="status-tab"
            :class="{ active: filters.status === tab.value }"
            @click="setStatus(tab.value)"
          >
            {{ tab.label }}
            <span v-if="tab.value === 'pending' && pendingCount > 0" class="pending-badge">{{ pendingCount }}</span>
          </button>
        </div>

        <el-input
          v-model="filters.keyword"
          placeholder="搜索文章标题"
          clearable
          style="width:220px"
          @input="debouncedFetch"
          @clear="fetchArticles"
        >
          <template #prefix>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
          </template>
        </el-input>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-spinner" style="height:200px">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
    </div>

    <!-- Empty -->
    <div v-else-if="articles.length === 0" class="empty-state">
      <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="var(--c-text-4)" stroke-width="1.2">
        <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
      </svg>
      <p>暂无文章</p>
    </div>

    <!-- Article list -->
    <div v-else class="card articles-table">
      <div v-for="article in articles" :key="article.articleId" class="article-row">
        <div class="article-cover" v-if="article.coverImage">
          <img :src="article.coverImage" :alt="article.title"/>
        </div>
        <div class="article-body">
          <div class="article-top">
            <a class="article-title-link" :href="`/article/${article.articleId}`" target="_blank">
              {{ article.title }}
            </a>
            <span class="badge" :class="`badge-${article.status}`">{{ statusLabel(article.status) }}</span>
          </div>
          <div class="article-meta">
            <span class="meta-item">
              <img :src="article.authorAvatar || defaultAvatar" class="avatar" style="width:16px;height:16px"/>
              {{ article.authorNickname || article.authorUsername }}
            </span>
            <span class="meta-item" v-if="article.categoryName">{{ article.categoryName }}</span>
            <span class="meta-item">{{ formatDate(article.createTime) }}</span>
          </div>
          <p v-if="article.summary" class="article-summary">{{ article.summary }}</p>
        </div>
        <div class="article-actions">
          <template v-if="article.status === 'pending'">
            <button class="btn btn-sm" style="background:#d1fae5;color:#059669;border:none" @click="approve(article)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
              通过
            </button>
            <button class="btn btn-sm" style="background:#fee2e2;color:#dc2626;border:none" @click="reject(article)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              驳回
            </button>
          </template>
          <button class="btn btn-ghost btn-sm" @click="dropArticle(article)" style="color:var(--c-danger)">下架</button>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div v-if="total > pageSize" class="pagination-wrap">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchArticles"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getAdminArticlesService, acceptArticleService, rejectArticleService, dropArticleService as dropService } from '../../api/admin.js'
import { getAdminStatsService } from '../../api/admin.js'

const articles = ref([])
const total = ref(0)
const pendingCount = ref(0)
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 10
const defaultAvatar = '/avatar/avatar1.png'

const filters = reactive({ status: 'pending', keyword: '' })

const statusTabs = [
  { label: '待审核', value: 'pending' },
  { label: '已发布', value: 'published' },
  { label: '全部', value: '' }
]

function setStatus(s) {
  filters.status = s
  currentPage.value = 1
  fetchArticles()
}

let fetchTimer = null
function debouncedFetch() {
  clearTimeout(fetchTimer)
  fetchTimer = setTimeout(() => { currentPage.value = 1; fetchArticles() }, 300)
}

async function fetchArticles() {
  loading.value = true
  try {
    const res = await getAdminArticlesService({
      status: filters.status || undefined,
      keyword: filters.keyword || undefined,
      page: currentPage.value,
      pageSize
    })
    articles.value = res.data.list || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}

async function loadPendingCount() {
  try {
    const res = await getAdminStatsService()
    pendingCount.value = res.data.pendingArticles || 0
  } catch {}
}

function statusLabel(s) {
  return { published: '已发布', pending: '审核中', draft: '草稿' }[s] || s
}

function formatDate(t) {
  if (!t) return ''
  return new Date(t).toLocaleDateString('zh-CN')
}

async function approve(a) {
  try {
    await acceptArticleService(a.articleId)
    ElMessage.success('已通过审核')
    pendingCount.value = Math.max(0, pendingCount.value - 1)
    fetchArticles()
  } catch {}
}

async function reject(a) {
  try {
    await ElMessageBox.confirm(`驳回《${a.title}》？文章将退回草稿状态。`, '驳回文章', {
      confirmButtonText: '驳回',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await rejectArticleService(a.articleId)
    ElMessage.success('已驳回')
    pendingCount.value = Math.max(0, pendingCount.value - 1)
    fetchArticles()
  } catch {}
}

async function dropArticle(a) {
  try {
    await ElMessageBox.confirm(`下架《${a.title}》吗？文章将从发现页隐藏。`, '下架文章', {
      confirmButtonText: '下架',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await dropService(a.articleId)
    ElMessage.success('已下架')
    fetchArticles()
  } catch {}
}

onMounted(() => {
  fetchArticles()
  loadPendingCount()
})
</script>

<style scoped>
.admin-articles {}

.page-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}
.page-title { font-size: 24px; font-weight: 800; color: var(--c-text); }
.page-subtitle { font-size: 14px; color: var(--c-text-3); margin-top: 4px; }

.filters-bar {
  padding: 12px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.filters-inner {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.status-tabs { display: flex; gap: 2px; }
.status-tab {
  padding: 6px 14px;
  border: none;
  background: none;
  border-radius: var(--radius);
  font-size: 14px;
  font-weight: 500;
  color: var(--c-text-3);
  cursor: pointer;
  transition: all var(--transition);
  display: flex;
  align-items: center;
  gap: 6px;
}
.status-tab:hover { background: var(--c-surface-2); color: var(--c-text); }
.status-tab.active { background: var(--c-primary-light); color: var(--c-primary); }

.pending-badge {
  background: var(--c-warning);
  color: white;
  font-size: 11px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: var(--radius-full);
  min-width: 18px;
  text-align: center;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--c-text-4);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.articles-table { overflow: hidden; }

.article-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--c-border-light);
  transition: background var(--transition);
}
.article-row:last-child { border-bottom: none; }
.article-row:hover { background: var(--c-surface-2); }

.article-cover {
  width: 80px;
  height: 60px;
  border-radius: var(--radius);
  overflow: hidden;
  flex-shrink: 0;
}
.article-cover img { width: 100%; height: 100%; object-fit: cover; }

.article-body { flex: 1; min-width: 0; }

.article-top {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.article-title-link {
  font-weight: 600;
  font-size: 15px;
  color: var(--c-text);
  text-decoration: none;
  flex: 1;
  min-width: 0;
}
.article-title-link:hover { color: var(--c-primary); }

.article-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--c-text-3);
}

.article-summary {
  font-size: 13px;
  color: var(--c-text-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.article-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>

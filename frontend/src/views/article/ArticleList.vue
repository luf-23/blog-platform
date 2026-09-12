<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, EditPen, Search, View, ChatDotRound, Delete, Folder, Plus } from '@element-plus/icons-vue'
import { deleteArticleService, getMyArticlesService, submitArticleForReviewService } from '../../api/article.js'
import { getCategoryListService } from '../../api/category.js'

const router = useRouter()
const route = useRoute()
const articles = ref([])
const categories = ref([])
const loading = ref(false)
const loadFailed = ref(false)
const sortOrder = ref('updated')
const filters = reactive({ title: '', status: '', categoryId: route.query.categoryId ? Number(route.query.categoryId) : null })
const failedCoverIds = ref(new Set())

const statusOptions = computed(() => [
  { label: '全部', value: '', count: articles.value.length },
  { label: '已发布', value: 'published', count: articles.value.filter(item => item.status === 'published').length },
  { label: '审核中', value: 'pending', count: articles.value.filter(item => item.status === 'pending').length },
  { label: '草稿', value: 'draft', count: articles.value.filter(item => item.status === 'draft').length }
])

const visibleArticles = computed(() => {
  const keyword = filters.title.trim().toLowerCase()
  return articles.value.filter(article => {
    const matchesTitle = !keyword || article.title?.toLowerCase().includes(keyword) || article.summary?.toLowerCase().includes(keyword)
    const matchesStatus = !filters.status || article.status === filters.status
    const matchesCategory = !filters.categoryId || article.categoryId === filters.categoryId
    return matchesTitle && matchesStatus && matchesCategory
  }).sort((a, b) => sortOrder.value === 'views'
    ? (Number(b.viewCount) || 0) - (Number(a.viewCount) || 0)
    : articleTimestamp(b) - articleTimestamp(a))
})

function articleTimestamp(article) {
  const value = sortOrder.value === 'created' ? article.createTime : article.updateTime || article.createTime
  return new Date(value || 0).getTime() || 0
}

function summaryText(article) {
  return (article.summary || '')
    .replace(/!?\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/^\s{0,3}(?:#{1,6}\s+|>\s?)/gm, '')
    .replace(/(\*\*|__|~~|`)(.+?)\1/g, '$2')
    .trim()
}

const hasActiveFilters = computed(() => Boolean(filters.title.trim() || filters.status || filters.categoryId))

async function fetchArticles() {
  loading.value = true
  loadFailed.value = false
  try {
    const result = await getMyArticlesService()
    articles.value = result.data || []
  } catch { loadFailed.value = true }
  finally { loading.value = false }
}

async function fetchCategories() {
  try {
    const result = await getCategoryListService()
    categories.value = result.data || []
  } catch {}
}

function selectStatus(status) { filters.status = status }
function clearFilters() { Object.assign(filters, { title: '', status: '', categoryId: null }) }
function categoryName(article) { return article.categoryName || categories.value.find(category => category.categoryId === article.categoryId)?.categoryName }
function statusLabel(status) { return { published: '已发布', pending: '审核中', draft: '草稿' }[status] || status }

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return `${date.getFullYear()}/${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`
}

function openArticle(article) {
  router.push(article.status === 'published' ? `/article/${article.articleId}` : `/article/edit/${article.articleId}`)
}
function editArticle(article) { router.push(`/article/edit/${article.articleId}`) }
function hasArticleCover(article) { return Boolean(article.coverImage && !failedCoverIds.value.has(article.articleId)) }
function markCoverFailed(articleId) { failedCoverIds.value = new Set([...failedCoverIds.value, articleId]) }
function coverInitial(article) { return article.title?.trim().slice(0, 1)?.toUpperCase() || '文' }

async function deleteArticle(article) {
  try {
    await ElMessageBox.confirm(`确定删除《${article.title}》吗？此操作不可恢复。`, '删除文章', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await deleteArticleService(article.articleId)
    articles.value = articles.value.filter(item => item.articleId !== article.articleId)
    ElMessage.success('文章已删除')
  } catch {}
}

async function submitForReview(article) {
  try {
    await submitArticleForReviewService(article.articleId)
    article.status = 'pending'
    ElMessage.success('已提交审核')
  } catch {}
}

onMounted(() => Promise.all([fetchArticles(), fetchCategories()]))
</script>

<template>
  <div class="content-page page-container">
    <header class="content-head">
      <div class="content-heading">
        <h1>内容管理</h1>
        <p>{{ loading ? '正在加载文章…' : loadFailed ? '管理你的文章与草稿' : `共 ${articles.length} 篇文章` }}</p>
      </div>
      <div class="head-actions">
        <router-link to="/article/categories" class="secondary-action"><el-icon><Folder /></el-icon>文章分组</router-link>
        <router-link to="/article/write" class="primary-action"><el-icon><Plus /></el-icon>新建文章</router-link>
      </div>
    </header>

    <section class="content-panel" aria-label="文章库" :aria-busy="loading">
      <nav class="status-tabs" aria-label="文章状态">
        <button v-for="item in statusOptions" :key="item.value" :class="{ active: filters.status === item.value }" :aria-pressed="filters.status === item.value" @click="selectStatus(item.value)">{{ item.label }}<span>{{ loading || loadFailed ? '?' : item.count }}</span></button>
      </nav>
      <div class="library-toolbar">
        <label class="search-field">
          <el-icon><Search /></el-icon>
          <input v-model="filters.title" aria-label="搜索标题或摘要" placeholder="搜索标题或摘要" />
          <button v-if="filters.title" aria-label="清空搜索" @click="filters.title = ''">×</button>
        </label>
        <div class="filter-row">
          <el-select v-model="filters.categoryId" placeholder="全部文章分组" aria-label="文章分组" clearable class="category-filter">
            <el-option v-for="category in categories" :key="category.categoryId" :label="category.categoryName" :value="category.categoryId" />
          </el-select>
          <el-select v-model="sortOrder" aria-label="文章排序" class="sort-filter">
            <el-option label="最近更新" value="updated" />
            <el-option label="最新创建" value="created" />
            <el-option label="最多阅读" value="views" />
          </el-select>
        </div>
      </div>
      <div v-if="hasActiveFilters && !loading && !loadFailed" class="filter-feedback" role="status"><span>找到 {{ visibleArticles.length }} 篇文章</span><button @click="clearFilters">清除筛选</button></div>

      <div v-if="loading" class="panel-state loading-state" role="status"><span class="content-spinner" aria-hidden="true"></span><span>正在加载文章…</span></div>
      <div v-else-if="loadFailed" class="panel-state" role="status"><el-icon class="state-icon"><Document /></el-icon><strong>文章暂时未能加载</strong><p>请稍后重试。</p><button @click="fetchArticles">重新加载</button></div>

      <table v-else-if="visibleArticles.length" class="article-table" aria-label="我的文章">
        <colgroup><col class="title-col" /><col class="status-col" /><col class="category-col" /><col class="metrics-col" /><col class="date-col" /><col class="actions-col" /></colgroup>
        <thead><tr><th scope="col">文章</th><th scope="col">状态</th><th scope="col">分组</th><th scope="col">数据</th><th scope="col">更新时间</th><th scope="col" class="actions-heading">操作</th></tr></thead>
        <tbody>
          <tr v-for="article in visibleArticles" :key="article.articleId" class="article-row">
            <td class="article-main">
              <div class="article-identity">
                <button class="article-cover" :class="{ placeholder: !hasArticleCover(article) }" :aria-label="`打开文章：${article.title || '无标题文章'}`" @click="openArticle(article)">
                  <img v-if="hasArticleCover(article)" :src="article.coverImage" alt="" loading="lazy" @error="markCoverFailed(article.articleId)" />
                  <span v-else aria-hidden="true">{{ coverInitial(article) }}</span>
                </button>
                <div class="article-copy">
                  <button class="article-title" :title="article.title" @click="openArticle(article)">{{ article.title || '无标题文章' }}</button>
                  <p class="article-excerpt">{{ summaryText(article) || '暂无摘要' }}</p>
                  <div v-if="article.tags?.length" class="article-tags">
                    <span v-for="tag in article.tags.slice(0, 3)" :key="tag.tagId">#{{ tag.tagName }}</span>
                    <span v-if="article.tags.length > 3" :title="article.tags.slice(3).map(tag => tag.tagName).join('、')">+{{ article.tags.length - 3 }}</span>
                  </div>
                </div>
              </div>
            </td>
            <td class="article-status"><span class="status-label" :class="`status-${article.status}`"><i aria-hidden="true"></i>{{ statusLabel(article.status) }}</span></td>
            <td class="article-category"><span :title="categoryName(article) || '未分组'">{{ categoryName(article) || '未分组' }}</span></td>
            <td class="article-data">
              <div class="article-metrics">
                <span :aria-label="`${article.viewCount || 0} 次阅读`" :title="`${article.viewCount || 0} 次阅读`"><el-icon><View /></el-icon>{{ article.viewCount || 0 }}</span>
                <span :aria-label="`${article.likeCount || 0} 次点赞`" :title="`${article.likeCount || 0} 次点赞`"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><path d="m12 21-1.5-1.3C5.1 15 2 12.2 2 8.8A4.8 4.8 0 0 1 6.9 4 5.3 5.3 0 0 1 12 7a5.3 5.3 0 0 1 5.1-3A4.8 4.8 0 0 1 22 8.8c0 3.4-3.1 6.2-8.5 10.9Z"/></svg>{{ article.likeCount || 0 }}</span>
                <span :aria-label="`${article.commentCount || 0} 条评论`" :title="`${article.commentCount || 0} 条评论`"><el-icon><ChatDotRound /></el-icon>{{ article.commentCount || 0 }}</span>
              </div>
            </td>
            <td class="article-date"><span class="mobile-date-label">更新于 </span>{{ formatDate(article.updateTime || article.createTime) }}</td>
            <td class="article-actions">
              <div class="row-actions">
                <button class="edit-action" :aria-label="`编辑文章：${article.title || '无标题文章'}`" @click="editArticle(article)">编辑</button>
                <button class="delete-action" :aria-label="`删除文章：${article.title || '无标题文章'}`" title="删除文章" @click="deleteArticle(article)"><el-icon><Delete /></el-icon></button>
                <button v-if="article.status === 'draft'" class="submit-action" @click="submitForReview(article)">提交审核</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-else-if="hasActiveFilters" class="panel-state" role="status">
        <el-icon class="state-icon"><Search /></el-icon><strong>没有匹配的文章</strong><p>试试更换关键词、状态或文章分组。</p><button @click="clearFilters">清除筛选</button>
      </div>
      <div v-else class="panel-state empty-library">
        <el-icon class="state-icon"><EditPen /></el-icon><strong>还没有文章</strong><p>新建一篇文章，或先保存为草稿。</p><router-link to="/article/write"><el-icon><Plus /></el-icon>新建文章</router-link>
      </div>
      <footer v-if="visibleArticles.length && !loading && !loadFailed" class="library-footer"><span>共 {{ visibleArticles.length }} 篇文章</span></footer>
    </section>
  </div>
</template>

<style scoped>
.content-page { width: min(1440px, calc(100% - 96px)); padding: 44px 0 40px; }
.content-head { display: flex; align-items: center; justify-content: space-between; gap: 24px; margin-bottom: 28px; }
.content-heading h1 { color: var(--c-text); font-size: 30px; line-height: 1.4; font-weight: 700; letter-spacing: -.035em; }
.content-heading p { margin-top: 8px; color: var(--c-text-3); font-size: 13px; }
.head-actions { display: flex; flex-shrink: 0; gap: 12px; }
.head-actions a { display: inline-flex; height: 40px; align-items: center; justify-content: center; gap: 8px; padding: 0 17px; border: 1px solid var(--c-border-strong); border-radius: 3px; background: transparent; color: var(--c-text-2); font-size: 13px; transition: border-color var(--transition), background var(--transition); }
.head-actions a:hover { border-color: var(--c-text-3); background: var(--c-surface-2); }
.head-actions .primary-action { border-color: var(--c-primary); background: var(--c-primary); color: var(--c-surface); }
.head-actions .primary-action:hover { border-color: var(--c-primary-hover); background: var(--c-primary-hover); }
.head-actions .el-icon { font-size: 16px; }
.status-tabs { display: flex; gap: 32px; border-bottom: 1px solid var(--c-border); }
.status-tabs > button { display: flex; align-items: center; gap: 8px; padding: 13px 4px 15px; margin-bottom: -1px; border: 0; border-bottom: 2px solid transparent; background: transparent; color: var(--c-text-2); font-size: 14px; white-space: nowrap; }
.status-tabs > button span { color: var(--c-text-3); font-size: 12px; font-variant-numeric: tabular-nums; }
.status-tabs > button:hover, .status-tabs > button.active, .status-tabs > button.active span { color: var(--c-primary); }
.status-tabs > button.active { border-bottom-color: var(--c-primary); }
.library-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 24px; padding: 22px 0; }
.search-field { display: flex; width: 360px; min-width: 0; height: 38px; align-items: center; gap: 10px; padding: 0 12px; border: 1px solid var(--c-border-strong); border-radius: 3px; color: var(--c-text-3); }
.search-field:focus-within { border-color: var(--c-primary); outline: 1px solid var(--c-primary); }
.search-field .el-icon { flex-shrink: 0; font-size: 16px; }
.search-field input { width: 100%; min-width: 0; border: 0; outline: 0; background: transparent; color: var(--c-text); font-size: 13px; }
.search-field input::placeholder { color: var(--c-text-3); }
.search-field button { flex-shrink: 0; padding: 0 3px; border: 0; background: transparent; color: var(--c-text-3); font-size: 20px; }
.filter-row { display: flex; gap: 12px; }
.category-filter { width: 168px; }
.sort-filter { width: 144px; }
.filter-row :deep(.el-select__wrapper) { min-height: 38px; border-radius: 3px; background: var(--c-surface); font-size: 13px; box-shadow: 0 0 0 1px var(--c-border-strong) inset; }
.filter-row :deep(.el-select__selected-item) { color: var(--c-text-2); }
.filter-row :deep(.el-select__wrapper.is-focused) { box-shadow: 0 0 0 1px var(--c-primary) inset; }
.filter-row :deep(.el-select__placeholder.is-transparent) { color: var(--c-text-3); }
.filter-feedback { display: flex; align-items: center; gap: 14px; margin: -8px 0 16px; color: var(--c-text-3); font-size: 12px; }
.filter-feedback button { padding: 0; border: 0; background: transparent; color: var(--c-primary); }
.article-table { width: 100%; border-collapse: collapse; table-layout: fixed; text-align: left; }
.status-col { width: 8%; }
.category-col { width: 10%; }
.metrics-col { width: 16%; }
.date-col { width: 12%; }
.actions-col { width: 100px; }
.article-table th { padding: 12px 16px; border-block: 1px solid var(--c-border); color: var(--c-text-3); font-size: 13px; font-weight: 400; }
.article-table td { height: 96px; padding: 14px 16px; border-bottom: 1px solid var(--c-border); color: var(--c-text-3); font-size: 13px; vertical-align: middle; }
.article-table th:first-child, .article-table td:first-child { padding-left: 12px; }
.article-table th:last-child, .article-table td:last-child { padding-right: 12px; }
.article-row { transition: background var(--transition); }
.article-row:hover { background: var(--c-surface-2); }
.article-identity { display: flex; align-items: center; gap: 20px; min-width: 0; }
.article-cover { display: grid; width: 88px; height: 60px; flex: 0 0 88px; place-items: center; overflow: hidden; padding: 0; border: 0; border-radius: 2px; background: var(--c-surface-3); }
.article-cover img { width: 100%; height: 100%; object-fit: cover; }
.article-cover.placeholder { color: var(--c-text-3); font-size: 24px; font-weight: 500; }
.article-copy { min-width: 0; }
.article-title { display: block; max-width: 100%; overflow: hidden; padding: 0; border: 0; background: transparent; color: var(--c-text); font-size: 16px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; text-align: left; }
.article-title:hover { color: var(--c-primary); }
.article-excerpt { overflow: hidden; margin-top: 4px; color: var(--c-text-3); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.article-tags { display: flex; gap: 10px; overflow: hidden; margin-top: 4px; color: var(--c-text-3); font-size: 11px; white-space: nowrap; }
.article-tags span { overflow: hidden; text-overflow: ellipsis; }
.status-label { display: inline-flex; align-items: center; gap: 7px; color: var(--c-text-2); white-space: nowrap; }
.status-label i { width: 6px; height: 6px; flex-shrink: 0; border-radius: 50%; background: var(--c-text-3); }
.status-published i { background: var(--c-success); }
.status-pending i { background: var(--c-warning); }
.article-category > span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.article-metrics { display: flex; align-items: center; flex-wrap: wrap; column-gap: 18px; row-gap: 3px; font-variant-numeric: tabular-nums; }
.article-metrics > span { display: inline-flex; align-items: center; gap: 5px; white-space: nowrap; }
.article-metrics .el-icon, .article-metrics svg { width: 15px; height: 15px; flex-shrink: 0; font-size: 15px; }
.article-date { white-space: nowrap; font-variant-numeric: tabular-nums; }
.mobile-date-label { display: none; }
.row-actions { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 4px; }
.row-actions button { display: inline-flex; min-height: 30px; align-items: center; justify-content: center; padding: 0; border: 0; background: transparent; color: var(--c-primary); font-size: 12px; white-space: nowrap; }
.row-actions button:hover { text-decoration: underline; }
.row-actions .delete-action { width: 28px; color: var(--c-text-3); font-size: 16px; }
.row-actions .delete-action:hover { color: var(--c-danger); }
.row-actions .submit-action { flex-basis: 100%; justify-content: flex-start; }
.library-footer { padding-top: 22px; color: var(--c-text-3); font-size: 12px; }
.panel-state { display: flex; min-height: 320px; align-items: center; justify-content: center; flex-direction: column; padding: 32px 20px; border-block: 1px solid var(--c-border); text-align: center; }
.state-icon { margin-bottom: 16px; color: var(--c-text-3); font-size: 28px; }
.panel-state strong { color: var(--c-text); font-size: 17px; font-weight: 600; }
.panel-state p { margin: 8px 0 18px; color: var(--c-text-3); font-size: 13px; }
.panel-state > button, .panel-state > a { display: inline-flex; align-items: center; gap: 7px; padding: 8px 15px; border: 1px solid var(--c-primary); border-radius: 3px; background: transparent; color: var(--c-primary); font-size: 13px; }
.loading-state { gap: 12px; color: var(--c-text-3); font-size: 13px; }
.content-spinner { width: 22px; height: 22px; border: 2px solid var(--c-border-strong); border-top-color: var(--c-primary); border-radius: 50%; animation: content-spin .7s linear infinite; }
.content-page button:focus-visible, .content-page a:focus-visible { outline: 2px solid var(--c-primary); outline-offset: 3px; }
@keyframes content-spin { to { transform: rotate(360deg); } }
@media (max-width: 1200px) {
  .status-col { width: 82px; }
  .category-col { width: 94px; }
  .metrics-col { width: 140px; }
  .date-col { width: 108px; }
  .actions-col { width: 86px; }
  .article-table th, .article-table td { padding-inline: 10px; }
  .article-identity { gap: 14px; }
  .article-cover { width: 72px; height: 52px; flex-basis: 72px; }
  .article-metrics { column-gap: 8px; }
}
@media (max-width: 900px) {
  .content-page { width: calc(100% - 48px); padding-top: 32px; }
  .library-toolbar { gap: 16px; }
  .search-field { flex: 1; }
  .category-filter { width: 144px; }
  .sort-filter { width: 124px; }
  .article-table, .article-table tbody { display: block; }
  .article-table colgroup, .article-table thead { display: none; }
  .article-table { border-top: 1px solid var(--c-border); }
  .article-row { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 10px 18px; padding: 20px 0; border-bottom: 1px solid var(--c-border); }
  .article-table td { display: block; min-width: 0; height: auto; padding: 0 !important; border: 0; }
  .article-main { grid-column: 1 / -1; }
  .article-table .article-status { grid-column: 1; }
  .article-table .article-category { grid-column: 2; }
  .article-table .article-data { grid-column: 1 / 3; grid-row: 4; }
  .article-table .article-date { grid-column: 1 / 3; grid-row: 3; }
  .article-table .article-actions { grid-column: 3; grid-row: 2 / 5; align-self: end; }
  .article-cover { width: 88px; height: 60px; flex-basis: 88px; }
  .article-metrics { column-gap: 16px; }
  .mobile-date-label { display: inline; }
  .row-actions { justify-content: flex-end; max-width: 120px; gap: 8px 18px; }
  .row-actions button { min-height: 34px; }
  .row-actions .submit-action { justify-content: flex-end; }
}
@media (max-width: 620px) {
  .content-page { width: calc(100% - 32px); padding-top: 24px; }
  .content-head { align-items: flex-start; flex-direction: column; gap: 20px; margin-bottom: 20px; }
  .content-heading h1 { font-size: 26px; }
  .head-actions { width: 100%; }
  .head-actions a { flex: 1; }
  .status-tabs { justify-content: space-between; gap: 10px; }
  .status-tabs > button { gap: 6px; padding-inline: 2px; font-size: 13px; }
  .library-toolbar { align-items: stretch; flex-direction: column; gap: 12px; padding: 18px 0; }
  .search-field { width: 100%; flex: auto; }
  .filter-row { gap: 12px; }
  .category-filter, .sort-filter { width: auto; flex: 1; min-width: 0; }
  .article-cover { width: 72px; height: 54px; flex-basis: 72px; }
  .article-title { font-size: 15px; }
  .article-tags { gap: 8px; }
}
@media (prefers-reduced-motion: reduce) {
  .content-page *, .content-page *::before, .content-page *::after { transition: none !important; }
  .content-spinner { animation: none; }
}
</style>

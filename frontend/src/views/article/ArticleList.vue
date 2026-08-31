<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteArticleService, getMyArticlesService, submitArticleForReviewService } from '../../api/article.js'
import { getCategoryListService } from '../../api/category.js'

const router = useRouter()
const route = useRoute()
const articles = ref([])
const categories = ref([])
const loading = ref(false)
const filters = reactive({ title: '', status: '', categoryId: route.query.categoryId ? Number(route.query.categoryId) : null })
const failedCoverIds = ref(new Set())

const statusOptions = computed(() => [
  { label: '全部', value: '', count: articles.value.length },
  { label: '已发布', value: 'published', count: articles.value.filter(item => item.status === 'published').length },
  { label: '审核中', value: 'pending', count: articles.value.filter(item => item.status === 'pending').length },
  { label: '草稿', value: 'draft', count: articles.value.filter(item => item.status === 'draft').length }
])

const overview = computed(() => [
  { label: '全部内容', value: articles.value.length, status: '', tone: 'all' },
  { label: '已发布', value: statusOptions.value[1].count, status: 'published', tone: 'published' },
  { label: '等待审核', value: statusOptions.value[2].count, status: 'pending', tone: 'pending' },
  { label: '草稿箱', value: statusOptions.value[3].count, status: 'draft', tone: 'draft' }
])

const visibleArticles = computed(() => {
  const keyword = filters.title.trim().toLowerCase()
  return articles.value.filter(article => {
    const matchesTitle = !keyword || article.title?.toLowerCase().includes(keyword) || article.summary?.toLowerCase().includes(keyword)
    const matchesStatus = !filters.status || article.status === filters.status
    const matchesCategory = !filters.categoryId || article.categoryId === filters.categoryId
    return matchesTitle && matchesStatus && matchesCategory
  })
})

const hasActiveFilters = computed(() => Boolean(filters.title.trim() || filters.status || filters.categoryId))

async function fetchArticles() {
  loading.value = true
  try {
    const result = await getMyArticlesService()
    articles.value = result.data || []
  } finally { loading.value = false }
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
  if (!value) return ''
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'short', day: 'numeric' }).format(new Date(value))
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
        <span>CREATOR STUDIO</span>
        <h1>内容管理</h1>
        <p>从草稿到发布，在一个清晰的工作流里管理你的技术文章。</p>
      </div>
      <div class="head-actions">
        <router-link to="/article/categories" class="secondary-action">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M20.6 13.4 13.4 20.6a2 2 0 0 1-2.8 0L2 12V2h10l8.6 8.6a2 2 0 0 1 0 2.8Z"/><path d="M7 7h.01"/></svg>
          文章分组
        </router-link>
        <router-link to="/article/write" class="primary-action">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z"/></svg>
          新建文章
        </router-link>
      </div>
    </header>

    <section class="overview-grid" aria-label="内容状态概览">
      <button v-for="item in overview" :key="item.tone" :class="[`tone-${item.tone}`, { active: filters.status === item.status }]" @click="selectStatus(item.status)">
        <span>{{ item.label }}</span><strong>{{ item.value }}</strong><i></i>
      </button>
    </section>

    <section class="content-panel">
      <header class="panel-head">
        <div><h2>文章库</h2><span>{{ visibleArticles.length }} 篇内容</span></div>
        <nav class="status-tabs" aria-label="文章状态">
          <button v-for="item in statusOptions" :key="item.value" :class="{ active: filters.status === item.value }" @click="selectStatus(item.value)">{{ item.label }}<span>{{ item.count }}</span></button>
        </nav>
      </header>

      <div class="filter-row">
        <label class="search-field">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></svg>
          <input v-model="filters.title" placeholder="搜索标题或摘要" />
          <button v-if="filters.title" aria-label="清空搜索" @click="filters.title = ''">×</button>
        </label>
        <el-select v-model="filters.categoryId" placeholder="全部文章分组" clearable class="category-filter">
          <el-option v-for="category in categories" :key="category.categoryId" :label="category.categoryName" :value="category.categoryId" />
        </el-select>
        <button v-if="hasActiveFilters" class="clear-filter" @click="clearFilters">清除筛选</button>
      </div>

      <div v-if="loading" class="panel-state loading-state">
        <span class="content-spinner" aria-hidden="true"></span>
        <span>正在整理你的内容…</span>
      </div>

      <div v-else-if="visibleArticles.length" class="article-list">
        <article v-for="article in visibleArticles" :key="article.articleId" class="article-row">
          <button class="article-cover" :class="{ placeholder: !hasArticleCover(article) }" @click="openArticle(article)">
            <img v-if="hasArticleCover(article)" :src="article.coverImage" :alt="`${article.title}封面`" @error="markCoverFailed(article.articleId)" />
            <template v-else><strong>{{ coverInitial(article) }}</strong><span>未设置封面</span></template>
          </button>

          <div class="article-body">
            <div class="article-title-line">
              <span class="status-badge" :class="`status-${article.status}`"><i></i>{{ statusLabel(article.status) }}</span>
              <button class="article-title" @click="openArticle(article)">{{ article.title }}</button>
            </div>
            <p>{{ article.summary || '还没有填写摘要，补充一句清晰的介绍会让文章更容易被读者理解。' }}</p>
            <div v-if="article.tags?.length" class="article-tags">
              <span v-for="tag in article.tags.slice(0, 4)" :key="tag.tagId"><small>{{ tag.parentName }}</small>#{{ tag.tagName }}</span>
              <em v-if="article.tags.length > 4">+{{ article.tags.length - 4 }}</em>
            </div>
            <footer>
              <span v-if="categoryName(article)">⌑ {{ categoryName(article) }}</span>
              <span>更新于 {{ formatDate(article.updateTime || article.createTime) }}</span>
              <span>◉ {{ article.viewCount || 0 }} 阅读</span>
              <span>♡ {{ article.likeCount || 0 }}</span>
              <span>◌ {{ article.commentCount || 0 }} 评论</span>
            </footer>
          </div>

          <div class="row-actions">
            <button v-if="article.status === 'draft'" class="submit-action" @click="submitForReview(article)">提交审核</button>
            <button @click="editArticle(article)">编辑</button>
            <button class="delete-action" aria-label="删除文章" @click="deleteArticle(article)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 7h16M9 7V4h6v3M6 7l1 14h10l1-14M10 11v6M14 11v6"/></svg>
            </button>
          </div>
        </article>
      </div>

      <div v-else-if="hasActiveFilters" class="panel-state empty-filtered">
        <div class="state-icon">⌕</div><strong>没有匹配的文章</strong><p>试试更换关键词、状态或文章分组。</p><button @click="clearFilters">查看全部内容</button>
      </div>

      <div v-else class="panel-state empty-library">
        <div class="empty-visual"><span>01</span><i></i><b>WRITE</b></div>
        <div><span>START YOUR FIRST DRAFT</span><strong>从一个真实的问题开始写</strong><p>记录背景、选择与结果。第一篇文章不必完美，只需要足够真实。</p><router-link to="/article/write">开始第一篇文章 →</router-link></div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.content-page { padding-top: 30px; padding-bottom: 52px; }.content-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 22px; }.content-heading > span { color: var(--c-primary); font-size: 10px; font-weight: 900; letter-spacing: .16em; }.content-heading h1 { margin-top: 3px; font-size: 29px; font-weight: 900; letter-spacing: -.035em; }.content-heading p { margin-top: 4px; color: var(--c-text-3); font-size: 13px; }.head-actions { display: flex; gap: 8px; }.head-actions a { display: inline-flex; height: 40px; align-items: center; gap: 7px; padding: 0 15px; border: 1px solid var(--c-border-strong); border-radius: var(--radius-sm); background: var(--c-surface); color: var(--c-text-2); font-size: 12px; font-weight: 800; }.head-actions svg { width: 16px; height: 16px; }.head-actions .primary-action { border-color: var(--c-primary); background: var(--c-primary); color: #fff; box-shadow: 0 8px 18px rgba(var(--c-primary-rgb), .16); }
.overview-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-bottom: 14px; }.overview-grid button { position: relative; display: flex; min-height: 86px; align-items: flex-start; justify-content: space-between; padding: 16px 17px; overflow: hidden; border: 1px solid var(--c-border); border-radius: var(--radius); background: var(--c-surface); color: var(--c-text-3); text-align: left; transition: border-color var(--transition), transform var(--transition); }.overview-grid button:hover { border-color: var(--c-border-strong); transform: translateY(-1px); }.overview-grid button.active { border-color: color-mix(in srgb, var(--c-primary) 48%, var(--c-border)); }.overview-grid button span { font-size: 11px; font-weight: 700; }.overview-grid button strong { color: var(--c-text); font-size: 25px; line-height: 1; }.overview-grid button i { position: absolute; right: 17px; bottom: 14px; width: 22px; height: 3px; border-radius: 3px; background: var(--c-border-strong); }.overview-grid .tone-published i { background: var(--c-success); }.overview-grid .tone-pending i { background: var(--c-warning); }.overview-grid .tone-draft i { background: var(--c-text-4); }.overview-grid .tone-all i { background: var(--c-primary); }
.content-panel { overflow: hidden; border: 1px solid var(--c-border); border-radius: var(--radius-lg); background: var(--c-surface); box-shadow: var(--shadow-xs); }.panel-head { display: flex; min-height: 66px; align-items: center; justify-content: space-between; gap: 18px; padding: 0 20px; border-bottom: 1px solid var(--c-border); }.panel-head > div { display: flex; align-items: baseline; gap: 9px; }.panel-head h2 { font-size: 16px; }.panel-head > div span { color: var(--c-text-4); font-size: 10px; }.status-tabs { display: flex; align-self: stretch; gap: 3px; }.status-tabs > button { position: relative; padding: 0 10px; border: 0; background: transparent; color: var(--c-text-3); font-size: 11px; }.status-tabs > button::after { position: absolute; right: 9px; bottom: -1px; left: 9px; height: 2px; border-radius: 2px 2px 0 0; background: var(--c-primary); content: ''; opacity: 0; }.status-tabs > button.active { color: var(--c-primary); font-weight: 800; }.status-tabs > button.active::after { opacity: 1; }.status-tabs span { margin-left: 4px; color: var(--c-text-4); font-size: 9px; }
.filter-row { display: flex; align-items: center; gap: 9px; padding: 12px 20px; border-bottom: 1px solid var(--c-border-light); background: var(--c-surface-2); }.search-field { display: flex; width: min(420px, 45%); height: 36px; align-items: center; gap: 8px; padding: 0 10px; border: 1px solid var(--c-border); border-radius: var(--radius-sm); background: var(--c-surface); }.search-field svg { width: 15px; height: 15px; color: var(--c-text-4); }.search-field input { min-width: 0; flex: 1; border: 0; outline: 0; background: transparent; color: var(--c-text); font-size: 11px; }.search-field button { border: 0; background: transparent; color: var(--c-text-4); font-size: 17px; }.category-filter { width: 180px; }.clear-filter { margin-left: auto; border: 0; background: transparent; color: var(--c-primary); font-size: 10px; font-weight: 700; }
.article-list { display: flex; flex-direction: column; }.article-row { display: grid; grid-template-columns: 148px minmax(0, 1fr) auto; align-items: center; gap: 18px; min-height: 136px; padding: 15px 20px; border-bottom: 1px solid var(--c-border-light); transition: background var(--transition); }.article-row:last-child { border: 0; }.article-row:hover { background: var(--c-surface-2); }.article-cover { position: relative; width: 148px; aspect-ratio: 16 / 9; padding: 0; overflow: hidden; border: 0; border-radius: 8px; background: var(--c-surface-3); }.article-cover img { width: 100%; height: 100%; object-fit: cover; transition: transform var(--transition); }.article-row:hover .article-cover img { transform: scale(1.035); }.article-cover.placeholder { display: grid; place-items: center; border: 1px solid var(--c-border); background: linear-gradient(135deg, var(--c-primary-soft), var(--c-surface-3)); color: var(--c-primary); }.article-cover.placeholder strong { font-size: 28px; }.article-cover.placeholder span { position: absolute; right: 8px; bottom: 6px; color: var(--c-text-4); font-size: 8px; }.article-body { min-width: 0; }.article-title-line { display: flex; align-items: center; gap: 8px; }.article-title { min-width: 0; padding: 0; overflow: hidden; border: 0; background: transparent; color: var(--c-text); font-size: 15px; font-weight: 800; text-align: left; text-overflow: ellipsis; white-space: nowrap; }.article-title:hover { color: var(--c-primary); }.status-badge { display: inline-flex; flex: 0 0 auto; align-items: center; gap: 5px; padding: 3px 7px; border-radius: 5px; background: var(--c-surface-3); color: var(--c-text-3); font-size: 9px; font-weight: 800; }.status-badge i { width: 5px; height: 5px; border-radius: 50%; background: currentColor; }.status-published { background: var(--c-success-soft); color: var(--c-success); }.status-pending { background: var(--c-warning-soft); color: var(--c-warning); }.article-body > p { margin: 7px 0; overflow: hidden; color: var(--c-text-3); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }.article-tags { display: flex; align-items: center; gap: 5px; margin-bottom: 8px; overflow: hidden; }.article-tags span { display: inline-flex; flex: 0 0 auto; gap: 3px; padding: 2px 5px; border: 1px solid var(--c-border-light); border-radius: 4px; background: var(--c-surface-2); color: var(--c-text-3); font-size: 8px; }.article-tags small { color: var(--c-text-4); font-size: 7px; }.article-tags em { color: var(--c-text-4); font-size: 8px; font-style: normal; }.article-body footer { display: flex; flex-wrap: wrap; gap: 12px; color: var(--c-text-4); font-size: 9px; }.row-actions { display: flex; align-items: center; gap: 6px; }.row-actions button { height: 32px; padding: 0 10px; border: 1px solid var(--c-border); border-radius: 6px; background: var(--c-surface); color: var(--c-text-2); font-size: 10px; font-weight: 700; }.row-actions button:hover { border-color: var(--c-primary); color: var(--c-primary); }.row-actions .submit-action { border-color: var(--c-primary); background: var(--c-primary); color: #fff; }.row-actions .delete-action { display: grid; width: 32px; padding: 0; place-items: center; color: var(--c-text-4); }.delete-action svg { width: 14px; height: 14px; }.row-actions .delete-action:hover { border-color: var(--c-danger); color: var(--c-danger); }
.panel-state { display: flex; min-height: 300px; align-items: center; justify-content: center; flex-direction: column; color: var(--c-text-4); }.loading-state { gap: 9px; font-size: 11px; }.content-spinner { display: block; width: 18px; height: 18px; flex: 0 0 18px; box-sizing: border-box; border: 2px solid var(--c-border-strong); border-top-color: var(--c-primary); border-radius: 50%; animation: content-spin .7s linear infinite; }.empty-filtered .state-icon { display: grid; width: 46px; height: 46px; place-items: center; border: 1px solid var(--c-border); border-radius: 50%; margin-bottom: 12px; font-size: 21px; }.empty-filtered strong { color: var(--c-text); font-size: 16px; }.empty-filtered p { margin: 4px 0 13px; font-size: 11px; }.empty-filtered button { padding: 7px 12px; border: 1px solid var(--c-primary); border-radius: 6px; background: transparent; color: var(--c-primary); font-size: 10px; font-weight: 700; }.empty-library { display: grid; min-height: 340px; grid-template-columns: 170px minmax(0, 360px); gap: 42px; }.empty-visual { position: relative; display: grid; width: 170px; height: 170px; place-items: center; border: 1px solid var(--c-border); background: var(--c-surface-2); }.empty-visual::before { position: absolute; inset: 9px; border: 1px solid var(--c-border-light); content: ''; }.empty-visual span { position: absolute; top: 17px; left: 18px; color: var(--c-text-4); font-size: 10px; }.empty-visual i { width: 50px; height: 1px; background: var(--c-primary); transform: rotate(-35deg); }.empty-visual b { position: absolute; right: 17px; bottom: 15px; color: var(--c-text-2); font-size: 11px; letter-spacing: .14em; }.empty-library > div:last-child > span { color: var(--c-primary); font-size: 9px; font-weight: 900; letter-spacing: .14em; }.empty-library strong { display: block; margin: 7px 0; color: var(--c-text); font-size: 20px; }.empty-library p { color: var(--c-text-3); font-size: 11px; line-height: 1.75; }.empty-library a { display: inline-flex; margin-top: 16px; color: var(--c-primary); font-size: 11px; font-weight: 800; }
@keyframes content-spin { to { transform: rotate(360deg); } }
@media (max-width: 900px) { .overview-grid { grid-template-columns: repeat(2, 1fr); }.panel-head { align-items: flex-start; flex-direction: column; padding-top: 15px; }.status-tabs { width: 100%; min-height: 44px; }.status-tabs button { flex: 1; }.article-row { grid-template-columns: 118px minmax(0, 1fr); }.article-cover { width: 118px; }.row-actions { grid-column: 1 / -1; justify-content: flex-end; } }
@media (max-width: 620px) { .content-page { width: calc(100% - 24px); padding-top: 20px; }.content-head { align-items: stretch; flex-direction: column; }.head-actions a { flex: 1; justify-content: center; }.overview-grid { gap: 7px; }.overview-grid button { min-height: 76px; }.filter-row { align-items: stretch; flex-direction: column; }.search-field, .category-filter { width: 100%; }.clear-filter { align-self: flex-end; margin-left: 0; }.article-row { grid-template-columns: 92px minmax(0, 1fr); gap: 12px; padding: 14px; }.article-cover { display: block; width: 92px; }.article-cover.placeholder { display: grid; }.article-cover.placeholder strong { font-size: 21px; }.article-body > p, .article-tags { display: none; }.article-title-line { align-items: flex-start; flex-direction: column; }.article-body footer span:nth-child(n+3) { display: none; }.row-actions { grid-column: 1 / -1; }.empty-library { grid-template-columns: 1fr; padding: 35px 24px; text-align: center; }.empty-visual { margin: 0 auto; } }
</style>

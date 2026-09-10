<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Collection, Document, CircleCheck, Clock, EditPen, Search, Grid, List, View, ChatDotRound, Delete, Folder, Plus, ArrowRight } from '@element-plus/icons-vue'
import { deleteArticleService, getMyArticlesService, submitArticleForReviewService } from '../../api/article.js'
import { getCategoryListService } from '../../api/category.js'

const router = useRouter()
const route = useRoute()
const articles = ref([])
const categories = ref([])
const loading = ref(false)
const loadFailed = ref(false)
const viewMode = ref('grid')
const sortOrder = ref('updated')
const filters = reactive({ title: '', status: '', categoryId: route.query.categoryId ? Number(route.query.categoryId) : null })
const failedCoverIds = ref(new Set())

const statusOptions = computed(() => [
  { label: '全部', value: '', count: articles.value.length },
  { label: '已发布', value: 'published', count: articles.value.filter(item => item.status === 'published').length },
  { label: '审核中', value: 'pending', count: articles.value.filter(item => item.status === 'pending').length },
  { label: '草稿', value: 'draft', count: articles.value.filter(item => item.status === 'draft').length }
])

const overview = computed(() => [
  { label: '全部内容', hint: '每一篇，都是积累', value: articles.value.length, status: '', tone: 'all', icon: Collection },
  { label: '已发布', hint: '与世界分享的想法', value: statusOptions.value[1].count, status: 'published', tone: 'published', icon: CircleCheck },
  { label: '等待审核', hint: '即将与读者见面', value: statusOptions.value[2].count, status: 'pending', tone: 'pending', icon: Clock },
  { label: '草稿箱', hint: '留给下一次灵感', value: statusOptions.value[3].count, status: 'draft', tone: 'draft', icon: EditPen }
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
        <span class="heading-eyebrow"><i></i> 创作工作台</span>
        <h1>内容管理<span>把想法，写成作品。</span></h1>
        <p>从第一行草稿，到每一次发布，在这里管理你的创作。</p>
      </div>
      <div class="head-actions">
        <router-link to="/article/categories" class="secondary-action"><el-icon><Folder /></el-icon>文章分组</router-link>
        <router-link to="/article/write" class="primary-action"><el-icon><Plus /></el-icon>新建文章</router-link>
      </div>
    </header>

    <section class="overview-grid" aria-label="内容状态概览">
      <button v-for="item in overview" :key="item.tone" :class="[`tone-${item.tone}`, { active: filters.status === item.status }]" :aria-pressed="filters.status === item.status" @click="selectStatus(item.status)">
        <span class="overview-icon"><el-icon><component :is="item.icon" /></el-icon></span>
        <span class="overview-copy"><span>{{ item.label }}</span><strong>{{ loading || loadFailed ? '—' : item.value }}<small>篇</small></strong><em>{{ item.hint }}</em></span>
        <el-icon class="overview-arrow"><ArrowRight /></el-icon>
      </button>
    </section>

    <section class="content-panel" :aria-busy="loading">
      <header class="panel-head">
        <div class="library-heading"><h2>文章库</h2><span>{{ loading || loadFailed ? '—' : visibleArticles.length }} 篇内容</span></div>
        <div class="view-switch" role="group" aria-label="文章展示方式">
          <button :class="{ active: viewMode === 'grid' }" :aria-pressed="viewMode === 'grid'" aria-label="卡片视图" title="卡片视图" @click="viewMode = 'grid'"><el-icon><Grid /></el-icon></button>
          <button :class="{ active: viewMode === 'list' }" :aria-pressed="viewMode === 'list'" aria-label="列表视图" title="列表视图" @click="viewMode = 'list'"><el-icon><List /></el-icon></button>
        </div>
      </header>
      <div class="library-toolbar">
        <nav class="status-tabs" aria-label="文章状态">
          <button v-for="item in statusOptions" :key="item.value" :class="{ active: filters.status === item.value }" :aria-pressed="filters.status === item.value" @click="selectStatus(item.value)">{{ item.label }}<span>{{ loading || loadFailed ? '—' : item.count }}</span></button>
        </nav>
        <div class="filter-row">
          <label class="search-field">
            <el-icon><Search /></el-icon>
            <input v-model="filters.title" aria-label="搜索文章" placeholder="搜索标题或摘要…" />
            <button v-if="filters.title" aria-label="清空搜索" @click="filters.title = ''">×</button>
          </label>
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
      <div v-if="hasActiveFilters" class="filter-feedback"><span>找到 {{ visibleArticles.length }} 篇文章</span><button @click="clearFilters">清除筛选</button></div>

      <div v-if="loading" class="panel-state loading-state"><span class="content-spinner" aria-hidden="true"></span><span>正在加载文章…</span></div>
      <div v-else-if="loadFailed" class="panel-state"><el-icon class="state-icon"><Document /></el-icon><strong>文章暂时未能加载</strong><p>请稍后重试，你的内容仍然保留。</p><button @click="fetchArticles">重新加载</button></div>

      <div v-else-if="visibleArticles.length" :class="['article-collection', `view-${viewMode}`]">
        <article v-for="article in visibleArticles" :key="article.articleId" class="article-item">
          <button class="article-cover" :class="{ placeholder: !hasArticleCover(article) }" :aria-label="`打开文章：${article.title}`" @click="openArticle(article)">
            <img v-if="hasArticleCover(article)" :src="article.coverImage" :alt="`${article.title}封面`" loading="lazy" @error="markCoverFailed(article.articleId)" />
            <template v-else><el-icon><Document /></el-icon><strong>{{ coverInitial(article) }}</strong><span>等待一张合适的封面</span></template>
            <span class="cover-open">{{ article.status === 'published' ? '阅读文章' : '继续创作' }}<el-icon><ArrowRight /></el-icon></span>
          </button>

          <div class="article-body">
            <div class="article-kicker">
              <span class="status-badge" :class="`status-${article.status}`"><i></i>{{ statusLabel(article.status) }}</span>
              <span class="article-category"><el-icon><Folder /></el-icon>{{ categoryName(article) || '未分组' }}</span>
            </div>
            <button class="article-title" :title="article.title" @click="openArticle(article)">{{ article.title || '无标题文章' }}</button>
            <p class="article-excerpt" :class="{ 'no-summary': !summaryText(article) }">{{ summaryText(article) || '还没有摘要，给这篇文章加一句介绍吧。' }}</p>
            <div v-if="article.tags?.length" class="article-tags">
              <span v-for="tag in article.tags.slice(0, 3)" :key="tag.tagId"># {{ tag.tagName }}</span>
              <span v-if="article.tags.length > 3">+{{ article.tags.length - 3 }}</span>
            </div>
            <div class="article-meta">
              <span class="updated-date">{{ formatDate(article.updateTime || article.createTime) }} 更新</span>
              <div class="article-metrics">
                <span :title="`${article.viewCount || 0} 次阅读`"><el-icon><View /></el-icon>{{ article.viewCount || 0 }}</span>
                <span :title="`${article.likeCount || 0} 次点赞`"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><path d="m12 21-1.5-1.3C5.1 15 2 12.2 2 8.8A4.8 4.8 0 0 1 6.9 4 5.3 5.3 0 0 1 12 7a5.3 5.3 0 0 1 5.1-3A4.8 4.8 0 0 1 22 8.8c0 3.4-3.1 6.2-8.5 10.9Z"/></svg>{{ article.likeCount || 0 }}</span>
                <span :title="`${article.commentCount || 0} 条评论`"><el-icon><ChatDotRound /></el-icon>{{ article.commentCount || 0 }}</span>
              </div>
            </div>
          </div>

          <div class="row-actions">
            <button class="edit-action" @click="editArticle(article)"><el-icon><EditPen /></el-icon>{{ article.status === 'draft' ? '继续创作' : '编辑文章' }}</button>
            <button v-if="article.status === 'draft'" class="submit-action" @click="submitForReview(article)">提交审核<el-icon><ArrowRight /></el-icon></button>
            <button class="delete-action" :aria-label="`删除文章：${article.title}`" title="删除文章" @click="deleteArticle(article)"><el-icon><Delete /></el-icon></button>
          </div>
        </article>
      </div>

      <div v-else-if="hasActiveFilters" class="panel-state">
        <el-icon class="state-icon"><Search /></el-icon><strong>没有匹配的文章</strong><p>试试更换关键词、状态或文章分组。</p><button @click="clearFilters">查看全部内容</button>
      </div>
      <div v-else class="panel-state empty-library">
        <el-icon class="state-icon"><EditPen /></el-icon><strong>你的下一篇作品，从这里开始</strong><p>记录一个想法，分享一份经验，让灵感慢慢成形。</p><router-link to="/article/write"><el-icon><Plus /></el-icon>写第一篇文章</router-link>
      </div>
      <footer v-if="visibleArticles.length && !loading && !loadFailed" class="library-footer"><span>共 {{ visibleArticles.length }} 篇文章</span><span>每一次记录，都有意义。</span></footer>
    </section>
  </div>
</template>

<style scoped>
.content-page { width: min(1800px, calc(100% - 64px)); padding: 38px 0 48px; }
.content-head { display: flex; align-items: center; justify-content: space-between; gap: 24px; margin-bottom: 30px; }
.heading-eyebrow { display: flex; align-items: center; gap: 7px; margin-bottom: 10px; color: var(--c-primary); font-size: 12px; font-weight: 700; letter-spacing: .1em; }
.heading-eyebrow i { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
.content-heading h1 { display: flex; align-items: center; gap: 20px; color: var(--c-text); font-size: 32px; font-weight: 800; letter-spacing: -.04em; }
.content-heading h1 span { padding-left: 20px; border-left: 1px solid var(--c-border-strong); color: var(--c-text-3); font-size: 15px; font-weight: 400; letter-spacing: .02em; }
.content-heading p { margin-top: 7px; color: var(--c-text-3); font-size: 13px; }
.head-actions { display: flex; flex-shrink: 0; gap: 10px; }
.head-actions a { display: inline-flex; height: 44px; align-items: center; gap: 8px; padding: 0 18px; border: 1px solid var(--c-border-strong); border-radius: 9px; background: var(--c-surface); color: var(--c-text-2); font-size: 13px; font-weight: 600; transition: background var(--transition), transform var(--transition); }
.head-actions a:hover { background: var(--c-surface-3); transform: translateY(-1px); }
.head-actions .primary-action { border-color: var(--c-primary); background: var(--c-primary); color: #fff; box-shadow: 0 5px 12px rgba(var(--c-primary-rgb), .15); }
.head-actions .primary-action:hover { background: var(--c-primary-hover); }
.head-actions .el-icon { font-size: 17px; }
.overview-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; margin-bottom: 34px; }
.overview-grid > button { position: relative; display: flex; min-height: 132px; align-items: flex-start; gap: 16px; padding: 22px; border: 1px solid var(--c-border); border-radius: 12px; background: var(--c-surface); text-align: left; transition: border-color var(--transition), box-shadow var(--transition); }
.overview-grid > button:hover { border-color: var(--c-primary); box-shadow: var(--shadow-sm); }
.overview-grid > button.active { border-color: var(--c-primary); background: var(--c-primary-soft); }
.overview-icon { display: grid; width: 40px; height: 40px; flex-shrink: 0; place-items: center; border: 1px solid var(--c-border-light); border-radius: 11px; background: var(--c-surface-2); color: var(--c-primary); font-size: 21px; }
.tone-published .overview-icon { background: var(--c-success-soft); color: var(--c-success); }
.tone-pending .overview-icon { background: var(--c-warning-soft); color: var(--c-warning); }
.tone-draft .overview-icon { color: var(--c-text-3); }
.overview-copy { display: flex; flex-direction: column; min-width: 0; }
.overview-copy > span { color: var(--c-text-2); font-size: 13px; font-weight: 600; }
.overview-copy strong { margin: 3px 0; color: var(--c-text); font-size: 32px; font-weight: 700; line-height: 1.25; font-variant-numeric: tabular-nums; }
.overview-copy small { margin-left: 8px; color: var(--c-text-3); font-size: 11px; font-weight: 400; }
.overview-copy em { color: var(--c-text-3); font-size: 11px; font-style: normal; }
.overview-arrow { position: absolute; top: 25px; right: 18px; color: var(--c-text-4); font-size: 13px; }
.overview-grid .tone-all.active { border-color: #2563eb; background: #2563eb; box-shadow: 0 7px 18px rgba(37, 99, 235, .14); }
.tone-all.active .overview-icon { border-color: #ffffff26; background: #ffffff1f; color: #fff; }
.tone-all.active .overview-copy > span, .tone-all.active .overview-copy strong { color: #fff; }
.tone-all.active .overview-copy em, .tone-all.active .overview-copy small, .tone-all.active .overview-arrow { color: #dbeafe; }
.panel-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 16px; }
.library-heading { display: flex; align-items: center; gap: 10px; }
.library-heading h2 { color: var(--c-text); font-size: 21px; font-weight: 750; }
.library-heading > span { padding: 3px 8px; border-radius: 6px; background: var(--c-surface-3); color: var(--c-text-3); font-size: 11px; }
.view-switch { display: flex; gap: 3px; padding: 3px; border: 1px solid var(--c-border); border-radius: 8px; background: var(--c-surface); }
.view-switch button { display: grid; width: 33px; height: 30px; place-items: center; border: 0; border-radius: 5px; background: transparent; color: var(--c-text-3); font-size: 17px; }
.view-switch button.active { background: var(--c-primary-soft); color: var(--c-primary); }
.library-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 18px; padding: 14px; margin-bottom: 22px; border: 1px solid var(--c-border); border-radius: 10px; background: var(--c-surface); }
.status-tabs { display: flex; flex-shrink: 0; gap: 4px; }
.status-tabs > button { display: flex; align-items: center; gap: 7px; padding: 8px 13px; border: 0; border-radius: 6px; background: transparent; color: var(--c-text-3); font-size: 13px; white-space: nowrap; }
.status-tabs > button.active { background: var(--c-primary-soft); color: var(--c-primary); font-weight: 700; }
.status-tabs span { font-size: 11px; font-variant-numeric: tabular-nums; opacity: .8; }
.filter-row { display: flex; min-width: 0; align-items: center; gap: 10px; }
.search-field { display: flex; width: clamp(180px, 18vw, 300px); height: 36px; align-items: center; gap: 8px; padding: 0 11px; border: 1px solid var(--c-border); border-radius: 6px; background: var(--c-surface-2); }
.search-field > .el-icon { flex-shrink: 0; color: var(--c-text-3); font-size: 16px; }
.search-field:focus-within { border-color: var(--c-primary); }
.search-field input { width: 100%; min-width: 0; border: 0; outline: 0; background: transparent; color: var(--c-text); font-size: 12px; }
.search-field button { border: 0; background: transparent; color: var(--c-text-3); font-size: 19px; }
.category-filter { width: 160px; }
.sort-filter { width: 128px; }
.filter-row :deep(.el-select__wrapper) { min-height: 36px; border-radius: 6px; font-size: 12px; }
.filter-feedback { display: flex; align-items: center; gap: 12px; margin: -7px 0 16px; color: var(--c-text-3); font-size: 12px; }
.filter-feedback button { padding: 0; border: 0; background: transparent; color: var(--c-primary); }
.article-collection { display: grid; gap: 22px; }
.view-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.article-item { display: flex; min-width: 0; overflow: hidden; flex-direction: column; border: 1px solid var(--c-border); border-radius: 12px; background: var(--c-surface); transition: border-color var(--transition), box-shadow var(--transition), transform var(--transition); }
.article-item:hover { border-color: var(--c-border-strong); box-shadow: var(--shadow-md); transform: translateY(-3px); }
.article-cover { position: relative; display: block; width: 100%; aspect-ratio: 2 / 1; flex-shrink: 0; padding: 0; overflow: hidden; border: 0; background: var(--c-surface-3); }
.article-cover img { width: 100%; height: 100%; object-fit: cover; transition: transform .3s ease; }
.article-cover:hover img { transform: scale(1.04); }
.cover-open { position: absolute; right: 12px; bottom: 12px; display: inline-flex; align-items: center; gap: 6px; padding: 6px 10px; border-radius: 6px; background: #0f172ace; color: #fff; font-size: 11px; opacity: 0; transform: translateY(4px); transition: opacity var(--transition), transform var(--transition); }
.article-cover:hover .cover-open, .article-cover:focus-visible .cover-open { opacity: 1; transform: translateY(0); }
.article-cover.placeholder { display: flex; align-items: center; justify-content: center; flex-direction: column; background-color: var(--c-primary-soft); background-image: radial-gradient(var(--c-border-strong) .8px, transparent .8px); background-size: 16px 16px; color: var(--c-primary); }
.article-cover.placeholder > .el-icon { position: absolute; left: 24px; top: 20px; font-size: 21px; opacity: .5; }
.article-cover.placeholder > strong { font-size: 48px; font-weight: 600; }
.article-cover.placeholder > span:not(.cover-open) { color: var(--c-text-3); font-size: 11px; }
.article-body { display: flex; min-width: 0; flex: 1; flex-direction: column; padding: 18px 20px 16px; font-family: inherit; }
.article-kicker { display: flex; min-width: 0; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 11px; }
.status-badge { display: inline-flex; flex-shrink: 0; align-items: center; gap: 5px; padding: 3px 7px; border-radius: 5px; background: var(--c-surface-3); color: var(--c-text-3); font-size: 11px; font-weight: 600; }
.status-badge i { width: 5px; height: 5px; border-radius: 50%; background: currentColor; }
.status-published { background: var(--c-success-soft); color: var(--c-success); }
.status-pending { background: var(--c-warning-soft); color: var(--c-warning); }
.article-category { display: flex; min-width: 0; align-items: center; gap: 5px; overflow: hidden; color: var(--c-text-3); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.article-category .el-icon { flex-shrink: 0; }
.article-title { display: -webkit-box; overflow: hidden; padding: 0; border: 0; background: transparent; color: var(--c-text); font-family: inherit; font-size: 18px; font-weight: 700; line-height: 1.5; text-align: left; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow-wrap: anywhere; }
.article-title:hover { color: var(--c-primary); }
.article-excerpt { display: -webkit-box; min-height: 42px; margin: 8px 0 12px; overflow: hidden; color: var(--c-text-3); font-size: 12px; line-height: 1.75; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow-wrap: anywhere; }
.article-excerpt.no-summary { opacity: .75; }
.article-tags { display: flex; flex-wrap: wrap; gap: 5px; margin-bottom: 15px; }
.article-tags span { max-width: 100%; padding: 2px 6px; overflow: hidden; border-radius: 4px; background: var(--c-surface-2); color: var(--c-text-3); font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.article-meta { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 8px; margin-top: auto; padding-top: 5px; color: var(--c-text-3); font-size: 11px; }
.article-metrics { display: flex; align-items: center; gap: 10px; }
.article-metrics > span { display: inline-flex; align-items: center; gap: 4px; }
.article-metrics .el-icon, .article-metrics svg { width: 14px; height: 14px; font-size: 14px; }
.row-actions { display: flex; align-items: center; gap: 10px; padding: 11px 20px; border-top: 1px solid var(--c-border-light); }
.row-actions button { display: inline-flex; min-height: 32px; align-items: center; justify-content: center; gap: 6px; padding: 0 8px; border: 0; border-radius: 5px; background: transparent; color: var(--c-text-3); font-size: 12px; font-weight: 600; }
.row-actions .edit-action { padding-left: 0; color: var(--c-primary); }
.row-actions button:hover { background: var(--c-primary-soft); color: var(--c-primary); }
.row-actions .delete-action { width: 32px; margin-left: auto; font-size: 15px; }
.row-actions .delete-action:hover { background: var(--c-danger-soft); color: var(--c-danger); }
.view-list { grid-template-columns: 1fr; gap: 12px; }
.view-list .article-item { display: grid; grid-template-columns: 200px minmax(0, 1fr) auto; align-items: center; padding: 16px; gap: 22px; }
.view-list .article-item:hover { transform: none; }
.view-list .article-cover { aspect-ratio: 16 / 10; border-radius: 8px; }
.view-list .article-body { padding: 0; }
.view-list .article-kicker { justify-content: flex-start; margin-bottom: 5px; }
.view-list .article-excerpt { min-height: 0; margin: 5px 0 8px; -webkit-line-clamp: 1; }
.view-list .article-tags { margin-bottom: 7px; }
.view-list .article-meta { justify-content: flex-start; gap: 18px; }
.view-list .row-actions { padding: 0 0 0 16px; border-top: 0; border-left: 1px solid var(--c-border); }
.library-footer { display: flex; justify-content: space-between; gap: 16px; margin-top: 26px; color: var(--c-text-3); font-size: 11px; }
.panel-state { display: flex; min-height: 340px; align-items: center; justify-content: center; flex-direction: column; padding: 32px 20px; border: 1px dashed var(--c-border-strong); border-radius: 12px; background: var(--c-surface); text-align: center; }
.state-icon { width: 60px; height: 60px; margin-bottom: 18px; border-radius: 16px; background: var(--c-primary-soft); color: var(--c-primary); font-size: 27px; }
.panel-state strong { color: var(--c-text); font-size: 19px; }
.panel-state p { margin: 8px 0 20px; color: var(--c-text-3); font-size: 13px; }
.panel-state > button, .panel-state > a { display: inline-flex; align-items: center; gap: 7px; padding: 9px 16px; border: 0; border-radius: 7px; background: var(--c-primary); color: #fff; font-size: 13px; }
.loading-state { gap: 12px; color: var(--c-text-3); font-size: 13px; }
.content-spinner { width: 24px; height: 24px; border: 2px solid var(--c-border-strong); border-top-color: var(--c-primary); border-radius: 50%; animation: content-spin .7s linear infinite; }
.content-page button:focus-visible, .content-page a:focus-visible { outline: 2px solid var(--c-primary); outline-offset: 3px; }
@keyframes content-spin { to { transform: rotate(360deg); } }
@media (max-width: 1550px) { .view-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 1180px) {
  .content-page { width: calc(100% - 40px); }
  .content-heading h1 span { display: none; }
  .overview-grid { gap: 12px; }
  .overview-grid > button { gap: 10px; padding: 18px 14px; }
  .overview-arrow { display: none; }
  .library-toolbar { align-items: stretch; flex-direction: column; gap: 12px; }
  .filter-row { padding-top: 12px; border-top: 1px solid var(--c-border-light); }
  .search-field { flex: 1; }
  .view-list .article-item { grid-template-columns: 160px minmax(0, 1fr); gap: 16px; }
  .view-list .row-actions { grid-column: 2; border: 0; padding: 0; }
}
@media (max-width: 900px) {
  .view-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
  .overview-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); margin-bottom: 26px; }
  .overview-grid > button { min-height: 118px; }
}
@media (max-width: 620px) {
  .content-page { width: calc(100% - 28px); padding-top: 24px; }
  .content-head { align-items: stretch; flex-direction: column; gap: 18px; margin-bottom: 22px; }
  .content-heading h1 { font-size: 28px; }
  .content-heading p { font-size: 12px; }
  .head-actions a { flex: 1; justify-content: center; }
  .overview-grid { gap: 10px; }
  .overview-grid > button { min-height: 110px; gap: 9px; padding: 14px 12px; }
  .overview-icon { width: 30px; height: 30px; border-radius: 8px; font-size: 17px; }
  .overview-copy strong { font-size: 27px; }
  .overview-copy em { font-size: 10px; }
  .library-toolbar { padding: 10px; }
  .status-tabs { justify-content: space-between; gap: 0; }
  .status-tabs > button { gap: 5px; padding: 8px 9px; font-size: 12px; }
  .filter-row { flex-wrap: wrap; gap: 8px; }
  .search-field { width: 100%; flex-basis: 100%; }
  .category-filter { width: auto; flex: 1; }
  .sort-filter { width: 120px; }
  .view-grid { grid-template-columns: 1fr; }
  .article-body { padding: 16px; }
  .row-actions { padding-inline: 16px; }
  .view-list .article-item { grid-template-columns: 88px minmax(0, 1fr); padding: 12px; gap: 12px; }
  .view-list .article-cover { aspect-ratio: 1; }
  .view-list .article-title { font-size: 16px; }
  .view-list .article-category, .view-list .article-excerpt, .view-list .article-tags, .view-list .article-cover.placeholder > span:not(.cover-open) { display: none; }
  .view-list .article-meta { gap: 4px; margin-top: 8px; }
  .view-list .row-actions { grid-column: 1 / -1; border-top: 1px solid var(--c-border-light); padding-top: 8px; }
  .library-footer { font-size: 10px; }
}
@media (prefers-reduced-motion: reduce) {
  .content-page *, .content-page *::before, .content-page *::after { transition: none !important; }
  .content-spinner { animation-duration: 1.5s; }
}
</style>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchArticlesService } from '../api/article.js'
import { getPopularTagsService } from '../api/tag.js'
import ArticleCard from '../components/article/ArticleCard.vue'

const route = useRoute()
const router = useRouter()
const articles = ref([])
const popularTags = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const currentPage = ref(1)
const total = ref(0)
const PAGE_SIZE = 8
let searchTimer = null

const filters = reactive({
  keyword: '',
  tagId: null,
  tagName: '',
  authorId: null,
  authorName: '',
  sort: 'hot'
})
const sortOptions = [
  { label: '阅读最多', value: 'hot' },
  { label: '最新', value: 'latest' },
  { label: '获赞最多', value: 'liked' }
]

const hasMore = computed(() => articles.value.length < total.value)
const featured = computed(() => [...articles.value].sort((a, b) => (b.viewCount || 0) - (a.viewCount || 0)).slice(0, 3))
const totals = computed(() => ({
  views: articles.value.reduce((sum, item) => sum + (item.viewCount || 0), 0),
  likes: articles.value.reduce((sum, item) => sum + (item.likeCount || 0), 0),
  comments: articles.value.reduce((sum, item) => sum + (item.commentCount || 0), 0)
}))

async function fetchArticles(append = false) {
  append ? loadingMore.value = true : loading.value = true
  try {
    const result = await searchArticlesService({
      keyword: filters.keyword || undefined,
      tagId: filters.tagId || undefined,
      authorId: filters.authorId || undefined,
      sort: filters.sort,
      page: currentPage.value,
      pageSize: PAGE_SIZE
    })
    const data = result.data || { list: [], total: 0 }
    articles.value = append ? articles.value.concat(data.list || []) : (data.list || [])
    total.value = data.total || 0
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function refresh() {
  currentPage.value = 1
  fetchArticles()
}

function debouncedSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    router.replace({ path: '/home', query: filters.keyword ? { keyword: filters.keyword } : {} })
    refresh()
  }, 350)
}

function setSort(value) {
  filters.sort = value
  refresh()
}

function toggleTag(tag) {
  if (filters.tagId === tag.tagId) {
    filters.tagId = null
    filters.tagName = ''
  } else {
    filters.tagId = tag.tagId
    filters.tagName = tag.tagName
  }
  refresh()
}

function onAuthorClick(author) {
  filters.authorId = author.userId
  filters.authorName = author.username
  refresh()
}

function clearAll() {
  Object.assign(filters, { keyword: '', tagId: null, tagName: '', authorId: null, authorName: '', sort: 'hot' })
  router.replace('/home')
  refresh()
}

function loadMore() {
  currentPage.value += 1
  fetchArticles(true)
}

function formatCount(value) {
  const number = Number(value || 0)
  if (number >= 10000) return (number / 10000).toFixed(1) + '万'
  if (number >= 1000) return (number / 1000).toFixed(1) + 'k'
  return String(number)
}

watch(() => route.query.keyword, keyword => {
  const next = typeof keyword === 'string' ? keyword : ''
  if (next !== filters.keyword) {
    filters.keyword = next
    refresh()
  }
})

onMounted(async () => {
  filters.keyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  const [, tagResult] = await Promise.all([fetchArticles(), getPopularTagsService(18)])
  popularTags.value = tagResult.data || []
})
</script>

<template>
  <div class="discovery-page workspace-page">
      <div class="page-container discovery-layout workspace-frame">
        <main class="feed-column workspace-scroll">
          <section class="discovery-heading">
            <div>
              <span>公开文章</span>
              <h1>文章列表</h1>
            </div>
            <p>按时间、热度和标签浏览已发布文章。</p>
          </section>

          <section class="discovery-tools">
            <div class="search-box">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></svg>
              <input id="discovery-search" v-model="filters.keyword" name="discovery-search" placeholder="搜索文章、作者或标签" @input="debouncedSearch" @keyup.enter="refresh" />
              <button v-if="filters.keyword" @click="filters.keyword = ''; clearAll()">×</button>
            </div>
            <div class="discovery-filters">
              <div class="feed-tabs">
                <button v-for="option in sortOptions" :key="option.value" :class="{ active: filters.sort === option.value }" @click="setSort(option.value)">{{ option.label }}</button>
              </div>
              <div class="quick-tags">
                <button v-for="tag in popularTags.slice(0, 5)" :key="tag.tagId" :class="{ active: filters.tagId === tag.tagId }" @click="toggleTag(tag)">{{ tag.tagName }}</button>
              </div>
            </div>
            <div v-if="filters.tagName || filters.authorName" class="active-filters">
              <span v-if="filters.tagName"># {{ filters.tagName }} <button @click="filters.tagId = null; filters.tagName = ''; refresh()">×</button></span>
              <span v-if="filters.authorName">@{{ filters.authorName }} <button @click="filters.authorId = null; filters.authorName = ''; refresh()">×</button></span>
              <button @click="clearAll">清除全部</button>
            </div>
          </section>

          <div v-if="loading" class="article-skeletons">
            <div v-for="i in 3" :key="i" class="article-skeleton surface-card"><i></i><div><b></b><span></span><span></span><em></em></div></div>
          </div>
          <div v-else-if="articles.length" class="article-feed">
            <ArticleCard v-for="article in articles" :key="article.articleId" :article="article" @tag-click="toggleTag" @author-click="onAuthorClick" />
          </div>
          <div v-else class="empty-state"><strong>没有找到相关文章</strong><p>尝试更换关键词或清除筛选条件。</p><button class="btn btn-secondary btn-sm" @click="clearAll">清除筛选</button></div>
          <div v-if="hasMore" class="load-more"><button class="btn btn-secondary" :disabled="loadingMore" @click="loadMore">{{ loadingMore ? '正在加载…' : '加载更多内容' }}</button></div>
        </main>

        <aside class="discovery-aside workspace-scroll">
          <section class="aside-block curated-block">
            <header><h2>本页阅读最多</h2></header>
            <router-link v-for="(item, index) in featured" :key="item.articleId" :to="'/article/' + item.articleId" class="featured-item">
              <span>{{ String(index + 1).padStart(2, '0') }}</span>
              <div><strong>{{ item.title }}</strong><small>{{ item.authorNickname || item.authorUsername }} · {{ formatCount(item.viewCount) }} 阅读</small></div>
            </router-link>
          </section>

          <section class="aside-block">
            <header><h2>热门标签</h2></header>
            <div class="tag-grid">
              <button v-for="tag in popularTags.slice(0, 12)" :key="tag.tagId" :class="{ active: filters.tagId === tag.tagId }" @click="toggleTag(tag)">
                <span>{{ tag.tagName }}</span><em>{{ tag.articleCount }}</em>
              </button>
            </div>
          </section>

          <section class="aside-block creator-data">
            <header><h2>内容数据</h2><span>当前结果</span></header>
            <div>
              <p><i class="view">◉</i><span>阅读量<strong>{{ formatCount(totals.views) }}</strong></span></p>
              <p><i class="like">♡</i><span>获赞数<strong>{{ formatCount(totals.likes) }}</strong></span></p>
              <p><i class="comment">▢</i><span>评论数<strong>{{ formatCount(totals.comments) }}</strong></span></p>
            </div>
            <router-link to="/article/write" class="btn btn-primary">写文章</router-link>
          </section>
        </aside>
      </div>
  </div>
</template>

<style scoped>
.discovery-page { background: var(--c-bg); }
.discovery-layout { display: grid; grid-template-columns: minmax(0, 1fr) 350px; gap: 46px; align-items: stretch; }
.feed-column, .discovery-aside { padding: 34px 4px 56px; }
.feed-column { min-width: 0; padding-right: 0; }

.discovery-tools { margin-bottom: 0; padding: 22px 0 16px; border-bottom: 2px solid var(--c-text); background: transparent; }
.search-box { display: flex; align-items: center; gap: 12px; padding: 0 0 15px; border-bottom: 1px solid var(--c-border-strong); }
.search-box svg { width: 20px; height: 20px; color: var(--c-text); }
.search-box input { min-width: 0; flex: 1; border: 0; outline: 0; background: transparent; color: var(--c-text); font-family: inherit; font-size: 16px; }
.search-box input::placeholder { color: var(--c-text-4); }
.search-box > button { border: 0; background: transparent; color: var(--c-text-4); font-size: 20px; }
.discovery-filters { display: flex; align-items: flex-end; justify-content: space-between; gap: 15px; }
.feed-tabs { display: flex; gap: 22px; }
.feed-tabs button { position: relative; padding: 14px 0 0; border: 0; background: transparent; color: var(--c-text-3); font-weight: 800; letter-spacing: .04em; }
.feed-tabs button::before { margin-right: 7px; color: var(--c-text-4); content: '·'; }
.feed-tabs button.active { color: var(--c-text); }
.feed-tabs button.active::before { color: var(--c-primary); content: '●'; font-size: 8px; }
.quick-tags { display: flex; gap: 5px; padding-top: 12px; overflow: auto; }
.quick-tags button { flex: 0 0 auto; padding: 4px 9px; border: 0; border-bottom: 1px solid transparent; background: transparent; color: var(--c-text-3); font-size: 11px; }
.quick-tags button:hover, .quick-tags button.active { border-color: var(--c-text); color: var(--c-text); }
.active-filters { display: flex; align-items: center; gap: 8px; padding-top: 12px; }
.active-filters span { display: inline-flex; align-items: center; gap: 5px; padding: 3px 7px; background: var(--c-primary-soft); color: var(--c-primary); font-size: 11px; font-weight: 700; }
.active-filters button { border: 0; background: transparent; color: inherit; }
.active-filters > button { margin-left: auto; color: var(--c-text-3); font-size: 11px; }

.article-feed { display: flex; flex-direction: column; }
.load-more { display: flex; justify-content: center; padding: 28px; border-top: 1px solid var(--c-border); }
.empty-state { padding: 50px 0; border-bottom: 1px solid var(--c-text); }

.discovery-aside { display: flex; flex-direction: column; gap: 36px; padding-left: 0; }
.aside-block { padding: 17px 0 0; border-top: 3px solid var(--c-text); }
.aside-block header { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 14px; }
.aside-block h2 { font-family: inherit; font-size: 19px; font-weight: 900; }
.aside-block header button { border: 0; background: transparent; color: var(--c-primary); font-size: 11px; font-weight: 700; }
.aside-block header > span { color: var(--c-text-4); font-size: 11px; }
.featured-item { display: grid; grid-template-columns: 34px 1fr; gap: 9px; padding: 13px 0; border-bottom: 1px solid var(--c-border); }
.featured-item:last-child { border-bottom-color: var(--c-text); }
.featured-item > span { color: var(--c-primary); font-family: Georgia, serif; font-size: 18px; font-weight: 700; font-style: italic; }
.featured-item div { display: flex; min-width: 0; flex-direction: column; gap: 3px; }
.featured-item strong { overflow: hidden; color: var(--c-text); font-family: inherit; font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
.featured-item small { color: var(--c-text-4); font-size: 10px; }
.tag-grid { display: flex; flex-wrap: wrap; gap: 7px 14px; }
.tag-grid button { display: inline-flex; align-items: baseline; gap: 6px; padding: 2px 0; border: 0; border-bottom: 1px solid var(--c-border-strong); background: transparent; color: var(--c-text-2); font-size: 11px; }
.tag-grid button:hover, .tag-grid button.active { border-color: var(--c-primary); color: var(--c-primary); }
.tag-grid em { color: var(--c-text-4); font-family: Georgia, serif; font-size: 10px; font-style: italic; }
.creator-data > div { display: grid; grid-template-columns: repeat(3, 1fr); margin: 6px 0 18px; border-block: 1px solid var(--c-border); }
.creator-data p { display: flex; flex-direction: column; gap: 3px; padding: 12px 6px; border-right: 1px solid var(--c-border); text-align: center; }
.creator-data p:last-child { border: 0; }
.creator-data i { font-size: 16px; font-style: normal; }
.creator-data .view { color: var(--c-community); }.creator-data .like { color: var(--c-danger); }.creator-data .comment { color: var(--c-primary); }
.creator-data p span { display: flex; flex-direction: column; color: var(--c-text-4); font-size: 9px; }
.creator-data strong { color: var(--c-text); font-family: Georgia, serif; font-size: 15px; }
.creator-data .btn { width: 100%; border-radius: 2px; background: var(--c-text); }

.article-skeletons { display: flex; flex-direction: column; }
.article-skeleton { display: grid; min-height: 198px; grid-template-columns: 236px 1fr; overflow: hidden; border-bottom: 1px solid var(--c-border); border-radius: 0; }
.article-skeleton > i { background: var(--c-surface-3); }
.article-skeleton > div { display: flex; flex-direction: column; gap: 12px; padding: 24px; }
.article-skeleton b, .article-skeleton span, .article-skeleton em { height: 14px; background: var(--c-surface-3); }
.article-skeleton b { width: 40%; height: 20px; }.article-skeleton span:nth-child(2) { width: 90%; }.article-skeleton span:nth-child(3) { width: 70%; }.article-skeleton em { width: 55%; margin-top: auto; }

@media (max-width: 1180px) { .discovery-layout { grid-template-columns: minmax(0, 1fr) 310px; gap: 30px; } }
@media (max-width: 900px) { .discovery-page { overflow-y: auto; }.discovery-layout { display: block; height: auto; }.feed-column, .discovery-aside { overflow: visible; }.discovery-aside { display: grid; grid-template-columns: repeat(2, 1fr); padding-top: 0; }.creator-data { grid-column: 1 / -1; } }
@media (max-width: 760px) { .discovery-filters { align-items: stretch; flex-direction: column; }.quick-tags { padding-bottom: 4px; }.article-skeleton { grid-template-columns: 1fr; }.article-skeleton > i { height: 180px; }.discovery-aside { grid-template-columns: 1fr; }.creator-data { grid-column: auto; } }

/* Developer discovery workspace */
.discovery-page { background: var(--c-bg); }
.discovery-layout { grid-template-columns: minmax(0, 1fr) 360px; gap: 24px; }
.feed-column, .discovery-aside { padding: 24px 4px 48px; }
.feed-column { padding-right: 4px; }
.discovery-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 14px; padding: 4px 2px 16px; border-bottom: 1px solid var(--c-border-strong); }
.discovery-heading span { display: block; margin-bottom: 5px; color: var(--c-primary); font-family: ui-monospace, "SFMono-Regular", Consolas, monospace; font-size: 10px; font-weight: 800; letter-spacing: .08em; }
.discovery-heading h1 { color: var(--c-text); font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif; font-size: 26px; font-weight: 800; letter-spacing: -.025em; line-height: 1.25; }
.discovery-heading p { color: var(--c-text-3); font-size: 12px; }
.discovery-tools { position: static; margin-bottom: 12px; padding: 16px 18px 0; border: 1px solid var(--c-border); border-radius: var(--radius-lg); background: var(--c-surface); box-shadow: var(--shadow-xs); }
.search-box { padding-bottom: 13px; border-bottom: 1px solid var(--c-border); }
.search-box svg { color: var(--c-text-4); }
.search-box input { font-family: inherit; font-size: 15px; }
.discovery-filters { border: 0; }
.feed-tabs { gap: 6px; }
.feed-tabs button { padding: 11px 11px 12px; color: var(--c-text-3); letter-spacing: 0; }
.feed-tabs button::before { display: none; }
.feed-tabs button::after { position: absolute; right: 10px; bottom: -1px; left: 10px; height: 3px; border-radius: 3px 3px 0 0; background: var(--c-primary); content: ''; opacity: 0; }
.feed-tabs button.active { color: var(--c-primary); }
.feed-tabs button.active::after { opacity: 1; }
.quick-tags { gap: 6px; padding-top: 0; padding-bottom: 8px; }
.quick-tags button { padding: 4px 9px; border: 1px solid var(--c-border); border-radius: 5px; background: var(--c-surface-2); font-family: ui-monospace, "SFMono-Regular", Consolas, monospace; }
.quick-tags button:hover, .quick-tags button.active { border-color: #93c5fd; background: var(--c-primary-soft); color: var(--c-primary); }
.article-feed { gap: 10px; }
.load-more { border: 0; }
.empty-state { padding: 42px 24px; border: 1px solid var(--c-border); border-radius: var(--radius-lg); background: var(--c-surface); }
.discovery-aside { gap: 14px; }
.aside-block { padding: 18px; border: 1px solid var(--c-border); border-radius: var(--radius-lg); background: var(--c-surface); box-shadow: var(--shadow-xs); }
.aside-block header { margin-bottom: 12px; }
.aside-block h2 { font-family: Inter, "PingFang SC", sans-serif; font-size: 16px; font-weight: 800; }
.aside-block h2::before { display: none; }
.featured-item { grid-template-columns: 28px 1fr; padding: 11px 0; }
.featured-item:last-child { border-bottom: 0; }
.featured-item > span { color: var(--c-primary); font-family: ui-monospace, Consolas, monospace; font-size: 13px; font-style: normal; }
.featured-item strong { font-family: inherit; font-size: 12px; font-weight: 700; }
.tag-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 7px; }
.tag-grid button { justify-content: space-between; padding: 7px 8px; border: 1px solid var(--c-border); border-radius: 5px; background: var(--c-surface-2); font-family: ui-monospace, Consolas, monospace; }
.tag-grid button:hover, .tag-grid button.active { border-color: #93c5fd; background: var(--c-primary-soft); }
.tag-grid em { font-family: inherit; font-style: normal; }
.creator-data > div { margin: 4px 0 14px; border: 1px solid var(--c-border); border-radius: 6px; }
.creator-data .btn { border-radius: var(--radius-sm); background: var(--c-primary); }

@media (max-width: 1180px) { .discovery-layout { grid-template-columns: minmax(0, 1fr) 320px; gap: 18px; } }
@media (max-width: 900px) { .discovery-layout { display: block; }.discovery-aside { display: grid; grid-template-columns: repeat(2, 1fr); }.creator-data { grid-column: 1 / -1; } }
@media (max-width: 760px) { .discovery-heading { align-items: flex-start; flex-direction: column; gap: 5px; }.discovery-heading h1 { font-size: 23px; }.discovery-filters { flex-direction: column; }.discovery-aside { grid-template-columns: 1fr; }.creator-data { grid-column: auto; } }
</style>

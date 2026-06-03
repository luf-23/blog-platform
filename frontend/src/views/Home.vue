<template>
  <div class="home">
    <div class="page-container">
      <div class="home-layout">
        <!-- Main feed -->
        <div class="feed-col">
          <!-- Search bar -->
          <div class="search-bar card">
            <div class="search-input-wrap">
              <svg class="search-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>
              </svg>
              <input
                v-model="filters.keyword"
                class="search-input"
                placeholder="搜索文章标题或摘要..."
                @keyup.enter="doSearch"
                @input="debouncedSearch"
              />
              <button v-if="filters.keyword" class="search-clear" @click="clearKeyword">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>

            <!-- Filter row -->
            <div class="filter-row">
              <div class="filter-group">
                <span class="filter-label">排序</span>
                <div class="sort-tabs">
                  <button
                    v-for="s in sortOptions"
                    :key="s.value"
                    class="sort-tab"
                    :class="{ active: filters.sort === s.value }"
                    @click="setSort(s.value)"
                  >{{ s.label }}</button>
                </div>
              </div>
              <button v-if="hasActiveFilters" class="btn btn-ghost btn-sm clear-filters" @click="clearFilters">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
                清除筛选
              </button>
            </div>

            <!-- Active filter chips -->
            <div v-if="activeChips.length > 0" class="active-chips">
              <div v-for="chip in activeChips" :key="chip.key" class="chip">
                <span>{{ chip.label }}</span>
                <button @click="removeChip(chip.key)">×</button>
              </div>
            </div>
          </div>

          <!-- Tag cloud (inline) -->
          <div class="tag-cloud">
            <button
              v-for="tag in popularTags"
              :key="tag.tagId"
              class="tag-pill"
              :class="{ active: filters.tagId === tag.tagId }"
              @click="toggleTag(tag)"
            >
              # {{ tag.tagName }}
              <span class="tag-count">{{ tag.articleCount }}</span>
            </button>
          </div>

          <!-- Feed -->
          <div v-if="loading && articles.length === 0" class="loading-spinner">
            <el-icon class="is-loading" :size="24"><Loading /></el-icon>
            <span style="margin-left:8px">加载中...</span>
          </div>

          <div v-else-if="articles.length === 0 && !loading" class="empty-feed">
            <svg width="60" height="60" viewBox="0 0 24 24" fill="none" stroke="var(--c-text-4)" stroke-width="1.5">
              <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/>
            </svg>
            <p>没有找到相关文章</p>
            <button class="btn btn-ghost btn-sm" @click="clearFilters">清除筛选</button>
          </div>

          <div v-else class="article-list">
            <ArticleCard
              v-for="article in articles"
              :key="article.articleId"
              :article="article"
              @tag-click="onTagClick"
              @author-click="onAuthorClick"
            />
          </div>

          <!-- Load more -->
          <div v-if="hasMore" class="load-more">
            <button class="btn btn-secondary" :class="{ loading: loadingMore }" @click="loadMore" :disabled="loadingMore">
              <el-icon v-if="loadingMore" class="is-loading"><Loading /></el-icon>
              <span>{{ loadingMore ? '加载中...' : '加载更多' }}</span>
            </button>
          </div>
        </div>

        <!-- Sidebar -->
        <aside class="sidebar-col">
          <!-- Author filter -->
          <div v-if="filters.authorName" class="card sidebar-card">
            <div class="sidebar-card-title">当前作者</div>
            <div class="author-filter-info">
              <span>@{{ filters.authorName }}</span>
              <button class="btn btn-ghost btn-sm" @click="clearAuthorFilter">清除</button>
            </div>
          </div>

          <!-- Hot tags -->
          <div class="card sidebar-card">
            <div class="sidebar-card-title">热门标签</div>
            <div class="sidebar-tags">
              <button
                v-for="tag in popularTags.slice(0, 15)"
                :key="tag.tagId"
                class="sidebar-tag"
                :class="{ active: filters.tagId === tag.tagId }"
                @click="toggleTag(tag)"
              >
                <span># {{ tag.tagName }}</span>
                <span class="sidebar-tag-count">{{ tag.articleCount }}</span>
              </button>
            </div>
          </div>

          <!-- Quick actions -->
          <div class="card sidebar-card">
            <div class="sidebar-card-title">快捷操作</div>
            <router-link to="/article/write" class="btn btn-primary" style="width:100%;justify-content:center">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
              写文章
            </router-link>
            <router-link to="/article/my" class="btn btn-secondary" style="width:100%;justify-content:center;margin-top:8px">
              我的博客
            </router-link>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
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
const hasMore = ref(false)
const PAGE_SIZE = 10

const filters = reactive({
  keyword: '',
  tagId: null,
  tagName: '',
  authorId: null,
  authorName: '',
  sort: 'latest'
})

const sortOptions = [
  { label: '最新', value: 'latest' },
  { label: '最热', value: 'hot' },
  { label: '最赞', value: 'liked' }
]

const hasActiveFilters = computed(() =>
  filters.keyword || filters.tagId || filters.authorId
)

const activeChips = computed(() => {
  const chips = []
  if (filters.tagId) chips.push({ key: 'tag', label: `# ${filters.tagName}` })
  if (filters.authorId) chips.push({ key: 'author', label: `@${filters.authorName}` })
  return chips
})

let searchTimer = null
function debouncedSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => doSearch(), 400)
}

function doSearch() {
  currentPage.value = 1
  articles.value = []
  fetchArticles(false)
}

function setSort(val) {
  filters.sort = val
  doSearch()
}

function clearKeyword() {
  filters.keyword = ''
  doSearch()
}

function clearFilters() {
  filters.keyword = ''
  filters.tagId = null
  filters.tagName = ''
  filters.authorId = null
  filters.authorName = ''
  filters.sort = 'latest'
  doSearch()
}

function removeChip(key) {
  if (key === 'tag') { filters.tagId = null; filters.tagName = '' }
  if (key === 'author') { filters.authorId = null; filters.authorName = '' }
  doSearch()
}

function toggleTag(tag) {
  if (filters.tagId === tag.tagId) {
    filters.tagId = null
    filters.tagName = ''
  } else {
    filters.tagId = tag.tagId
    filters.tagName = tag.tagName
  }
  doSearch()
}

function onTagClick(tag) {
  filters.tagId = tag.tagId
  filters.tagName = tag.tagName
  doSearch()
}

function onAuthorClick(author) {
  filters.authorId = author.userId
  filters.authorName = author.username
  doSearch()
}

function clearAuthorFilter() {
  filters.authorId = null
  filters.authorName = ''
  doSearch()
}

async function fetchArticles(append = false) {
  if (append) {
    loadingMore.value = true
  } else {
    loading.value = true
  }
  try {
    const params = {
      keyword: filters.keyword || undefined,
      tagId: filters.tagId || undefined,
      authorId: filters.authorId || undefined,
      sort: filters.sort,
      page: currentPage.value,
      pageSize: PAGE_SIZE
    }
    const res = await searchArticlesService(params)
    const data = res.data
    if (append) {
      articles.value.push(...data.list)
    } else {
      articles.value = data.list
    }
    hasMore.value = data.list.length === PAGE_SIZE && (currentPage.value * PAGE_SIZE) < data.total
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadMore() {
  currentPage.value++
  await fetchArticles(true)
}

onMounted(async () => {
  const [, tagsRes] = await Promise.all([
    fetchArticles(false),
    getPopularTagsService(20)
  ])
  popularTags.value = tagsRes.data || []
})
</script>

<style scoped>
.home { min-height: 100%; }

.home-layout {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 24px;
  align-items: start;
}

/* Search bar */
.search-bar {
  padding: 16px 20px;
  margin-bottom: 16px;
}

.search-input-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-icon { color: var(--c-text-4); flex-shrink: 0; }

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  background: transparent;
  color: var(--c-text);
}
.search-input::placeholder { color: var(--c-text-4); }

.search-clear {
  border: none;
  background: none;
  color: var(--c-text-4);
  cursor: pointer;
  padding: 2px;
  border-radius: 4px;
  display: flex;
  align-items: center;
}
.search-clear:hover { color: var(--c-text); background: var(--c-surface-2); }

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--c-border);
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.filter-label { font-size: 13px; color: var(--c-text-3); white-space: nowrap; }

.sort-tabs {
  display: flex;
  gap: 2px;
  background: var(--c-surface-2);
  padding: 2px;
  border-radius: 8px;
}

.sort-tab {
  padding: 4px 12px;
  border: none;
  background: none;
  font-size: 13px;
  font-weight: 500;
  color: var(--c-text-3);
  border-radius: 6px;
  cursor: pointer;
  transition: all var(--transition);
}
.sort-tab.active {
  background: var(--c-surface);
  color: var(--c-primary);
  box-shadow: var(--shadow-sm);
}
.sort-tab:hover:not(.active) { color: var(--c-text); }

.active-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: var(--c-primary-light);
  color: var(--c-primary);
  padding: 3px 8px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 500;
}
.chip button {
  border: none;
  background: none;
  color: var(--c-primary);
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  padding: 0;
}

.clear-filters { color: var(--c-text-3); }

/* Tag cloud */
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}

.tag-count {
  background: rgba(255,255,255,0.3);
  padding: 0 4px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
}
.tag-pill .tag-count { color: inherit; }
.tag-pill.active .tag-count { background: rgba(255,255,255,0.3); color: white; }

/* Feed */
.article-list { display: flex; flex-direction: column; gap: 12px; }

.empty-feed {
  text-align: center;
  padding: 60px 20px;
  color: var(--c-text-4);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.load-more {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* Sidebar */
.sidebar-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 16px;
  max-height: calc(100dvh - var(--nav-height) - 56px);
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-right: 2px;
}

.sidebar-card { padding: 16px 20px; }

.sidebar-card-title {
  font-size: 13px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--c-text-3);
  margin-bottom: 12px;
}

.sidebar-tags { display: flex; flex-direction: column; gap: 2px; }

.sidebar-tag {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 8px;
  border: none;
  background: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 13px;
  color: var(--c-text-3);
  transition: all var(--transition);
  text-align: left;
}
.sidebar-tag:hover { background: var(--c-surface-2); color: var(--c-text); }
.sidebar-tag.active { background: var(--c-primary-light); color: var(--c-primary); }

.sidebar-tag-count {
  font-size: 11px;
  background: var(--c-surface-2);
  padding: 1px 6px;
  border-radius: var(--radius-full);
  font-weight: 600;
}
.sidebar-tag.active .sidebar-tag-count { background: var(--c-primary); color: white; }

.author-filter-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  color: var(--c-text);
}

@media (max-width: 900px) {
  .home-layout {
    grid-template-columns: 1fr;
  }
  .sidebar-col { position: static; order: -1; }
}
</style>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useTokenStore } from '../store/token.js'
import { DEFAULT_ARTICLE_COVER_URL as defaultCover, DEFAULT_AVATAR_URL as defaultAvatar } from '../constants/assets.js'
import {
  getCommunityFeedService,
  getFollowingCommunityFeedService,
  getCommunityMetaService,
  toggleCommunityFollowService
} from '../api/community.js'

const route = useRoute()
const router = useRouter()
const tokenStore = useTokenStore()
const articles = ref([])
const meta = reactive({ hotTags: [], recommendedCreators: [] })
const loading = ref(false)
const refreshing = ref(false)
const hasLoaded = ref(false)
const loadingMore = ref(false)
const mode = ref('hot')
const selectedTags = ref([])
const keyword = ref('')
const page = ref(1)
const total = ref(0)
const followedUsers = reactive(new Set())
const PAGE_SIZE = 10
let feedRequestSequence = 0

const hasMore = computed(() => articles.value.length < total.value)
const selectedTagIds = computed(() => selectedTags.value.map(tag => tag.tagId))

function valueOf(object, camel, snake) {
  return object?.[camel] ?? object?.[snake]
}

function requireLogin() {
  if (tokenStore.token) return true
  router.push({ path: '/login', query: { redirect: route.fullPath } })
  return false
}

async function fetchFeed(append = false) {
  const requestSequence = ++feedRequestSequence
  if (append) {
    loadingMore.value = true
  } else if (!hasLoaded.value && articles.value.length === 0) {
    loading.value = true
  } else {
    refreshing.value = true
  }

  try {
    const service = mode.value === 'following'
      ? getFollowingCommunityFeedService
      : getCommunityFeedService
    const result = await service({
      sort: mode.value === 'hot' ? 'hot' : 'latest',
      tagIds: selectedTagIds.value.length ? selectedTagIds.value.join(',') : undefined,
      keyword: keyword.value.trim() || undefined,
      page: page.value,
      pageSize: PAGE_SIZE
    })
    if (requestSequence !== feedRequestSequence) return

    const data = result.data || {}
    const list = data.list || []
    articles.value = append ? articles.value.concat(list) : list
    total.value = data.total || 0
  } finally {
    if (requestSequence === feedRequestSequence) {
      loading.value = false
      refreshing.value = false
      loadingMore.value = false
      hasLoaded.value = true
    }
  }
}

async function fetchMeta() {
  const result = await getCommunityMetaService(Boolean(tokenStore.token))
  meta.hotTags = result.data?.hotTags || []
  meta.recommendedCreators = result.data?.recommendedCreators || []
  followedUsers.clear()
  meta.recommendedCreators.forEach(creator => {
    if (valueOf(creator, 'following', 'following')) {
      followedUsers.add(valueOf(creator, 'userId', 'user_id'))
    }
  })
}

function refresh() {
  page.value = 1
  fetchFeed()
}

function setMode(nextMode) {
  if (nextMode === 'following' && !requireLogin()) return
  if (mode.value === nextMode) return
  mode.value = nextMode
  refresh()
}

function selectTag(tag) {
  const id = valueOf(tag, 'tagId', 'tag_id')
  const index = selectedTags.value.findIndex(item => item.tagId === id)
  if (index >= 0) selectedTags.value.splice(index, 1)
  else selectedTags.value.push({ tagId: id, tagName: valueOf(tag, 'tagName', 'tag_name') || '' })
  refresh()
}

function isTagSelected(tag) {
  const id = valueOf(tag, 'tagId', 'tag_id')
  return selectedTags.value.some(item => item.tagId === id)
}

function clearSelectedTags() {
  selectedTags.value = []
  refresh()
}

async function loadMore() {
  page.value += 1
  await fetchFeed(true)
}

async function toggleFollow(creator) {
  if (!requireLogin()) return
  const id = valueOf(creator, 'userId', 'user_id')
  if (followedUsers.has(id)) {
    try {
      const name = valueOf(creator, 'nickname', 'nickname') || valueOf(creator, 'username', 'username')
      await ElMessageBox.confirm(
        `确定取消关注“${name}”吗？取消后将不再优先看到对方的更新。`,
        '取消关注',
        {
          confirmButtonText: '取消关注',
          cancelButtonText: '保留关注',
          type: 'warning'
        }
      )
    } catch {
      return
    }
  }
  const result = await toggleCommunityFollowService(id)
  if (result.data?.following) {
    followedUsers.add(id)
    ElMessage.success('已关注该作者')
  } else {
    followedUsers.delete(id)
    ElMessage.success('已取消关注')
  }
}

function formatDate(value) {
  if (!value) return ''
  return new Intl.DateTimeFormat('zh-CN', { month: 'short', day: 'numeric' }).format(new Date(value))
}

function formatCount(value) {
  const count = Number(value || 0)
  if (count >= 10000) return `${(count / 10000).toFixed(1)}万`
  if (count >= 1000) return `${(count / 1000).toFixed(1)}k`
  return String(count)
}

watch(() => route.query.keyword, value => {
  const next = typeof value === 'string' ? value : ''
  if (next === keyword.value) return
  keyword.value = next
  refresh()
})

onMounted(() => {
  keyword.value = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  return Promise.all([fetchFeed(), fetchMeta()])
})
</script>

<template>
  <div class="feed-page workspace-page">
      <div class="page-container feed-layout workspace-frame">
        <aside class="left-sidebar workspace-scroll">
          <section v-if="!tokenStore.token" class="join-card">
            <h2><strong>BYTE</strong> 是面向开发者的技术社区</h2>
            <p>分享实践、记录问题，与认真写代码的人一起成长。</p>
            <router-link to="/login" class="join-primary">创建账户</router-link>
            <router-link to="/login" class="join-login">登录</router-link>
          </section>

          <nav class="side-nav" aria-label="站点导航">
            <router-link to="/home" class="active">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="m3 11 9-8 9 8"/><path d="M5 10v10h14V10M9 20v-6h6v6"/></svg>首页
            </router-link>
            <router-link to="/article/write">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z"/></svg>写文章
            </router-link>
            <router-link to="/article/my">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 5h16M4 12h16M4 19h10"/></svg>我的文章
            </router-link>
            <router-link to="/article/categories">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z"/></svg>文章分组
            </router-link>
            <router-link to="/ai/chat">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 3v3M5.6 5.6l2.1 2.1M3 12h3M18 12h3M16.3 7.7l2.1-2.1"/><rect x="6" y="8" width="12" height="11" rx="3"/><path d="M9 13h.01M15 13h.01M9 16h6"/></svg>写作助手
            </router-link>
            <router-link to="/announcement">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 8-3 8h18s-3-1-3-8M10 20h4"/></svg>站点公告
            </router-link>
          </nav>

          <section class="left-topics">
            <header>
              <div><span>快速筛选</span><h3>热门标签</h3></div>
              <button v-if="selectedTags.length" type="button" class="clear-topic" @click="clearSelectedTags">清除</button>
            </header>
            <div class="topic-cloud">
              <button v-for="tag in meta.hotTags.slice(0, 7)" :key="valueOf(tag, 'tagId', 'tag_id')" :class="{ active: isTagSelected(tag) }" @click="selectTag(tag)">
                <span>#{{ valueOf(tag, 'tagName', 'tag_name') }}</span><small>{{ valueOf(tag, 'articleCount', 'article_count') }}</small>
              </button>
            </div>
          </section>
          <footer class="side-footer"><strong>BYTE</strong><span>© 2026 · 为认真创作的人而建</span></footer>
        </aside>

        <main class="feed-main workspace-scroll">
          <header class="feed-tabs">
            <button :class="{ active: mode === 'hot' }" @click="setMode('hot')">推荐</button>
            <button :class="{ active: mode === 'latest' }" @click="setMode('latest')">最新</button>
            <button :class="{ active: mode === 'following' }" @click="setMode('following')">关注</button>
            <span class="feed-count"><i v-if="refreshing" class="feed-spinner" aria-label="正在加载"></i>{{ total }} 篇文章</span>
          </header>

          <div v-if="keyword || selectedTags.length" class="filter-strip">
            <span>筛选结果</span>
            <strong v-if="keyword">“{{ keyword }}”</strong>
            <div v-if="selectedTags.length" class="filter-tags"><button v-for="tag in selectedTags" :key="tag.tagId" @click="selectTag(tag)">#{{ tag.tagName }} ×</button></div>
            <button class="clear-filter" @click="keyword = ''; selectedTags = []; router.replace('/home'); refresh()">全部清除</button>
          </div>

          <div v-if="loading && !hasLoaded" class="feed-loading">
            <div v-for="item in 4" :key="item" class="feed-skeleton" :class="{ featured: item === 1 }"><i></i><span></span><span></span><span></span></div>
          </div>

          <div v-else-if="articles.length" class="article-feed" :aria-busy="refreshing">
            <article v-for="article in articles" :key="article.articleId" class="feed-card" @click="router.push(`/article/${article.articleId}`)">
              <div class="featured-cover"><img :src="article.coverImage || defaultCover" :alt="article.title" /></div>
              <div class="feed-card__body">
                <header class="author-line">
                  <button @click.stop="router.push(`/profile/${article.authorUsername}`)"><img :src="article.authorAvatar || defaultAvatar" alt="" /></button>
                  <div><strong>{{ article.authorNickname || article.authorUsername }}</strong><time>{{ formatDate(article.createTime) }}</time></div>
                </header>
                <div class="article-content">
                  <h2>{{ article.title }}</h2>
                  <p>{{ article.summary || '作者暂未填写摘要，进入文章查看完整内容。' }}</p>
                  <div class="card-tags">
                    <button v-for="tag in (article.tags || []).slice(0, 4)" :key="tag.tagId" @click.stop="selectTag(tag)">#{{ tag.tagName }}</button>
                  </div>
                  <footer>
                    <div class="engagement">
                      <span><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12Z"/><circle cx="12" cy="12" r="3"/></svg>{{ formatCount(article.viewCount) }} 阅读</span>
                      <span><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="m12 21-1.5-1.3C5.1 15 2 12.2 2 8.8A4.8 4.8 0 0 1 6.9 4 5.3 5.3 0 0 1 12 7a5.3 5.3 0 0 1 5.1-3A4.8 4.8 0 0 1 22 8.8c0 3.4-3.1 6.2-8.5 10.9Z"/></svg>{{ formatCount(article.likeCount) }} 喜欢</span>
                      <span><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 15a3 3 0 0 1-3 3H8l-5 3V6a3 3 0 0 1 3-3h12a3 3 0 0 1 3 3Z"/></svg>{{ formatCount(article.commentCount) }} 评论</span>
                    </div>
                    <span class="read-time">{{ Math.max(2, Math.ceil((article.content?.length || article.summary?.length || 600) / 500)) }} 分钟阅读</span>
                  </footer>
                </div>
              </div>
            </article>

            <button v-if="hasMore" class="load-more" :disabled="loadingMore" @click="loadMore">{{ loadingMore ? '加载中…' : '加载更多' }}</button>
          </div>

          <div v-else class="empty-feed"><strong>没有找到文章</strong><p>换个关键词或减少组合标签再试试。</p><button @click="keyword = ''; selectedTags = []; refresh()">查看全部</button></div>
        </main>

        <aside class="right-sidebar workspace-scroll">
          <section class="right-card creators">
            <header><div><span>社区成员</span><h2>值得关注的作者</h2></div><small>持续输出</small></header>
            <div class="creator-list">
              <article v-for="creator in meta.recommendedCreators" :key="valueOf(creator, 'userId', 'user_id')">
                <button class="creator-info" @click="router.push(`/profile/${valueOf(creator, 'username', 'username')}`)">
                  <img :src="valueOf(creator, 'avatarImage', 'avatar_image') || defaultAvatar" alt="" />
                  <span><strong>{{ valueOf(creator, 'nickname', 'nickname') || valueOf(creator, 'username', 'username') }}</strong><small>{{ valueOf(creator, 'signature', 'signature') || `@${valueOf(creator, 'username', 'username')}` }}</small><em>{{ valueOf(creator, 'articleCount', 'article_count') || 0 }} 篇 · {{ formatCount(valueOf(creator, 'followerCount', 'follower_count')) }} 关注者</em></span>
                </button>
                <button class="follow" :class="{ active: followedUsers.has(valueOf(creator, 'userId', 'user_id')) }" @click="toggleFollow(creator)">{{ followedUsers.has(valueOf(creator, 'userId', 'user_id')) ? '已关注' : '关注' }}</button>
              </article>
            </div>
          </section>
        </aside>
      </div>
  </div>
</template>

<style scoped>
.feed-page { background: #f5f5f5; }
[data-theme="dark"] .feed-page { background: var(--c-bg); }
.feed-scroll { height: 100%; }
.feed-layout { display: grid; grid-template-columns: 248px minmax(0, 1fr) 320px; align-items: start; gap: 18px; padding-top: 18px; padding-bottom: 60px; }
.left-sidebar, .right-sidebar { display: flex; min-width: 0; flex-direction: column; gap: 14px; }
.join-card, .right-card { border: 1px solid var(--c-border); border-radius: 7px; background: var(--c-surface); }
.join-card { padding: 20px; }.join-card h2 { font-size: 19px; line-height: 1.42; }.join-card h2 strong { color: var(--c-primary); }.join-card p { margin: 15px 0 18px; color: var(--c-text-3); font-size: 13px; line-height: 1.65; }
.join-primary, .join-login { display: flex; height: 40px; align-items: center; justify-content: center; border: 1px solid var(--c-primary); border-radius: 6px; color: var(--c-primary); font-weight: 700; }.join-primary:hover { background: var(--c-primary); color: #fff; }.join-login { height: 36px; margin-top: 5px; border-color: transparent; color: var(--c-text-3); }
.side-nav { display: flex; flex-direction: column; gap: 2px; }.side-nav a { display: flex; align-items: center; gap: 11px; padding: 9px 11px; border-radius: 6px; color: var(--c-text-2); font-size: 14px; }.side-nav a:hover, .side-nav a.active { background: var(--c-primary-soft); color: var(--c-primary); }.side-nav svg { width: 21px; height: 21px; flex: 0 0 auto; }
.left-topics { padding: 12px 10px 10px; border-top: 1px solid var(--c-border); }
.left-topics > header { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 11px; }
.left-topics > header span { color: var(--c-text-4); font-size: 9px; }
.left-topics h3 { margin-top: 1px; color: var(--c-text); font-size: 13px; }
.left-topics .clear-topic { width: auto; padding: 3px 0; border: 0; background: transparent; color: var(--c-primary); font-size: 9px; }
.topic-cloud { display: flex; flex-direction: column; gap: 2px; }
.topic-cloud button { display: flex; min-width: 0; min-height: 32px; align-items: center; justify-content: space-between; gap: 5px; padding: 6px 7px; border: 0; border-radius: 5px; background: transparent; color: var(--c-text-3); font-size: 11px; text-align: left; }
.topic-cloud button span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.topic-cloud button small { color: var(--c-text-4); font-size: 9px; }
.topic-cloud button:hover, .topic-cloud button.active { background: var(--c-primary-soft); color: var(--c-primary); }
.side-footer { display: flex; align-items: center; gap: 7px; padding: 2px 4px; color: var(--c-text-4); font-size: 9px; }.side-footer strong { color: var(--c-text-3); font-size: 10px; }
.feed-main { min-width: 0; }.feed-tabs { display: flex; height: 46px; align-items: center; gap: 8px; padding: 0 5px; }.feed-tabs button { padding: 8px 11px; border: 0; border-radius: 6px; background: transparent; color: var(--c-text-3); font-size: 15px; }.feed-tabs button.active { color: var(--c-text); font-weight: 800; }.feed-tabs button:hover { background: var(--c-surface); color: var(--c-primary); }.feed-count { display: inline-flex; min-width: 76px; align-items: center; justify-content: flex-end; gap: 7px; margin-left: auto; color: var(--c-text-4); font-size: 11px; }.feed-spinner { width: 13px; height: 13px; border: 2px solid var(--c-border-strong); border-top-color: var(--c-primary); border-radius: 50%; animation: spin .7s linear infinite; }
.filter-strip { display: flex; align-items: center; gap: 8px; padding: 10px 14px; border: 1px solid var(--c-border); border-radius: 7px; margin-bottom: 9px; background: var(--c-surface); color: var(--c-text-3); font-size: 11px; }.filter-strip strong { color: var(--c-primary); }.filter-tags { display: flex; min-width: 0; flex-wrap: wrap; gap: 5px; }.filter-tags button { padding: 3px 7px; border: 0; border-radius: 4px; background: var(--c-primary-soft); color: var(--c-primary); font-size: 10px; }.clear-filter { margin-left: auto; padding: 3px 0; border: 0; background: transparent; color: var(--c-text-4); font-size: 10px; }
.article-feed { display: flex; flex-direction: column; gap: 9px; }.feed-card { overflow: hidden; border: 1px solid var(--c-border); border-radius: 7px; background: var(--c-surface); cursor: pointer; transition: border-color var(--transition), box-shadow var(--transition); }.feed-card:hover { border-color: var(--c-border-strong); box-shadow: var(--shadow-xs); }.featured-cover { height: 310px; overflow: hidden; background: var(--c-surface-3); }.featured-cover img { width: 100%; height: 100%; object-fit: cover; }.feed-card__body { padding: 18px 20px 17px; }.author-line { display: flex; align-items: center; gap: 9px; }.author-line > button { padding: 0; border: 0; background: transparent; }.author-line img { width: 34px; height: 34px; border-radius: 50%; object-fit: cover; }.author-line div { display: flex; flex-direction: column; }.author-line strong { color: var(--c-text-2); font-size: 12px; }.author-line time { color: var(--c-text-4); font-size: 10px; }
.article-content { padding-left: 43px; }.article-content h2 { margin: 9px 0 7px; color: var(--c-text); font-size: 23px; font-weight: 800; letter-spacing: -.02em; line-height: 1.35; }.featured .article-content h2 { font-size: 29px; }.feed-card:hover h2 { color: var(--c-primary); }.article-content > p { margin-bottom: 10px; color: var(--c-text-3); font-size: 13px; line-height: 1.65; }
.card-tags { display: flex; flex-wrap: wrap; gap: 2px; }.card-tags button { padding: 4px 7px; border: 0; border-radius: 4px; background: transparent; color: var(--c-text-3); font-size: 11px; }.card-tags button:hover { background: var(--c-surface-2); color: var(--c-primary); }.article-content footer { display: flex; align-items: center; justify-content: space-between; gap: 15px; margin-top: 11px; }.engagement { display: flex; gap: 10px; }.engagement span { display: inline-flex; align-items: center; gap: 5px; padding: 5px 7px; border-radius: 5px; color: var(--c-text-3); font-size: 11px; }.engagement span:hover { background: var(--c-surface-2); }.engagement svg { width: 16px; height: 16px; }.read-time { color: var(--c-text-4); font-size: 10px; }
.load-more { height: 42px; border: 1px solid var(--c-border); border-radius: 7px; background: var(--c-surface); color: var(--c-text-3); font-weight: 700; }.load-more:hover { border-color: var(--c-primary); color: var(--c-primary); }
.right-card { overflow: hidden; }.right-card > header { display: flex; align-items: flex-end; justify-content: space-between; gap: 10px; padding: 14px 15px 12px; border-bottom: 1px solid var(--c-border); }.right-card > header span, .right-card > header > small { color: var(--c-text-4); font-size: 9px; }.right-card > header h2 { margin-top: 2px; font-size: 16px; }
.creator-list { max-height: 236px; overflow-y: auto; overscroll-behavior: contain; scrollbar-width: thin; }.creators article { display: flex; align-items: center; gap: 8px; padding: 12px 14px; border-bottom: 1px solid var(--c-border-light); }.creators article:last-child { border-bottom: 0; }.creator-info { display: flex; min-width: 0; flex: 1; align-items: center; gap: 9px; padding: 0; border: 0; background: transparent; text-align: left; }.creator-info img { width: 40px; height: 40px; flex: 0 0 auto; border: 1px solid var(--c-border); border-radius: 50%; object-fit: cover; }.creator-info > span { display: flex; min-width: 0; flex-direction: column; }.creator-info strong { overflow: hidden; color: var(--c-text); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }.creator-info small { overflow: hidden; color: var(--c-text-4); font-size: 8px; text-overflow: ellipsis; white-space: nowrap; }.creator-info em { margin-top: 2px; color: var(--c-text-3); font-size: 8px; font-style: normal; }.follow { min-width: 52px; padding: 5px 8px; border: 1px solid var(--c-primary); border-radius: 5px; background: transparent; color: var(--c-primary); font-size: 9px; font-weight: 700; }.follow.active { border-color: var(--c-border); color: var(--c-text-3); }
.feed-loading { display: flex; flex-direction: column; gap: 9px; }.feed-skeleton { display: flex; min-height: 180px; flex-direction: column; gap: 12px; padding: 24px 28px; border: 1px solid var(--c-border); border-radius: 7px; background: var(--c-surface); }.feed-skeleton.featured { min-height: 470px; padding-top: 338px; }.feed-skeleton i, .feed-skeleton span { height: 13px; border-radius: 5px; background: var(--c-surface-3); animation: pulse 1.3s infinite; }.feed-skeleton i { width: 150px; }.feed-skeleton span:nth-child(2) { width: 70%; height: 25px; }.feed-skeleton span:nth-child(3) { width: 94%; }.feed-skeleton span:nth-child(4) { width: 58%; }
.empty-feed { padding: 72px 20px; border: 1px solid var(--c-border); border-radius: 7px; background: var(--c-surface); text-align: center; }.empty-feed strong { font-size: 18px; }.empty-feed p { margin: 6px 0 16px; color: var(--c-text-3); }.empty-feed button { padding: 7px 13px; border: 1px solid var(--c-primary); border-radius: 5px; background: transparent; color: var(--c-primary); }
@keyframes pulse { 50% { opacity: .48; } }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 1200px) { .feed-layout { grid-template-columns: 220px minmax(0, 1fr); }.right-sidebar { display: none; } }
@media (max-width: 860px) { .feed-page { overflow-y: auto; }.feed-scroll { height: auto; overflow: visible; }.feed-layout { grid-template-columns: 1fr; }.left-sidebar { display: none; }.featured-cover { height: 270px; } }
@media (max-width: 560px) { .feed-layout { width: 100%; padding: 8px 8px 40px; }.feed-tabs { padding-inline: 4px; }.feed-card__body { padding: 15px 13px; }.article-content { padding-left: 0; }.article-content h2, .featured .article-content h2 { font-size: 20px; }.featured-cover { height: 205px; }.article-content > p { display: none; }.card-tags button:nth-child(n+4) { display: none; }.read-time { display: none; }.engagement { gap: 2px; } }

/* Three independent columns keep navigation and discovery context in place. */
.feed-layout { height: 100%; min-height: 0; align-items: stretch; padding-top: 0; padding-bottom: 0; }
.left-sidebar, .right-sidebar { height: 100%; padding: 18px 2px 42px; }
.feed-main { min-height: 0; padding: 18px 2px 52px; }
.feed-card { display: grid; min-height: 178px; grid-template-columns: minmax(0, 1fr) 184px; border-radius: var(--radius-sm); }
.feed-card__body { grid-column: 1; grid-row: 1; padding: 16px 18px; }
.featured-cover { grid-column: 2; grid-row: 1; height: auto; min-height: 146px; margin: 15px 15px 15px 0; border-radius: 4px; }
.article-content h2, .featured .article-content h2 { margin-top: 7px; font-size: 21px; }
.article-content > p { display: -webkit-box; margin-bottom: 8px; overflow: hidden; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.engagement { flex-wrap: wrap; gap: 4px; }
.engagement span { padding: 4px 6px; }

@media (max-width: 860px) {
  .feed-layout { height: auto; }
  .feed-main { overflow: visible; }
  .feed-card { grid-template-columns: minmax(0, 1fr) 150px; }
  .featured-cover { height: auto; min-height: 132px; }
}
@media (max-width: 560px) {
  .feed-card { display: block; min-height: 0; }
  .featured-cover { height: 190px; margin: 0; border-radius: 0; }
  .article-content > p { display: -webkit-box; -webkit-line-clamp: 2; }
}
</style>

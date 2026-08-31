<template>
  <div class="article-page workspace-page">
    <div class="reading-progress"><i :style="{ width: readingProgress + '%' }"></i></div>
    <div class="article-shell page-container workspace-frame">
      <div class="article-layout">
        <aside v-if="article" class="article-actions">
          <button :class="{ active: article.isLiked }" @click="toggleLike"><svg viewBox="0 0 24 24" :fill="article.isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2"><path d="m12 21-1.5-1.3C5.1 15 2 12.2 2 8.8A4.8 4.8 0 0 1 6.9 4 5.3 5.3 0 0 1 12 7a5.3 5.3 0 0 1 5.1-3A4.8 4.8 0 0 1 22 8.8c0 3.4-3.1 6.2-8.5 10.9Z"/></svg><span>点赞</span><b>{{ article.likeCount }}</b></button>
          <button @click="scrollToComments"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a3 3 0 0 1-3 3H8l-5 3V6a3 3 0 0 1 3-3h12a3 3 0 0 1 3 3Z"/></svg><span>评论</span><b>{{ article.commentCount }}</b></button>
          <button :class="{ active: bookmarked }" @click="bookmarked = !bookmarked"><svg viewBox="0 0 24 24" :fill="bookmarked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2"><path d="M6 3h12a1 1 0 0 1 1 1v17l-7-4-7 4V4a1 1 0 0 1 1-1Z"/></svg><span>收藏</span></button>
          <button @click="shareArticle"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><path d="m8.6 10.5 6.8-4M8.6 13.5l6.8 4"/></svg><span>分享</span></button>
        </aside>

        <div class="article-col workspace-scroll">
          <div v-if="loadingArticle" class="loading-spinner" style="height:200px">
            <el-icon class="is-loading" :size="28"><Loading /></el-icon>
          </div>

          <template v-else-if="article">
            <article class="article-main surface-card">
              <div class="article-inner">
                <header class="article-header">
                  <div v-if="article.categoryName" class="article-category">{{ article.categoryName }}</div>
                  <h1 class="article-title">{{ article.title }}</h1>
                  <p v-if="article.summary" class="article-summary">{{ article.summary }}</p>

                  <div class="article-meta">
                    <div class="article-author" @click="goToAuthor">
                      <img :src="article.authorAvatar || defaultAvatar" class="avatar" style="width:40px;height:40px"/>
                      <div>
                        <div class="author-name">{{ article.authorNickname || article.authorUsername }}</div>
                        <div class="author-date">{{ formatDate(article.createTime) }} · {{ readingMinutes }} 分钟阅读</div>
                      </div>
                    </div>
                    <div class="article-stats">
                      <span class="stat">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                        {{ article.viewCount }} 次阅读
                      </span>
                    </div>
                  </div>

                  <div v-if="article.tags && article.tags.length > 0" class="article-tags">
                    <span v-for="tag in article.tags" :key="tag.tagId" class="tag-pill">
                      # {{ tag.tagName }}
                    </span>
                  </div>
                </header>

                <div v-if="article.coverImage && !coverFailed" class="article-cover">
                  <img :src="article.coverImage" :alt="`${article.title}封面`" @error="coverFailed = true" />
                </div>
                <div v-else class="article-cover article-cover--tech" aria-hidden="true">
                  <span>// {{ article.categoryName || 'TECH ARTICLE' }}</span>
                  <strong>&lt;/&gt;</strong>
                  <code>const knowledge = share(experience)</code>
                </div>

                <div class="article-body" v-html="renderedContent"></div>
              </div>
            </article>

            <section id="comments" class="article-comments surface-card">
              <CommentSection
                :article-id="article.articleId"
                :author-id="article.userId"
                :total-count="article.commentCount"
                @count-change="(n) => article.commentCount = n"
              />
            </section>
          </template>

          <div v-else class="empty-feed" style="margin-top:60px">
            <p>文章不存在或已被删除</p>
            <router-link to="/home" class="btn btn-secondary">返回首页</router-link>
          </div>
        </div>

        <aside v-if="article" class="article-sidebar workspace-scroll">
          <section v-if="toc.length" class="toc-card surface-card">
            <h2>本文目录</h2>
            <button v-for="item in toc" :key="item.id" :class="['toc-level-' + item.level, { active: activeTocId === item.id }]" @click="scrollTo(item.id)">{{ item.text }}</button>
          </section>
          <section class="author-card surface-card">
            <h2>关于作者</h2>
            <div class="author-card__profile" @click="goToAuthor">
              <img :src="article.authorAvatar || defaultAvatar" alt="" />
              <div><strong>{{ article.authorNickname || article.authorUsername }}</strong><span>@{{ article.authorUsername }}</span></div>
            </div>
            <footer><span>本文获赞<strong>{{ article.likeCount }}</strong></span><span>本文阅读<strong>{{ article.viewCount }}</strong></span></footer>
          </section>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import DOMPurify from 'dompurify'
import { getArticleDetailService } from '../../api/article.js'
import { likeArticleService, unlikeArticleService } from '../../api/articleLike.js'
import { useUserInfoStore } from '../../store/userInfo.js'
import CommentSection from '../../components/comment/CommentSection.vue'
import { DEFAULT_AVATAR_URL as defaultAvatar } from '../../constants/assets.js'

const route = useRoute()
const router = useRouter()
const userInfoStore = useUserInfoStore()

const article = ref(null)
const loadingArticle = ref(false)
const toc = ref([])
const activeTocId = ref('')
const readingProgress = ref(0)
const bookmarked = ref(false)
const coverFailed = ref(false)

const readingMinutes = computed(() => Math.max(3, Math.ceil((article.value?.content?.length || 800) / 400)))

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight(str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang }).value}</code></pre>`
      } catch (_) {}
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  }
})

const renderedContent = computed(() => {
  if (!article.value?.content) return ''
  const raw = md.render(article.value.content)
  return DOMPurify.sanitize(raw)
})

function buildToc(htmlStr) {
  const parser = new DOMParser()
  const doc = parser.parseFromString(htmlStr, 'text/html')
  const headings = doc.querySelectorAll('h1,h2,h3')
  const items = []
  headings.forEach((h, i) => {
    const id = `heading-${i}`
    items.push({
      id,
      text: h.textContent,
      level: parseInt(h.tagName[1])
    })
  })
  return items
}

async function loadArticle() {
  const id = route.params.id
  if (!id) return
  loadingArticle.value = true
  try {
    const res = await getArticleDetailService(id)
    article.value = res.data
    await nextTick()
    toc.value = buildToc(renderedContent.value)
    injectTocIds()
    observeHeadings()
  } catch {}
  finally { loadingArticle.value = false }
}

function injectTocIds() {
  const container = document.querySelector('.article-body')
  if (!container) return
  const headings = container.querySelectorAll('h1,h2,h3')
  headings.forEach((h, i) => { h.id = `heading-${i}` })
}

function observeHeadings() {
  const headings = document.querySelectorAll('.article-body h1,.article-body h2,.article-body h3')
  if (!headings.length) return
  const observer = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (entry.isIntersecting) activeTocId.value = entry.target.id
    })
  }, { root: document.querySelector('.article-col'), rootMargin: '-80px 0px -60% 0px' })
  headings.forEach(h => observer.observe(h))
}

function scrollTo(id) {
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function scrollToComments() {
  const el = document.getElementById('comments')
  if (el) el.scrollIntoView({ behavior: 'smooth' })
}

function updateReadingProgress() {
  const scroller = document.querySelector('.article-col')
  if (!scroller) return
  const max = scroller.scrollHeight - scroller.clientHeight
  readingProgress.value = max > 0 ? Math.min(100, Math.round(scroller.scrollTop / max * 100)) : 0
}

async function shareArticle() {
  const url = window.location.href
  try {
    if (navigator.share) await navigator.share({ title: article.value?.title, url })
    else {
      await navigator.clipboard.writeText(url)
      ElMessage.success('文章链接已复制')
    }
  } catch {}
}

async function toggleLike() {
  if (!userInfoStore.userInfo) { ElMessage.warning('请先登录'); return }
  try {
    if (article.value.isLiked) {
      await unlikeArticleService(article.value.articleId)
      article.value.isLiked = false
      article.value.likeCount--
    } else {
      await likeArticleService(article.value.articleId)
      article.value.isLiked = true
      article.value.likeCount++
    }
  } catch {}
}

function goToAuthor() {
  router.push(`/profile/${article.value.authorUsername}`)
}

function formatDate(time) {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

onMounted(async () => {
  await loadArticle()
  document.querySelector('.article-col')?.addEventListener('scroll', updateReadingProgress, { passive: true })
})
onUnmounted(() => document.querySelector('.article-col')?.removeEventListener('scroll', updateReadingProgress))
</script>

<style scoped>
.article-page {
  position: relative;
}

.article-shell {
  margin: 0 auto;
}

.article-layout {
  display: grid;
  height: 100%;
  min-height: 0;
  grid-template-columns: 74px minmax(0, 880px) 286px;
  gap: 20px;
  align-items: stretch;
  justify-content: center;
}

.article-col {
  min-width: 0;
  padding: 24px 4px 56px;
}

.article-main {
  overflow: hidden;
  border: 0;
  border-inline: 1px solid var(--c-border);
  border-radius: 0;
  background: var(--c-surface);
  box-shadow: none;
}

.article-cover {
  height: clamp(190px, 28vw, 320px);
  margin: 6px 0 30px;
  overflow: hidden;
  border-radius: 2px;
}
.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: saturate(.86) contrast(1.02);
}

.article-inner { padding: 34px clamp(24px, 4vw, 54px) 50px; }

.article-category {
  display: inline-flex;
  margin-bottom: 16px;
  padding: 2px 0 2px 9px;
  border-left: 3px solid var(--c-primary);
  border-radius: 0;
  background: transparent;
  color: var(--c-primary);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: .08em;
}

.article-title {
  color: var(--c-text);
  font-family: inherit;
  font-size: clamp(30px, 4vw, 46px);
  font-weight: 900;
  line-height: 1.2;
  letter-spacing: -0.03em;
  margin-bottom: 0;
}
.article-summary { margin-top: 13px; color: var(--c-text-3); font-family: inherit; font-size: 16px; line-height: 1.75; }

.article-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 18px 0 12px;
}

.article-author {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}
.article-author:hover .author-name { color: var(--c-primary); }

.author-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--c-text);
  transition: color var(--transition);
}
.author-date { margin-top: 2px; color: var(--c-text-4); font-size: 12px; }

.article-stats {
  display: flex;
  align-items: center;
  gap: 12px;
}
.stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--c-text-4);
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-bottom: 24px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--c-border);
}
.article-tags :deep(.el-tag) { padding-inline: 0; border: 0; border-bottom: 1px solid var(--c-border-strong); border-radius: 0; background: transparent; color: var(--c-text-3); }

.article-body {
  margin: 0;
  color: var(--c-text-2);
}

.reading-progress { position: fixed; z-index: 101; top: var(--nav-height); right: 0; left: 0; height: 2px; background: transparent; }.reading-progress i { display: block; height: 100%; background: var(--c-primary); transition: width .1s linear; }
.article-actions { position: sticky; top: 24px; display: flex; align-items: center; flex-direction: column; gap: 0; padding-top: 42px; }.article-actions button { display: flex; width: 58px; align-items: center; justify-content: center; flex-direction: column; gap: 2px; padding: 11px 4px; border: 0; border-bottom: 1px solid var(--c-border-strong); border-radius: 0; background: transparent; color: var(--c-text-3); font-size: 10px; transition: all var(--transition); }.article-actions button:first-child { border-top: 2px solid var(--c-text); }.article-actions button:hover, .article-actions button.active { color: var(--c-primary); }.article-actions svg { width: 19px; height: 19px; }.article-actions b { font-family: Georgia, serif; font-size: 10px; }
.article-sidebar { position: sticky; top: 20px; display: flex; flex-direction: column; gap: 14px; }.toc-card, .author-card { padding: 18px; }.toc-card h2, .author-card h2 { margin-bottom: 14px; font-size: 16px; }.toc-card button { position: relative; display: block; width: 100%; padding: 6px 8px 6px 14px; overflow: hidden; border: 0; background: transparent; color: var(--c-text-3); font-size: 12px; text-align: left; text-overflow: ellipsis; white-space: nowrap; }.toc-card button::before { position: absolute; top: 7px; bottom: 7px; left: 0; width: 2px; border-radius: 2px; background: var(--c-border); content: ''; }.toc-card button:hover, .toc-card button.active { color: var(--c-primary); }.toc-card button.active::before { background: var(--c-primary); }.toc-card .toc-level-3 { padding-left: 26px; }
.author-card__profile { display: flex; align-items: center; gap: 10px; cursor: pointer; }.author-card__profile img { width: 48px; height: 48px; border-radius: 50%; object-fit: cover; }.author-card__profile div { display: flex; flex-direction: column; }.author-card__profile span { color: var(--c-text-4); font-size: 11px; }.author-card > p { margin: 13px 0; color: var(--c-text-3); font-size: 12px; line-height: 1.7; }.author-card > .btn { width: 100%; }.author-card footer { display: grid; grid-template-columns: repeat(2, 1fr); margin-top: 14px; border-top: 1px solid var(--c-border); padding-top: 13px; }.author-card footer span { display: flex; align-items: center; flex-direction: column; color: var(--c-text-4); font-size: 9px; }.author-card footer strong { color: var(--c-text); font-size: 12px; }
.article-comments { margin-top: 16px; padding: 4px; overflow: hidden; }

/* Side rails belong to the viewport; only the article column scrolls. */
.article-actions { position: static; padding-top: 66px; }
.article-sidebar { position: static; padding: 24px 3px 48px; }

@media (max-width: 1080px) { .article-layout { grid-template-columns: 64px minmax(0, 880px); }.article-sidebar { display: none; } }
@media (max-width: 720px) { .article-page { overflow-y: auto; }.article-shell, .article-layout { height: auto; }.article-layout { grid-template-columns: 1fr; }.article-col { overflow: visible; padding-top: 12px; }.article-actions { z-index: 2; align-items: stretch; flex-direction: row; padding: 12px 0 0; overflow-x: auto; }.article-actions button { min-width: 58px; flex: 1; }.article-inner { padding: 24px 20px 36px; }.article-title { font-size: 28px; }.article-meta { align-items: flex-start; flex-direction: column; gap: 12px; } }

/* Technical reading workspace */
.article-main { border: 1px solid var(--c-border); border-radius: var(--radius-lg); background: var(--c-surface); box-shadow: var(--shadow-xs); }
.article-category { margin-bottom: 14px; padding: 4px 9px; border: 0; border-radius: 5px; background: var(--c-primary-soft); color: var(--c-primary); font-family: ui-monospace, Consolas, monospace; letter-spacing: .02em; }
.article-title { font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif; font-weight: 850; line-height: 1.22; }
.article-summary { font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif; }
.article-body { font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif; line-height: 1.85; }
.article-body :deep(h1), .article-body :deep(h2), .article-body :deep(h3), .article-body :deep(h4) { font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif; }
.article-cover { border-radius: 7px; }
.article-cover img { filter: none; }
.article-cover--tech { position: relative; display: flex; align-items: flex-start; justify-content: space-between; flex-direction: column; padding: 28px 32px; border: 1px solid #1e293b; background-color: #0f172a; background-image: linear-gradient(rgba(96,165,250,.07) 1px, transparent 1px), linear-gradient(90deg, rgba(96,165,250,.07) 1px, transparent 1px); background-size: 28px 28px; color: #dbeafe; }
.article-cover--tech span, .article-cover--tech code { font-family: ui-monospace, "SFMono-Regular", Consolas, monospace; font-size: 12px; }
.article-cover--tech span { color: #93c5fd; }
.article-cover--tech strong { align-self: center; color: #60a5fa; font-family: ui-monospace, Consolas, monospace; font-size: clamp(52px, 8vw, 84px); line-height: 1; }
.article-cover--tech code { color: #94a3b8; }
.article-tags { gap: 7px; }
.article-tags :deep(.el-tag) { padding-inline: 8px; border: 1px solid var(--c-border); border-radius: 5px; background: var(--c-surface-2); color: var(--c-text-3); font-family: ui-monospace, Consolas, monospace; }
.reading-progress i { background: var(--c-primary); }
.article-actions { gap: 8px; }
.article-actions button { border: 1px solid var(--c-border); border-radius: var(--radius-sm); background: var(--c-surface); }
.article-actions button:first-child { border-top: 1px solid var(--c-border); }
.article-actions button:hover, .article-actions button.active { border-color: #93c5fd; background: var(--c-primary-soft); color: var(--c-primary); }
.article-sidebar { gap: 14px; }
.toc-card, .author-card { padding: 18px; border: 1px solid var(--c-border); border-radius: var(--radius-lg); background: var(--c-surface); box-shadow: var(--shadow-xs); }
.toc-card h2, .author-card h2 { font-family: Inter, "PingFang SC", sans-serif; font-size: 16px; }
.toc-card h2::before, .author-card h2::before { display: none; }
.toc-card button.active::before { background: var(--c-primary); }
</style>

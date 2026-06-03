<template>
  <div class="article-page">
    <div class="article-shell">
      <div class="article-layout">
        <div class="article-col">
          <div v-if="loadingArticle" class="loading-spinner" style="height:200px">
            <el-icon class="is-loading" :size="28"><Loading /></el-icon>
          </div>

          <template v-else-if="article">
            <article class="article-main card">
              <div v-if="article.coverImage" class="article-cover">
                <img :src="article.coverImage" :alt="article.title" />
              </div>

              <div class="article-inner">
                <header class="article-header">
                  <div v-if="article.categoryName" class="article-category">{{ article.categoryName }}</div>
                  <div class="article-title-row">
                    <h1 class="article-title">{{ article.title }}</h1>
                    <button
                      class="article-like"
                      :class="{ liked: article.isLiked }"
                      @click="toggleLike"
                    >
                      <svg width="18" height="18" viewBox="0 0 24 24" :fill="article.isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
                        <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/>
                      </svg>
                      <span>{{ article.likeCount }}</span>
                    </button>
                  </div>

                  <div class="article-meta">
                    <div class="article-author" @click="goToAuthor">
                      <img :src="article.authorAvatar || defaultAvatar" class="avatar" style="width:40px;height:40px"/>
                      <div>
                        <div class="author-name">{{ article.authorNickname || article.authorUsername }}</div>
                        <div class="author-date">{{ formatDate(article.createTime) }}</div>
                      </div>
                    </div>
                    <div class="article-stats">
                      <span class="stat">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                        {{ article.viewCount }}
                      </span>
                    </div>
                  </div>

                  <div v-if="article.tags && article.tags.length > 0" class="article-tags">
                    <span v-for="tag in article.tags" :key="tag.tagId" class="tag-pill">
                      # {{ tag.tagName }}
                    </span>
                  </div>
                </header>

                <div class="article-body" v-html="renderedContent"></div>
              </div>
            </article>
          </template>

          <div v-else class="empty-feed" style="margin-top:60px">
            <p>文章不存在或已被删除</p>
            <router-link to="/home" class="btn btn-secondary">返回首页</router-link>
          </div>
        </div>

        <aside v-if="article" id="comments" class="article-comment-panel">
          <CommentSection
            :article-id="article.articleId"
            :author-id="article.userId"
            :total-count="article.commentCount"
            panel
            @count-change="(n) => article.commentCount = n"
          />
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
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

const route = useRoute()
const router = useRouter()
const userInfoStore = useUserInfoStore()

const article = ref(null)
const loadingArticle = ref(false)
const toc = ref([])
const activeTocId = ref('')

const defaultAvatar = 'https://luf-23.oss-cn-wuhan-lr.aliyuncs.com/avatar/default.png'

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
  }, { rootMargin: '-80px 0px -60% 0px' })
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

onMounted(loadArticle)
</script>

<style scoped>
.article-page {
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #f6f7fb;
}

.article-shell {
  width: min(1220px, calc(100vw - 56px));
  height: 100%;
  margin: 0 auto;
  padding: 16px 0;
  box-sizing: border-box;
}

.article-layout {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 22px;
  align-items: stretch;
}

.article-col {
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.article-col::-webkit-scrollbar {
  display: none;
}

.article-main {
  overflow: hidden;
  border-radius: 12px;
  border: 1px solid #edf0f6;
  box-shadow: 0 12px 36px rgba(15, 23, 42, 0.05);
  background: #fff;
}

.article-cover {
  height: 164px;
  overflow: hidden;
}
.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.article-inner {
  padding: 26px 32px 38px;
}

.article-category {
  display: inline-flex;
  font-size: 12px;
  font-weight: 700;
  color: #10b981;
  background: #ecfdf5;
  padding: 4px 9px;
  border-radius: 999px;
  margin-bottom: 14px;
}

.article-title-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
}

.article-title {
  font-size: 29px;
  font-weight: 800;
  line-height: 1.24;
  color: #111827;
  letter-spacing: -0.03em;
  margin-bottom: 0;
}

.article-like {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 36px;
  padding: 0 14px;
  border: 1px solid #e3e7ef;
  border-radius: 10px;
  background: #fff;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  transition: all var(--transition);
}
.article-like:hover {
  border-color: #cfd6e3;
  background: #f8fafc;
}
.article-like.liked {
  color: #ef4444;
  border-color: #fecaca;
  background: #fff1f2;
}

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
  color: #1f2937;
  transition: color var(--transition);
}
.author-date { font-size: 12px; color: #8a93a3; margin-top: 2px; }

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
  color: #8a93a3;
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 24px;
  padding-bottom: 18px;
  border-bottom: 1px solid #edf0f6;
}

.article-body {
  margin: 0;
  color: #334155;
}

.article-comment-panel {
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #edf0f6;
  border-radius: 12px;
  box-shadow: 0 12px 36px rgba(15, 23, 42, 0.05);
}

.article-comment-panel :deep(.comment-section) {
  height: 100%;
}

@media (max-width: 900px) {
  .article-page {
    height: auto;
    overflow: visible;
  }
  .article-shell {
    height: auto;
  }
  .article-layout { grid-template-columns: 1fr; height: auto; }
  .article-col {
    overflow: visible;
  }
  .article-comment-panel {
    height: auto;
    min-height: auto;
    border-left: none;
  }
  .article-title { font-size: 22px; }
  .article-inner { padding: 20px; }
  .article-shell { width: min(100%, calc(100vw - 24px)); }
}
</style>

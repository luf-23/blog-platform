<template>
  <div class="article-page workspace-page" :class="{ 'has-comments': commentsOpen }">
    <div class="reading-progress"><i :style="{ width: readingProgress + '%' }"></i></div>
    <div class="article-shell page-container workspace-frame">
      <div class="article-layout">
        <aside v-if="article" class="article-actions">
          <button :class="{ active: article.isLiked }" :disabled="likeUpdating" @click="toggleLike"><svg viewBox="0 0 24 24" :fill="article.isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2"><path d="m12 21-1.5-1.3C5.1 15 2 12.2 2 8.8A4.8 4.8 0 0 1 6.9 4 5.3 5.3 0 0 1 12 7a5.3 5.3 0 0 1 5.1-3A4.8 4.8 0 0 1 22 8.8c0 3.4-3.1 6.2-8.5 10.9Z"/></svg><span>点赞</span><b>{{ article.likeCount }}</b></button>
          <button
            ref="commentToggleButton"
            :class="{ active: commentsOpen }"
            :aria-expanded="commentsOpen"
            :aria-label="commentsOpen ? '收起评论' : '展开评论'"
            aria-controls="article-comment-drawer"
            @click="toggleComments"
          ><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a3 3 0 0 1-3 3H8l-5 3V6a3 3 0 0 1 3-3h12a3 3 0 0 1 3 3Z"/></svg><span>评论</span><b>{{ article.commentCount }}</b></button>
          <button @click="shareArticle"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><path d="m8.6 10.5 6.8-4M8.6 13.5l6.8 4"/></svg><span>分享</span></button>
        </aside>

        <div ref="articleScroller" class="article-col workspace-scroll">
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

                <div class="article-cover">
                  <img :src="!coverFailed && article.coverImage ? article.coverImage : defaultCover" :alt="`${article.title}封面`" @error="coverFailed = true" />
                </div>

                <div ref="articleBody" class="article-body article-prose" v-html="renderedContent" @click="handleArticleBodyClick"></div>
              </div>
            </article>

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
            <button
              v-if="!isAuthor"
              class="btn btn-secondary btn-sm author-follow"
              :class="{ active: authorMetrics.following }"
              @click="toggleFollowAuthor"
            >{{ authorMetrics.following ? '已关注' : '关注作者' }}</button>
            <footer><span>本文获赞<strong>{{ article.likeCount }}</strong></span><span>本文阅读<strong>{{ article.viewCount }}</strong></span></footer>
          </section>
        </aside>
      </div>
    </div>

    <Transition name="comment-backdrop">
      <button
        v-if="commentsOpen"
        type="button"
        class="comment-drawer-backdrop"
        aria-label="关闭评论"
        @click="closeComments"
      ></button>
    </Transition>
    <aside
      v-if="article && commentsMounted"
      id="article-comment-drawer"
      class="comment-drawer"
      :class="{ 'is-open': commentsOpen }"
      :aria-hidden="!commentsOpen"
      :inert="!commentsOpen"
      aria-label="文章评论"
    >
      <CommentSection
        panel
        :article-id="article.articleId"
        :author-id="article.userId"
        :total-count="article.commentCount"
        @count-change="(n) => article.commentCount = n"
        @close="closeComments"
      />
    </aside>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import DOMPurify from 'dompurify'
import { getArticleDetailService } from '../../api/article.js'
import { likeArticleService, unlikeArticleService } from '../../api/articleLike.js'
import { getCommunityFollowStateService, getCommunityProfileService, toggleCommunityFollowService } from '../../api/community.js'
import { useUserInfoStore } from '../../store/userInfo.js'
import CommentSection from '../../components/comment/CommentSection.vue'
import { createMarkdownRenderer } from '../../utils/markdown/index.js'
import { DEFAULT_ARTICLE_COVER_URL as defaultCover, DEFAULT_AVATAR_URL as defaultAvatar } from '../../constants/assets.js'

const route = useRoute()
const router = useRouter()
const userInfoStore = useUserInfoStore()

const article = ref(null)
const loadingArticle = ref(false)
const likeUpdating = ref(false)
const toc = ref([])
const activeTocId = ref('')
const readingProgress = ref(0)
const coverFailed = ref(false)
const commentsOpen = ref(false)
const commentsMounted = ref(false)
const commentsOpening = ref(false)
const authorMetrics = ref({ following: false, followerCount: 0 })
const commentToggleButton = ref(null)
const articleScroller = ref(null)
const articleBody = ref(null)
let headingObserver = null
let commentOpenFrame = 0

const readingMinutes = computed(() => Math.max(3, Math.ceil((article.value?.content?.length || 800) / 400)))
const isAuthor = computed(() => Boolean(
  userInfoStore.userInfo?.userId && article.value?.userId === userInfoStore.userInfo.userId
))

const md = createMarkdownRenderer()

const renderedContent = computed(() => {
  if (!article.value?.content) return ''
  const raw = md.render(article.value.content)
  return DOMPurify.sanitize(raw)
})

async function loadArticle() {
  const id = route.params.id
  if (!id) return
  loadingArticle.value = true
  try {
    const res = await getArticleDetailService(id)
    article.value = res.data
    const profileService = userInfoStore.userInfo ? getCommunityFollowStateService : getCommunityProfileService
    profileService(article.value.userId)
      .then(result => { authorMetrics.value = result.data || authorMetrics.value })
      .catch(() => {})
  } catch {}
  finally { loadingArticle.value = false }

  if (article.value) {
    await nextTick()
    setupToc()
  }
}

function setupToc() {
  headingObserver?.disconnect()
  setupCodeBlocks()
  const headings = articleBody.value?.querySelectorAll('h1,h2,h3') || []
  toc.value = Array.from(headings).map((heading, index) => {
    const id = `heading-${index}`
    heading.id = id
    return {
      id,
      text: heading.textContent?.trim() || `章节 ${index + 1}`,
      level: Number(heading.tagName.slice(1))
    }
  })
  activeTocId.value = toc.value[0]?.id || ''
  observeHeadings(headings)

  const hashId = decodeURIComponent(window.location.hash.slice(1))
  if (hashId && toc.value.some(item => item.id === hashId)) {
    requestAnimationFrame(() => scrollTo(hashId, false))
  }
}

function setupCodeBlocks() {
  const blocks = articleBody.value?.querySelectorAll('pre') || []
  blocks.forEach(pre => {
    if (pre.parentElement?.classList.contains('article-code-block')) return

    const code = pre.querySelector('code')
    const rawCode = code?.textContent || ''
    const normalizedCode = rawCode.replace(/\r\n?/g, '\n').replace(/\n$/, '')
    const lineCount = Math.max(1, normalizedCode.split('\n').length)

    const wrapper = document.createElement('div')
    wrapper.className = 'article-code-block'
    pre.parentNode?.insertBefore(wrapper, pre)

    const lineNumbers = document.createElement('div')
    lineNumbers.className = 'article-code-lines'
    lineNumbers.setAttribute('aria-hidden', 'true')
    const lineFragment = document.createDocumentFragment()
    for (let line = 1; line <= lineCount; line += 1) {
      const number = document.createElement('span')
      number.textContent = String(line)
      lineFragment.appendChild(number)
    }
    lineNumbers.appendChild(lineFragment)
    wrapper.appendChild(lineNumbers)
    wrapper.appendChild(pre)

    const button = document.createElement('button')
    button.type = 'button'
    button.className = 'article-code-copy'
    button.setAttribute('aria-label', '复制代码')
    button.textContent = '复制'
    wrapper.appendChild(button)
  })
}

async function handleArticleBodyClick(event) {
  const button = event.target.closest('.article-code-copy')
  if (!button) return
  const code = button.parentElement?.querySelector('code')
  if (!code) return

  try {
    await copyCodeText(code.textContent || '')
    button.textContent = '已复制'
    button.classList.add('copied')
    window.setTimeout(() => {
      button.textContent = '复制'
      button.classList.remove('copied')
    }, 1600)
  } catch (_) {
    ElMessage.error('复制失败，请手动选择代码复制')
  }
}

async function copyCodeText(text) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }

  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  const copied = document.execCommand('copy')
  textarea.remove()
  if (!copied) throw new Error('copy failed')
}

function observeHeadings(headings) {
  if (!headings.length) return
  headingObserver = new IntersectionObserver(entries => {
    const visible = entries
      .filter(entry => entry.isIntersecting)
      .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)
    if (visible[0]) activeTocId.value = visible[0].target.id
  }, { root: articleScroller.value, rootMargin: '-32px 0px -70% 0px', threshold: 0 })
  headings.forEach(heading => headingObserver.observe(heading))
}

function scrollTo(id, smooth = true) {
  const scroller = articleScroller.value
  const target = articleBody.value?.querySelector(`#${id}`)
  if (!scroller || !target) return

  const top = target.getBoundingClientRect().top
    - scroller.getBoundingClientRect().top
    + scroller.scrollTop
    - 24
  activeTocId.value = id
  scroller.scrollTo({ top: Math.max(0, top), behavior: smooth ? 'smooth' : 'auto' })
  window.history.replaceState(null, '', `${window.location.pathname}${window.location.search}#${id}`)
}

function toggleComments() {
  if (commentsOpen.value || commentsOpening.value) closeComments()
  else openComments()
}

async function openComments() {
  if (commentsOpen.value || commentsOpening.value) return
  commentsOpening.value = true
  commentsMounted.value = true
  await nextTick()
  cancelAnimationFrame(commentOpenFrame)
  commentOpenFrame = requestAnimationFrame(() => {
    commentsOpen.value = true
    commentsOpening.value = false
  })
}

function closeComments() {
  cancelAnimationFrame(commentOpenFrame)
  commentsOpening.value = false
  commentsOpen.value = false
  requestAnimationFrame(() => commentToggleButton.value?.focus({ preventScroll: true }))
}

function handlePageKeydown(event) {
  if (event.key === 'Escape' && commentsOpen.value) closeComments()
}

function updateReadingProgress() {
  const scroller = articleScroller.value
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
  if (likeUpdating.value || !article.value) return
  const wasLiked = Boolean(article.value.isLiked)
  likeUpdating.value = true
  try {
    const result = wasLiked
      ? await unlikeArticleService(article.value.articleId)
      : await likeArticleService(article.value.articleId)
    article.value.isLiked = result.data?.isLiked ?? !wasLiked
    article.value.likeCount = result.data?.likeCount
      ?? Math.max(0, (article.value.likeCount || 0) + (wasLiked ? -1 : 1))
  } catch {}
  finally { likeUpdating.value = false }
}

async function toggleFollowAuthor(event) {
  event?.stopPropagation()
  if (!userInfoStore.userInfo) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  try {
    const result = await toggleCommunityFollowService(article.value.userId)
    authorMetrics.value = { ...authorMetrics.value, ...(result.data || {}) }
    ElMessage.success(authorMetrics.value.following ? '已关注作者' : '已取消关注')
  } catch (_) {}
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
  articleScroller.value?.addEventListener('scroll', updateReadingProgress, { passive: true })
  window.addEventListener('keydown', handlePageKeydown)
})
onUnmounted(() => {
  cancelAnimationFrame(commentOpenFrame)
  headingObserver?.disconnect()
  articleScroller.value?.removeEventListener('scroll', updateReadingProgress)
  window.removeEventListener('keydown', handlePageKeydown)
})
</script>

<style scoped>
.article-page {
  --comment-drawer-width: clamp(400px, 31vw, 468px);
  --comment-content-shift: clamp(200px, 15.5vw, 234px);
  --comment-motion-duration: .32s;
  --comment-motion-ease: cubic-bezier(.2, .75, .25, 1);
  position: relative;
}

.article-shell {
  margin: 0 auto;
  transform: translate3d(0, 0, 0);
  transition: transform var(--comment-motion-duration) var(--comment-motion-ease);
  will-change: transform;
}

@media (min-width: 1100px) {
  .article-page.has-comments .article-shell {
    transform: translate3d(calc(0px - var(--comment-content-shift)), 0, 0);
  }
  .article-page.has-comments .article-sidebar {
    visibility: hidden;
    opacity: 0;
    pointer-events: none;
    transform: translate3d(-24px, 0, 0);
    transition:
      opacity .15s ease,
      transform .22s var(--comment-motion-ease),
      visibility 0s linear .22s;
  }
}

.comment-drawer {
  position: fixed;
  z-index: 140;
  top: var(--nav-height);
  right: 0;
  bottom: 0;
  width: var(--comment-drawer-width);
  overflow: hidden;
  border-left: 1px solid var(--c-border);
  background: var(--c-surface);
  box-shadow: -14px 0 36px rgba(15, 23, 42, .1);
  pointer-events: none;
  transform: translate3d(100%, 0, 0);
  transition: transform var(--comment-motion-duration) var(--comment-motion-ease);
  will-change: transform;
}
.comment-drawer.is-open {
  pointer-events: auto;
  transform: translate3d(0, 0, 0);
}
.comment-drawer-backdrop { display: none; }

.article-layout {
  display: grid;
  height: 100%;
  min-height: 0;
  grid-template-areas: "toc article actions";
  grid-template-columns: 270px minmax(0, 880px) 72px;
  gap: 20px;
  align-items: stretch;
  justify-content: center;
}

.article-col {
  grid-area: article;
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
/* Side rails belong to the viewport; only the article column scrolls. */
.article-actions { position: static; padding-top: 66px; }
.article-sidebar {
  position: static;
  min-width: 0;
  padding: 24px 3px 48px;
  opacity: 1;
  transform: translate3d(0, 0, 0);
  transition:
    opacity .16s ease .08s,
    transform .24s var(--comment-motion-ease) .04s,
    visibility 0s linear;
}

@media (max-width: 1080px) { .article-layout { grid-template-columns: 64px minmax(0, 880px); }.article-sidebar { display: none; } }
@media (max-width: 720px) { .article-page { overflow-y: auto; }.article-shell, .article-layout { height: auto; }.article-layout { grid-template-columns: 1fr; }.article-col { overflow: visible; padding-top: 12px; }.article-actions { z-index: 2; align-items: stretch; flex-direction: row; padding: 12px 0 0; overflow-x: auto; }.article-actions button { min-width: 58px; flex: 1; }.article-inner { padding: 24px 20px 36px; }.article-title { font-size: 28px; }.article-meta { align-items: flex-start; flex-direction: column; gap: 12px; } }

/* Technical reading workspace */
.article-main { border: 0; border-inline: 1px solid var(--c-border); border-radius: 0; background: var(--c-surface); box-shadow: none; }
.article-category { margin-bottom: 14px; padding: 4px 9px; border: 0; border-radius: 5px; background: var(--c-primary-soft); color: var(--c-primary); font-family: ui-monospace, Consolas, monospace; letter-spacing: .02em; }
.article-title { font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif; font-weight: 850; line-height: 1.22; }
.article-summary { font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif; }
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
.article-actions button:first-child.active { border-color: color-mix(in srgb, var(--c-danger) 42%, var(--c-border)); background: var(--c-danger-soft); color: var(--c-danger); }
.article-actions button:disabled { cursor: wait; opacity: .7; }
.article-sidebar { gap: 14px; }
.toc-card, .author-card { padding: 18px; border: 1px solid var(--c-border); border-radius: var(--radius-lg); background: var(--c-surface); box-shadow: var(--shadow-xs); }
.toc-card h2, .author-card h2 { font-family: Inter, "PingFang SC", sans-serif; font-size: 16px; }
.toc-card h2::before, .author-card h2::before { display: none; }
.toc-card button.active::before { background: var(--c-primary); }

/* Reading layout: catalogue stays on the left, article actions stay on the right. */
.article-actions { grid-area: actions; }
.article-sidebar { grid-area: toc; }
.author-follow { width: 100%; margin-top: 14px; }
.author-follow.active { background: var(--c-surface-2); color: var(--c-text-3); }

@media (max-width: 1240px) {
  .article-layout { grid-template-columns: 230px minmax(0, 820px) 64px; gap: 14px; }
}
@media (max-width: 1080px) {
  .article-layout { grid-template-areas: "article actions"; grid-template-columns: minmax(0, 880px) 64px; }
}
@media (max-width: 720px) {
  .article-page { --comment-drawer-width: 100%; }
  .article-layout { grid-template-areas: "actions" "article"; grid-template-columns: 1fr; }
  .comment-drawer {
    top: auto;
    width: 100%;
    height: min(84dvh, 760px);
    border-top: 1px solid var(--c-border);
    border-left: 0;
    border-radius: 18px 18px 0 0;
    box-shadow: 0 -16px 36px rgba(15, 23, 42, .14);
    transform: translate3d(0, 100%, 0);
  }
  .comment-drawer::after {
    position: absolute;
    z-index: 5;
    top: 7px;
    left: 50%;
    width: 38px;
    height: 4px;
    border-radius: 4px;
    background: var(--c-border-strong);
    content: '';
    opacity: .75;
    transform: translateX(-50%);
  }
  .comment-drawer.is-open { transform: translate3d(0, 0, 0); }
  .comment-drawer-backdrop {
    position: fixed;
    z-index: 139;
    inset: var(--nav-height) 0 0;
    display: block;
    border: 0;
    background: rgba(15, 23, 42, .34);
  }
  .comment-backdrop-enter-active, .comment-backdrop-leave-active { transition: opacity .22s ease; }
  .comment-backdrop-enter-from, .comment-backdrop-leave-to { opacity: 0; }
}

@media (prefers-reduced-motion: reduce) {
  .article-shell,
  .article-sidebar,
  .comment-drawer,
  .comment-drawer-backdrop { transition-duration: .01ms !important; transition-delay: 0s !important; }
}
</style>

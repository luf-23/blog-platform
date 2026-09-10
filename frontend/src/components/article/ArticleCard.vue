<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { DEFAULT_ARTICLE_COVER_URL as defaultCover, DEFAULT_AVATAR_URL as defaultAvatar } from '../../constants/assets.js'

const props = defineProps({ article: { type: Object, required: true } })
defineEmits(['tag-click', 'author-click'])

const router = useRouter()
const coverFailed = ref(false)
const readingMinutes = computed(() => {
  const contentLength = props.article.content?.length || props.article.summary?.length * 4 || 800
  return Math.max(3, Math.ceil(contentLength / 400))
})

function gotoDetail() {
  router.push('/article/' + props.article.articleId)
}

function formatDate(time) {
  if (!time) return ''
  const date = new Date(time)
  const diff = Date.now() - date.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + ' 天前'
  return date.toLocaleDateString('zh-CN')
}

function formatCount(value) {
  const number = Number(value || 0)
  if (number >= 10000) return (number / 10000).toFixed(1) + '万'
  if (number >= 1000) return (number / 1000).toFixed(1) + 'k'
  return String(number)
}
</script>

<template>
  <article class="article-card surface-card" tabindex="0" @click="gotoDetail" @keyup.enter="gotoDetail">
    <div class="article-card__cover">
      <img :src="!coverFailed && article.coverImage ? article.coverImage : defaultCover" :alt="`${article.title}封面`" loading="lazy" @error="coverFailed = true" />
    </div>

    <div class="article-card__body">
      <div class="article-card__topline">
        <button class="article-card__author" @click.stop="$emit('author-click', { userId: article.userId, username: article.authorUsername })">
          <img :src="article.authorAvatar || defaultAvatar" :alt="article.authorNickname || article.authorUsername" />
          <span>{{ article.authorNickname || article.authorUsername || '平台作者' }}</span>
        </button>
        <span>·</span>
        <time>{{ formatDate(article.createTime || article.updateTime) }}</time>
        <span v-if="article.status && article.status !== 'published'" class="status" :class="'status--' + article.status">
          {{ article.status === 'draft' ? '草稿' : '审核中' }}
        </span>
      </div>

      <h2>{{ article.title }}</h2>
      <p>{{ article.summary || '作者还没有填写摘要，点击阅读全文。' }}</p>

      <div class="article-card__tags">
        <button v-if="article.categoryName" @click.stop>{{ article.categoryName }}</button>
        <button v-for="tag in (article.tags || []).slice(0, 3)" :key="tag.tagId" @click.stop="$emit('tag-click', tag)"># {{ tag.tagName }}</button>
      </div>

      <footer>
        <div class="article-card__stats">
          <span>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12Z"/><circle cx="12" cy="12" r="3"/></svg>
            {{ formatCount(article.viewCount) }}
          </span>
          <span>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m12 21-1.5-1.3C5.1 15 2 12.2 2 8.8A4.8 4.8 0 0 1 6.9 4 5.3 5.3 0 0 1 12 7a5.3 5.3 0 0 1 5.1-3A4.8 4.8 0 0 1 22 8.8c0 3.4-3.1 6.2-8.5 10.9Z"/></svg>
            {{ formatCount(article.likeCount) }}
          </span>
          <span>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a3 3 0 0 1-3 3H8l-5 3V6a3 3 0 0 1 3-3h12a3 3 0 0 1 3 3Z"/></svg>
            {{ formatCount(article.commentCount) }}
          </span>
          <span>{{ readingMinutes }} 分钟阅读</span>
        </div>
      </footer>
    </div>
  </article>
</template>

<style scoped>
.article-card { display: grid; min-height: 214px; grid-template-columns: 252px minmax(0, 1fr); overflow: hidden; border: 0; border-bottom: 1px solid var(--c-border-strong); border-radius: 0; background: transparent; box-shadow: none; cursor: pointer; outline: none; transition: background var(--transition); }
.article-card:hover, .article-card:focus-visible { background: color-mix(in srgb, var(--c-surface) 52%, transparent); }
.article-card__cover { min-height: 214px; margin: 20px 0; overflow: hidden; background: var(--c-surface-2); }
.article-card__cover img { width: 100%; height: 100%; object-fit: cover; filter: saturate(.82) contrast(1.02); transition: transform .45s ease, filter .45s ease; }
.article-card:hover .article-card__cover img { filter: saturate(1) contrast(1.02); transform: scale(1.025); }
.article-card__cover--fallback { position: relative; display: grid; place-items: center; border: 1px solid var(--c-border-strong); background: var(--c-surface-2); }
.article-card__cover--fallback span { position: relative; z-index: 1; color: var(--c-text); font-family: ui-monospace, Consolas, monospace; font-size: 26px; font-weight: 900; letter-spacing: .1em; }
.article-card__cover--fallback i, .article-card__cover--fallback b { position: absolute; width: 72%; height: 1px; background: var(--c-border-strong); content: ''; transform: rotate(-26deg); }
.article-card__cover--fallback b { transform: rotate(26deg); }
.article-card__body { position: relative; display: flex; min-width: 0; flex-direction: column; padding: 20px 6px 18px 28px; }
.article-card__index { position: absolute; top: 17px; right: 5px; color: var(--c-border-strong); font-family: Georgia, serif; font-size: 32px; font-weight: 700; font-style: italic; line-height: 1; }
.article-card__topline { display: flex; align-items: center; gap: 7px; padding-right: 52px; color: var(--c-text-4); font-size: 11px; }
.article-card__author { display: inline-flex; align-items: center; gap: 7px; padding: 0; border: 0; background: transparent; color: var(--c-text-2); font-weight: 700; }
.article-card__author img { width: 24px; height: 24px; border-radius: 50%; object-fit: cover; filter: grayscale(.2); }
.article-card__author:hover { color: var(--c-primary); }
.article-card__body h2 { margin: 13px 0 7px; overflow: hidden; color: var(--c-text); font-family: inherit; font-size: 22px; font-weight: 900; letter-spacing: -.015em; line-height: 1.35; text-overflow: ellipsis; white-space: nowrap; transition: color var(--transition); }
.article-card:hover h2 { color: var(--c-primary); }
.article-card__body > p { display: -webkit-box; overflow: hidden; color: var(--c-text-3); font-family: inherit; font-size: 13px; line-height: 1.75; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.article-card__tags { display: flex; gap: 12px; margin-top: 9px; }
.article-card__tags button { padding: 1px 0; border: 0; border-bottom: 1px solid var(--c-border); border-radius: 0; background: transparent; color: var(--c-text-3); font-size: 10px; }
.article-card__tags button:first-child { color: #a33d2d; }
.article-card__tags button:hover { border-color: var(--c-primary); color: var(--c-primary); }
.article-card footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-top: auto; padding-top: 11px; }
.article-card__stats { display: flex; align-items: center; gap: 14px; color: var(--c-text-4); font-size: 10px; }
.article-card__stats span { display: inline-flex; align-items: center; gap: 4px; }
.article-card__stats svg { width: 14px; height: 14px; }
.bookmark { display: inline-flex; align-items: center; gap: 5px; padding: 4px 0; border: 0; border-bottom: 1px solid transparent; border-radius: 0; background: transparent; color: var(--c-text-3); font-size: 11px; }
.bookmark:hover, .bookmark.active { border-color: var(--c-primary); color: var(--c-primary); }
.bookmark svg { width: 15px; height: 15px; }
.status { margin-left: auto; padding: 2px 7px; border-radius: 2px; font-weight: 700; }.status--draft { background: var(--c-surface-3); }.status--pending { background: var(--c-warning-soft); color: var(--c-warning); }

@media (max-width: 700px) {
  .article-card { grid-template-columns: 1fr; }
  .article-card__cover { height: 190px; min-height: 0; margin-bottom: 0; }
  .article-card__body { padding-left: 0; }
  .article-card__body h2 { white-space: normal; }
}
@media (max-width: 480px) {
  .article-card__stats span:nth-child(3), .article-card__stats span:last-child, .bookmark span { display: none; }
}

/* Compact technical content card */
.article-card { min-height: 202px; grid-template-columns: 224px minmax(0, 1fr); border: 1px solid var(--c-border); border-radius: var(--radius-lg); background: var(--c-surface); box-shadow: var(--shadow-xs); }
.article-card:hover, .article-card:focus-visible { border-color: #bfdbfe; background: var(--c-surface); box-shadow: var(--shadow-sm); transform: translateY(-1px); }
.article-card__cover { min-height: 200px; margin: 0; }
.article-card__cover img { filter: none; }
.article-card__cover--fallback { border: 0; border-right: 1px solid rgba(148,163,184,.18); background-color: #0f172a; background-image: linear-gradient(rgba(96,165,250,.08) 1px, transparent 1px), linear-gradient(90deg, rgba(96,165,250,.08) 1px, transparent 1px); background-size: 22px 22px; }
.article-card__cover--fallback::before { position: absolute; top: 22px; right: 20px; color: rgba(147,197,253,.52); font-family: ui-monospace, Consolas, monospace; font-size: 12px; content: '</>'; }
.article-card__cover--fallback::after { position: absolute; right: 20px; bottom: 20px; left: 20px; height: 34px; border-top: 1px solid rgba(148,163,184,.25); border-bottom: 1px solid rgba(148,163,184,.14); content: ''; }
.article-card__cover--fallback span { padding: 8px 12px; border: 1px solid rgba(147,197,253,.42); border-radius: 6px; color: #dbeafe; font-family: ui-monospace, "SFMono-Regular", Consolas, monospace; font-size: 16px; font-weight: 800; letter-spacing: .04em; }
.article-card__cover--fallback i, .article-card__cover--fallback b { display: none; }
.article-card__body { padding: 19px 20px 16px; }
.article-card__index { display: none; }
.article-card__topline { padding-right: 0; font-size: 11px; }
.article-card__body h2 { margin: 11px 0 6px; font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif; font-size: 19px; font-weight: 800; letter-spacing: -.015em; }
.article-card__body > p { font-family: inherit; font-size: 12px; line-height: 1.7; }
.article-card__tags { gap: 6px; margin-top: 9px; }
.article-card__tags button { padding: 3px 7px; border: 1px solid var(--c-border); border-radius: 4px; background: var(--c-surface-2); color: var(--c-text-3); font-family: ui-monospace, Consolas, monospace; }
.article-card__tags button:first-child { color: var(--c-primary); }
.article-card__tags button:hover { border-color: #93c5fd; background: var(--c-primary-soft); }
.bookmark { padding: 4px 6px; border: 0; border-radius: 5px; }
.bookmark:hover, .bookmark.active { border: 0; background: var(--c-primary-soft); }

@media (max-width: 700px) { .article-card { grid-template-columns: 1fr; }.article-card__cover { height: 178px; min-height: 0; }.article-card__body { padding: 17px; } }
</style>

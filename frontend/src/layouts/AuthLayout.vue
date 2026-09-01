<script setup>
import BrandMark from '../components/common/BrandMark.vue'
</script>

<template>
  <div class="auth-layout">
    <section class="auth-story">
      <router-link to="/home" class="auth-story__brand"><BrandMark /></router-link>
      <div class="auth-story__copy">
        <p class="eyebrow">BLOG PLATFORM</p>
        <h1>写作、发布<br />与交流</h1>
        <p>支持 Markdown 文章、分类、标签、评论和作者关注。</p>
      </div>
      <div class="story-canvas" aria-hidden="true">
        <span class="ink-line ink-line--one"></span>
        <span class="ink-line ink-line--two"></span>
        <article class="story-card story-card--main">
          <div class="story-image"><i></i><b></b></div>
          <div><small>Markdown</small><strong>文章编辑与预览</strong><p>支持代码块、图片和常用 Markdown 格式。</p><footer>草稿 · 审核 · 发布</footer></div>
        </article>
        <article class="story-card story-card--quote"><b>↩</b><span>评论与回复</span><small>文章讨论</small></article>
        <article class="story-card story-card--saved"><b>✎</b><span>自动保存<small>新文章草稿保存在本地</small></span></article>
        <article class="story-card story-card--topic"><b>#</b><span>分类与标签<small>用于整理和筛选文章</small></span><em>›</em></article>
        <svg class="story-feather" viewBox="0 0 170 250" fill="none">
          <path d="M28 233C52 163 84 94 151 16M45 189c25-2 52-14 79-39M64 146c-17-3-30-10-42-21M85 105c18-2 36-10 53-25M109 66c-11-3-20-8-29-17" stroke="currentColor" stroke-width="5" stroke-linecap="round"/>
          <path d="M151 16c-41 16-65 45-72 85 30-8 53-27 72-85ZM76 108c-31 4-48 18-54 42 25 3 43-11 54-42ZM65 151c-29 12-41 30-36 54 24-4 37-22 36-54Z" fill="currentColor" opacity=".78"/>
        </svg>
      </div>
      <p class="auth-story__foot">© 2026 Blog-Platform</p>
    </section>

    <section class="auth-panel">
      <div class="auth-panel__mobile-brand"><BrandMark /></div>
      <div class="auth-card surface-card"><router-view /></div>
      <div class="auth-legal">密码经加密后存储</div>
    </section>
  </div>
</template>

<style scoped>
.auth-layout { display: grid; width: 100%; height: 100dvh; min-height: 0; grid-template-columns: minmax(480px, 1.12fr) minmax(480px, .88fr); overflow: hidden; background: #fbfaf7; color: #161b2a; }
.auth-story { position: relative; height: 100%; min-height: 0; overflow: hidden; padding: 38px clamp(44px, 6vw, 94px); background: radial-gradient(circle at 72% 34%, #f1efff 0, transparent 31%), radial-gradient(circle at 18% 76%, #eef9f7 0, transparent 26%), #fbfaf7; }
.auth-story__brand { position: relative; z-index: 4; }
.auth-story__copy { position: relative; z-index: 2; margin-top: clamp(76px, 11vh, 130px); }.auth-story__copy h1 { margin: 8px 0 12px; font-family: "Songti SC", "STSong", serif; font-size: clamp(44px, 5vw, 72px); line-height: 1.18; letter-spacing: .08em; }.auth-story__copy > p:last-child { color: #737c8e; font-size: 17px; }
.story-canvas { position: absolute; right: 6%; bottom: 8%; width: 72%; height: 48%; color: #1f2d58; }
.story-card { position: absolute; z-index: 2; border: 1px solid rgba(81,70,229,.1); border-radius: 6px; background: rgba(255,255,255,.89); box-shadow: 0 12px 28px rgba(34,45,73,.06); backdrop-filter: blur(6px); }
.story-card--main { right: 5%; bottom: 12%; display: grid; width: min(480px, 70%); grid-template-columns: 120px 1fr; gap: 14px; padding: 16px; transform: rotate(3deg); }.story-card--main strong { display: block; margin: 4px 0; font-size: 16px; }.story-card--main small { color: #5146e5; }.story-card--main p, .story-card--main footer { color: #7d8696; font-size: 11px; }.story-card--main footer { margin-top: 10px; }
.story-image { position: relative; overflow: hidden; border-radius: 4px; background: linear-gradient(145deg, #dcd9ff, #b8d7df); }.story-image i, .story-image b { position: absolute; right: -8px; bottom: -30px; width: 120px; height: 75px; border-radius: 60% 0 0; background: #6e77b7; transform: rotate(-9deg); }.story-image b { right: 43px; bottom: -42px; background: #748fa0; }
.story-card--quote { top: 2%; right: 1%; width: 235px; padding: 14px 16px; transform: rotate(-2deg); }.story-card--quote > b { color: #9c97ef; font-size: 22px; }.story-card--quote span { display: block; color: #475064; font-size: 12px; }.story-card--quote small { display: block; margin-top: 8px; color: #7d8696; font-size: 10px; }
.story-card--saved { left: 20%; bottom: 0; display: flex; align-items: center; gap: 10px; padding: 12px 18px; transform: rotate(2deg); }.story-card--saved > b { color: #109b9a; font-size: 24px; }.story-card--saved span, .story-card--topic span { display: flex; flex-direction: column; font-size: 12px; font-weight: 700; }.story-card--saved small, .story-card--topic small { color: #8d95a4; font-size: 9px; font-weight: 400; }
.story-card--topic { right: -2%; bottom: -6%; display: flex; align-items: center; gap: 10px; min-width: 220px; padding: 13px 16px; transform: rotate(-4deg); }.story-card--topic > b { color: #5146e5; font-size: 22px; }.story-card--topic em { margin-left: auto; color: #9ba4b3; font-style: normal; }
.story-feather { position: absolute; bottom: -26px; left: -22px; z-index: 1; width: 170px; color: #2f3b6c; transform: rotate(-14deg); opacity: .9; }
.ink-line { position: absolute; z-index: 0; border: 2px solid rgba(81,70,229,.12); border-radius: 50%; }.ink-line--one { right: 5%; bottom: 0; width: 520px; height: 240px; transform: rotate(15deg); }.ink-line--two { right: 15%; bottom: 5%; width: 430px; height: 180px; transform: rotate(-12deg); }
.auth-story__foot { position: absolute; bottom: 24px; left: clamp(44px, 6vw, 94px); color: #9ba4b3; font-size: 11px; }
.auth-panel { display: flex; height: 100%; min-height: 0; align-items: center; justify-content: center; flex-direction: column; padding: 40px; overflow-x: hidden; overflow-y: auto; overscroll-behavior: contain; background: #f7f8fa; }
.auth-card { width: min(500px, 100%); padding: 42px 46px; border-radius: 8px; box-shadow: 0 14px 34px rgba(23, 31, 54, .07); }
.auth-panel__mobile-brand { display: none; margin-bottom: 24px; }.auth-legal { display: flex; gap: 12px; margin-top: 24px; color: #8c95a5; font-size: 12px; }

@media (max-width: 980px) { .auth-layout { grid-template-columns: 1fr; }.auth-story { display: none; }.auth-panel { min-height: 100%; }.auth-panel__mobile-brand { display: block; } }
@media (max-width: 520px) { .auth-panel { padding: 20px 14px; }.auth-card { padding: 30px 22px; } }
</style>

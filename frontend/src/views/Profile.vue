<template>
  <div class="profile-page page-shell">
    <!-- Banner -->
    <div class="profile-banner" :style="bannerStyle">
      <div class="banner-overlay"></div>
    </div>

    <div class="page-container">
      <!-- User card -->
      <div class="profile-card card">
        <div class="profile-card-left">
          <div class="avatar-wrap">
            <img :src="profileUser?.avatarImage || defaultAvatar" class="profile-avatar" alt="头像"/>
            <button v-if="isMe" class="avatar-edit-btn" @click="showEditDialog = true">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            </button>
          </div>
          <div class="profile-info">
            <h1 class="profile-name">{{ profileUser?.nickname || profileUser?.username }}</h1>
            <p class="profile-username">@{{ profileUser?.username }}</p>
            <p v-if="profileUser?.signature" class="profile-bio">{{ profileUser.signature }}</p>
            <div class="profile-meta">
              <span v-if="profileUser?.email" class="profile-meta-item">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
                {{ profileUser.email }}
              </span>
              <span class="profile-meta-item">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                {{ formatDate(profileUser?.createTime) }} 加入
              </span>
            </div>
          </div>
        </div>
        <div v-if="isMe" class="profile-card-right">
          <button class="btn btn-secondary" @click="showEditDialog = true">编辑资料</button>
          <button class="btn btn-ghost" @click="shareProfile">分享主页</button>
        </div>
      </div>

      <section class="profile-stats surface-card">
        <p><i>▤</i><span>文章<strong>{{ articles.length }}</strong></span></p>
        <p><i>◉</i><span>总阅读<strong>{{ formatCount(profileStats.views) }}</strong></span></p>
        <p><i>♡</i><span>获赞<strong>{{ formatCount(profileStats.likes) }}</strong></span></p>
        <p><i>▢</i><span>评论<strong>{{ formatCount(profileStats.comments) }}</strong></span></p>
      </section>

      <nav class="profile-tabs">
        <button>主页</button><button class="active">文章</button><button v-if="isMe" @click="$router.push('/article/my')">草稿</button><button>收藏</button><button>关于</button>
      </nav>

      <div class="profile-layout">
        <div class="profile-content">
          <div class="section-header">
            <div><h2 class="section-title">发布的文章</h2><span class="section-count">{{ articles.length }} 篇</span></div>
            <el-input placeholder="搜索他的文章" style="width:220px" clearable />
          </div>

          <div v-if="loading" class="loading-spinner"><el-icon class="is-loading" :size="20"><Loading /></el-icon></div>
          <div v-else-if="articles.length === 0" class="empty-state surface-card"><p>还没有发布文章</p></div>
          <div v-else class="article-list"><ArticleCard v-for="a in articles" :key="a.articleId" :article="a"/></div>
        </div>

        <aside class="profile-aside">
          <section class="surface-card trend-card">
            <header><h3>创作趋势</h3><span>最近 30 天</span></header>
            <div class="mini-chart"><i v-for="(height, index) in trendBars" :key="index" :style="{ height: height + '%' }"></i></div>
            <p><span>阅读量</span><strong>较上月 +18.6%</strong></p>
          </section>
          <section class="surface-card category-card">
            <header><h3>内容分类</h3><router-link v-if="isMe" to="/article/categories">管理分类</router-link></header>
            <p v-for="category in profileCategories" :key="category.name"><span>▱ {{ category.name }}</span><strong>{{ category.count }}</strong></p>
            <p v-if="!profileCategories.length" class="muted">暂无分类</p>
          </section>
          <section v-if="isMe" class="surface-card quick-card">
            <h3>快捷操作</h3>
            <router-link to="/article/write" class="btn btn-primary">✎ 写文章</router-link>
            <router-link to="/article/categories" class="btn btn-secondary">▱ 新建分类</router-link>
            <button class="btn btn-secondary" @click="showEditDialog = true">♙ 编辑资料</button>
          </section>
        </aside>
      </div>
    </div>

    <!-- Edit dialog -->
    <el-dialog v-model="showEditDialog" title="编辑个人资料" width="520px" :close-on-click-modal="false">
      <el-form ref="editFormRef" :model="editForm" label-position="top">
        <el-form-item label="头像 URL">
          <div class="avatar-preview-row">
            <img :src="editForm.avatarImage || defaultAvatar" class="avatar" style="width:56px;height:56px"/>
            <el-input v-model="editForm.avatarImage" placeholder="输入头像图片 URL" style="flex:1"/>
          </div>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" maxlength="50" show-word-limit/>
        </el-form-item>
        <el-form-item label="个性签名">
          <el-input v-model="editForm.signature" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="介绍一下你自己..."/>
        </el-form-item>
        <el-form-item label="背景图 URL">
          <el-input v-model="editForm.backgroundImage" placeholder="输入背景图片 URL"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingEdit" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getUserInfoByNameService, updateUserInfoService, getUserInfoService } from '../api/user.js'
import { searchArticlesService } from '../api/article.js'
import { useUserInfoStore } from '../store/userInfo.js'
import ArticleCard from '../components/article/ArticleCard.vue'

const route = useRoute()
const userInfoStore = useUserInfoStore()

const profileUser = ref(null)
const articles = ref([])
const loading = ref(false)
const showEditDialog = ref(false)
const savingEdit = ref(false)
const editFormRef = ref()

const defaultAvatar = '/avatar/avatar1.png'
const trendBars = [34, 46, 52, 38, 68, 56, 74, 48, 62, 81, 70, 88]

const isMe = computed(() => {
  const u = userInfoStore.userInfo
  if (!u || !profileUser.value) return false
  return u.userId === profileUser.value.userId
})

const bannerStyle = computed(() => {
  const img = profileUser.value?.backgroundImage
  return img ? { backgroundImage: `url(${img})` } : {}
})
const profileStats = computed(() => ({
  views: articles.value.reduce((sum, article) => sum + (article.viewCount || 0), 0),
  likes: articles.value.reduce((sum, article) => sum + (article.likeCount || 0), 0),
  comments: articles.value.reduce((sum, article) => sum + (article.commentCount || 0), 0)
}))
const profileCategories = computed(() => {
  const counts = new Map()
  articles.value.forEach(article => {
    const name = article.categoryName || '未分类'
    counts.set(name, (counts.get(name) || 0) + 1)
  })
  return [...counts.entries()].map(([name, count]) => ({ name, count })).slice(0, 6)
})

const editForm = reactive({
  nickname: '',
  signature: '',
  avatarImage: '',
  backgroundImage: ''
})

async function loadProfile() {
  const username = route.params.username
  loading.value = true
  try {
    if (username) {
      const res = await getUserInfoByNameService({ username })
      profileUser.value = res.data
    } else {
      const res = await getUserInfoService()
      profileUser.value = res.data
    }
    if (profileUser.value) {
      loadArticles()
      if (isMe.value) {
        editForm.nickname = profileUser.value.nickname || ''
        editForm.signature = profileUser.value.signature || ''
        editForm.avatarImage = profileUser.value.avatarImage || ''
        editForm.backgroundImage = profileUser.value.backgroundImage || ''
      }
    }
  } finally { loading.value = false }
}

async function loadArticles() {
  try {
    const res = await searchArticlesService({ authorId: profileUser.value.userId, pageSize: 20 })
    articles.value = res.data.list || []
  } catch {}
}

async function saveProfile() {
  savingEdit.value = true
  try {
    await updateUserInfoService({
      nickname: editForm.nickname,
      signature: editForm.signature,
      avatarImage: editForm.avatarImage,
      backgroundImage: editForm.backgroundImage
    })
    const infoRes = await getUserInfoService()
    userInfoStore.setUserInfo(infoRes.data)
    profileUser.value = infoRes.data
    ElMessage.success('资料已更新')
    showEditDialog.value = false
  } finally { savingEdit.value = false }
}

function formatDate(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}`
}

function formatCount(value) {
  const number = Number(value || 0)
  if (number >= 10000) return (number / 10000).toFixed(1) + '万'
  if (number >= 1000) return (number / 1000).toFixed(1) + 'k'
  return String(number)
}

async function shareProfile() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    ElMessage.success('主页链接已复制')
  } catch {}
}

onMounted(loadProfile)
watch(() => route.params.username, loadProfile)
</script>

<style scoped>
.profile-page { padding-top: 0; }

.profile-banner {
  height: 220px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  background-size: cover;
  background-position: center;
  position: relative;
}

.banner-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, transparent 60%, rgba(0,0,0,0.3));
}

.profile-card {
  margin-top: -50px;
  padding: 24px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
  position: relative;
}

.profile-card-left {
  display: flex;
  align-items: flex-end;
  gap: 20px;
}
.profile-card-right { display: flex; gap: 7px; }

.avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.profile-avatar {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  border: 4px solid var(--c-surface);
  object-fit: cover;
  background: var(--c-border);
  margin-top: -44px;
}

.avatar-edit-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--c-primary);
  color: white;
  border: 2px solid var(--c-surface);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.profile-name {
  font-size: 22px;
  font-weight: 800;
  color: var(--c-text);
  margin-bottom: 2px;
}

.profile-username {
  font-size: 14px;
  color: var(--c-text-3);
  margin-bottom: 6px;
}

.profile-bio {
  font-size: 14px;
  color: var(--c-text-2);
  margin-bottom: 8px;
  max-width: 400px;
}

.profile-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.profile-meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: var(--c-text-3);
}

/* Content */
.profile-stats { display: grid; grid-template-columns: repeat(4, 1fr); margin-bottom: 14px; padding: 16px 22px; }
.profile-stats p { display: flex; align-items: center; justify-content: center; gap: 12px; border-right: 1px solid var(--c-border); }
.profile-stats p:last-child { border: 0; }
.profile-stats i { color: var(--c-primary); font-size: 23px; font-style: normal; }
.profile-stats span { display: flex; flex-direction: column; color: var(--c-text-3); font-size: 11px; }
.profile-stats strong { color: var(--c-text); font-size: 21px; }
.profile-tabs { display: flex; gap: 18px; margin-bottom: 18px; border-bottom: 1px solid var(--c-border); }
.profile-tabs button { position: relative; padding: 11px 12px 13px; border: 0; background: transparent; color: var(--c-text-3); font-weight: 600; }
.profile-tabs button.active { color: var(--c-primary); }
.profile-tabs button.active::after { position: absolute; right: 8px; bottom: -1px; left: 8px; height: 3px; border-radius: 3px 3px 0 0; background: var(--c-primary); content: ''; }
.profile-layout { display: grid; grid-template-columns: minmax(0, 1fr) 310px; gap: 18px; align-items: start; }
.profile-content { min-width: 0; }

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}
.section-header > div { display: flex; align-items: center; gap: 10px; }
.section-title { font-size: 18px; font-weight: 700; color: var(--c-text); }
.section-count {
  background: var(--c-surface-2);
  color: var(--c-text-3);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: var(--c-text-4);
  font-size: 14px;
}

.article-list { display: flex; flex-direction: column; gap: 12px; }
.profile-aside { position: sticky; top: 18px; display: flex; max-height: calc(100dvh - var(--nav-height) - 36px); flex-direction: column; gap: 12px; padding-right: 2px; overflow-y: auto; overscroll-behavior: contain; scrollbar-gutter: stable; }
.profile-aside section { padding: 17px; }
.profile-aside header { display: flex; align-items: center; justify-content: space-between; }
.profile-aside h3 { font-size: 15px; }
.profile-aside header span, .profile-aside header a { color: var(--c-text-4); font-size: 10px; }
.mini-chart { display: flex; height: 110px; align-items: flex-end; gap: 6px; padding: 16px 2px 8px; border-bottom: 1px solid var(--c-border); background: repeating-linear-gradient(to bottom, transparent 0, transparent 26px, var(--c-border) 27px); }
.mini-chart i { flex: 1; min-height: 8px; border-radius: 4px 4px 1px 1px; background: linear-gradient(to top, var(--c-primary), #8179ef); }
.trend-card > p { display: flex; justify-content: space-between; margin-top: 10px; color: var(--c-text-3); font-size: 10px; }
.trend-card > p strong { color: var(--c-success); }
.category-card p { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid var(--c-border); color: var(--c-text-2); font-size: 12px; }
.category-card p:last-child { border: 0; }
.category-card p strong { color: var(--c-text-4); }
.quick-card { display: flex; flex-direction: column; gap: 8px; }
.quick-card h3 { margin-bottom: 4px; }
.quick-card .btn { width: 100%; }

/* Avatar preview row */
.avatar-preview-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

@media (max-width: 980px) {
  .profile-layout { grid-template-columns: 1fr; }
  .profile-aside { position: static; display: grid; grid-template-columns: repeat(2, 1fr); }
  .quick-card { grid-column: 1 / -1; }
  .profile-banner { height: 180px; }
}
@media (max-width: 680px) {
  .profile-card, .profile-card-left { align-items: flex-start; flex-direction: column; }
  .profile-card-right { width: 100%; }
  .profile-stats { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .profile-stats p:nth-child(2) { border-right: 0; }
  .profile-tabs { overflow-x: auto; }
  .section-header { align-items: flex-start; flex-direction: column; }
  .section-header :deep(.el-input) { width: 100% !important; }
  .profile-aside { grid-template-columns: 1fr; }
  .quick-card { grid-column: auto; }
}
</style>

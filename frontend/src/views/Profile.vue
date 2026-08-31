<template>
  <div class="profile-page page-shell">
    <!-- Banner -->
    <div class="profile-banner" :style="bannerStyle">
      <div class="banner-overlay"></div>
      <button v-if="isMe" class="banner-edit" @click="beginImageUpload('background')">更换背景图</button>
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
        <div class="profile-card-right">
          <button v-if="isMe" class="btn btn-secondary" @click="showEditDialog = true">编辑资料</button>
          <button v-else class="btn btn-primary" :class="{ following: profileStats.following }" @click="toggleFollow">
            {{ profileStats.following ? '已关注' : '关注' }}
          </button>
          <button class="btn btn-ghost" @click="shareProfile">分享主页</button>
        </div>
      </div>

      <section class="profile-stats surface-card">
        <button type="button" :class="{ active: activeSection === 'articles' }" @click="selectSection('articles')"><i>▤</i><span>文章<strong>{{ profileStats.articleCount }}</strong></span></button>
        <p><i>◉</i><span>总阅读<strong>{{ formatCount(profileStats.views) }}</strong></span></p>
        <p><i>♡</i><span>获赞<strong>{{ formatCount(profileStats.likes) }}</strong></span></p>
        <p><i>▢</i><span>评论<strong>{{ formatCount(profileStats.comments) }}</strong></span></p>
        <button type="button" :class="{ active: activeSection === 'followers' }" @click="selectSection('followers')"><i>◎</i><span>关注者<strong>{{ formatCount(profileStats.followerCount) }}</strong></span></button>
        <button type="button" :class="{ active: activeSection === 'following' }" @click="selectSection('following')"><i>→</i><span>正在关注<strong>{{ formatCount(profileStats.followingCount) }}</strong></span></button>
      </section>

      <nav class="profile-tabs" aria-label="主页内容">
        <button v-for="tab in profileTabs" :key="tab.value" type="button" :class="{ active: activeSection === tab.value }" @click="selectSection(tab.value)">
          {{ tab.label }}<span>{{ tab.count }}</span>
        </button>
      </nav>

      <div class="profile-layout" :class="{ 'profile-layout--wide': activeSection !== 'articles' }">
        <div class="profile-content">
          <div class="section-header">
            <div><h2 class="section-title">{{ sectionTitle }}</h2><span class="section-count">{{ sectionCount }}</span></div>
            <el-input v-if="activeSection === 'articles'" v-model="articleSearch" placeholder="搜索文章" style="width:220px" clearable />
          </div>

          <div v-if="loading || relationshipLoading" class="loading-spinner"><el-icon class="is-loading" :size="20"><Loading /></el-icon></div>
          <template v-else-if="activeSection === 'articles'">
            <div v-if="filteredArticles.length === 0" class="empty-state surface-card"><p>{{ articleSearch ? '没有匹配的文章' : '还没有发布文章' }}</p></div>
            <div v-else class="article-list"><ArticleCard v-for="a in filteredArticles" :key="a.articleId" :article="a"/></div>
          </template>
          <template v-else>
            <div v-if="!relationshipUsers.length" class="empty-state surface-card">
              <p>{{ activeSection === 'followers' ? '暂时还没有关注者' : '暂时还没有关注任何人' }}</p>
            </div>
            <div v-else class="people-list">
              <article v-for="person in relationshipUsers" :key="person.userId" class="person-card surface-card">
                <button type="button" class="person-main" @click="openProfile(person.username)">
                  <img :src="person.avatarImage || defaultAvatar" :alt="person.nickname || person.username" />
                  <span class="person-copy">
                    <strong>{{ person.nickname || person.username }}</strong>
                    <small>@{{ person.username }}</small>
                    <em>{{ person.signature || '这位用户还没有填写个人简介。' }}</em>
                  </span>
                </button>
                <div class="person-meta">
                  <span><strong>{{ person.articleCount || 0 }}</strong> 篇文章</span>
                  <span><strong>{{ formatCount(person.followerCount) }}</strong> 位关注者</span>
                </div>
                <button
                  v-if="Number(person.userId) !== Number(userInfoStore.userInfo?.userId)"
                  type="button"
                  class="person-follow"
                  :class="{ following: person.following }"
                  @click="togglePersonFollow(person)"
                >{{ person.following ? '已关注' : '关注' }}</button>
              </article>
              <button v-if="relationshipHasMore" type="button" class="load-people" @click="loadMoreRelationships">加载更多</button>
            </div>
          </template>
        </div>

        <aside v-if="activeSection === 'articles'" class="profile-aside">
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
        <el-form-item label="头像">
          <div class="avatar-preview-row">
            <img :src="editForm.avatarImage || defaultAvatar" class="avatar" style="width:56px;height:56px"/>
            <div class="image-field">
              <button type="button" class="btn btn-secondary btn-sm" @click="beginImageUpload('avatar')">更换头像</button>
              <small>支持 JPG、PNG、WEBP，图片大小不超过 5MB</small>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" maxlength="50" show-word-limit/>
        </el-form-item>
        <el-form-item label="个性签名">
          <el-input v-model="editForm.signature" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="介绍一下你自己..."/>
        </el-form-item>
        <el-form-item label="背景图">
          <div class="image-field image-field--wide">
            <button type="button" class="btn btn-secondary btn-sm" @click="beginImageUpload('background')">更换背景图</button>
            <small>建议选择横向图片，效果更自然</small>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingEdit" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <UploadImageDialog
      v-model:visible="uploadDialogVisible"
      :title="uploadTarget === 'avatar' ? '选择头像' : '选择背景图'"
      :loading="uploadingImage"
      hint="支持 JPG、PNG、WEBP，图片大小不超过 5MB"
      @confirm="uploadProfileImage"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getUserInfoByNameService, updateUserInfoService, getUserInfoService } from '../api/user.js'
import { searchArticlesService } from '../api/article.js'
import {
  getCommunityFollowStateService,
  getCommunityProfileFollowersService,
  getCommunityProfileFollowingService,
  getCommunityProfileService,
  toggleCommunityFollowService
} from '../api/community.js'
import { useUserInfoStore } from '../store/userInfo.js'
import ArticleCard from '../components/article/ArticleCard.vue'
import UploadImageDialog from '../components/common/UploadImageDialog.vue'
import { OSSClient, uploadImageToOss } from '../utils/oss/index.js'
import { DEFAULT_AVATAR_URL as defaultAvatar, DEFAULT_PROFILE_BACKGROUND_URL as defaultBackground } from '../constants/assets.js'

const route = useRoute()
const router = useRouter()
const userInfoStore = useUserInfoStore()

const profileUser = ref(null)
const articles = ref([])
const loading = ref(false)
const showEditDialog = ref(false)
const savingEdit = ref(false)
const editFormRef = ref()
const articleSearch = ref('')
const profileMetrics = ref({})
const activeSection = ref('articles')
const relationshipUsers = ref([])
const relationshipLoading = ref(false)
const relationshipPage = ref(1)
const relationshipTotal = ref(0)
const uploadDialogVisible = ref(false)
const uploadingImage = ref(false)
const uploadTarget = ref('avatar')

const isMe = computed(() => {
  const u = userInfoStore.userInfo
  if (!u || !profileUser.value) return false
  return u.userId === profileUser.value.userId
})

const bannerStyle = computed(() => {
  const img = profileUser.value?.backgroundImage || defaultBackground
  return { backgroundImage: `url(${img})` }
})
const profileStats = computed(() => ({
  articleCount: Number(profileMetrics.value.articleCount ?? articles.value.length),
  views: Number(profileMetrics.value.viewCount ?? articles.value.reduce((sum, article) => sum + (article.viewCount || 0), 0)),
  likes: Number(profileMetrics.value.likeCount ?? articles.value.reduce((sum, article) => sum + (article.likeCount || 0), 0)),
  comments: Number(profileMetrics.value.commentCount ?? articles.value.reduce((sum, article) => sum + (article.commentCount || 0), 0)),
  followerCount: Number(profileMetrics.value.followerCount || 0),
  followingCount: Number(profileMetrics.value.followingCount || 0),
  following: Boolean(profileMetrics.value.following)
}))
const profileTabs = computed(() => [
  { label: '文章', value: 'articles', count: profileStats.value.articleCount },
  { label: '关注者', value: 'followers', count: profileStats.value.followerCount },
  { label: '正在关注', value: 'following', count: profileStats.value.followingCount }
])
const sectionTitle = computed(() => ({
  articles: '发布的文章',
  followers: '关注者',
  following: '正在关注'
}[activeSection.value]))
const sectionCount = computed(() => {
  const count = activeSection.value === 'articles'
    ? profileStats.value.articleCount
    : activeSection.value === 'followers'
      ? profileStats.value.followerCount
      : profileStats.value.followingCount
  return `${formatCount(count)} ${activeSection.value === 'articles' ? '篇' : '人'}`
})
const relationshipHasMore = computed(() => relationshipUsers.value.length < relationshipTotal.value)
const filteredArticles = computed(() => {
  const query = articleSearch.value.trim().toLowerCase()
  if (!query) return articles.value
  return articles.value.filter(article =>
    `${article.title || ''} ${article.summary || ''}`.toLowerCase().includes(query)
  )
})
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
      await Promise.all([loadArticles(), loadProfileMetrics()])
      if (activeSection.value !== 'articles') await loadRelationships(activeSection.value)
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
    const res = await searchArticlesService({ authorId: profileUser.value.userId, pageSize: 50 })
    articles.value = res.data.list || []
  } catch {}
}

async function loadProfileMetrics() {
  try {
    const service = userInfoStore.userInfo ? getCommunityFollowStateService : getCommunityProfileService
    const result = await service(profileUser.value.userId)
    profileMetrics.value = result.data || {}
  } catch {
    const result = await getCommunityProfileService(profileUser.value.userId).catch(() => null)
    profileMetrics.value = result?.data || {}
  }
}

let relationshipRequestSequence = 0
async function loadRelationships(section, append = false) {
  if (!profileUser.value || section === 'articles') return
  const requestSequence = ++relationshipRequestSequence
  const nextPage = append ? relationshipPage.value + 1 : 1
  relationshipLoading.value = true
  try {
    const service = section === 'followers'
      ? getCommunityProfileFollowersService
      : getCommunityProfileFollowingService
    const result = await service(profileUser.value.userId, { page: nextPage, pageSize: 20 })
    if (requestSequence !== relationshipRequestSequence || activeSection.value !== section) return
    const list = result.data?.list || []
    relationshipUsers.value = append ? relationshipUsers.value.concat(list) : list
    relationshipPage.value = nextPage
    relationshipTotal.value = Number(result.data?.total || 0)
  } finally {
    if (requestSequence === relationshipRequestSequence) relationshipLoading.value = false
  }
}

async function selectSection(section) {
  if (!['articles', 'followers', 'following'].includes(section)) section = 'articles'
  if (activeSection.value === section && (section === 'articles' || relationshipUsers.value.length)) return
  activeSection.value = section
  relationshipUsers.value = []
  relationshipTotal.value = section === 'articles' ? profileStats.value.articleCount : 0
  if (section !== 'articles') await loadRelationships(section)
}

function loadMoreRelationships() {
  return loadRelationships(activeSection.value, true)
}

function openProfile(username) {
  if (username) router.push(`/profile/${username}`)
}

async function confirmUnfollow(name) {
  try {
    await ElMessageBox.confirm(
      `确定取消关注“${name}”吗？取消后将不再优先看到对方的更新。`,
      '取消关注',
      {
        confirmButtonText: '取消关注',
        cancelButtonText: '保留关注',
        type: 'warning'
      }
    )
    return true
  } catch {
    return false
  }
}

async function togglePersonFollow(person) {
  if (!userInfoStore.userInfo) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (person.following && !await confirmUnfollow(person.nickname || person.username)) return
  try {
    const result = await toggleCommunityFollowService(person.userId)
    Object.assign(person, {
      following: Boolean(result.data?.following),
      followerCount: Number(result.data?.followerCount ?? person.followerCount ?? 0)
    })
    ElMessage.success(person.following ? '已关注' : '已取消关注')
    if (isMe.value && activeSection.value === 'following' && !person.following) {
      await Promise.all([loadRelationships('following'), loadProfileMetrics()])
    }
  } catch {}
}

async function toggleFollow() {
  if (!userInfoStore.userInfo) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (profileStats.value.following && !await confirmUnfollow(profileUser.value.nickname || profileUser.value.username)) return
  try {
    const result = await toggleCommunityFollowService(profileUser.value.userId)
    profileMetrics.value = { ...profileMetrics.value, ...(result.data || {}) }
    ElMessage.success(profileStats.value.following ? '已关注' : '已取消关注')
  } catch {}
}

function beginImageUpload(target) {
  uploadTarget.value = target
  showEditDialog.value = true
  uploadDialogVisible.value = true
}

async function uploadProfileImage(file) {
  uploadingImage.value = true
  try {
    const type = uploadTarget.value === 'avatar' ? OSSClient.IMAGE_TYPE.AVATAR : OSSClient.IMAGE_TYPE.BACKGROUND
    const url = await uploadImageToOss(file, type, profileUser.value?.userId)
    if (uploadTarget.value === 'avatar') editForm.avatarImage = url
    else editForm.backgroundImage = url
    uploadDialogVisible.value = false
    ElMessage.success('图片已更新，请保存资料')
  } catch (error) {
    ElMessage.error(error?.message || '图片上传失败')
  } finally {
    uploadingImage.value = false
  }
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
watch(() => route.params.username, () => {
  activeSection.value = 'articles'
  relationshipUsers.value = []
  relationshipTotal.value = 0
  articleSearch.value = ''
  loadProfile()
})
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
.banner-edit { position: absolute; right: max(24px, calc((100% - var(--content-width)) / 2 + 24px)); bottom: 18px; z-index: 1; padding: 7px 11px; border: 1px solid rgba(255,255,255,.72); border-radius: var(--radius-sm); background: rgba(15,23,42,.56); color: #fff; font-size: 12px; font-weight: 700; backdrop-filter: blur(8px); }
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
.profile-stats { display: grid; grid-template-columns: repeat(6, 1fr); margin-bottom: 18px; padding: 16px 18px; }
.profile-stats p, .profile-stats button { display: flex; min-width: 0; align-items: center; justify-content: center; gap: 12px; padding: 5px 8px; border: 0; border-right: 1px solid var(--c-border); border-radius: 0; background: transparent; text-align: left; }
.profile-stats > :last-child { border-right: 0; }
.profile-stats button { cursor: pointer; transition: color var(--transition), background var(--transition); }
.profile-stats button:hover, .profile-stats button.active { border-radius: 7px; background: var(--c-primary-soft); }
.profile-stats button:hover i, .profile-stats button.active i, .profile-stats button.active strong { color: var(--c-primary); }
.profile-stats i { color: var(--c-primary); font-size: 23px; font-style: normal; }
.profile-stats span { display: flex; flex-direction: column; color: var(--c-text-3); font-size: 11px; }
.profile-stats strong { color: var(--c-text); font-size: 21px; }
.profile-card-right .following { border-color: var(--c-border-strong); background: var(--c-surface-2); color: var(--c-text-3); box-shadow: none; }
.profile-tabs { display: flex; gap: 8px; margin-bottom: 18px; border-bottom: 1px solid var(--c-border); }
.profile-tabs button { position: relative; display: inline-flex; align-items: center; gap: 7px; padding: 11px 12px 13px; border: 0; background: transparent; color: var(--c-text-3); font-weight: 600; }
.profile-tabs button span { display: grid; min-width: 20px; height: 20px; padding: 0 5px; place-items: center; border-radius: 10px; background: var(--c-surface-2); color: var(--c-text-4); font-size: 10px; }
.profile-tabs button.active { color: var(--c-primary); }
.profile-tabs button.active span { background: var(--c-primary-soft); color: var(--c-primary); }
.profile-tabs button.active::after { position: absolute; right: 8px; bottom: -1px; left: 8px; height: 3px; border-radius: 3px 3px 0 0; background: var(--c-primary); content: ''; }
.profile-layout { display: grid; grid-template-columns: minmax(0, 1fr) 310px; gap: 18px; align-items: start; }
.profile-layout--wide { grid-template-columns: minmax(0, 1fr); }
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
.people-list { display: flex; flex-direction: column; border-top: 1px solid var(--c-border); }
.person-card { position: relative; display: grid; min-height: 92px; grid-template-columns: minmax(260px, 1fr) auto auto; align-items: center; gap: 24px; padding: 14px 4px; border: 0; border-bottom: 1px solid var(--c-border); border-radius: 0; background: transparent; box-shadow: none; }
.person-main { display: flex; min-width: 0; align-items: flex-start; gap: 12px; padding: 0; border: 0; background: transparent; text-align: left; }
.person-main > img { width: 52px; height: 52px; flex: 0 0 auto; border: 1px solid var(--c-border); border-radius: 50%; object-fit: cover; }
.person-copy { display: flex; min-width: 0; flex-direction: column; }
.person-copy strong { overflow: hidden; color: var(--c-text); font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
.person-copy small { margin-top: 1px; color: var(--c-text-4); font-size: 10px; }
.person-copy em { max-width: 520px; margin-top: 5px; overflow: hidden; color: var(--c-text-3); font-size: 11px; font-style: normal; line-height: 1.55; text-overflow: ellipsis; white-space: nowrap; }
.person-main:hover strong { color: var(--c-primary); }
.person-meta { display: flex; align-items: center; gap: 16px; color: var(--c-text-4); font-size: 10px; white-space: nowrap; }
.person-meta strong { color: var(--c-text-2); font-size: 11px; }
.person-follow { min-width: 58px; height: 30px; padding: 0 10px; border: 1px solid var(--c-primary); border-radius: 6px; background: var(--c-primary); color: #fff; font-size: 10px; font-weight: 700; }
.person-follow.following { border-color: var(--c-border); background: var(--c-surface-2); color: var(--c-text-3); }
.load-people { height: 40px; margin-top: 12px; border: 1px solid var(--c-border); border-radius: 7px; background: var(--c-surface); color: var(--c-text-3); font-size: 12px; font-weight: 700; }
.load-people:hover { border-color: var(--c-primary); color: var(--c-primary); }
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
.image-field { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: 8px; }
.image-field--wide { width: 100%; }
.image-field small { color: var(--c-text-4); font-size: 11px; line-height: 1.5; }

@media (max-width: 980px) {
  .profile-layout { grid-template-columns: 1fr; }
  .profile-aside { position: static; display: grid; grid-template-columns: repeat(2, 1fr); }
  .quick-card { grid-column: 1 / -1; }
  .profile-banner { height: 180px; }
  .profile-stats { grid-template-columns: repeat(3, 1fr); }
  .profile-stats p:nth-child(3) { border-right: 0; }
  .person-card { grid-template-columns: minmax(0, 1fr) auto; gap: 12px; }
  .person-meta { grid-column: 1; padding-left: 64px; }
  .person-follow { grid-column: 2; grid-row: 1 / span 2; }
}
@media (max-width: 680px) {
  .profile-card, .profile-card-left { align-items: flex-start; flex-direction: column; }
  .profile-card-right { width: 100%; }
  .profile-stats { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .profile-stats > :nth-child(2n) { border-right: 0; }
  .profile-tabs { overflow-x: auto; }
  .section-header { align-items: flex-start; flex-direction: column; }
  .section-header :deep(.el-input) { width: 100% !important; }
  .profile-aside { grid-template-columns: 1fr; }
  .quick-card { grid-column: auto; }
  .person-card { align-items: start; }
  .person-meta { flex-direction: column; align-items: flex-start; gap: 2px; }
}
</style>

<template>
  <div class="profile-page">
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
        </div>
      </div>

      <!-- Articles -->
      <div class="profile-content">
        <div class="section-header">
          <h2 class="section-title">发布的文章</h2>
          <span class="section-count">{{ articles.length }} 篇</span>
        </div>

        <div v-if="loading" class="loading-spinner"><el-icon class="is-loading" :size="20"><Loading /></el-icon></div>
        <div v-else-if="articles.length === 0" class="empty-state">
          <p>还没有发布文章</p>
        </div>
        <div v-else class="article-list">
          <ArticleCard v-for="a in articles" :key="a.articleId" :article="a"/>
        </div>
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

const defaultAvatar = 'https://luf-23.oss-cn-wuhan-lr.aliyuncs.com/avatar/default.png'

const isMe = computed(() => {
  const u = userInfoStore.userInfo
  if (!u || !profileUser.value) return false
  return u.userId === profileUser.value.userId
})

const bannerStyle = computed(() => {
  const img = profileUser.value?.backgroundImage
  return img ? { backgroundImage: `url(${img})` } : {}
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

onMounted(loadProfile)
watch(() => route.params.username, loadProfile)
</script>

<style scoped>
.profile-page {}

.profile-banner {
  height: 200px;
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
  margin-bottom: 24px;
  position: relative;
}

.profile-card-left {
  display: flex;
  align-items: flex-end;
  gap: 20px;
}

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
.profile-content {}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
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

/* Avatar preview row */
.avatar-preview-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}
</style>

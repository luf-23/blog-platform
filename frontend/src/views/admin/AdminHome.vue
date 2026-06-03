<template>
  <div class="admin-home page-container">
    <section class="admin-hero">
      <div class="hero-user">
        <div class="hero-avatar">
          <img :src="userInfo?.avatarImage || defaultAvatar" :alt="userInfo?.nickname || userInfo?.username" />
        </div>
        <div>
          <h1>欢迎回来，{{ userInfo?.nickname || userInfo?.username || '管理员' }}</h1>
          <p>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>
            </svg>
            {{ currentTimeText }}
          </p>
        </div>
      </div>
      <div class="hero-art">
        <div class="art-card">
          <div class="art-line short"></div>
          <div class="art-line"></div>
          <div class="art-chart">
            <span></span><span></span><span></span>
          </div>
        </div>
        <div class="art-plant"></div>
      </div>
    </section>

    <div class="stats-grid">
      <div class="stat-card card" v-for="s in stats" :key="s.key">
        <div class="stat-icon" :style="{ background: s.bg, color: s.color }">
          <component :is="s.icon"/>
        </div>
        <div class="stat-info">
          <div class="stat-label">{{ s.label }}</div>
          <div class="stat-value">{{ s.format ? s.format(statsData[s.key]) : (statsData[s.key] ?? '—') }}</div>
          <div v-if="s.sub" class="stat-sub">{{ s.sub }}</div>
        </div>
      </div>
    </div>

    <div class="quick-nav">
      <router-link to="/admin/users" class="qn-card card">
        <div class="qn-icon" style="background:#eef2ff;color:#4f46e5">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg>
        </div>
        <div class="qn-info">
          <div class="qn-title">用户管理</div>
          <div class="qn-desc">管理用户账号与权限</div>
        </div>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--c-text-4)" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
      </router-link>

      <router-link to="/admin/articles" class="qn-card card">
        <div class="qn-icon" style="background:#fef3c7;color:#f59e0b">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
        </div>
        <div class="qn-info">
          <div class="qn-title">文章审核</div>
          <div class="qn-desc">审核待发布文章</div>
          <div v-if="statsData.pendingArticles > 0" class="qn-badge">{{ statsData.pendingArticles }} 待审核</div>
        </div>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--c-text-4)" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
      </router-link>

      <div class="qn-card card" style="cursor:default">
        <div class="qn-icon" style="background:#f5f3ff;color:#8b5cf6">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg>
        </div>
        <div class="qn-info">
          <div class="qn-title">系统公告</div>
          <div class="qn-desc">发布系统通知和公告</div>
        </div>
        <button class="btn btn-secondary btn-sm" @click="showAnnouncementDialog = true">发布公告</button>
      </div>
    </div>

    <!-- Announcement dialog -->
    <el-dialog v-model="showAnnouncementDialog" title="发布公告" width="480px" :close-on-click-modal="false">
      <el-form ref="annFormRef" :model="annForm" :rules="annRules" label-position="top">
        <el-form-item label="标题" prop="title">
          <el-input v-model="annForm.title" placeholder="公告标题" maxlength="100"/>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="annForm.content" type="textarea" :rows="4" placeholder="公告内容" maxlength="1000"/>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="annForm.type">
            <el-radio value="info">信息</el-radio>
            <el-radio value="success">成功</el-radio>
            <el-radio value="warning">警告</el-radio>
            <el-radio value="danger">紧急</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAnnouncementDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingAnn" @click="publishAnnouncement">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, h } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserInfoStore } from '../../store/userInfo.js'
import { getAdminStatsService, addAnnouncementService } from '../../api/admin.js'

const userInfoStore = useUserInfoStore()
const userInfo = computed(() => userInfoStore.userInfo)
const defaultAvatar = 'https://luf-23.oss-cn-wuhan-lr.aliyuncs.com/avatar/default.png'
const currentTimeText = computed(() => {
  const formatter = new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long',
    hour: '2-digit',
    minute: '2-digit'
  })
  return formatter.format(new Date())
})

const statsData = ref({})
const showAnnouncementDialog = ref(false)
const savingAnn = ref(false)
const annFormRef = ref()
const annForm = reactive({ title: '', content: '', type: 'info' })
const annRules = {
  title: [{ required: true, message: '请填写标题', trigger: 'blur' }],
  content: [{ required: true, message: '请填写内容', trigger: 'blur' }]
}

const UsersIcon = () => h('svg', { width: 24, height: 24, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 2 }, [
  h('path', { d: 'M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2' }),
  h('circle', { cx: 9, cy: 7, r: 4 })
])
const ArticlesIcon = () => h('svg', { width: 24, height: 24, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 2 }, [
  h('path', { d: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z' }),
  h('polyline', { points: '14 2 14 8 20 8' })
])
const PubIcon = () => h('svg', { width: 24, height: 24, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 2 }, [
  h('polyline', { points: '20 6 9 17 4 12' })
])
const PendingIcon = () => h('svg', { width: 24, height: 24, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 2 }, [
  h('circle', { cx: 12, cy: 12, r: 10 }),
  h('polyline', { points: '12 6 12 12 16 14' })
])
const CommentIcon = () => h('svg', { width: 24, height: 24, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 2 }, [
  h('path', { d: 'M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z' })
])

const stats = [
  { key: 'totalUsers', label: '总用户数', icon: UsersIcon, bg: '#eef2ff', color: '#4f46e5', sub: '较昨日 2.35%' },
  { key: 'totalArticles', label: '文章总数', icon: ArticlesIcon, bg: '#ecfdf5', color: '#10b981', sub: '较昨日 1.48%' },
  { key: 'pendingArticles', label: '待审核', icon: PendingIcon, bg: '#fff7ed', color: '#f97316', sub: '需要及时处理' },
  {
    key: 'totalComments',
    label: '评论活跃度',
    icon: CommentIcon,
    bg: '#f5f3ff',
    color: '#8b5cf6',
    sub: '互动数据',
    format: (v) => v == null ? '—' : `${Math.min(99, Math.max(0, Math.round(v / 10)))}%`
  }
]

async function loadStats() {
  try {
    const res = await getAdminStatsService()
    statsData.value = res.data || {}
  } catch {}
}

async function publishAnnouncement() {
  if (!annFormRef.value) return
  await annFormRef.value.validate(async (valid) => {
    if (!valid) return
    savingAnn.value = true
    try {
      await addAnnouncementService({ title: annForm.title, content: annForm.content, type: annForm.type })
      ElMessage.success('公告已发布')
      showAnnouncementDialog.value = false
      annForm.title = ''
      annForm.content = ''
      annForm.type = 'info'
    } finally { savingAnn.value = false }
  })
}

onMounted(loadStats)
</script>

<style scoped>
.admin-home {
  max-width: 1240px;
  padding-top: 4px;
}

.admin-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-height: 108px;
  margin-bottom: 16px;
  padding: 24px 28px;
  border: 1px solid #e8ecff;
  border-radius: 12px;
  background:
    linear-gradient(90deg, rgba(238, 242, 255, 0.95), rgba(245, 247, 255, 0.9)),
    radial-gradient(circle at 78% 20%, rgba(129, 140, 248, 0.22), transparent 32%);
  overflow: hidden;
  position: relative;
}

.hero-user {
  display: flex;
  align-items: center;
  gap: 18px;
  position: relative;
  z-index: 1;
}

.hero-avatar {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  padding: 5px;
  background: #fff;
  box-shadow: 0 12px 28px rgba(79, 70, 229, 0.18);
}
.hero-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.hero-user h1 {
  color: #1f2937;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.hero-user p {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
}

.hero-art {
  position: absolute;
  right: 54px;
  top: 18px;
  width: 220px;
  height: 90px;
  opacity: 0.72;
}

.art-card {
  position: absolute;
  right: 20px;
  top: 4px;
  width: 122px;
  height: 72px;
  border: 4px solid #c7d2fe;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.72);
  padding: 12px;
}
.art-line {
  height: 7px;
  margin-bottom: 8px;
  border-radius: 999px;
  background: #c7d2fe;
}
.art-line.short { width: 36px; }
.art-chart {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 22px;
  margin-top: 4px;
}
.art-chart span {
  width: 12px;
  border-radius: 999px 999px 0 0;
  background: #818cf8;
}
.art-chart span:nth-child(1) { height: 10px; }
.art-chart span:nth-child(2) { height: 18px; }
.art-chart span:nth-child(3) { height: 14px; }
.art-plant {
  position: absolute;
  left: 26px;
  bottom: 5px;
  width: 18px;
  height: 36px;
  border-radius: 999px 999px 4px 4px;
  background: #a5b4fc;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 14px;
}

.stat-card {
  min-height: 108px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border-radius: 10px;
  border-color: #edf0f7;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.04);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-label { font-size: 13px; color: #4b5563; margin-bottom: 4px; font-weight: 600; }
.stat-value { font-size: 24px; font-weight: 800; color: #111827; line-height: 1.15; }
.stat-sub { font-size: 12px; color: #10b981; margin-top: 5px; }

.quick-nav {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  margin-bottom: 12px;
  border: 1px solid #edf0f7;
  border-radius: 10px;
  background: #fff;
  overflow: hidden;
}

.qn-card {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 76px;
  padding: 16px 22px;
  border: none;
  border-radius: 0;
  box-shadow: none;
  text-decoration: none;
  color: inherit;
  transition: all var(--transition);
}
.qn-card + .qn-card { border-left: 1px solid #edf0f7; }
a.qn-card:hover {
  transform: none;
  box-shadow: none;
  background: #fafbff;
}

.qn-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.qn-info { flex: 1; min-width: 0; }
.qn-title { font-weight: 700; font-size: 14px; color: #111827; }
.qn-desc { font-size: 12px; color: #6b7280; margin-top: 2px; }
.qn-badge {
  display: inline-flex;
  margin-top: 6px;
  font-size: 11px;
  background: var(--c-warning-light);
  color: var(--c-warning);
  padding: 1px 8px;
  border-radius: var(--radius-full);
  font-weight: 600;
}

@media (max-width: 980px) {
  .stats-grid,
  .quick-nav {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .qn-card + .qn-card { border-left: none; border-top: 1px solid #edf0f7; }
}

@media (max-width: 640px) {
  .admin-hero { padding: 20px; }
  .hero-art { display: none; }
  .stats-grid,
  .quick-nav {
    grid-template-columns: 1fr;
  }
}
</style>

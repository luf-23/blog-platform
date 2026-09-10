<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  acceptArticleService,
  addAnnouncementService,
  getAdminArticlesService,
  getAdminStatsService,
  rejectArticleService
} from '../../api/admin.js'
import { useAdminPendingArticles } from '../../composables/useAdminPendingArticles.js'

const statsData = ref({})
const pendingArticles = ref([])
const showAnnouncementDialog = ref(false)
const savingAnn = ref(false)
const annFormRef = ref()
const annForm = reactive({ title: '', content: '', type: 'info' })
const annRules = {
  title: [{ required: true, message: '请填写标题', trigger: 'blur' }],
  content: [{ required: true, message: '请填写内容', trigger: 'blur' }]
}
const { refreshPendingArticles } = useAdminPendingArticles()
const todayLabel = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date())
const kpis = computed(() => [
  { label: '用户', value: statsData.value.totalUsers ?? '—', icon: '♙', tone: 'blue' },
  { label: '全部文章', value: statsData.value.totalArticles ?? '—', icon: '▤', tone: 'blue' },
  { label: '已发布', value: statsData.value.publishedArticles ?? '—', icon: '✓', tone: 'green' },
  { label: '待审核', value: statsData.value.pendingArticles ?? '—', icon: '◷', tone: 'amber' },
  { label: '评论', value: statsData.value.totalComments ?? '—', icon: '▢', tone: 'blue' }
])

async function loadDashboard() {
  const [statsResult, articleResult] = await Promise.all([
    getAdminStatsService(),
    getAdminArticlesService({ status: 'pending', page: 1, pageSize: 4 })
  ])
  statsData.value = statsResult.data || {}
  pendingArticles.value = articleResult.data?.list || []
}

async function review(article, accepted) {
  if (accepted) await acceptArticleService(article.articleId)
  else await rejectArticleService(article.articleId)
  ElMessage.success(accepted ? '文章已通过审核' : '文章已驳回')
  await Promise.all([loadDashboard(), refreshPendingArticles()])
}

async function publishAnnouncement() {
  await annFormRef.value?.validate(async valid => {
    if (!valid) return
    savingAnn.value = true
    try {
      await addAnnouncementService({ ...annForm })
      ElMessage.success('公告已发布')
      showAnnouncementDialog.value = false
      Object.assign(annForm, { title: '', content: '', type: 'info' })
    } finally {
      savingAnn.value = false
    }
  })
}

function formatTime(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(loadDashboard)
</script>

<template>
  <div class="admin-home">
    <header class="dashboard-head">
      <div><p>{{ todayLabel }}</p><h1>管理后台</h1></div>
      <div><button class="btn btn-primary" @click="showAnnouncementDialog = true">◖ 发布公告</button></div>
    </header>

    <section class="kpi-grid">
      <article v-for="item in kpis" :key="item.label" class="kpi-card surface-card">
        <div><span>{{ item.label }}</span><strong>{{ item.value }}</strong></div>
        <i :class="item.tone">{{ item.icon }}</i>
      </article>
    </section>

    <div class="dashboard-bottom-grid">
      <section class="review-card surface-card">
        <header><h2>待审核文章</h2><router-link to="/admin/articles">查看全部 ›</router-link></header>
        <div class="review-table">
          <div class="review-row review-row--head"><span>标题</span><span>作者</span><span>提交时间</span><span>操作</span></div>
          <div v-for="article in pendingArticles" :key="article.articleId" class="review-row">
            <strong>{{ article.title }}</strong><span>{{ article.authorNickname || article.authorUsername }}</span><span>{{ formatTime(article.createTime) }}</span>
            <span class="review-actions"><button @click="review(article, true)">通过</button><button @click="review(article, false)">驳回</button><router-link :to="'/article/' + article.articleId">查看</router-link></span>
          </div>
          <div v-if="!pendingArticles.length" class="review-empty">暂无待审核文章</div>
        </div>
      </section>
    </div>

    <el-dialog v-model="showAnnouncementDialog" title="发布公告" width="480px" :close-on-click-modal="false">
      <el-form ref="annFormRef" :model="annForm" :rules="annRules" label-position="top">
        <el-form-item label="标题" prop="title"><el-input v-model="annForm.title" maxlength="100" /></el-form-item>
        <el-form-item label="内容" prop="content"><el-input v-model="annForm.content" type="textarea" :rows="5" maxlength="1000" /></el-form-item>
        <el-form-item label="类型"><el-radio-group v-model="annForm.type"><el-radio value="info">信息</el-radio><el-radio value="success">成功</el-radio><el-radio value="warning">警告</el-radio><el-radio value="danger">紧急</el-radio></el-radio-group></el-form-item>
      </el-form>
      <template #footer><el-button @click="showAnnouncementDialog = false">取消</el-button><el-button type="primary" :loading="savingAnn" @click="publishAnnouncement">发布</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-home { width: min(1450px, calc(100% - 44px)); margin: 0 auto; padding: 26px 0 40px; }
.dashboard-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 20px; }.dashboard-head h1 { margin: 2px 0; font-size: 28px; }.dashboard-head p, .dashboard-head span { color: var(--c-text-3); font-size: 11px; }.dashboard-head > div:last-child { display: flex; align-items: center; gap: 10px; }
.kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-bottom: 14px; }.kpi-card { display: flex; align-items: flex-start; justify-content: space-between; padding: 20px; }.kpi-card > div { display: flex; flex-direction: column; }.kpi-card span { color: var(--c-text-3); font-size: 12px; }.kpi-card strong { margin: 3px 0; font-size: 29px; line-height: 1.2; }.kpi-card > i { display: grid; width: 44px; height: 44px; place-items: center; border-radius: 50%; font-size: 20px; font-style: normal; }.blue { color: var(--c-primary) !important; }.green { color: var(--c-success) !important; }.amber { color: var(--c-warning) !important; }.red { color: var(--c-danger) !important; }.purple { color: #8b5cf6 !important; }.kpi-card > i.blue { background: var(--c-primary-soft); }.kpi-card > i.green { background: var(--c-success-soft); }.kpi-card > i.amber { background: var(--c-warning-soft); }.kpi-card > i.red { background: var(--c-danger-soft); }
.review-card { padding: 19px; }.review-card > header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }.review-card h2 { font-size: 16px; }
.dashboard-bottom-grid { display: block; }.review-card header { margin-bottom: 12px; }.review-card header > a { color: var(--c-primary); font-size: 11px; }.review-table { overflow-x: auto; border: 1px solid var(--c-border); border-radius: var(--radius); }.review-row { display: grid; min-width: 650px; grid-template-columns: minmax(220px, 1.8fr) 1fr 1fr 1.3fr; align-items: center; gap: 10px; padding: 10px 12px; border-bottom: 1px solid var(--c-border); color: var(--c-text-3); font-size: 10px; }.review-row:last-child { border: 0; }.review-row--head { background: var(--c-surface-2); color: var(--c-text-2); font-weight: 700; }.review-row > strong { overflow: hidden; color: var(--c-text-2); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }.review-actions { display: flex; gap: 5px; }.review-actions button, .review-actions a { padding: 4px 7px; border: 1px solid var(--c-border); border-radius: 5px; background: var(--c-surface); color: var(--c-text-2); }.review-actions button:first-child { border-color: var(--c-primary); background: var(--c-primary); color: #fff; }.review-actions button:nth-child(2) { border-color: var(--c-danger); color: var(--c-danger); }.review-empty { padding: 36px; color: var(--c-text-4); text-align: center; }
@media (max-width: 1100px) { .kpi-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 700px) { .dashboard-head { align-items: flex-start; flex-direction: column; }.dashboard-head > div:last-child { width: 100%; flex-wrap: wrap; }.kpi-grid { grid-template-columns: 1fr; }.admin-home { width: calc(100% - 28px); } }
</style>

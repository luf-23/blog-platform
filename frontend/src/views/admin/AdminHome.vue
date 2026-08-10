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

const statsData = ref({})
const pendingArticles = ref([])
const showAnnouncementDialog = ref(false)
const savingAnn = ref(false)
const annFormRef = ref()
const chartMode = ref('阅读')
const annForm = reactive({ title: '', content: '', type: 'info' })
const annRules = {
  title: [{ required: true, message: '请填写标题', trigger: 'blur' }],
  content: [{ required: true, message: '请填写内容', trigger: 'blur' }]
}
const chartValues = [42, 49, 46, 58, 69, 61, 74]
const kpis = computed(() => [
  { label: '总用户', value: statsData.value.totalUsers ?? '—', change: '+5.34%', icon: '♙', tone: 'blue' },
  { label: '已发布文章', value: statsData.value.publishedArticles ?? '—', change: '+12.08%', icon: '▤', tone: 'green' },
  { label: '待审核', value: statsData.value.pendingArticles ?? '—', change: '需及时处理', icon: '◷', tone: 'amber' },
  { label: '举报待处理', value: 18, change: '-5.26%', icon: '!', tone: 'red' }
])
const todos = computed(() => [
  { label: '待审核文章', value: statsData.value.pendingArticles || 0, tone: 'amber' },
  { label: '举报待处理', value: 18, tone: 'red' },
  { label: '待处理评论', value: Math.min(statsData.value.totalComments || 0, 32), tone: 'blue' },
  { label: '待审核用户', value: 7, tone: 'green' },
  { label: '待发布公告', value: 2, tone: 'purple' }
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
  await loadDashboard()
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
  if (!value) return '刚刚'
  return new Date(value).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(loadDashboard)
</script>

<template>
  <div class="admin-home">
    <header class="dashboard-head">
      <div><p>2026 年 8 月 10 日 · 星期一</p><h1>运营概览</h1><span>掌握平台内容与社区的实时状态</span></div>
      <div><el-date-picker type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" size="default" /><button class="btn btn-primary" @click="showAnnouncementDialog = true">◖ 发布公告</button></div>
    </header>

    <section class="kpi-grid">
      <article v-for="item in kpis" :key="item.label" class="kpi-card surface-card">
        <div><span>{{ item.label }}</span><strong>{{ item.value }}</strong><small :class="item.tone">{{ item.change }} <b v-if="item.change.startsWith('+')">↑</b></small></div>
        <i :class="item.tone">{{ item.icon }}</i>
      </article>
    </section>

    <div class="dashboard-main-grid">
      <section class="chart-card surface-card">
        <header><div><h2>内容趋势</h2><p>平台内容与用户增长概览</p></div><nav><button v-for="mode in ['阅读','发布','互动']" :key="mode" :class="{ active: chartMode === mode }" @click="chartMode = mode">{{ mode }}</button></nav></header>
        <div class="chart-area">
          <div class="chart-y"><span>2k</span><span>1.5k</span><span>1k</span><span>500</span><span>0</span></div>
          <div class="chart-plot">
            <svg viewBox="0 0 700 220" preserveAspectRatio="none">
              <defs><linearGradient id="areaGradient" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stop-color="#5146e5" stop-opacity=".22"/><stop offset="1" stop-color="#5146e5" stop-opacity=".01"/></linearGradient></defs>
              <path d="M0 175 C70 155 75 125 120 132 S195 162 235 120 S310 62 350 86 S430 154 475 127 S545 104 585 120 S660 82 700 62 V220 H0Z" fill="url(#areaGradient)"/>
              <path d="M0 175 C70 155 75 125 120 132 S195 162 235 120 S310 62 350 86 S430 154 475 127 S545 104 585 120 S660 82 700 62" fill="none" stroke="#5146e5" stroke-width="4"/>
              <path d="M0 198 C90 190 130 184 190 190 S300 155 350 166 S470 145 520 159 S620 131 700 140" fill="none" stroke="#109b9a" stroke-width="3" stroke-dasharray="7 7"/>
            </svg>
            <div class="chart-labels"><span v-for="date in ['08-04','08-05','08-06','08-07','08-08','08-09','08-10']" :key="date">{{ date }}</span></div>
          </div>
        </div>
      </section>

      <section class="todo-card surface-card">
        <header><h2>今日待办</h2><span>{{ todos.reduce((sum, item) => sum + item.value, 0) }} 项</span></header>
        <router-link v-for="todo in todos" :key="todo.label" :to="todo.label.includes('文章') ? '/admin/articles' : '/admin/home'">
          <i :class="todo.tone">▣</i><strong>{{ todo.label }}</strong><b :class="todo.tone">{{ todo.value }}</b><span>›</span>
        </router-link>
      </section>
    </div>

    <div class="dashboard-bottom-grid">
      <section class="review-card surface-card">
        <header><div><h2>待审核文章</h2><p>优先处理风险内容与等待时间较长的投稿</p></div><router-link to="/admin/articles">查看全部 ›</router-link></header>
        <div class="review-table">
          <div class="review-row review-row--head"><span>标题</span><span>作者</span><span>提交时间</span><span>风险提示</span><span>操作</span></div>
          <div v-for="(article, index) in pendingArticles" :key="article.articleId" class="review-row">
            <strong>{{ article.title }}</strong><span>{{ article.authorNickname || article.authorUsername }}</span><span>{{ formatTime(article.createTime) }}</span>
            <span><i :class="index === 2 ? 'risk-high' : index === 0 ? 'risk-mid' : 'risk-low'">{{ index === 2 ? '高风险' : index === 0 ? '中风险' : '低风险' }}</i></span>
            <span class="review-actions"><button @click="review(article, true)">通过</button><button @click="review(article, false)">驳回</button><router-link :to="'/article/' + article.articleId">查看</router-link></span>
          </div>
          <div v-if="!pendingArticles.length" class="review-empty">没有待审核文章，工作台很干净。</div>
        </div>
      </section>

      <aside class="risk-column">
        <section class="surface-card risk-card">
          <h2>内容风险分布</h2>
          <div><div class="risk-donut"><span>总计<strong>321</strong></span></div><ul><li><i class="red"></i>高风险 <b>12% (39)</b></li><li><i class="amber"></i>中风险 <b>28% (90)</b></li><li><i class="green"></i>低风险 <b>60% (192)</b></li></ul></div>
        </section>
        <section class="surface-card activity-card">
          <h2>最近动态</h2>
          <p><time>14:32</time><i class="red"></i><span>用户“清风徐来”的文章被举报<small>举报原因：内容涉嫌引战</small></span></p>
          <p><time>13:48</time><i class="amber"></i><span>文章《区块链的未来趋势》待审核<small>作者：链上观察</small></span></p>
          <p><time>12:15</time><i class="green"></i><span>用户 FutureWalker 已通过审核<small>注册时间：2026-08-10</small></span></p>
        </section>
      </aside>
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
.kpi-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 14px; }.kpi-card { display: flex; align-items: flex-start; justify-content: space-between; padding: 20px; }.kpi-card > div { display: flex; flex-direction: column; }.kpi-card span { color: var(--c-text-3); font-size: 12px; }.kpi-card strong { margin: 3px 0; font-size: 29px; line-height: 1.2; }.kpi-card small { color: var(--c-text-3); font-size: 10px; }.kpi-card small.blue, .kpi-card small.green { color: var(--c-success); }.kpi-card > i { display: grid; width: 44px; height: 44px; place-items: center; border-radius: 50%; font-size: 20px; font-style: normal; }.blue { color: var(--c-primary) !important; }.green { color: var(--c-success) !important; }.amber { color: var(--c-warning) !important; }.red { color: var(--c-danger) !important; }.purple { color: #8b5cf6 !important; }.kpi-card > i.blue { background: var(--c-primary-soft); }.kpi-card > i.green { background: var(--c-success-soft); }.kpi-card > i.amber { background: var(--c-warning-soft); }.kpi-card > i.red { background: var(--c-danger-soft); }
.dashboard-main-grid { display: grid; grid-template-columns: minmax(0, 1fr) 330px; gap: 14px; margin-bottom: 14px; }.chart-card, .todo-card, .review-card, .risk-card, .activity-card { padding: 19px; }.chart-card > header, .todo-card > header, .review-card > header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }.chart-card h2, .todo-card h2, .review-card h2, .risk-card h2, .activity-card h2 { font-size: 16px; }.chart-card header p, .review-card header p { color: var(--c-text-4); font-size: 10px; }.chart-card nav { display: flex; padding: 2px; border-radius: 7px; background: var(--c-surface-2); }.chart-card nav button { padding: 5px 10px; border: 0; border-radius: 5px; background: transparent; color: var(--c-text-3); font-size: 10px; }.chart-card nav button.active { background: var(--c-surface); box-shadow: var(--shadow-xs); color: var(--c-primary); }
.chart-area { display: grid; height: 270px; grid-template-columns: 28px 1fr; gap: 8px; padding-top: 20px; }.chart-y { display: flex; justify-content: space-between; flex-direction: column; padding-bottom: 24px; color: var(--c-text-4); font-size: 9px; }.chart-plot { position: relative; background: repeating-linear-gradient(to bottom, var(--c-border) 0, var(--c-border) 1px, transparent 1px, transparent 53px); }.chart-plot svg { width: 100%; height: calc(100% - 24px); }.chart-labels { display: flex; justify-content: space-between; color: var(--c-text-4); font-size: 9px; }
.todo-card header span { color: var(--c-warning); font-size: 10px; }.todo-card > a { display: grid; grid-template-columns: auto 1fr auto auto; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid var(--c-border); }.todo-card > a:last-child { border: 0; }.todo-card > a i { display: grid; width: 29px; height: 29px; place-items: center; border-radius: 8px; background: var(--c-surface-2); font-style: normal; }.todo-card > a strong { font-size: 12px; }.todo-card > a b { font-size: 15px; }.todo-card > a span { color: var(--c-text-4); }
.dashboard-bottom-grid { display: grid; grid-template-columns: minmax(0, 1fr) 330px; gap: 14px; align-items: start; }.review-card header { margin-bottom: 12px; }.review-card header > a { color: var(--c-primary); font-size: 11px; }.review-table { overflow-x: auto; border: 1px solid var(--c-border); border-radius: var(--radius); }.review-row { display: grid; min-width: 760px; grid-template-columns: minmax(220px, 1.8fr) 1fr 1fr .7fr 1.3fr; align-items: center; gap: 10px; padding: 10px 12px; border-bottom: 1px solid var(--c-border); color: var(--c-text-3); font-size: 10px; }.review-row:last-child { border: 0; }.review-row--head { background: var(--c-surface-2); color: var(--c-text-2); font-weight: 700; }.review-row > strong { overflow: hidden; color: var(--c-text-2); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }.review-row i { padding: 3px 7px; border-radius: 5px; font-style: normal; }.risk-low { background: var(--c-success-soft); color: var(--c-success); }.risk-mid { background: var(--c-warning-soft); color: var(--c-warning); }.risk-high { background: var(--c-danger-soft); color: var(--c-danger); }.review-actions { display: flex; gap: 5px; }.review-actions button, .review-actions a { padding: 4px 7px; border: 1px solid var(--c-border); border-radius: 5px; background: var(--c-surface); color: var(--c-text-2); }.review-actions button:first-child { border-color: var(--c-primary); background: var(--c-primary); color: #fff; }.review-actions button:nth-child(2) { border-color: var(--c-danger); color: var(--c-danger); }.review-empty { padding: 36px; color: var(--c-text-4); text-align: center; }
.risk-column { display: flex; flex-direction: column; gap: 14px; }.risk-card > div { display: flex; align-items: center; gap: 18px; margin-top: 16px; }.risk-donut { display: grid; width: 105px; height: 105px; flex: 0 0 auto; place-items: center; border-radius: 50%; background: conic-gradient(var(--c-danger) 0 12%, var(--c-warning) 12% 40%, var(--c-success) 40% 100%); }.risk-donut::before { position: absolute; width: 68px; height: 68px; border-radius: 50%; background: var(--c-surface); content: ''; }.risk-donut span { position: relative; z-index: 1; display: flex; flex-direction: column; color: var(--c-text-4); font-size: 8px; text-align: center; }.risk-donut strong { color: var(--c-text); font-size: 16px; }.risk-card ul { display: flex; flex: 1; flex-direction: column; gap: 8px; margin: 0; padding: 0; list-style: none; }.risk-card li { display: grid; grid-template-columns: auto 1fr auto; gap: 6px; color: var(--c-text-3); font-size: 10px; }.risk-card li i { width: 7px; height: 7px; margin-top: 4px; border-radius: 50%; background: currentColor; }.risk-card li b { color: var(--c-text-4); font-weight: 500; }
.activity-card p { display: grid; grid-template-columns: 36px auto 1fr; gap: 7px; margin-top: 12px; color: var(--c-text-2); font-size: 10px; }.activity-card time { color: var(--c-text-4); }.activity-card p > i { width: 7px; height: 7px; margin-top: 4px; border-radius: 50%; background: currentColor; }.activity-card p span { display: flex; flex-direction: column; }.activity-card small { color: var(--c-text-4); font-size: 8px; }
@media (max-width: 1100px) { .kpi-grid { grid-template-columns: repeat(2, 1fr); }.dashboard-main-grid, .dashboard-bottom-grid { grid-template-columns: 1fr; }.risk-column { display: grid; grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 700px) { .dashboard-head { align-items: flex-start; flex-direction: column; }.dashboard-head > div:last-child { width: 100%; flex-wrap: wrap; }.kpi-grid { grid-template-columns: 1fr; }.risk-column { grid-template-columns: 1fr; }.admin-home { width: calc(100% - 28px); }.chart-area { height: 220px; } }
</style>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addAnnouncementService, deleteAnnouncementService, getAnnouncementService } from '../../api/admin.js'

const announcements = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogOpen = ref(false)
const form = reactive({ title: '', content: '', type: 'info' })
const typeOptions = [
  { label: '通知', value: 'info' }, { label: '成功', value: 'success' },
  { label: '注意', value: 'warning' }, { label: '紧急', value: 'danger' }
]
const urgentCount = computed(() => announcements.value.filter(item => item.type === 'danger' || item.type === 'warning').length)

async function fetchAnnouncements() {
  loading.value = true
  try {
    const result = await getAnnouncementService()
    announcements.value = [...(result.data || [])].reverse()
  } finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, { title: '', content: '', type: 'info' })
  dialogOpen.value = true
}

async function publish() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('请填写公告标题和内容')
    return
  }
  saving.value = true
  try {
    await addAnnouncementService({ title: form.title.trim(), content: form.content.trim(), type: form.type })
    ElMessage.success('公告已发布')
    dialogOpen.value = false
    await fetchAnnouncements()
  } finally { saving.value = false }
}

async function remove(item) {
  try {
    await ElMessageBox.confirm(`删除公告《${item.title}》？`, '删除公告', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await deleteAnnouncementService(item.id)
    announcements.value = announcements.value.filter(announcement => announcement.id !== item.id)
    ElMessage.success('公告已删除')
  } catch {}
}

function typeLabel(type) {
  return typeOptions.find(item => item.value === type)?.label || '通知'
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—'
}

onMounted(fetchAnnouncements)
</script>

<template>
  <div class="admin-page announcement-manager page-container">
    <header class="admin-page__head">
      <div><span>COMMUNICATION</span><h1>公告管理</h1><p>发布平台通知，并维护用户可见的公告记录。</p></div>
      <button class="primary-action" @click="openCreate">＋ 发布公告</button>
    </header>

    <section class="summary-line">
      <div><strong>{{ announcements.length }}</strong><span>全部公告</span></div>
      <i></i><div><strong>{{ urgentCount }}</strong><span>重要通知</span></div>
      <router-link to="/announcement">查看用户端公告 →</router-link>
    </section>

    <div v-if="loading" class="manager-state">正在读取公告…</div>
    <section v-else-if="announcements.length" class="announcement-list">
      <article v-for="item in announcements" :key="item.id" :class="`tone-${item.type}`">
        <div class="announcement-meta"><span>{{ typeLabel(item.type) }}</span><time>{{ formatDate(item.date) }}</time></div>
        <h2>{{ item.title }}</h2><p>{{ item.content }}</p>
        <button @click="remove(item)">删除</button>
      </article>
    </section>
    <div v-else class="manager-state"><strong>还没有公告</strong><span>发布第一条平台通知，让用户及时了解变化。</span></div>

    <el-dialog v-model="dialogOpen" title="发布公告" width="560px" destroy-on-close>
      <div class="announcement-form">
        <label><span>公告类型</span><el-select v-model="form.type" style="width: 100%"><el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></label>
        <label><span>标题</span><el-input v-model="form.title" maxlength="100" show-word-limit placeholder="用一句话说明公告主题" /></label>
        <label><span>内容</span><el-input v-model="form.content" type="textarea" :rows="6" maxlength="2000" show-word-limit placeholder="写明变更内容、影响范围和必要说明" /></label>
      </div>
      <template #footer><el-button @click="dialogOpen = false">取消</el-button><el-button type="primary" :loading="saving" @click="publish">确认发布</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-page { padding-top: 28px; padding-bottom: 48px; }.admin-page__head { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 20px; }.admin-page__head > div > span { color: var(--c-primary); font-size: 10px; font-weight: 800; letter-spacing: .16em; }.admin-page__head h1 { margin-top: 3px; font-size: 26px; letter-spacing: -.02em; }.admin-page__head p { margin-top: 5px; color: var(--c-text-3); font-size: 13px; }.primary-action { height: 38px; padding: 0 16px; border: 0; border-radius: var(--radius-sm); background: var(--c-primary); color: #fff; font-weight: 700; box-shadow: 0 8px 18px rgba(var(--c-primary-rgb), .17); }
.summary-line { display: flex; align-items: center; gap: 20px; padding: 16px 20px; border: 1px solid var(--c-border); border-radius: var(--radius-lg); margin-bottom: 14px; background: var(--c-surface); }.summary-line div { display: flex; align-items: baseline; gap: 7px; }.summary-line strong { font-size: 20px; }.summary-line span { color: var(--c-text-3); font-size: 10px; }.summary-line i { width: 1px; height: 24px; background: var(--c-border); }.summary-line a { margin-left: auto; color: var(--c-primary); font-size: 11px; font-weight: 700; }
.announcement-list { display: flex; flex-direction: column; gap: 10px; }.announcement-list article { position: relative; padding: 20px 58px 20px 22px; border: 1px solid var(--c-border); border-left: 4px solid var(--c-info); border-radius: var(--radius); background: var(--c-surface); box-shadow: var(--shadow-xs); }.announcement-list article.tone-success { border-left-color: var(--c-success); }.announcement-list article.tone-warning { border-left-color: var(--c-warning); }.announcement-list article.tone-danger { border-left-color: var(--c-danger); }.announcement-meta { display: flex; align-items: center; gap: 10px; }.announcement-meta span { padding: 2px 7px; border-radius: 5px; background: var(--c-primary-soft); color: var(--c-primary); font-size: 9px; font-weight: 800; }.announcement-meta time { color: var(--c-text-4); font-size: 9px; }.announcement-list h2 { margin: 9px 0 5px; font-size: 16px; }.announcement-list p { color: var(--c-text-3); font-size: 12px; line-height: 1.75; white-space: pre-wrap; }.announcement-list article > button { position: absolute; top: 18px; right: 18px; border: 0; background: transparent; color: var(--c-text-4); font-size: 10px; }.announcement-list article > button:hover { color: var(--c-danger); }.manager-state { display: flex; min-height: 260px; align-items: center; justify-content: center; flex-direction: column; gap: 5px; border: 1px dashed var(--c-border); border-radius: var(--radius-lg); color: var(--c-text-4); }.manager-state strong { color: var(--c-text-2); font-size: 16px; }.announcement-form { display: flex; flex-direction: column; gap: 17px; }.announcement-form label > span { display: block; margin-bottom: 7px; color: var(--c-text-2); font-size: 11px; font-weight: 700; }
@media (max-width: 640px) { .admin-page__head { align-items: stretch; flex-direction: column; }.summary-line { flex-wrap: wrap; }.summary-line a { width: 100%; margin-left: 0; }.announcement-list article { padding-right: 22px; }.announcement-list article > button { position: static; margin-top: 12px; } }
</style>

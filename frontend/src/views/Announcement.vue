<template>
  <div class="announcement-page page-container-narrow">
    <div class="page-top">
      <h1 class="page-title">系统公告</h1>
      <p class="page-subtitle">来自平台的官方通知和消息</p>
    </div>

    <div v-if="loading" class="loading-spinner" style="height:200px">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
    </div>

    <div v-else-if="announcements.length === 0" class="empty-state">
      <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="var(--c-text-4)" stroke-width="1.2">
        <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/>
      </svg>
      <p>暂无公告</p>
    </div>

    <div v-else class="ann-list">
      <div
        v-for="ann in announcements"
        :key="ann.id"
        class="ann-card card"
        :class="`ann-${ann.type}`"
      >
        <div class="ann-header">
          <div class="ann-badge" :class="`badge-${ann.type}`">
            <component :is="typeIcon(ann.type)" :size="14"/>
            {{ typeLabel(ann.type) }}
          </div>
          <time class="ann-time">{{ formatDate(ann.date) }}</time>
        </div>
        <h3 class="ann-title">{{ ann.title }}</h3>
        <p class="ann-content">{{ ann.content }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, h } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { getAnnouncementService } from '../api/admin.js'

const announcements = ref([])
const loading = ref(false)

const typeLabels = { success: '成功', info: '通知', warning: '注意', danger: '紧急' }
const typeLabel = (t) => typeLabels[t] || t

function typeIcon(type) {
  const paths = {
    success: h('svg', { width: 14, height: 14, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 2 }, [h('polyline', { points: '20 6 9 17 4 12' })]),
    info: h('svg', { width: 14, height: 14, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 2 }, [h('circle', { cx: 12, cy: 12, r: 10 }), h('line', { x1: 12, y1: 8, x2: 12, y2: 16 }), h('line', { x1: 12, y1: 16, x2: 12, y2: 20 })]),
    warning: h('svg', { width: 14, height: 14, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 2 }, [h('path', { d: 'M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z' }), h('line', { x1: 12, y1: 9, x2: 12, y2: 13 }), h('line', { x1: 12, y1: 17, x2: 12.01, y2: 17 })]),
    danger: h('svg', { width: 14, height: 14, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': 2 }, [h('circle', { cx: 12, cy: 12, r: 10 }), h('line', { x1: 12, y1: 8, x2: 12, y2: 12 }), h('line', { x1: 12, y1: 16, x2: 12.01, y2: 16 })])
  }
  return paths[type] || paths.info
}

function formatDate(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getAnnouncementService()
    announcements.value = (res.data || []).reverse()
  } finally { loading.value = false }
})
</script>

<style scoped>
.announcement-page {}

.page-top {
  margin-bottom: 28px;
  text-align: center;
}
.page-title { font-size: 28px; font-weight: 800; color: var(--c-text); }
.page-subtitle { font-size: 15px; color: var(--c-text-3); margin-top: 6px; }

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--c-text-4);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.ann-list { display: flex; flex-direction: column; gap: 12px; }

.ann-card {
  padding: 20px 24px;
  border-left: 4px solid var(--c-primary);
  transition: all var(--transition);
}
.ann-success { border-left-color: var(--c-success); }
.ann-warning { border-left-color: var(--c-warning); }
.ann-danger { border-left-color: var(--c-danger); }
.ann-info { border-left-color: var(--c-info); }

.ann-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.ann-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
}
.badge-success { background: var(--c-success-light); color: var(--c-success); }
.badge-warning { background: var(--c-warning-light); color: var(--c-warning); }
.badge-danger { background: var(--c-danger-light); color: var(--c-danger); }
.badge-info { background: var(--c-info-light); color: var(--c-info); }

.ann-time { font-size: 12px; color: var(--c-text-4); }

.ann-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--c-text);
  margin-bottom: 8px;
}

.ann-content {
  font-size: 14px;
  color: var(--c-text-2);
  line-height: 1.7;
}
</style>

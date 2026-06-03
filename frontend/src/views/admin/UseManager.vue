<template>
  <div class="admin-users page-container">
    <div class="page-top">
      <div class="page-heading">
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">查看平台注册用户，共 {{ total }} 人</p>
      </div>
    </div>

    <div v-if="loading" class="loading-spinner" style="height:200px">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
    </div>

    <div v-else class="card users-table">
      <!-- Header -->
      <div class="table-header">
        <span class="th" style="flex:2">用户</span>
        <span class="th" style="flex:1.5">邮箱</span>
        <span class="th" style="width:80px">角色</span>
        <span class="th" style="width:120px">注册时间</span>
        <span class="th" style="width:100px">最后登录</span>
      </div>

      <div v-for="user in users" :key="user.userId" class="table-row">
        <div class="td user-info" style="flex:2">
          <img :src="user.avatarImage || defaultAvatar" class="avatar" style="width:36px;height:36px;flex-shrink:0"/>
          <div>
            <div class="user-name">{{ user.nickname || user.username }}</div>
            <div class="user-id">@{{ user.username }}</div>
          </div>
        </div>
        <div class="td" style="flex:1.5;color:var(--c-text-3);font-size:13px">{{ user.email || '—' }}</div>
        <div class="td" style="width:80px">
          <span class="badge" :class="user.role === 'admin' ? 'badge-published' : 'badge-draft'">
            {{ user.role === 'admin' ? '管理员' : '用户' }}
          </span>
        </div>
        <div class="td" style="width:120px;font-size:13px;color:var(--c-text-3)">{{ formatDate(user.createTime) }}</div>
        <div class="td" style="width:100px;font-size:13px;color:var(--c-text-3)">{{ formatDate(user.lastLogin) || '从未' }}</div>
      </div>
    </div>

    <div v-if="total > pageSize" class="pagination-wrap">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchUsers"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { getAdminUsersService } from '../../api/admin.js'

const users = ref([])
const total = ref(0)
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 15
const defaultAvatar = 'https://luf-23.oss-cn-wuhan-lr.aliyuncs.com/avatar/default.png'

async function fetchUsers() {
  loading.value = true
  try {
    const res = await getAdminUsersService({ page: currentPage.value, pageSize })
    users.value = res.data.list || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}

function formatDate(t) {
  if (!t) return null
  return new Date(t).toLocaleDateString('zh-CN')
}

onMounted(fetchUsers)
</script>

<style scoped>
.admin-users {}

.page-top {
  margin-bottom: 24px;
}
.page-title { font-size: 24px; font-weight: 800; color: var(--c-text); }
.page-subtitle { font-size: 14px; color: var(--c-text-3); margin-top: 4px; }

.users-table { overflow: hidden; }

.table-header {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid var(--c-border);
  background: var(--c-surface-2);
}

.th {
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--c-text-3);
}

.table-row {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid var(--c-border-light);
  transition: background var(--transition);
}
.table-row:last-child { border-bottom: none; }
.table-row:hover { background: var(--c-surface-2); }

.td { padding-right: 12px; }

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-name { font-weight: 600; font-size: 14px; color: var(--c-text); }
.user-id { font-size: 12px; color: var(--c-text-3); }

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BrandMark from '../components/common/BrandMark.vue'
import { useUserInfoStore } from '../store/userInfo.js'
import { getAdminStatsService } from '../api/admin.js'
import { DEFAULT_AVATAR_URL as defaultAvatar } from '../constants/assets.js'

const route = useRoute()
const router = useRouter()
const userInfoStore = useUserInfoStore()
const user = computed(() => userInfoStore.userInfo)
const pendingArticles = ref(0)
const items = [
  { label: '概览', path: '/admin/home', icon: '⌂' },
  { label: '文章审核', path: '/admin/articles', icon: '▤' },
  { label: '标签管理', path: '/admin/tags', icon: '#' },
  { label: '公告管理', path: '/admin/announcements', icon: '◖' },
  { label: '用户管理', path: '/admin/users', icon: '♙' },
]

onMounted(async () => {
  try {
    const res = await getAdminStatsService()
    pendingArticles.value = Number(res.data?.pendingArticles || 0)
  } catch {}
})
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <router-link to="/admin/home" class="admin-brand"><BrandMark admin /></router-link>
      <nav>
        <button
          v-for="item in items"
          :key="item.path"
          :class="{ active: route.path === item.path, disabled: item.disabled }"
          @click="item.disabled ? null : router.push(item.path)"
        ><i>{{ item.icon }}</i><span>{{ item.label }}</span><em v-if="item.label === '文章审核' && pendingArticles">{{ pendingArticles }}</em></button>
      </nav>
      <div class="admin-user">
        <img :src="user?.avatarImage || defaultAvatar" alt="" />
        <div><strong>{{ user?.nickname || 'BYTE 运营' }}</strong><span>超级管理员</span></div>
        <button @click="router.push('/home')">↗</button>
      </div>
    </aside>
    <section class="admin-workspace">
      <header class="admin-topbar">
        <div class="admin-search"><span>⌕</span><input placeholder="搜索用户、文章或操作" /></div>
        <router-link to="/home">返回站点</router-link>
        <router-link to="/announcement" class="notification">♧<i></i></router-link>
        <img :src="user?.avatarImage || defaultAvatar" alt="" />
      </header>
      <main><router-view /></main>
    </section>
  </div>
</template>

<style scoped>
.admin-layout { display: grid; height: 100%; grid-template-columns: 236px minmax(0, 1fr); overflow: hidden; background: #f5f6f8; }
.admin-sidebar { display: flex; min-height: 0; flex-direction: column; padding: 22px 14px 14px; border-right: 1px solid var(--c-border); background: var(--c-surface); }
.admin-brand { display: flex; padding: 0 9px 23px; border-bottom: 1px solid var(--c-border); }
.admin-sidebar nav { display: flex; flex: 1; flex-direction: column; gap: 5px; padding: 18px 0; }
.admin-sidebar nav button { display: grid; grid-template-columns: 28px 1fr auto; align-items: center; gap: 8px; padding: 10px 12px; border: 0; border-radius: var(--radius-sm); background: transparent; color: var(--c-text-3); text-align: left; transition: all var(--transition); }
.admin-sidebar nav button:hover:not(.disabled), .admin-sidebar nav button.active { background: var(--c-primary); color: #fff; box-shadow: 0 8px 20px rgba(var(--c-primary-rgb), .2); }
.admin-sidebar nav button.disabled { opacity: .55; cursor: not-allowed; }.admin-sidebar nav i { font-size: 18px; font-style: normal; text-align: center; }.admin-sidebar nav span { font-weight: 600; }.admin-sidebar nav em { min-width: 22px; padding: 1px 6px; border-radius: var(--radius-full); background: var(--c-danger); color: #fff; font-size: 9px; font-style: normal; text-align: center; }
.admin-user { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 9px; padding: 11px; border-radius: var(--radius); background: var(--c-surface-2); }.admin-user img { width: 38px; height: 38px; border-radius: 50%; object-fit: cover; }.admin-user div { display: flex; min-width: 0; flex-direction: column; }.admin-user strong { overflow: hidden; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }.admin-user span { color: var(--c-text-4); font-size: 9px; }.admin-user button { border: 0; background: transparent; color: var(--c-text-3); }
.admin-workspace { display: flex; min-width: 0; flex-direction: column; overflow: hidden; }.admin-topbar { display: flex; height: 64px; flex: 0 0 auto; align-items: center; gap: 16px; padding: 0 28px; border-bottom: 1px solid var(--c-border); background: var(--c-surface); }.admin-search { display: flex; width: min(420px, 42vw); align-items: center; gap: 8px; padding: 8px 12px; border: 1px solid var(--c-border); border-radius: var(--radius-sm); background: var(--c-surface-2); }.admin-search input { min-width: 0; flex: 1; border: 0; outline: 0; background: transparent; color: var(--c-text); }.admin-topbar > a:first-of-type { margin-left: auto; color: var(--c-text-3); font-size: 12px; }.admin-topbar > img { width: 34px; height: 34px; border-radius: 50%; object-fit: cover; }.notification { position: relative; font-size: 18px; }.notification i { position: absolute; top: 0; right: -1px; width: 6px; height: 6px; border-radius: 50%; background: var(--c-danger); }.admin-workspace > main { min-height: 0; flex: 1; overflow: auto; }
@media (max-width: 820px) { .admin-layout { grid-template-columns: 70px minmax(0, 1fr); }.admin-brand :deep(.brand-mark__text), .admin-sidebar nav span, .admin-sidebar nav em, .admin-user div, .admin-user button { display: none; }.admin-sidebar nav button { grid-template-columns: 1fr; }.admin-user { display: grid; place-items: center; padding: 6px; }.admin-topbar { padding: 0 14px; } }
</style>

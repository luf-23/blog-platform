<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Search, Calendar, Position } from "@element-plus/icons-vue";

import PageHeader from "../../components/common/PageHeader.vue";
import EmptyState from "../../components/common/EmptyState.vue";
import { getUsersService } from "../../api/admin.js";

const router = useRouter();
const userList = ref([]);
const loading = ref(false);
const keyword = ref("");

async function fetchUsers() {
  loading.value = true;
  try {
    const result = await getUsersService();
    userList.value = (result.data || []).map((item) => ({
      userId: item.userId,
      username: item.username,
      nickname: item.nickname,
      signature: item.signature,
      avatarImage: item.avatarImage,
      backgroundImage: item.backgroundImage,
      lastLoginTime: item.lastLogin,
      lastLoginIp: item.lastLoginIp,
      createTime: item.createTime,
      updateTime: item.updateTime
    }));
  } catch {
    ElMessage.error("加载用户列表失败");
  } finally {
    loading.value = false;
  }
}

fetchUsers();

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  if (!kw) return userList.value;
  return userList.value.filter((u) => {
    return (
      (u.username || "").toLowerCase().includes(kw) ||
      (u.nickname || "").toLowerCase().includes(kw)
    );
  });
});

function open(user) {
  router.push({ name: "Profile", query: { author: user.username } });
}

function formatDate(value) {
  if (!value) return "暂无";
  return new Date(value).toLocaleString("zh-CN", { hour12: false });
}
</script>

<template>
  <div class="bp-page">
    <PageHeader title="用户管理" subtitle="查看、搜索平台用户">
      <template #actions>
        <el-input
          v-model="keyword"
          :prefix-icon="Search"
          placeholder="按用户名 / 昵称搜索"
          clearable
          style="width: 240px"
        />
      </template>
    </PageHeader>

    <div v-loading="loading">
      <div v-if="filtered.length" class="user-grid">
        <article
          v-for="user in filtered"
          :key="user.userId"
          class="user-card bp-card bp-card-hover"
          @click="open(user)"
        >
          <div
            class="user-card__cover"
            :style="{
              backgroundImage: user.backgroundImage
                ? `url(${user.backgroundImage})`
                : 'none'
            }"
          />
          <div class="user-card__head">
            <el-avatar :size="64" :src="user.avatarImage || '/avatar/avatar1.png'" />
            <div>
              <h4>{{ user.nickname || user.username }}</h4>
              <p>@{{ user.username }}</p>
            </div>
          </div>
          <p class="user-card__signature">
            {{ user.signature || "TA 还没有签名" }}
          </p>
          <div class="user-card__meta">
            <span>
              <el-icon><Calendar /></el-icon>
              注册：{{ formatDate(user.createTime) }}
            </span>
            <span>
              <el-icon><Position /></el-icon>
              IP：{{ user.lastLoginIp || "未知" }}
            </span>
          </div>
        </article>
      </div>
      <EmptyState
        v-else-if="!loading"
        title="没有匹配的用户"
        description="尝试调整搜索关键词"
      />
    </div>
  </div>
</template>

<style scoped>
.user-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.user-card {
  cursor: pointer;
  padding: 0;
  overflow: hidden;
}

.user-card__cover {
  height: 96px;
  background-size: cover;
  background-position: center;
  background-color: var(--bp-gradient-soft);
  background-image: var(--bp-gradient-hero);
}

.user-card__head {
  display: flex;
  align-items: flex-end;
  gap: 14px;
  padding: 0 18px;
  margin-top: -32px;
}

.user-card__head h4 {
  font-size: 15px;
  font-weight: 600;
}

.user-card__head p {
  margin-top: 2px;
  font-size: 12px;
  color: var(--bp-color-text-tertiary);
}

.user-card__signature {
  margin: 16px 18px 12px;
  padding: 10px 12px;
  font-size: 13px;
  color: var(--bp-color-text-secondary);
  background: var(--bp-color-bg-soft);
  border-radius: 10px;
  font-style: italic;
  border-left: 3px solid var(--bp-color-primary);
}

.user-card__meta {
  padding: 0 18px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  color: var(--bp-color-text-tertiary);
}

.user-card__meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
</style>

<script setup>
import { useRouter } from "vue-router";

const router = useRouter();

function goHome() {
  router.push("/home");
}

function goBack() {
  if (window.history.length > 1) router.back();
  else router.push("/home");
}
</script>

<template>
  <div class="not-found">
    <div class="not-found__content">
      <span class="not-found__badge">404</span>
      <h1 class="not-found__title">页面走丢了</h1>
      <p class="not-found__desc">
        我们没找到你想访问的页面。
        也许它已经被移除、改名，或者从来没存在过。
      </p>
      <div class="not-found__actions">
        <el-button type="primary" @click="goHome">返回首页</el-button>
        <el-button @click="goBack">返回上一页</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background: var(--bp-color-bg);
}

.not-found__content {
  max-width: 460px;
  text-align: center;
}

.not-found__badge {
  display: inline-block;
  font-size: 88px;
  font-weight: 800;
  letter-spacing: -0.04em;
  background: var(--bp-gradient-hero);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.not-found__title {
  margin-top: 8px;
  font-size: 26px;
}

.not-found__desc {
  margin-top: 12px;
  color: var(--bp-color-text-secondary);
  line-height: 1.7;
}

.not-found__actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>

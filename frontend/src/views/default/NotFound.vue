<script setup>
import { useRouter } from "vue-router";
const router = useRouter();
function goHome() { router.push("/home"); }
function goBack() { if (window.history.length > 1) router.back(); else router.push("/home"); }
</script>

<template>
  <div class="not-found">
    <div class="nf-content">
      <div class="nf-code">404</div>
      <h1 class="nf-title">页面走丢了</h1>
      <p class="nf-desc">这个页面不存在，可能已被删除或移动到了其他地址。</p>
      <div class="nf-actions">
        <button class="btn btn-primary" @click="goHome">返回首页</button>
        <button class="btn btn-secondary" @click="goBack">返回上一页</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background: var(--c-bg);
}

.nf-content {
  max-width: 420px;
  text-align: center;
}

.nf-code {
  font-size: 96px;
  font-weight: 900;
  line-height: 1;
  background: linear-gradient(135deg, var(--c-primary) 0%, #7c3aed 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 16px;
}

.nf-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--c-text);
  margin-bottom: 12px;
}

.nf-desc {
  font-size: 15px;
  color: var(--c-text-3);
  line-height: 1.7;
  margin-bottom: 28px;
}

.nf-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>

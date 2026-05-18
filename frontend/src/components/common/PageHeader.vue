<script setup>
import { useRouter } from "vue-router";
import { ArrowLeft } from "@element-plus/icons-vue";

defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: "" },
  showBack: { type: Boolean, default: false }
});

const router = useRouter();

function back() {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push("/home");
  }
}
</script>

<template>
  <header class="page-header">
    <div class="page-header__left">
      <button v-if="showBack" class="page-header__back" @click="back">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </button>
      <div class="page-header__title-block">
        <h1 class="page-header__title">{{ title }}</h1>
        <p v-if="subtitle" class="page-header__subtitle">{{ subtitle }}</p>
      </div>
    </div>
    <div class="page-header__right">
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 8px 0;
  flex-wrap: wrap;
}

.page-header__left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  flex: 1;
}

.page-header__back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: var(--bp-color-bg-elevated);
  border: 1px solid var(--bp-color-border);
  border-radius: 10px;
  color: var(--bp-color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.page-header__back:hover {
  background: var(--bp-color-primary-soft);
  color: var(--bp-color-primary);
}

.page-header__title-block {
  min-width: 0;
}

.page-header__title {
  font-size: clamp(20px, 2.4vw, 26px);
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--bp-color-text-primary);
}

.page-header__subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: var(--bp-color-text-tertiary);
}

.page-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
</style>

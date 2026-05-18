<script setup>
import { useRouter } from "vue-router";
import { ArrowLeft } from "@element-plus/icons-vue";

defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: "" },
  showBack: { type: Boolean, default: false },
  compact: { type: Boolean, default: false }
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
  <header
    class="page-header"
    :class="{ 'page-header--compact': compact }"
  >
    <div class="page-header__left">
      <button v-if="showBack" class="page-header__back" @click="back">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <div class="page-header__title-wrap">
        <h1 class="page-header__title">{{ title }}</h1>
        <p v-if="subtitle && !compact" class="page-header__subtitle">
          {{ subtitle }}
        </p>
      </div>
    </div>
    <div v-if="$slots.actions" class="page-header__right">
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.page-header--compact {
  min-height: 36px;
}

.page-header__left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
}

.page-header__back {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  background: transparent;
  border: 1px solid var(--bp-color-border);
  border-radius: 8px;
  color: var(--bp-color-text-secondary);
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease, border-color 0.2s ease;
  flex-shrink: 0;
}

.page-header__back:hover {
  background: var(--bp-color-primary-soft);
  color: var(--bp-color-primary);
  border-color: var(--bp-color-primary-soft-strong);
}

.page-header__title-wrap {
  min-width: 0;
}

.page-header__title {
  font-size: clamp(18px, 2vw, 20px);
  font-weight: 600;
  letter-spacing: -0.02em;
  color: var(--bp-color-text-primary);
  line-height: 1.3;
}

.page-header__subtitle {
  margin-top: 2px;
  font-size: 12px;
  color: var(--bp-color-text-tertiary);
  line-height: 1.4;
}

.page-header__right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
</style>

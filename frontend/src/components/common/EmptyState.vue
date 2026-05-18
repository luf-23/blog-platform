<script setup>
defineProps({
  title: { type: String, default: "暂无数据" },
  description: { type: String, default: "" }
});
</script>

<template>
  <div class="empty-state">
    <div class="empty-state__icon">
      <svg viewBox="0 0 64 64" fill="none">
        <circle cx="32" cy="32" r="30" stroke="currentColor" stroke-width="1.5" opacity="0.2" />
        <path
          d="M20 38c4-2 8-3 12-3s8 1 12 3"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
        />
        <circle cx="24" cy="26" r="2" fill="currentColor" />
        <circle cx="40" cy="26" r="2" fill="currentColor" />
      </svg>
    </div>
    <h4 class="empty-state__title">{{ title }}</h4>
    <p v-if="description" class="empty-state__desc">{{ description }}</p>
    <div class="empty-state__action">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  color: var(--bp-color-text-tertiary);
  text-align: center;
}

.empty-state__icon {
  width: 96px;
  height: 96px;
  border-radius: 999px;
  background: var(--bp-color-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--bp-color-primary);
  margin-bottom: 16px;
}

.empty-state__icon svg {
  width: 56px;
  height: 56px;
}

.empty-state__title {
  font-size: 16px;
  color: var(--bp-color-text-primary);
  margin-bottom: 6px;
}

.empty-state__desc {
  font-size: 13px;
  color: var(--bp-color-text-tertiary);
  max-width: 320px;
  line-height: 1.5;
}

.empty-state__action {
  margin-top: 16px;
}
</style>

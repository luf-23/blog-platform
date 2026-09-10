<script setup>
defineProps({
  compact: { type: Boolean, default: false },
  admin: { type: Boolean, default: false }
})
</script>

<template>
  <span
    class="brand-mark"
    :class="{ 'brand-mark--compact': compact }"
    role="img"
    :aria-label="admin ? 'Blog-Platform 管理后台' : 'Blog-Platform'"
  >
    <span class="brand-mark__icon" aria-hidden="true">
      <span>BP</span>
    </span>
    <span v-if="admin && !compact" class="brand-mark__text">
      <small>ADMIN</small>
    </span>
  </span>
</template>

<style scoped>
.brand-mark {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--c-text);
}

.brand-mark__icon {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border: 1px solid color-mix(in srgb, var(--c-primary) 78%, #0f172a);
  border-radius: var(--radius);
  background: var(--c-primary);
  box-shadow: 0 5px 14px rgba(var(--c-primary-rgb), .18);
  color: #fff;
}

.brand-mark__icon span {
  font-family: Inter, ui-sans-serif, sans-serif;
  font-size: 13px;
  font-weight: 900;
  line-height: 1;
  letter-spacing: -.08em;
}

.brand-mark__text {
  display: inline-flex;
  align-items: baseline;
  gap: 7px;
  white-space: nowrap;
}

.brand-mark__text small {
  color: var(--c-text-3);
  font: 700 9px ui-monospace, "SFMono-Regular", Consolas, monospace;
  letter-spacing: .1em;
}

.brand-mark--compact .brand-mark__icon {
  width: 30px;
  height: 30px;
  border-radius: var(--radius-sm);
  box-shadow: none;
}

.brand-mark--compact .brand-mark__icon span { font-size: 11px; }
</style>

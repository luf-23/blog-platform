<script setup>
defineProps({
  compact: { type: Boolean, default: false },
  admin: { type: Boolean, default: false }
})
</script>

<template>
  <span class="brand-mark" :class="{ 'brand-mark--compact': compact }">
    <span class="brand-mark__icon" aria-hidden="true">
      <span>{墨}</span>
    </span>
    <span v-if="!compact" class="brand-mark__text">
      <strong>墨语</strong>
      <small v-if="admin">管理台</small>
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
  border: 1px solid #1d4ed8;
  border-radius: 8px;
  background: #2563eb;
  color: #fff;
}
.brand-mark__icon span { font-family: ui-monospace, "SFMono-Regular", Consolas, monospace; font-size: 13px; font-weight: 800; line-height: 1; letter-spacing: -.12em; transform: translateX(-1px); }

.brand-mark__text {
  display: flex;
  align-items: baseline;
  gap: 7px;
  white-space: nowrap;
}

.brand-mark__text strong {
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: .04em;
}

.brand-mark__text small {
  color: var(--c-text-3);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: .04em;
}

.brand-mark--compact .brand-mark__icon { width: 30px; height: 30px; }
</style>

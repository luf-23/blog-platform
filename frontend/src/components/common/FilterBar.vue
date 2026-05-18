<template>
  <div class="filter-bar">
    <div class="filter-bar__fields">
      <slot />
    </div>
    <div v-if="$slots.actions" class="filter-bar__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 10px 12px;
  border-radius: var(--bp-radius-sm);
  background: var(--bp-color-bg-elevated);
  border: 1px solid var(--bp-color-border);
}

.filter-bar__fields {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
  flex-wrap: wrap;
}

.filter-bar__fields > :deep(.el-input),
.filter-bar__fields > :deep(.el-select) {
  flex: 1;
  min-width: 140px;
}

.filter-bar__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-bar__fields {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-bar__fields > :deep(.el-input),
  .filter-bar__fields > :deep(.el-select) {
    width: 100%;
    min-width: 0;
  }

  .filter-bar__actions {
    justify-content: flex-end;
  }
}
</style>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  tags: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  limit: { type: Number, default: 5 }
})
const emit = defineEmits(['update:modelValue'])

const activeParentId = ref(null)
const keyword = ref('')
const groups = computed(() => props.tags.filter(tag => tag.parentId == null).map(parent => ({
  ...parent,
  children: props.tags
    .filter(tag => tag.parentId === parent.tagId)
    .sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0) || a.tagName.localeCompare(b.tagName, 'zh-CN'))
})).filter(group => group.children.length))
const activeGroup = computed(() => groups.value.find(group => group.tagId === activeParentId.value) || groups.value[0])
const selectedTags = computed(() => props.modelValue.map(name => props.tags.find(tag => tag.tagName === name)).filter(Boolean))
const searchResults = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  if (!query) return []
  return props.tags.filter(tag => tag.parentId != null && (
    tag.tagName?.toLowerCase().includes(query) || tag.parentName?.toLowerCase().includes(query)
  ))
})

watch(groups, value => {
  if (!value.some(group => group.tagId === activeParentId.value)) activeParentId.value = value[0]?.tagId ?? null
}, { immediate: true })

function isSelected(tag) { return props.modelValue.includes(tag.tagName) }
function toggle(tag) {
  if (isSelected(tag)) {
    emit('update:modelValue', props.modelValue.filter(name => name !== tag.tagName))
    return
  }
  if (props.modelValue.length >= props.limit) return ElMessage.warning(`一篇文章最多选择 ${props.limit} 个标签`)
  emit('update:modelValue', [...props.modelValue, tag.tagName])
}
function remove(name) { emit('update:modelValue', props.modelValue.filter(item => item !== name)) }
</script>

<template>
  <div class="taxonomy-picker" :class="{ loading }">
    <div v-if="selectedTags.length" class="selected-panel">
      <div class="selected-heading"><span>已选标签</span><small>{{ modelValue.length }}/{{ limit }}</small></div>
      <div class="selected-tags">
        <span v-for="tag in selectedTags" :key="tag.tagId" :title="`${tag.parentName} / ${tag.tagName}`">
          <i>#</i><strong>{{ tag.tagName }}</strong>
          <button type="button" :aria-label="`移除 ${tag.tagName}`" @click="remove(tag.tagName)">×</button>
        </span>
      </div>
    </div>

    <label class="tag-search">
      <span>⌕</span>
      <input v-model="keyword" type="search" placeholder="搜索二级标签" />
      <small>{{ modelValue.length }}/{{ limit }}</small>
    </label>

    <div v-if="loading" class="picker-state">正在加载标签体系…</div>
    <div v-else-if="!groups.length" class="picker-state">管理员还没有配置可选的二级标签</div>
    <template v-else-if="keyword.trim()">
      <div v-if="searchResults.length" class="search-results">
        <button v-for="tag in searchResults" :key="tag.tagId" type="button" :class="{ selected: isSelected(tag) }" @click="toggle(tag)">
          <span><small>{{ tag.parentName }}</small><strong># {{ tag.tagName }}</strong></span><i>{{ isSelected(tag) ? '✓' : '+' }}</i>
        </button>
      </div>
      <div v-else class="picker-state compact">没有匹配的二级标签</div>
    </template>
    <template v-else>
      <div class="picker-section-title"><span>内容方向</span><small>单选</small></div>
      <div class="parent-tabs" role="tablist" aria-label="一级标签">
        <button v-for="group in groups" :key="group.tagId" type="button" role="tab" :aria-selected="activeGroup?.tagId === group.tagId" :class="{ active: activeGroup?.tagId === group.tagId }" @click="activeParentId = group.tagId">
          {{ group.tagName }}<small>{{ group.children.length }}</small>
        </button>
      </div>
      <div class="picker-section-title leaf-title"><span>{{ activeGroup?.tagName }}标签</span><small>可多选</small></div>
      <div class="leaf-grid" role="tabpanel">
        <button v-for="tag in activeGroup?.children" :key="tag.tagId" type="button" :class="{ selected: isSelected(tag) }" @click="toggle(tag)">
          <span># {{ tag.tagName }}</span><i>{{ isSelected(tag) ? '✓' : '+' }}</i>
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.taxonomy-picker { overflow: hidden; border: 1px solid var(--c-border); border-radius: 7px; background: var(--c-surface); }
.selected-panel { padding: 10px; border-bottom: 1px solid var(--c-border-light); background: var(--c-surface-2); }
.selected-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 7px; color: var(--c-text-3); font-size: 10px; font-weight: 700; }
.selected-heading small { color: var(--c-primary); font-size: 10px; }
.selected-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.selected-tags > span { display: inline-flex; min-width: 0; min-height: 29px; align-items: center; gap: 5px; padding: 4px 6px 4px 8px; border: 1px solid color-mix(in srgb, var(--c-primary) 24%, var(--c-border)); border-radius: 6px; background: var(--c-surface); color: var(--c-text-2); font-size: 10px; box-shadow: var(--shadow-xs); }
.selected-tags i { color: var(--c-primary); font-style: normal; font-weight: 800; }
.selected-tags strong { max-width: 92px; overflow: hidden; font-size: 10px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.selected-tags button { display: grid; width: 18px; height: 18px; padding: 0; place-items: center; border: 0; border-radius: 50%; background: var(--c-surface-2); color: var(--c-text-4); font-size: 13px; line-height: 1; }
.selected-tags button:hover { color: var(--c-danger); }

.tag-search { display: flex; height: 40px; align-items: center; gap: 8px; padding: 0 11px; border-bottom: 1px solid var(--c-border-light); }
.tag-search > span { color: var(--c-text-4); font-size: 15px; }
.tag-search input { min-width: 0; flex: 1; border: 0; outline: 0; background: transparent; color: var(--c-text); font-size: 12px; }
.tag-search input::placeholder { color: var(--c-text-4); }
.tag-search small { color: var(--c-text-4); font-size: 10px; }

.picker-section-title { display: flex; align-items: center; justify-content: space-between; padding: 10px 10px 6px; color: var(--c-text-3); font-size: 11px; font-weight: 650; }
.picker-section-title small { color: var(--c-text-4); font-size: 9px; font-weight: 500; }
.picker-section-title.leaf-title { padding-top: 11px; border-top: 1px solid var(--c-border-light); }

.parent-tabs { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 6px; padding: 0 9px 10px; }
.parent-tabs button { display: flex; min-width: 0; min-height: 35px; align-items: center; justify-content: space-between; gap: 5px; padding: 7px 8px; overflow: hidden; border: 1px solid var(--c-border); border-radius: 5px; background: var(--c-surface-2); color: var(--c-text-3); font-size: 11px; text-align: left; white-space: nowrap; }
.parent-tabs button.active { border-color: color-mix(in srgb, var(--c-primary) 45%, var(--c-border)); background: var(--c-primary-soft); color: var(--c-primary); font-weight: 700; }
.parent-tabs button:hover { border-color: var(--c-border-strong); color: var(--c-text); }
.parent-tabs small { display: grid; min-width: 18px; height: 18px; flex: 0 0 auto; place-items: center; border-radius: 9px; background: var(--c-surface); color: var(--c-text-4); font-size: 9px; }

.leaf-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 6px; padding: 0 9px 10px; }
.leaf-grid button,
.search-results button { display: flex; min-width: 0; min-height: 35px; align-items: center; justify-content: space-between; gap: 6px; padding: 7px 8px; overflow: hidden; border: 1px solid var(--c-border-light); border-radius: 5px; background: var(--c-surface-2); color: var(--c-text-2); font-size: 11px; text-align: left; }
.leaf-grid button span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.leaf-grid button:hover,
.search-results button:hover { border-color: color-mix(in srgb, var(--c-primary) 45%, var(--c-border)); }
.leaf-grid button.selected,
.search-results button.selected { border-color: color-mix(in srgb, var(--c-primary) 55%, var(--c-border)); background: var(--c-primary-soft); color: var(--c-primary); font-weight: 700; }
.leaf-grid i,
.search-results i { flex: 0 0 auto; color: var(--c-primary); font-size: 12px; font-style: normal; font-weight: 800; }

.search-results { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 6px; padding: 9px; }
.search-results button span { display: flex; min-width: 0; flex-direction: column; }
.search-results small { overflow: hidden; color: var(--c-text-4); font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }
.search-results strong { overflow: hidden; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.picker-state { display: grid; min-height: 100px; padding: 18px; place-items: center; color: var(--c-text-4); font-size: 11px; text-align: center; }
.picker-state.compact { min-height: 70px; }
</style>

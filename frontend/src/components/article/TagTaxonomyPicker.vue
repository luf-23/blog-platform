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
    <div v-if="selectedTags.length" class="selected-tags">
      <span v-for="tag in selectedTags" :key="tag.tagId">
        <small>{{ tag.parentName }}</small># {{ tag.tagName }}
        <button type="button" :aria-label="`移除 ${tag.tagName}`" @click="remove(tag.tagName)">×</button>
      </span>
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
      <div class="parent-tabs" role="tablist" aria-label="一级标签">
        <button v-for="group in groups" :key="group.tagId" type="button" role="tab" :aria-selected="activeGroup?.tagId === group.tagId" :class="{ active: activeGroup?.tagId === group.tagId }" @click="activeParentId = group.tagId">
          {{ group.tagName }}<small>{{ group.children.length }}</small>
        </button>
      </div>
      <div class="leaf-grid" role="tabpanel">
        <button v-for="tag in activeGroup?.children" :key="tag.tagId" type="button" :class="{ selected: isSelected(tag) }" @click="toggle(tag)">
          <span># {{ tag.tagName }}</span><i>{{ isSelected(tag) ? '✓' : '+' }}</i>
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.taxonomy-picker { overflow: hidden; border: 1px solid var(--c-border); border-radius: 8px; background: var(--c-surface); }
.selected-tags { display: flex; flex-wrap: wrap; gap: 5px; padding: 8px; border-bottom: 1px solid var(--c-border-light); background: var(--c-primary-soft); }
.selected-tags > span { display: inline-flex; align-items: center; gap: 4px; padding: 4px 6px; border: 1px solid color-mix(in srgb, var(--c-primary) 28%, var(--c-border)); border-radius: 5px; background: var(--c-surface); color: var(--c-primary); font-size: 10px; font-weight: 700; }
.selected-tags small { color: var(--c-text-4); font-size: 8px; font-weight: 500; }.selected-tags button { padding: 0; border: 0; background: transparent; color: var(--c-text-4); font-size: 14px; line-height: 1; }
.tag-search { display: flex; height: 36px; align-items: center; gap: 7px; padding: 0 9px; border-bottom: 1px solid var(--c-border-light); }.tag-search > span { color: var(--c-text-4); }.tag-search input { min-width: 0; flex: 1; border: 0; outline: 0; background: transparent; color: var(--c-text); font-size: 10px; }.tag-search small { color: var(--c-text-4); font-size: 9px; }
.parent-tabs { display: flex; gap: 3px; padding: 6px; overflow-x: auto; border-bottom: 1px solid var(--c-border-light); background: var(--c-surface-2); scrollbar-width: thin; }.parent-tabs button { display: inline-flex; min-width: max-content; align-items: center; gap: 4px; padding: 5px 7px; border: 1px solid transparent; border-radius: 5px; background: transparent; color: var(--c-text-3); font-size: 9px; }.parent-tabs button.active { border-color: color-mix(in srgb, var(--c-primary) 30%, var(--c-border)); background: var(--c-surface); color: var(--c-primary); font-weight: 800; }.parent-tabs small { display: grid; min-width: 15px; height: 15px; place-items: center; border-radius: 8px; background: var(--c-surface-3); font-size: 8px; }
.leaf-grid { display: grid; max-height: 176px; grid-template-columns: 1fr; gap: 5px; padding: 7px; overflow-y: auto; }.leaf-grid button, .search-results button { display: flex; min-height: 31px; align-items: center; justify-content: space-between; gap: 8px; padding: 5px 8px; border: 1px solid var(--c-border-light); border-radius: 5px; background: var(--c-surface-2); color: var(--c-text-2); font-size: 9px; text-align: left; }.leaf-grid button:hover, .search-results button:hover { border-color: #93c5fd; }.leaf-grid button.selected, .search-results button.selected { border-color: #93c5fd; background: var(--c-primary-soft); color: var(--c-primary); font-weight: 800; }.leaf-grid i, .search-results i { color: var(--c-primary); font-size: 11px; font-style: normal; font-weight: 900; }
.search-results { display: flex; max-height: 200px; flex-direction: column; gap: 5px; padding: 7px; overflow-y: auto; }.search-results button span { display: flex; min-width: 0; flex-direction: column; }.search-results small { color: var(--c-text-4); font-size: 8px; }.search-results strong { overflow: hidden; font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }
.picker-state { display: grid; min-height: 110px; padding: 18px; place-items: center; color: var(--c-text-4); font-size: 9px; text-align: center; }.picker-state.compact { min-height: 70px; }
</style>

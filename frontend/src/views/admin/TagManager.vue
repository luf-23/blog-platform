<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addAdminTagService,
  deleteAdminTagService,
  getAdminTagsService,
  updateAdminTagService
} from '../../api/admin.js'

const tags = ref([])
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const dialogOpen = ref(false)
const editingId = ref(null)
const expanded = ref(new Set())
const form = reactive({ tagName: '', parentId: null, sortOrder: 0 })

const roots = computed(() => tags.value.filter(tag => tag.parentId == null))
const groups = computed(() => roots.value.map(root => ({
  ...root,
  children: tags.value
    .filter(tag => tag.parentId === root.tagId)
    .sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0) || a.tagName.localeCompare(b.tagName, 'zh-CN'))
})))
const filteredGroups = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  if (!query) return groups.value
  return groups.value.reduce((result, group) => {
    const rootMatch = group.tagName?.toLowerCase().includes(query)
    const children = rootMatch ? group.children : group.children.filter(tag => tag.tagName?.toLowerCase().includes(query))
    if (rootMatch || children.length) result.push({ ...group, children })
    return result
  }, [])
})
const leafTags = computed(() => tags.value.filter(tag => tag.parentId != null))
const usedCount = computed(() => leafTags.value.filter(tag => Number(tag.articleCount) > 0).length)
const totalReferences = computed(() => leafTags.value.reduce((sum, tag) => sum + Number(tag.articleCount || 0), 0))

async function fetchTags() {
  loading.value = true
  try {
    const result = await getAdminTagsService()
    tags.value = result.data || []
    if (!expanded.value.size) expanded.value = new Set(roots.value.map(tag => tag.tagId))
  } finally {
    loading.value = false
  }
}

function openCreate(parentId = null) {
  editingId.value = null
  Object.assign(form, { tagName: '', parentId, sortOrder: nextSortOrder(parentId) })
  dialogOpen.value = true
}

function openEdit(tag) {
  editingId.value = tag.tagId
  Object.assign(form, { tagName: tag.tagName, parentId: tag.parentId ?? null, sortOrder: tag.sortOrder || 0 })
  dialogOpen.value = true
}

function nextSortOrder(parentId) {
  const siblings = tags.value.filter(tag => (tag.parentId ?? null) === (parentId ?? null))
  return Math.max(0, ...siblings.map(tag => Number(tag.sortOrder || 0))) + 10
}

function toggleGroup(tagId) {
  const next = new Set(expanded.value)
  next.has(tagId) ? next.delete(tagId) : next.add(tagId)
  expanded.value = next
}

async function saveTag() {
  const name = form.tagName.trim()
  if (!name) return ElMessage.warning('请输入标签名称')
  if (name.length > 30) return ElMessage.warning('标签名称不能超过 30 个字符')

  saving.value = true
  const payload = { tagName: name, parentId: form.parentId, sortOrder: Number(form.sortOrder || 0) }
  try {
    if (editingId.value) {
      await updateAdminTagService(editingId.value, payload)
      ElMessage.success('标签已更新')
    } else {
      await addAdminTagService(payload)
      ElMessage.success(form.parentId ? '二级标签已创建' : '一级标签已创建')
    }
    dialogOpen.value = false
    await fetchTags()
    if (form.parentId) expanded.value.add(form.parentId)
  } finally {
    saving.value = false
  }
}

async function removeTag(tag) {
  const childCount = tags.value.filter(item => item.parentId === tag.tagId).length
  if (childCount) return ElMessage.warning(`请先移动或删除该大类下的 ${childCount} 个二级标签`)
  if (Number(tag.articleCount) > 0) return ElMessage.warning(`仍有 ${tag.articleCount} 篇文章使用该标签，请先调整相关文章`)
  try {
    await ElMessageBox.confirm(`删除${tag.parentId ? '二级标签' : '一级标签'}「${tag.tagName}」？`, '删除标签', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await deleteAdminTagService(tag.tagId)
    tags.value = tags.value.filter(item => item.tagId !== tag.tagId)
    ElMessage.success('标签已删除')
  } catch {}
}

function formatDate(value) {
  if (!value) return '—'
  return new Date(value).toLocaleDateString('zh-CN')
}

onMounted(fetchTags)
</script>

<template>
  <div class="admin-page tag-manager page-container">
    <header class="admin-page__head">
      <div><span>CONTENT TAXONOMY</span><h1>标签体系</h1><p>一级标签组织内容领域，二级标签用于文章标注与检索。</p></div>
      <button class="primary-action" @click="openCreate(null)">＋ 新建一级标签</button>
    </header>

    <section class="metric-row">
      <article><span>一级标签</span><strong>{{ roots.length }}</strong><small>内容大类</small></article>
      <article><span>二级标签</span><strong>{{ leafTags.length }}</strong><small>{{ usedCount }} 个正在使用</small></article>
      <article><span>内容关联</span><strong>{{ totalReferences }}</strong><small>篇次引用</small></article>
    </section>

    <section class="manager-card">
      <header class="manager-toolbar">
        <div class="manager-search"><span>⌕</span><input v-model="keyword" placeholder="搜索一级或二级标签" /></div>
        <small>{{ filteredGroups.length }} 个大类 · {{ leafTags.length }} 个可选标签</small>
      </header>

      <div class="taxonomy-head"><span>大类 / 二级标签</span><span>使用情况</span><span>操作</span></div>
      <div v-if="loading" class="manager-state">正在读取标签体系…</div>
      <div v-else-if="filteredGroups.length" class="taxonomy-list">
        <article v-for="group in filteredGroups" :key="group.tagId" class="taxonomy-group">
          <header>
            <button class="group-toggle" :aria-expanded="expanded.has(group.tagId)" @click="toggleGroup(group.tagId)">
              <i>{{ expanded.has(group.tagId) ? '−' : '+' }}</i>
              <span><strong>{{ group.tagName }}</strong><small>{{ group.children.length }} 个二级标签 · 创建于 {{ formatDate(group.createTime) }}</small></span>
            </button>
            <div class="usage"><strong>{{ group.articleCount || 0 }}</strong><span>篇文章</span></div>
            <div class="row-actions">
              <button class="add-child" @click="openCreate(group.tagId)">＋ 添加二级</button>
              <button @click="openEdit(group)">编辑</button>
              <button class="danger" @click="removeTag(group)">删除</button>
            </div>
          </header>

          <div v-if="expanded.has(group.tagId)" class="children">
            <div v-for="tag in group.children" :key="tag.tagId" class="child-row">
              <div class="child-name"><i>#</i><span><strong>{{ tag.tagName }}</strong><small>排序 {{ tag.sortOrder || 0 }}</small></span></div>
              <div class="usage"><strong>{{ tag.articleCount || 0 }}</strong><span>篇文章</span></div>
              <div class="row-actions">
                <button @click="openEdit(tag)">编辑</button>
                <button class="danger" :class="{ muted: Number(tag.articleCount) > 0 }" @click="removeTag(tag)">删除</button>
              </div>
            </div>
            <button v-if="!group.children.length" class="empty-children" @click="openCreate(group.tagId)">这个大类还没有二级标签，立即添加 →</button>
          </div>
        </article>
      </div>
      <div v-else class="manager-state"><strong>{{ keyword ? '没有匹配的标签' : '还没有标签体系' }}</strong><p>{{ keyword ? '换个关键词试试。' : '先创建一级标签，再为它添加二级标签。' }}</p></div>
    </section>

    <el-dialog v-model="dialogOpen" :title="editingId ? '编辑标签' : (form.parentId ? '新建二级标签' : '新建一级标签')" width="460px" destroy-on-close>
      <div class="level-notice" :class="form.parentId ? 'is-leaf' : 'is-root'">
        <i>{{ form.parentId ? 'L2' : 'L1' }}</i>
        <span><strong>{{ form.parentId ? '二级标签可贴到文章' : '一级标签仅用于分组' }}</strong><small>{{ form.parentId ? '作者将在对应大类下选择它。' : '创建后请继续添加二级标签。' }}</small></span>
      </div>
      <label class="field-label" for="tag-parent">所属一级标签</label>
      <el-select id="tag-parent" v-model="form.parentId" clearable placeholder="不选择表示一级标签" style="width:100%">
        <el-option v-for="root in roots.filter(item => item.tagId !== editingId)" :key="root.tagId" :label="root.tagName" :value="root.tagId" />
      </el-select>
      <label class="field-label spaced" for="tag-name">标签名称</label>
      <el-input id="tag-name" v-model="form.tagName" maxlength="30" show-word-limit :placeholder="form.parentId ? '例如：前端开发' : '例如：技术研发'" @keyup.enter="saveTag" />
      <label class="field-label spaced" for="tag-order">同级排序</label>
      <el-input-number id="tag-order" v-model="form.sortOrder" :min="0" :max="9999" :step="10" controls-position="right" />
      <template #footer><el-button @click="dialogOpen = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveTag">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-page { padding-top: 28px; padding-bottom: 48px; }
.admin-page__head { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 20px; }
.admin-page__head span { color: var(--c-primary); font-size: 10px; font-weight: 800; letter-spacing: .16em; }
.admin-page__head h1 { margin-top: 3px; font-size: 26px; letter-spacing: -.02em; }
.admin-page__head p { margin-top: 5px; color: var(--c-text-3); font-size: 13px; }
.primary-action { height: 38px; padding: 0 16px; border: 0; border-radius: var(--radius-sm); background: var(--c-primary); color: #fff; font-weight: 700; box-shadow: 0 8px 18px rgba(var(--c-primary-rgb), .17); }
.metric-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 14px; }
.metric-row article { display: grid; grid-template-columns: 1fr auto; align-items: baseline; padding: 16px 18px; border: 1px solid var(--c-border); border-radius: var(--radius); background: var(--c-surface); }
.metric-row span { color: var(--c-text-3); font-size: 11px; }.metric-row strong { font-size: 23px; }.metric-row small { grid-column: 1 / -1; margin-top: 3px; color: var(--c-text-4); font-size: 9px; }
.manager-card { overflow: hidden; border: 1px solid var(--c-border); border-radius: var(--radius-lg); background: var(--c-surface); box-shadow: var(--shadow-xs); }
.manager-toolbar { display: flex; min-height: 62px; align-items: center; justify-content: space-between; gap: 18px; padding: 10px 18px; border-bottom: 1px solid var(--c-border); }
.manager-toolbar small { color: var(--c-text-4); }.manager-search { display: flex; width: min(420px, 70%); align-items: center; gap: 8px; padding: 8px 11px; border: 1px solid var(--c-border); border-radius: var(--radius-sm); background: var(--c-surface-2); }
.manager-search input { min-width: 0; flex: 1; border: 0; outline: 0; background: transparent; color: var(--c-text); }
.taxonomy-head, .taxonomy-group > header, .child-row { display: grid; grid-template-columns: minmax(280px, 1fr) 110px 240px; align-items: center; gap: 16px; }
.taxonomy-head { min-height: 36px; padding: 0 18px; border-bottom: 1px solid var(--c-border-light); background: var(--c-surface-2); color: var(--c-text-4); font-size: 9px; font-weight: 800; letter-spacing: .06em; }
.taxonomy-group { border-bottom: 1px solid var(--c-border); }.taxonomy-group:last-child { border: 0; }.taxonomy-group > header { min-height: 74px; padding: 10px 18px; }
.group-toggle { display: flex; min-width: 0; align-items: center; gap: 11px; border: 0; background: transparent; text-align: left; }.group-toggle > i { display: grid; width: 30px; height: 30px; flex: 0 0 auto; place-items: center; border-radius: 7px; background: var(--c-primary-soft); color: var(--c-primary); font-size: 16px; font-style: normal; font-weight: 800; }.group-toggle > span, .child-name > span { display: flex; min-width: 0; flex-direction: column; }.group-toggle strong { color: var(--c-text); font-size: 13px; }.group-toggle small, .child-name small { margin-top: 2px; color: var(--c-text-4); font-size: 9px; }
.usage { display: flex; flex-direction: column; }.usage strong { font-size: 14px; }.usage span { color: var(--c-text-4); font-size: 9px; }.row-actions { display: flex; justify-content: flex-end; gap: 6px; }.row-actions button { padding: 6px 9px; border: 1px solid var(--c-border); border-radius: 6px; background: var(--c-surface); color: var(--c-text-2); font-size: 10px; }.row-actions button:hover { border-color: var(--c-primary); color: var(--c-primary); }.row-actions .add-child { border-color: color-mix(in srgb, var(--c-primary) 35%, var(--c-border)); color: var(--c-primary); }.row-actions .danger { color: var(--c-danger); }.row-actions .muted { color: var(--c-text-4); cursor: not-allowed; }
.children { border-top: 1px solid var(--c-border-light); background: var(--c-surface-2); }.child-row { min-height: 54px; padding: 8px 18px 8px 58px; border-bottom: 1px solid var(--c-border-light); }.child-row:last-child { border: 0; }.child-name { display: flex; min-width: 0; align-items: center; gap: 9px; }.child-name > i { color: var(--c-primary); font-style: normal; font-weight: 800; }.child-name strong { font-size: 11px; }.empty-children { width: 100%; padding: 18px 58px; border: 0; background: transparent; color: var(--c-primary); font-size: 10px; text-align: left; }
.manager-state { display: flex; min-height: 230px; align-items: center; justify-content: center; flex-direction: column; color: var(--c-text-4); font-size: 11px; }.manager-state strong { color: var(--c-text-2); font-size: 14px; }.manager-state p { margin-top: 5px; }
.level-notice { display: flex; align-items: center; gap: 10px; padding: 11px 12px; border: 1px solid var(--c-border); border-radius: 8px; margin-bottom: 16px; background: var(--c-surface-2); }.level-notice i { display: grid; width: 32px; height: 32px; place-items: center; border-radius: 6px; background: var(--c-primary-soft); color: var(--c-primary); font: 800 10px ui-monospace, Consolas, monospace; }.level-notice span { display: flex; flex-direction: column; }.level-notice strong { color: var(--c-text-2); font-size: 11px; }.level-notice small { color: var(--c-text-4); font-size: 9px; }.field-label { display: block; margin-bottom: 7px; color: var(--c-text-2); font-size: 11px; font-weight: 700; }.field-label.spaced { margin-top: 15px; }
@media (max-width: 820px) { .taxonomy-head { display: none; }.taxonomy-group > header, .child-row { grid-template-columns: minmax(0, 1fr) auto; }.usage { display: none; }.row-actions { grid-column: 1 / -1; }.child-row { padding-left: 32px; } }
@media (max-width: 620px) { .admin-page__head { align-items: stretch; flex-direction: column; }.metric-row { grid-template-columns: 1fr; }.manager-toolbar { align-items: stretch; flex-direction: column; }.manager-search { width: 100%; }.taxonomy-group > header, .child-row { grid-template-columns: 1fr; }.row-actions { justify-content: flex-start; flex-wrap: wrap; }.child-row { padding-left: 22px; } }
</style>

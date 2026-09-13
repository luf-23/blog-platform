<template>
  <div class="categories-page page-container">
    <header class="categories-head">
      <div class="categories-heading">
        <h1>文章分组</h1>
        <p>{{ loading ? '正在加载分组…' : loadFailed ? '管理你的个人文章分组' : `共 ${categories.length} 个分组 · 已归类 ${totalArticleCount} 篇文章` }}</p>
      </div>
      <div class="head-actions">
        <router-link to="/article/my" class="secondary-action"><el-icon><ArrowLeft /></el-icon>返回内容管理</router-link>
        <button class="primary-action" @click="openCreate"><el-icon><Plus /></el-icon>新建分组</button>
      </div>
    </header>

    <section class="category-panel" aria-label="我的分组" :aria-busy="loading">
      <header class="panel-head">
        <h2>全部分组<span>{{ loading || loadFailed ? '—' : categories.length }}</span></h2>
        <p>个人分组，仅用于整理自己的文章</p>
      </header>
      <div class="library-toolbar">
        <div class="search-tools">
          <label class="search-field">
            <el-icon><Search /></el-icon>
            <input v-model="keyword" aria-label="搜索分组名称或描述" placeholder="搜索分组名称或描述" />
            <button v-if="keyword" aria-label="清空搜索" @click="keyword = ''">×</button>
          </label>
          <div v-if="keyword.trim() && !loading && !loadFailed" class="filter-feedback" role="status">
            <span>找到 {{ visibleCategories.length }} 个分组</span><button @click="keyword = ''">清除筛选</button>
          </div>
        </div>
        <el-select v-model="sortOrder" aria-label="分组排序" class="sort-filter">
          <el-option label="最近更新" value="updated" />
          <el-option label="最新创建" value="created" />
          <el-option label="文章最多" value="articles" />
        </el-select>
      </div>

      <div v-if="loading" class="panel-state loading-state" role="status">
        <span class="category-spinner" aria-hidden="true"></span><span>正在加载分组…</span>
      </div>
      <div v-else-if="loadFailed" class="panel-state" role="status">
        <el-icon class="state-icon"><Folder /></el-icon><strong>分组暂时未能加载</strong><p>请稍后重试。</p><button @click="fetchCategories">重新加载</button>
      </div>

      <table v-else-if="visibleCategories.length" class="category-table" aria-label="文章分组列表">
        <colgroup><col /><col class="count-col" /><col class="date-col" /><col class="actions-col" /></colgroup>
        <thead><tr><th scope="col">分组</th><th scope="col">文章数量</th><th scope="col">更新时间</th><th scope="col">操作</th></tr></thead>
        <tbody>
          <tr v-for="cat in visibleCategories" :key="cat.categoryId" class="category-row">
            <td class="category-main">
              <div class="category-identity">
                <router-link :to="categoryLink(cat)" class="cat-cover" :class="{ fallback: !showCategoryCover(cat) }" :aria-label="`查看分组：${cat.categoryName}`">
                  <img v-if="showCategoryCover(cat)" :src="cat.coverImage" alt="" loading="lazy" @error="markCoverFailed(cat.categoryId)" />
                  <span v-else aria-hidden="true">{{ cat.categoryName?.slice(0, 1) || '组' }}</span>
                </router-link>
                <div class="cat-copy">
                  <router-link :to="categoryLink(cat)" class="cat-title" :title="cat.categoryName">{{ cat.categoryName }}</router-link>
                  <p :title="cat.categoryDescription">{{ cat.categoryDescription || '暂无描述' }}</p>
                </div>
              </div>
            </td>
            <td class="cat-count"><span>{{ cat.articleCount || 0 }}</span><span class="mobile-count-label"> 篇文章</span></td>
            <td class="cat-date"><span class="mobile-date-label">更新于 </span>{{ formatDate(cat.updateTime || cat.createTime) }}</td>
            <td class="category-actions">
              <div class="cat-actions">
                <router-link :to="categoryLink(cat)">查看内容</router-link>
                <button :aria-label="`编辑分组：${cat.categoryName}`" @click="startEdit(cat)">编辑</button>
                <button class="delete-action" :aria-label="`删除分组：${cat.categoryName}`" title="删除分组" @click="deleteCategory(cat)"><el-icon><Delete /></el-icon></button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-else-if="keyword.trim()" class="panel-state" role="status">
        <el-icon class="state-icon"><Search /></el-icon><strong>没有匹配的分组</strong><p>试试其他关键词，或清除筛选查看全部分组。</p><button @click="keyword = ''">清除筛选</button>
      </div>
      <div v-else class="panel-state">
        <el-icon class="state-icon"><Folder /></el-icon><strong>还没有文章分组</strong><p>创建分组后，写文章时可以选择它。</p><button @click="openCreate"><el-icon><Plus /></el-icon>新建分组</button>
      </div>
      <footer v-if="visibleCategories.length && !loading && !loadFailed" class="library-footer">共 {{ visibleCategories.length }} 个分组</footer>
    </section>
    <el-dialog v-model="showAddDialog" :title="editingCat ? '编辑分组' : '新建分组'" width="440px" :close-on-click-modal="false" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <el-form-item label="分组名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="如：前端开发" maxlength="50" show-word-limit/>
        </el-form-item>
        <el-form-item label="分组描述" prop="categoryDescription">
          <el-input v-model="form.categoryDescription" type="textarea" :rows="3" placeholder="分组描述（可选）" maxlength="200" show-word-limit/>
        </el-form-item>
        <el-form-item label="分组封面（可选）" prop="coverImage">
          <div class="cover-field">
            <div v-if="form.coverImage" class="cover-form-preview"><img :src="form.coverImage" alt="分组封面预览" /></div>
            <div class="cover-field-actions">
              <button type="button" class="btn btn-secondary btn-sm" @click="coverUploadVisible = true">{{ form.coverImage ? '更换封面' : '上传封面' }}</button>
              <button v-if="form.coverImage" type="button" class="btn btn-ghost btn-sm" @click="form.coverImage = ''">移除封面</button>
            </div>
            <small>建议使用 16:9 横向图片；不上传时会显示文字封面。</small>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCategory">{{ editingCat ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>

    <UploadImageDialog
      v-model:visible="coverUploadVisible"
      title="选择分组封面"
      default-ratio="16:9"
      :loading="uploadingCover"
      hint="支持 JPG、PNG、WEBP，图片大小不超过 5MB"
      @confirm="uploadCategoryCover"
    />
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Delete, Folder, Plus, Search } from '@element-plus/icons-vue'
import { getCategoryListService, addCategoryService, updateCategoryService, deleteCategoryService } from '../../api/category.js'
import UploadImageDialog from '../../components/common/UploadImageDialog.vue'
import { useUserInfoStore } from '../../store/userInfo.js'
import { OSSClient, uploadImageToOss } from '../../utils/oss/index.js'

const categories = ref([])
const loading = ref(false)
const loadFailed = ref(false)
const keyword = ref('')
const sortOrder = ref('updated')
const saving = ref(false)
const uploadingCover = ref(false)
const coverUploadVisible = ref(false)
const showAddDialog = ref(false)
const editingCat = ref(null)
const failedCategoryCovers = ref(new Set())
const formRef = ref()
const userInfoStore = useUserInfoStore()
const totalArticleCount = computed(() => categories.value.reduce((total, category) => total + Number(category.articleCount || 0), 0))
const visibleCategories = computed(() => {
  const query = keyword.value.trim().toLocaleLowerCase()
  return categories.value.filter(category => !query ||
    `${category.categoryName || ''} ${category.categoryDescription || ''}`.toLocaleLowerCase().includes(query)
  ).sort((a, b) => {
    if (sortOrder.value === 'articles') return Number(b.articleCount || 0) - Number(a.articleCount || 0)
    const dateValue = category => {
      const value = sortOrder.value === 'created' ? category.createTime : category.updateTime || category.createTime
      return new Date(value || 0).getTime() || 0
    }
    return dateValue(b) - dateValue(a)
  })
})
const categoryLink = category => ({ path: '/article/my', query: { categoryId: category.categoryId } })

const form = reactive({ categoryName: '', categoryDescription: '', coverImage: '' })
const formRules = {
  categoryName: [{ required: true, message: '请输入分组名称', trigger: 'blur' }]
}

async function fetchCategories() {
  loading.value = true
  loadFailed.value = false
  try {
    const res = await getCategoryListService()
    categories.value = res.data || []
    failedCategoryCovers.value = new Set()
  } catch {
    loadFailed.value = true
  } finally { loading.value = false }
}

function startEdit(cat) {
  editingCat.value = cat
  form.categoryName = cat.categoryName
  form.categoryDescription = cat.categoryDescription || ''
  form.coverImage = cat.coverImage || ''
  showAddDialog.value = true
}

function openCreate() {
  resetForm()
  showAddDialog.value = true
}

function resetForm() {
  editingCat.value = null
  form.categoryName = ''
  form.categoryDescription = ''
  form.coverImage = ''
  formRef.value?.clearValidate()
}

function closeDialog() {
  showAddDialog.value = false
}

async function uploadCategoryCover(file) {
  uploadingCover.value = true
  try {
    form.coverImage = await uploadImageToOss(
      file,
      OSSClient.IMAGE_TYPE.ARTICLE_COVER,
      userInfoStore.userInfo?.userId
    )
    coverUploadVisible.value = false
    ElMessage.success('封面已更新')
  } catch (error) {
    ElMessage.error(error?.message || '封面上传失败')
  } finally {
    uploadingCover.value = false
  }
}

async function saveCategory() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (editingCat.value) {
        await updateCategoryService({
          categoryId: editingCat.value.categoryId,
          categoryName: form.categoryName,
          categoryDescription: form.categoryDescription,
          coverImage: form.coverImage
        })
        ElMessage.success('分组已更新')
      } else {
        await addCategoryService({
          categoryName: form.categoryName,
          categoryDescription: form.categoryDescription,
          coverImage: form.coverImage
        })
        ElMessage.success('分组已创建')
      }
      closeDialog()
      fetchCategories()
    } finally { saving.value = false }
  })
}

async function deleteCategory(cat) {
  try {
    await ElMessageBox.confirm(`确定删除分组「${cat.categoryName}」吗？该分组下的文章将变为未分组。`, '删除分组', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCategoryService(cat.categoryId)
    ElMessage.success('已删除')
    fetchCategories()
  } catch {}
}

function showCategoryCover(cat) {
  return Boolean(cat.coverImage && !failedCategoryCovers.value.has(cat.categoryId))
}

function markCoverFailed(categoryId) {
  failedCategoryCovers.value = new Set([...failedCategoryCovers.value, categoryId])
}

function formatDate(value) {
  if (!value || Number.isNaN(new Date(value).getTime())) return '—'
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(value))
}

onMounted(fetchCategories)
</script>

<style scoped>
.categories-page { width: min(1440px, calc(100% - 96px)); padding: 20px 0 40px; }
.categories-head { display: flex; align-items: center; justify-content: space-between; gap: 24px; margin-bottom: 28px; }
.categories-heading { display: flex; align-items: baseline; flex-wrap: wrap; gap: 4px 16px; min-width: 0; }
.categories-heading h1 { color: var(--c-text); font-size: 30px; line-height: 1.4; font-weight: 700; letter-spacing: -.035em; }
.categories-heading p { color: var(--c-text-3); font-size: 13px; }
.head-actions { display: flex; flex-shrink: 0; gap: 12px; }
.head-actions a, .head-actions button { display: inline-flex; height: 40px; align-items: center; justify-content: center; gap: 8px; padding: 0 17px; border: 1px solid var(--c-border-strong); border-radius: 3px; background: transparent; color: var(--c-text-2); font-size: 13px; transition: border-color var(--transition), background var(--transition); }
.head-actions a:hover { border-color: var(--c-text-3); background: var(--c-surface-2); }
.head-actions .primary-action { border-color: var(--c-primary); background: var(--c-primary); color: #fff; }
.head-actions .primary-action:hover { border-color: var(--c-primary-hover); background: var(--c-primary-hover); }
.head-actions .el-icon { font-size: 16px; }
.panel-head { display: flex; align-items: center; justify-content: space-between; gap: 20px; border-bottom: 1px solid var(--c-border); }
.panel-head h2 { display: flex; align-items: center; gap: 8px; padding: 13px 4px 15px; margin-bottom: -1px; border-bottom: 2px solid var(--c-primary); color: var(--c-primary); font-size: 14px; font-weight: 400; white-space: nowrap; }
.panel-head h2 span { font-size: 12px; font-variant-numeric: tabular-nums; }
.panel-head p { color: var(--c-text-3); font-size: 12px; }
.library-toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 16px 24px; padding: 22px 0; }
.search-tools { display: flex; flex: 1 1 520px; min-width: 0; align-items: center; flex-wrap: wrap; gap: 12px 20px; }
.search-field { display: flex; width: 360px; min-width: 0; height: 38px; align-items: center; gap: 10px; padding: 0 12px; border: 1px solid var(--c-border-strong); border-radius: 3px; color: var(--c-text-3); }
.search-field:focus-within { border-color: var(--c-primary); outline: 1px solid var(--c-primary); }
.search-field .el-icon { flex-shrink: 0; font-size: 16px; }
.search-field input { width: 100%; min-width: 0; border: 0; outline: 0; background: transparent; color: var(--c-text); font-size: 13px; }
.search-field input::placeholder { color: var(--c-text-3); }
.search-field button { flex-shrink: 0; padding: 0 3px; border: 0; background: transparent; color: var(--c-text-3); font-size: 20px; }
.sort-filter { width: 144px; flex-shrink: 0; margin-left: auto; }
.sort-filter :deep(.el-select__wrapper) { min-height: 38px; border-radius: 3px; background: var(--c-surface); font-size: 13px; box-shadow: 0 0 0 1px var(--c-border-strong) inset; }
.sort-filter :deep(.el-select__selected-item) { color: var(--c-text-2); }
.sort-filter :deep(.el-select__wrapper.is-focused) { box-shadow: 0 0 0 1px var(--c-primary) inset; }
.filter-feedback { display: flex; align-items: center; flex-shrink: 0; gap: 14px; color: var(--c-text-3); font-size: 12px; white-space: nowrap; }
.filter-feedback button { padding: 0; border: 0; background: transparent; color: var(--c-primary); }
.category-table { width: 100%; border-collapse: collapse; table-layout: fixed; text-align: left; }
.count-col { width: 14%; }
.date-col { width: 18%; }
.actions-col { width: 180px; }
.category-table th { padding: 12px 16px; border-block: 1px solid var(--c-border); color: var(--c-text-3); font-size: 13px; font-weight: 400; }
.category-table td { height: 96px; padding: 14px 16px; border-bottom: 1px solid var(--c-border); color: var(--c-text-3); font-size: 13px; vertical-align: middle; }
.category-table th:first-child, .category-table td:first-child { padding-left: 12px; }
.category-table th:last-child, .category-table td:last-child { padding-right: 12px; }
.category-row { transition: background var(--transition); }
.category-row:hover { background: var(--c-surface-2); }
.category-identity { display: flex; align-items: center; gap: 20px; min-width: 0; }
.cat-cover { display: grid; width: 88px; height: 60px; flex: 0 0 88px; place-items: center; overflow: hidden; border-radius: 2px; background: var(--c-surface-3); }
.cat-cover img { width: 100%; height: 100%; object-fit: cover; }
.cat-cover.fallback { color: var(--c-text-3); font-size: 24px; font-weight: 500; }
.cat-copy { min-width: 0; }
.cat-title { display: block; overflow: hidden; color: var(--c-text); font-size: 16px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.cat-title:hover { color: var(--c-primary); }
.cat-copy p { overflow: hidden; margin-top: 4px; color: var(--c-text-3); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.cat-count, .cat-date { font-variant-numeric: tabular-nums; white-space: nowrap; }
.mobile-count-label, .mobile-date-label { display: none; }
.cat-actions { display: flex; align-items: center; gap: 12px; }
.cat-actions a, .cat-actions button { display: inline-flex; flex-shrink: 0; min-height: 30px; align-items: center; justify-content: center; padding: 0; border: 0; background: transparent; color: var(--c-primary); font-size: 12px; white-space: nowrap; }
.cat-actions a:hover, .cat-actions button:hover { text-decoration: underline; }
.cat-actions .delete-action { width: 28px; flex-shrink: 0; color: var(--c-text-3); font-size: 16px; }
.cat-actions .delete-action:hover { color: var(--c-danger); }
.library-footer { padding-top: 22px; color: var(--c-text-3); font-size: 12px; }
.panel-state { display: flex; min-height: 320px; align-items: center; justify-content: center; flex-direction: column; padding: 32px 20px; border-block: 1px solid var(--c-border); text-align: center; }
.state-icon { margin-bottom: 16px; color: var(--c-text-3); font-size: 28px; }
.panel-state strong { color: var(--c-text); font-size: 17px; font-weight: 600; }
.panel-state p { margin: 8px 0 18px; color: var(--c-text-3); font-size: 13px; }
.panel-state > button { display: inline-flex; align-items: center; gap: 7px; padding: 8px 15px; border: 1px solid var(--c-primary); border-radius: 3px; background: transparent; color: var(--c-primary); font-size: 13px; }
.loading-state { gap: 12px; color: var(--c-text-3); font-size: 13px; }
.category-spinner { width: 22px; height: 22px; border: 2px solid var(--c-border-strong); border-top-color: var(--c-primary); border-radius: 50%; animation: category-spin .7s linear infinite; }
.categories-page button:focus-visible, .categories-page a:focus-visible { outline: 2px solid var(--c-primary); outline-offset: 3px; }
.cover-field { width: 100%; }
.cover-field > small { display: block; margin-top: 7px; color: var(--c-text-3); font-size: 12px; }
.cover-field-actions { display: flex; gap: 8px; }
.cover-form-preview { width: 100%; aspect-ratio: 16 / 9; overflow: hidden; border: 1px solid var(--c-border); border-radius: 3px; margin-bottom: 8px; background: var(--c-surface-2); }
.cover-form-preview img { width: 100%; height: 100%; object-fit: cover; }
.categories-page :deep(.el-dialog) { max-width: calc(100vw - 32px); }
@keyframes category-spin { to { transform: rotate(360deg); } }
@media (max-width: 900px) {
  .categories-page { width: calc(100% - 48px); }
  .category-table, .category-table tbody { display: block; }
  .category-table colgroup, .category-table thead { display: none; }
  .category-table { border-top: 1px solid var(--c-border); }
  .category-row { display: grid; grid-template-columns: 1fr auto; align-items: center; gap: 10px 18px; padding: 20px 0; border-bottom: 1px solid var(--c-border); }
  .category-table td { display: block; min-width: 0; height: auto; padding: 0; border: 0; }
  .category-main { grid-column: 1 / -1; }
  .cat-count { grid-column: 1; }
  .cat-date { grid-column: 1; font-size: 12px !important; }
  .category-actions { grid-column: 2; grid-row: 2 / 4; align-self: end; }
  .category-identity { gap: 14px; }
  .mobile-count-label, .mobile-date-label { display: inline; }
  .cat-actions { gap: 18px; }
  .cat-actions a, .cat-actions button { min-height: 34px; }
}
@media (max-width: 620px) {
  .categories-page { width: calc(100% - 32px); padding-top: 16px; }
  .categories-head { align-items: flex-start; flex-direction: column; gap: 20px; margin-bottom: 20px; }
  .categories-heading h1 { font-size: 26px; }
  .head-actions { width: 100%; }
  .head-actions a, .head-actions button { flex: 1; padding-inline: 10px; }
  .panel-head { align-items: flex-start; flex-direction: column; gap: 0; }
  .panel-head h2 { order: 1; }
  .panel-head p { padding-bottom: 4px; font-size: 11px; }
  .library-toolbar { align-items: stretch; flex-direction: column; gap: 12px; padding: 18px 0; }
  .search-tools { flex: auto; align-items: stretch; flex-direction: column; }
  .search-field { width: 100%; padding-inline: 9px; gap: 6px; }
  .sort-filter { width: 100%; margin-left: 0; }
  .cat-cover { width: 72px; height: 54px; flex-basis: 72px; }
  .cat-title { font-size: 15px; }
  .cat-actions { gap: 12px; }
  .category-actions { grid-column: 1 / -1; grid-row: auto; }
  .cat-actions { justify-content: flex-end; }
}
@media (prefers-reduced-motion: reduce) {
  .categories-page *, .categories-page *::before, .categories-page *::after { transition: none !important; }
  .category-spinner { animation: none; }
}
</style>

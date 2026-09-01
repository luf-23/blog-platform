<template>
  <div class="categories-page page-container">
    <header class="categories-head">
      <div class="categories-heading">
        <h1>文章分组</h1>
        <p>创建和管理个人文章分组。</p>
      </div>
      <div class="head-actions">
        <router-link to="/article/my" class="secondary-action">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="m15 18-6-6 6-6"/></svg>
          返回内容管理
        </router-link>
        <button class="primary-action" @click="openCreate">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 5v14M5 12h14"/></svg>
          新建分组
        </button>
      </div>
    </header>

    <section class="category-overview" aria-label="分组概览">
      <article class="overview-card">
        <span>分组数量</span>
        <strong>{{ categories.length }}</strong>
        <p>个文章分组</p>
        <i class="tone-blue"></i>
      </article>
      <article class="overview-card">
        <span>已归类文章</span>
        <strong>{{ totalArticleCount }}</strong>
        <p>篇文章已有归属</p>
        <i class="tone-green"></i>
      </article>
      <aside class="organize-note">
        <div class="note-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H10l2 2h5.5A2.5 2.5 0 0 1 20 7.5v9a2.5 2.5 0 0 1-2.5 2.5h-11A2.5 2.5 0 0 1 4 16.5Z"/><path d="M8 10h8M8 14h5"/></svg>
        </div>
        <div><strong>个人分组</strong><p>只用于整理自己的文章，不影响平台公共标签。</p></div>
      </aside>
    </section>

    <section class="category-panel">
      <header class="panel-head">
        <div><h2>我的分组</h2><span>{{ categories.length }} 个分组</span></div>
        <p>用于文章筛选和归类</p>
      </header>

      <div v-if="loading" class="panel-state">
        <span class="category-spinner" aria-hidden="true"></span>
        <span>正在加载分组…</span>
      </div>

      <div v-else-if="categories.length === 0" class="panel-state empty-state">
        <div class="empty-visual" aria-hidden="true"><i></i></div>
        <div><strong>还没有文章分组</strong><p>创建分组后，写文章时可以选择它。</p><button @click="openCreate">新建分组</button></div>
      </div>

      <div v-else class="category-list">
        <article v-for="(cat, index) in categories" :key="cat.categoryId" class="category-row">
          <div class="cat-cover" :class="{ fallback: !showCategoryCover(cat) }">
            <img v-if="showCategoryCover(cat)" :src="cat.coverImage" :alt="`${cat.categoryName}封面`" @error="markCoverFailed(cat.categoryId)" />
            <template v-else><strong>{{ cat.categoryName?.slice(0, 1) || '组' }}</strong><span>{{ String(index + 1).padStart(2, '0') }}</span></template>
          </div>
          <div class="cat-copy">
            <h3>{{ cat.categoryName }}</h3>
            <p>{{ cat.categoryDescription || '未填写描述' }}</p>
            <small>更新于 {{ formatDate(cat.updateTime || cat.createTime) }}</small>
          </div>
          <div class="cat-count"><strong>{{ cat.articleCount || 0 }}</strong><span>篇文章</span></div>
          <div class="cat-actions">
            <router-link :to="{ path: '/article/my', query: { categoryId: cat.categoryId } }" class="view-action">
              查看内容
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="m9 18 6-6-6-6"/></svg>
            </router-link>
            <button class="edit-action" @click="startEdit(cat)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z"/></svg>
              编辑
            </button>
            <button class="delete-action" :aria-label="`删除分组 ${cat.categoryName}`" @click="deleteCategory(cat)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 7h16M9 7V4h6v3M6 7l1 14h10l1-14M10 11v6M14 11v6"/></svg>
            </button>
          </div>
        </article>
      </div>
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
      :loading="uploadingCover"
      hint="支持 JPG、PNG、WEBP，图片大小不超过 5MB"
      @confirm="uploadCategoryCover"
    />
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategoryListService, addCategoryService, updateCategoryService, deleteCategoryService } from '../../api/category.js'
import UploadImageDialog from '../../components/common/UploadImageDialog.vue'
import { useUserInfoStore } from '../../store/userInfo.js'
import { OSSClient, uploadImageToOss } from '../../utils/oss/index.js'

const categories = ref([])
const loading = ref(false)
const saving = ref(false)
const uploadingCover = ref(false)
const coverUploadVisible = ref(false)
const showAddDialog = ref(false)
const editingCat = ref(null)
const failedCategoryCovers = ref(new Set())
const formRef = ref()
const userInfoStore = useUserInfoStore()
const totalArticleCount = computed(() => categories.value.reduce((total, category) => total + Number(category.articleCount || 0), 0))

const form = reactive({ categoryName: '', categoryDescription: '', coverImage: '' })
const formRules = {
  categoryName: [{ required: true, message: '请输入分组名称', trigger: 'blur' }]
}

async function fetchCategories() {
  loading.value = true
  try {
    const res = await getCategoryListService()
    categories.value = res.data || []
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
  if (!value) return '—'
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'short', day: 'numeric' }).format(new Date(value))
}

onMounted(fetchCategories)
</script>

<style scoped>
.categories-page { padding-top: 20px; padding-bottom: 52px; }
.categories-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 22px; }
.categories-heading { transform: translateY(-6px); }
.categories-heading > span { color: var(--c-primary); font-size: 10px; font-weight: 900; letter-spacing: .16em; }
.categories-heading h1 { margin-top: 4px; color: var(--c-text); font-size: 24px; font-weight: 850; line-height: 1.35; letter-spacing: -.025em; }
.categories-heading p { margin-top: 4px; color: var(--c-text-3); font-size: 13px; }
.head-actions { display: flex; flex: 0 0 auto; gap: 8px; transform: translateY(-6px); }
.head-actions a, .head-actions button { display: inline-flex; height: 40px; align-items: center; gap: 7px; padding: 0 15px; border: 1px solid var(--c-border-strong); border-radius: var(--radius-sm); background: var(--c-surface); color: var(--c-text-2); font-size: 12px; font-weight: 800; }
.head-actions svg { width: 16px; height: 16px; }
.head-actions .primary-action { border-color: var(--c-primary); background: var(--c-primary); color: #fff; box-shadow: 0 8px 18px rgba(var(--c-primary-rgb), .16); }
.head-actions .primary-action:hover { background: var(--c-primary-hover); }
.head-actions .secondary-action:hover { border-color: var(--c-primary); color: var(--c-primary); }

.category-overview { display: grid; grid-template-columns: 190px 190px minmax(360px, 1fr); gap: 10px; margin-bottom: 14px; }
.overview-card { position: relative; min-height: 112px; padding: 16px 17px; overflow: hidden; border: 1px solid var(--c-border); border-radius: var(--radius); background: var(--c-surface); box-shadow: var(--shadow-xs); }
.overview-card > span { color: var(--c-text-3); font-size: 11px; font-weight: 700; }
.overview-card strong { display: block; margin: 7px 0 2px; color: var(--c-text); font-size: 29px; line-height: 1; }
.overview-card p { color: var(--c-text-4); font-size: 10px; }
.overview-card i { position: absolute; top: 16px; right: 16px; width: 7px; height: 7px; border-radius: 50%; box-shadow: 0 0 0 5px var(--c-primary-soft); }
.overview-card .tone-blue { background: var(--c-primary); }
.overview-card .tone-green { background: var(--c-success); box-shadow: 0 0 0 5px var(--c-success-soft); }
.organize-note { display: flex; min-width: 0; align-items: center; gap: 16px; padding: 17px 20px; border: 1px solid color-mix(in srgb, var(--c-primary) 18%, var(--c-border)); border-radius: var(--radius); background: linear-gradient(120deg, var(--c-surface), var(--c-primary-soft)); }
.note-icon { display: grid; width: 50px; height: 50px; flex: 0 0 auto; place-items: center; border: 1px solid color-mix(in srgb, var(--c-primary) 25%, var(--c-border)); border-radius: var(--radius); background: var(--c-surface); color: var(--c-primary); }
.note-icon svg { width: 24px; height: 24px; }
.organize-note div:last-child { min-width: 0; }
.organize-note span { color: var(--c-primary); font-size: 8px; font-weight: 900; letter-spacing: .14em; }
.organize-note strong { display: block; margin: 3px 0 2px; overflow: hidden; color: var(--c-text); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.organize-note p { color: var(--c-text-3); font-size: 10px; }

.category-panel { overflow: hidden; border: 1px solid var(--c-border); border-radius: var(--radius-lg); background: var(--c-surface); box-shadow: var(--shadow-xs); }
.panel-head { display: flex; min-height: 66px; align-items: center; justify-content: space-between; gap: 18px; padding: 0 20px; border-bottom: 1px solid var(--c-border); }
.panel-head > div { display: flex; align-items: baseline; gap: 9px; }
.panel-head h2 { font-size: 16px; }
.panel-head span, .panel-head p { color: var(--c-text-4); font-size: 10px; }
.panel-state { display: flex; min-height: 300px; align-items: center; justify-content: center; flex-direction: column; gap: 9px; color: var(--c-text-4); font-size: 11px; }
.category-spinner { width: 18px; height: 18px; border: 2px solid var(--c-border-strong); border-top-color: var(--c-primary); border-radius: 50%; animation: category-spin .7s linear infinite; }
.category-list { display: flex; flex-direction: column; }
.category-row { display: grid; min-height: 118px; grid-template-columns: 136px minmax(0, 1fr) 100px auto; align-items: center; gap: 18px; padding: 14px 20px; border-bottom: 1px solid var(--c-border-light); transition: background var(--transition); }
.category-row:last-child { border-bottom: 0; }
.category-row:hover { background: var(--c-surface-2); }
.cat-cover { position: relative; width: 136px; aspect-ratio: 16 / 9; overflow: hidden; border: 1px solid var(--c-border); border-radius: 8px; background: var(--c-surface-3); }
.cat-cover img { width: 100%; height: 100%; object-fit: cover; transition: transform var(--transition); }.category-row:hover .cat-cover img { transform: scale(1.035); }
.cat-cover.fallback { display: grid; place-items: center; background: linear-gradient(135deg, var(--c-primary-soft), var(--c-surface-3)); color: var(--c-primary); }.cat-cover.fallback strong { font-size: 24px; }.cat-cover.fallback span { position: absolute; right: 7px; bottom: 5px; color: var(--c-text-4); font: 700 8px ui-monospace, Consolas, monospace; }
.cat-copy { min-width: 0; }
.cat-copy h3 { overflow: hidden; color: var(--c-text); font-size: 14px; font-weight: 800; text-overflow: ellipsis; white-space: nowrap; }
.cat-copy p { display: -webkit-box; margin-top: 5px; overflow: hidden; color: var(--c-text-3); font-size: 11px; line-height: 1.6; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }.cat-copy small { display: block; margin-top: 6px; color: var(--c-text-4); font-size: 9px; }
.cat-count { display: flex; flex-direction: column; }
.cat-count strong { color: var(--c-text); font-size: 17px; line-height: 1.1; }
.cat-count span { margin-top: 3px; color: var(--c-text-4); font-size: 9px; }
.cat-actions { display: flex; align-items: center; gap: 6px; }
.cat-actions a, .cat-actions button { display: inline-flex; height: 32px; align-items: center; justify-content: center; gap: 4px; padding: 0 10px; border: 1px solid var(--c-border); border-radius: var(--radius-sm); background: var(--c-surface); color: var(--c-text-2); font-size: 10px; font-weight: 700; transition: all var(--transition); }
.cat-actions svg { width: 14px; height: 14px; }
.cat-actions .view-action { border-color: color-mix(in srgb, var(--c-primary) 30%, var(--c-border)); color: var(--c-primary); }
.cat-actions .view-action:hover { border-color: var(--c-primary); background: var(--c-primary); color: #fff; }
.cat-actions .edit-action:hover { border-color: var(--c-primary); color: var(--c-primary); }
.cat-actions .delete-action { width: 32px; padding: 0; color: var(--c-text-4); }
.cat-actions .delete-action:hover { border-color: var(--c-danger); color: var(--c-danger); }

.empty-state { display: grid; min-height: 340px; grid-template-columns: 150px minmax(0, 380px); gap: 38px; }
.empty-visual { position: relative; display: grid; width: 150px; height: 150px; place-items: center; border: 1px solid var(--c-border); background: var(--c-surface-2); }
.empty-visual::before { position: absolute; inset: 9px; border: 1px solid var(--c-border-light); content: ''; }
.empty-visual span { position: absolute; top: 16px; left: 17px; color: var(--c-text-4); font-size: 9px; }
.empty-visual i { width: 48px; height: 1px; background: var(--c-primary); transform: rotate(-35deg); }
.empty-visual b { position: absolute; right: 15px; bottom: 14px; color: var(--c-text-2); font-size: 10px; letter-spacing: .14em; }
.empty-state > div:last-child > span { color: var(--c-primary); font-size: 8px; font-weight: 900; letter-spacing: .14em; }
.empty-state strong { display: block; margin: 7px 0; color: var(--c-text); font-size: 20px; }
.empty-state p { color: var(--c-text-3); font-size: 11px; line-height: 1.75; }
.empty-state button { padding: 0; border: 0; margin-top: 16px; background: transparent; color: var(--c-primary); font-size: 11px; font-weight: 800; }
.cover-field { width: 100%; }.cover-field > small { display: block; margin-top: 7px; color: var(--c-text-4); font-size: 10px; }.cover-field-actions { display: flex; gap: 8px; }.cover-form-preview { width: 100%; aspect-ratio: 16 / 6; overflow: hidden; border: 1px solid var(--c-border); border-radius: 7px; margin-bottom: 8px; background: var(--c-surface-2); }.cover-form-preview img { width: 100%; height: 100%; object-fit: cover; }
@keyframes category-spin { to { transform: rotate(360deg); } }

@media (max-width: 900px) {
  .category-overview { grid-template-columns: repeat(2, 1fr); }
  .organize-note { grid-column: 1 / -1; }
  .category-row { grid-template-columns: 112px minmax(0, 1fr) auto; }
  .cat-cover { width: 112px; }
  .cat-count { display: none; }
}

@media (max-width: 620px) {
  .categories-page { width: calc(100% - 24px); padding-top: 14px; }
  .categories-heading { transform: none; }
  .head-actions { transform: none; }
  .categories-heading h1 { font-size: 22px; }
  .categories-head { align-items: stretch; flex-direction: column; }
  .head-actions a, .head-actions button { flex: 1; justify-content: center; }
  .category-overview { grid-template-columns: repeat(2, 1fr); }
  .organize-note { display: none; }
  .panel-head p { display: none; }
  .category-row { grid-template-columns: 86px minmax(0, 1fr); gap: 12px; padding: 14px; }
  .cat-cover { width: 86px; }
  .cat-actions { grid-column: 1 / -1; justify-content: flex-end; }
  .empty-state { grid-template-columns: 1fr; padding: 34px 22px; text-align: center; }
  .empty-visual { margin: 0 auto; }
}
</style>

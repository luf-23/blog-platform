<template>
  <div class="categories-page page-container">
    <div class="page-top">
      <div class="page-heading">
        <h1 class="page-title">分类管理</h1>
        <p class="page-subtitle">为文章创建分类，更好地组织内容</p>
      </div>
      <div class="page-actions">
        <router-link to="/article/my" class="btn btn-secondary">← 我的文章</router-link>
        <button class="btn btn-primary" @click="showAddDialog = true">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          新建分类
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading-spinner" style="height:200px">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
    </div>

    <div v-else-if="categories.length === 0" class="empty-state">
      <svg width="60" height="60" viewBox="0 0 24 24" fill="none" stroke="var(--c-text-4)" stroke-width="1.2">
        <path d="M20.59 13.41l-7.17 7.17a2 2 0 01-2.83 0L2 12V2h10l8.59 8.59a2 2 0 010 2.82z"/>
      </svg>
      <p>还没有分类，快来创建一个吧！</p>
      <button class="btn btn-primary" @click="showAddDialog = true">新建分类</button>
    </div>

    <div v-else class="category-grid">
      <div v-for="cat in categories" :key="cat.categoryId" class="category-card card">
        <div class="cat-header">
          <div class="cat-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20.59 13.41l-7.17 7.17a2 2 0 01-2.83 0L2 12V2h10l8.59 8.59a2 2 0 010 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/>
            </svg>
          </div>
          <div class="cat-actions">
            <button class="btn btn-ghost btn-icon" @click="startEdit(cat)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            </button>
            <button class="btn btn-ghost btn-icon" @click="deleteCategory(cat)" style="color:var(--c-danger)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a1 1 0 011-1h4a1 1 0 011 1v2"/></svg>
            </button>
          </div>
        </div>
        <h3 class="cat-name">{{ cat.categoryName }}</h3>
        <p class="cat-desc">{{ cat.categoryDescription || '暂无描述' }}</p>
        <div class="cat-footer">
          <span class="cat-count">{{ cat.articleCount || 0 }} 篇文章</span>
          <router-link
            :to="{ path: '/article/my', query: { categoryId: cat.categoryId } }"
            class="btn btn-ghost btn-sm"
          >查看文章</router-link>
        </div>
      </div>
    </div>

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="showAddDialog" :title="editingCat ? '编辑分类' : '新建分类'" width="440px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="如：前端开发" maxlength="50" show-word-limit/>
        </el-form-item>
        <el-form-item label="分类描述" prop="categoryDescription">
          <el-input v-model="form.categoryDescription" type="textarea" :rows="3" placeholder="简单描述一下这个分类..." maxlength="200" show-word-limit/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCategory">{{ editingCat ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getCategoryListService, addCategoryService, updateCategoryService, deleteCategoryService } from '../../api/category.js'

const categories = ref([])
const loading = ref(false)
const saving = ref(false)
const showAddDialog = ref(false)
const editingCat = ref(null)
const formRef = ref()

const form = reactive({ categoryName: '', categoryDescription: '' })
const formRules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
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
  showAddDialog.value = true
}

function closeDialog() {
  showAddDialog.value = false
  editingCat.value = null
  form.categoryName = ''
  form.categoryDescription = ''
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
          categoryDescription: form.categoryDescription
        })
        ElMessage.success('分类已更新')
      } else {
        await addCategoryService({
          categoryName: form.categoryName,
          categoryDescription: form.categoryDescription
        })
        ElMessage.success('分类已创建')
      }
      closeDialog()
      fetchCategories()
    } finally { saving.value = false }
  })
}

async function deleteCategory(cat) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${cat.categoryName}」吗？该分类下的文章将失去分类。`, '删除分类', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCategoryService(cat.categoryId)
    ElMessage.success('已删除')
    fetchCategories()
  } catch {}
}

onMounted(fetchCategories)
</script>

<style scoped>
.categories-page {}

.page-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}
.page-heading {}
.page-title { font-size: 24px; font-weight: 800; color: var(--c-text); }
.page-subtitle { font-size: 14px; color: var(--c-text-3); margin-top: 4px; }
.page-actions { display: flex; gap: 8px; flex-shrink: 0; }

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--c-text-4);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  font-size: 15px;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.category-card {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cat-icon {
  width: 40px;
  height: 40px;
  background: var(--c-primary-light);
  color: var(--c-primary);
  border-radius: var(--radius);
  display: flex;
  align-items: center;
  justify-content: center;
}

.cat-actions { display: flex; gap: 2px; }

.cat-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--c-text);
}

.cat-desc {
  font-size: 13px;
  color: var(--c-text-3);
  line-height: 1.5;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.cat-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
}

.cat-count {
  font-size: 12px;
  color: var(--c-text-4);
  font-weight: 600;
}
</style>

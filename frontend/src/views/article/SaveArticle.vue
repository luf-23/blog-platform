<template>
  <div class="save-article page-container">
    <div class="editor-header">
      <div class="editor-nav">
        <button class="btn btn-ghost btn-sm" @click="$router.back()">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 5l-7 7 7 7"/></svg>
          返回
        </button>
        <h1 class="editor-title">{{ isEdit ? '编辑文章' : '写文章' }}</h1>
      </div>
      <div class="editor-actions">
        <button class="btn btn-secondary" @click="saveDraft" :disabled="saving">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21H5a2 2 0 01-2-2V5a2 2 0 012-2h11l5 5v11a2 2 0 01-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
          保存草稿
        </button>
        <button class="btn btn-primary" @click="submitForReview" :disabled="saving">
          <el-icon v-if="saving" class="is-loading"><Loading /></el-icon>
          提交审核
        </button>
      </div>
    </div>

    <div class="editor-layout">
      <!-- Main editor -->
      <div class="editor-main card">
        <input
          v-model="form.title"
          class="title-input"
          placeholder="文章标题（最多100字）"
          maxlength="100"
        />
        <textarea
          v-model="form.summary"
          class="summary-input"
          placeholder="文章摘要（可选，最多200字，不填则自动截取内容）"
          maxlength="200"
          rows="2"
        ></textarea>
        <div class="editor-divider"></div>
        <MdEditor
          v-model="form.content"
          :theme="isDark ? 'dark' : 'light'"
          :toolbars="toolbars"
          :preview="false"
          placeholder="开始你的创作..."
          style="height: 500px; border-radius: 0 0 var(--radius-lg) var(--radius-lg); border: none;"
        />
      </div>

      <!-- Settings sidebar -->
      <aside class="editor-sidebar">
        <!-- Status -->
        <div class="card sidebar-block">
          <div class="sidebar-block-title">文章设置</div>

          <el-form label-position="top" size="small">
            <el-form-item label="所属分类">
              <el-select v-model="form.categoryId" placeholder="选择分类" style="width:100%">
                <el-option v-for="c in categories" :key="c.categoryId" :label="c.categoryName" :value="c.categoryId"/>
              </el-select>
              <div style="margin-top:6px">
                <router-link to="/article/categories" class="btn btn-ghost btn-sm" style="font-size:12px">+ 新建分类</router-link>
              </div>
            </el-form-item>

            <el-form-item label="文章标签">
              <div class="tag-input-area">
                <div class="tag-list">
                  <span v-for="(tag, i) in form.tagNames" :key="i" class="tag-pill">
                    # {{ tag }}
                    <button @click="removeTag(i)" style="border:none;background:none;cursor:pointer;color:inherit;font-size:14px;line-height:1;padding:0">×</button>
                  </span>
                </div>
                <div class="tag-input-row" v-if="form.tagNames.length < 5">
                  <input
                    v-model="tagInput"
                    class="tag-input"
                    placeholder="添加标签（回车确认）"
                    maxlength="20"
                    @keydown.enter.prevent="addTag"
                    @keydown.tab.prevent="addTag"
                  />
                </div>
              </div>
              <div style="font-size:12px;color:var(--c-text-4);margin-top:4px">最多5个标签</div>
            </el-form-item>
          </el-form>
        </div>

        <!-- Cover image -->
        <div class="card sidebar-block">
          <div class="sidebar-block-title">封面图</div>
          <div class="cover-preview" v-if="form.coverImage">
            <img :src="form.coverImage" alt="封面"/>
            <button class="cover-remove" @click="form.coverImage = ''">×</button>
          </div>
          <div v-else class="cover-placeholder">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--c-text-4)" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
            <span>暂无封面</span>
          </div>
          <el-input v-model="form.coverImage" placeholder="输入封面图片 URL" size="small" style="margin-top:8px"/>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { addArticleService, updateArticleService, getArticleDetailService } from '../../api/article.js'
import { getCategoryListService } from '../../api/category.js'
import { useTheme } from '../../composables/useTheme.js'

const route = useRoute()
const router = useRouter()
const { isDark } = useTheme()

const isEdit = computed(() => !!route.params.id)
const saving = ref(false)
const categories = ref([])
const tagInput = ref('')

const form = reactive({
  articleId: null,
  title: '',
  summary: '',
  content: '',
  categoryId: null,
  coverImage: '',
  status: 'draft',
  tagNames: []
})

const toolbars = [
  'bold', 'italic', 'strikeThrough', '-',
  'title', 'quote', 'unorderedList', 'orderedList', '-',
  'code', 'link', 'image', 'table', '-',
  'prettier', 'preview'
]

function addTag() {
  const t = tagInput.value.trim()
  if (!t) return
  if (form.tagNames.includes(t)) { tagInput.value = ''; return }
  if (form.tagNames.length >= 5) { ElMessage.warning('最多添加5个标签'); return }
  form.tagNames.push(t)
  tagInput.value = ''
}

function removeTag(i) {
  form.tagNames.splice(i, 1)
}

function buildPayload(status) {
  return {
    articleId: form.articleId,
    title: form.title.trim(),
    summary: form.summary.trim() || undefined,
    content: form.content,
    categoryId: form.categoryId || undefined,
    coverImage: form.coverImage || undefined,
    status,
    tagNames: form.tagNames
  }
}

async function saveDraft() {
  if (!form.title.trim()) { ElMessage.warning('请填写文章标题'); return }
  saving.value = true
  try {
    const payload = buildPayload('draft')
    if (isEdit.value) {
      await updateArticleService(payload)
    } else {
      const res = await addArticleService(payload)
      form.articleId = res.data
    }
    ElMessage.success('已保存草稿')
  } finally { saving.value = false }
}

async function submitForReview() {
  if (!form.title.trim()) { ElMessage.warning('请填写文章标题'); return }
  if (!form.content.trim()) { ElMessage.warning('请填写文章内容'); return }
  saving.value = true
  try {
    const payload = buildPayload('published')
    if (isEdit.value) {
      await updateArticleService(payload)
    } else {
      await addArticleService(payload)
    }
    ElMessage.success('已提交审核，等待管理员审核')
    router.push('/article/my')
  } finally { saving.value = false }
}

async function loadArticle() {
  if (!isEdit.value) return
  try {
    const res = await getArticleDetailService(route.params.id)
    const a = res.data
    form.articleId = a.articleId
    form.title = a.title
    form.summary = a.summary || ''
    form.content = a.content
    form.categoryId = a.categoryId
    form.coverImage = a.coverImage || ''
    form.status = a.status
    form.tagNames = (a.tags || []).map(t => t.tagName)
  } catch { router.push('/article/my') }
}

onMounted(() => {
  getCategoryListService().then(r => { categories.value = r.data || [] }).catch(() => {})
  loadArticle()
})
</script>

<style scoped>
.save-article {}

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}
.editor-nav { display: flex; align-items: center; gap: 12px; }
.editor-title { font-size: 20px; font-weight: 700; color: var(--c-text); }
.editor-actions { display: flex; gap: 8px; }

.editor-layout {
  display: grid;
  grid-template-columns: 1fr 260px;
  gap: 20px;
  align-items: start;
}

.editor-main { overflow: hidden; }

.title-input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 22px;
  font-weight: 700;
  padding: 20px 24px 12px;
  background: transparent;
  color: var(--c-text);
  font-family: inherit;
}
.title-input::placeholder { color: var(--c-text-4); }

.summary-input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 14px;
  padding: 0 24px 16px;
  background: transparent;
  color: var(--c-text-3);
  font-family: inherit;
  resize: none;
  line-height: 1.6;
}
.summary-input::placeholder { color: var(--c-text-4); }

.editor-divider { height: 1px; background: var(--c-border); }

/* Sidebar */
.editor-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 16px;
  max-height: calc(100dvh - var(--nav-height) - 56px);
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-right: 2px;
}

.sidebar-block { padding: 16px 20px; }
.sidebar-block-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text-3);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 12px;
}

/* Tag input */
.tag-input-area {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px;
  border: 1px solid var(--c-border);
  border-radius: var(--radius);
  background: var(--c-surface-2);
}

.tag-list { display: flex; flex-wrap: wrap; gap: 4px; }
.tag-input-row { display: flex; }
.tag-input {
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: var(--c-text);
  width: 100%;
  font-family: inherit;
  padding: 2px 0;
}
.tag-input::placeholder { color: var(--c-text-4); }

/* Cover */
.cover-preview {
  position: relative;
  border-radius: var(--radius);
  overflow: hidden;
  aspect-ratio: 16/9;
}
.cover-preview img { width: 100%; height: 100%; object-fit: cover; }
.cover-remove {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0,0,0,0.6);
  color: white;
  border: none;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 20px;
  background: var(--c-surface-2);
  border-radius: var(--radius);
  color: var(--c-text-4);
  font-size: 13px;
}

@media (max-width: 900px) {
  .editor-layout { grid-template-columns: 1fr; }
  .editor-sidebar { position: static; }
}
</style>

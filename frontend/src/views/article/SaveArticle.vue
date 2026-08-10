<template>
  <div class="save-article">
    <div class="editor-header">
      <div class="editor-nav">
        <button class="btn btn-ghost btn-sm" @click="$router.back()">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 5l-7 7 7 7"/></svg>
          返回
        </button>
        <div><h1 class="editor-title">{{ form.title || (isEdit ? '编辑文章' : '无标题文章') }}</h1><span class="autosave-state">✓ {{ lastSavedAt ? '已自动保存 ' + lastSavedAt : '本地自动保存已开启' }}</span></div>
      </div>
      <div class="editor-modes">
        <button :class="{ active: editorMode === 'edit' }" @click="editorMode = 'edit'">✎ 编辑</button>
        <button :class="{ active: editorMode === 'split' }" @click="editorMode = 'split'">▣ 分屏预览</button>
        <button :class="{ active: editorMode === 'preview' }" @click="editorMode = 'preview'">◉ 预览</button>
      </div>
      <div class="editor-actions">
        <button class="btn btn-secondary" @click="saveDraft" :disabled="saving">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21H5a2 2 0 01-2-2V5a2 2 0 012-2h11l5 5v11a2 2 0 01-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
          保存草稿
        </button>
        <button class="btn btn-primary" @click="submitForReview" :disabled="saving">
          <el-icon v-if="saving" class="is-loading"><Loading /></el-icon>
          发布
        </button>
      </div>
    </div>

    <div class="editor-layout">
      <aside class="outline-panel surface-card">
        <header><strong>文章大纲</strong><span>{{ outline.length }} 节</span></header>
        <nav v-if="outline.length">
          <button v-for="(item, index) in outline" :key="index" :class="'level-' + item.level">{{ item.text }}</button>
        </nav>
        <div v-else class="outline-empty">使用 Markdown 标题后，将在这里生成文章大纲。</div>
        <footer><span>字数 {{ wordCount }}</span><span>约 {{ readingMinutes }} 分钟</span></footer>
      </aside>

      <!-- Main editor -->
      <div class="editor-main surface-card">
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
          class="markdown-editor"
          v-model="form.content"
          :theme="isDark ? 'dark' : 'light'"
          :toolbars="toolbars"
          :preview="editorMode === 'split'"
          :preview-only="editorMode === 'preview'"
          placeholder="开始你的创作..."
        />
        <footer class="editor-status"><span>Markdown</span><span>字数：{{ wordCount }}</span><span>预计阅读：{{ readingMinutes }} 分钟</span><b>{{ lastSavedAt ? '全部更改已保存' : '正在编辑' }}</b></footer>
      </div>

      <!-- Settings sidebar -->
      <aside class="editor-sidebar">
        <!-- Status -->
        <div class="surface-card sidebar-block">
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
        <div class="surface-card sidebar-block">
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

        <div class="surface-card sidebar-block">
          <div class="sidebar-block-title">发布前检查 <span class="check-count">{{ completedChecks }}/4</span></div>
          <div class="publish-checks">
            <p :class="{ done: form.title.trim() }"><i>{{ form.title.trim() ? '✓' : '!' }}</i>标题已填写</p>
            <p :class="{ done: form.content.trim() }"><i>{{ form.content.trim() ? '✓' : '!' }}</i>正文已填写</p>
            <p :class="{ done: form.categoryId }"><i>{{ form.categoryId ? '✓' : '!' }}</i>选择文章分类</p>
            <p :class="{ done: form.summary.trim() }"><i>{{ form.summary.trim() ? '✓' : '!' }}</i>{{ form.summary.trim() ? '摘要已填写' : '缺少文章摘要' }}</p>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { addArticleService, updateArticleService, getMyArticleDetailService } from '../../api/article.js'
import { getCategoryListService } from '../../api/category.js'
import { useTheme } from '../../composables/useTheme.js'

const route = useRoute()
const router = useRouter()
const { isDark } = useTheme()

const isEdit = computed(() => !!route.params.id)
const saving = ref(false)
const categories = ref([])
const tagInput = ref('')
const editorMode = ref('edit')
const lastSavedAt = ref('')
let autosaveTimer = null

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

const wordCount = computed(() => form.content.replace(/[#>*_\-\[\]()]/g, '').replace(/\s+/g, '').length)
const readingMinutes = computed(() => Math.max(1, Math.ceil(wordCount.value / 400)))
const outline = computed(() => form.content.split('\n').map(line => {
  const match = line.match(/^(#{1,3})\s+(.+)/)
  return match ? { level: match[1].length, text: match[2].trim() } : null
}).filter(Boolean))
const completedChecks = computed(() => [form.title.trim(), form.content.trim(), form.categoryId, form.summary.trim()].filter(Boolean).length)
const draftKey = computed(() => 'moyu-editor-draft-' + (route.params.id || 'new'))

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
    if (form.articleId) {
      await updateArticleService(payload)
    } else {
      const res = await addArticleService(payload)
      form.articleId = res.data
    }
    lastSavedAt.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    localStorage.removeItem(draftKey.value)
    ElMessage.success('已保存草稿')
  } finally { saving.value = false }
}

async function submitForReview() {
  if (!form.title.trim()) { ElMessage.warning('请填写文章标题'); return }
  if (!form.content.trim()) { ElMessage.warning('请填写文章内容'); return }
  saving.value = true
  try {
    const payload = buildPayload('published')
    if (form.articleId) {
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
    const res = await getMyArticleDetailService(route.params.id)
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

function saveLocalDraft() {
  if (!form.title && !form.content) return
  localStorage.setItem(draftKey.value, JSON.stringify({
    title: form.title,
    summary: form.summary,
    content: form.content,
    categoryId: form.categoryId,
    coverImage: form.coverImage,
    tagNames: form.tagNames,
    savedAt: Date.now()
  }))
  lastSavedAt.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function restoreLocalDraft() {
  if (isEdit.value) return
  const raw = localStorage.getItem(draftKey.value)
  if (!raw) return
  try {
    const draft = JSON.parse(raw)
    Object.assign(form, draft)
    lastSavedAt.value = new Date(draft.savedAt).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    ElMessage.info('已恢复本地草稿')
  } catch {}
}

onMounted(() => {
  getCategoryListService().then(r => { categories.value = r.data || [] }).catch(() => {})
  loadArticle()
  restoreLocalDraft()
  autosaveTimer = window.setInterval(saveLocalDraft, 12000)
})
onUnmounted(() => window.clearInterval(autosaveTimer))
watch(() => [form.title, form.summary, form.content, form.categoryId, form.coverImage, form.tagNames.join(',')], () => {
  lastSavedAt.value = ''
})
</script>

<style scoped>
.save-article { display: flex; width: min(1500px, calc(100% - 32px)); height: 100%; min-height: 0; margin: 0 auto; padding: 14px 0; flex-direction: column; overflow: hidden; }

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 52px;
  flex: 0 0 auto;
  margin-bottom: 12px;
}
.editor-nav { display: flex; align-items: center; gap: 12px; }
.editor-title { max-width: 360px; overflow: hidden; color: var(--c-text); font-size: 17px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.editor-nav > div { display: flex; flex-direction: column; }
.autosave-state { color: var(--c-success); font-size: 10px; }
.editor-actions { display: flex; gap: 8px; }
.editor-modes { display: inline-flex; padding: 3px; border: 1px solid var(--c-border); border-radius: var(--radius); background: var(--c-surface); }
.editor-modes button { padding: 7px 11px; border: 0; border-radius: 7px; background: transparent; color: var(--c-text-3); font-size: 12px; font-weight: 600; }
.editor-modes button.active { background: var(--c-primary-soft); color: var(--c-primary); }

.editor-layout {
  display: grid;
  min-height: 0;
  flex: 1;
  grid-template-columns: 190px minmax(0, 1fr) 286px;
  gap: 14px;
  align-items: stretch;
}

.editor-main { display: flex; min-height: 0; flex-direction: column; overflow: hidden; }
.outline-panel { display: flex; min-height: 0; flex-direction: column; padding: 16px 10px; overflow: hidden; }
.outline-panel header { display: flex; align-items: center; justify-content: space-between; padding: 0 7px 12px; border-bottom: 1px solid var(--c-border); }
.outline-panel header span { color: var(--c-text-4); font-size: 10px; }
.outline-panel nav { display: flex; flex: 1; flex-direction: column; gap: 2px; padding-top: 10px; overflow: auto; }
.outline-panel nav button { padding: 7px 8px; overflow: hidden; border: 0; border-radius: 6px; background: transparent; color: var(--c-text-3); font-size: 11px; text-align: left; text-overflow: ellipsis; white-space: nowrap; }
.outline-panel nav button:hover { background: var(--c-primary-soft); color: var(--c-primary); }
.outline-panel nav .level-2 { padding-left: 18px; }
.outline-panel nav .level-3 { padding-left: 30px; }
.outline-empty { padding: 22px 8px; color: var(--c-text-4); font-size: 11px; line-height: 1.7; }
.outline-panel footer { display: flex; justify-content: space-between; padding: 12px 7px 0; border-top: 1px solid var(--c-border); color: var(--c-text-4); font-size: 9px; }

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
.markdown-editor { min-height: 0; flex: 1; border: 0; border-radius: 0; }
.editor-status { display: flex; align-items: center; gap: 18px; padding: 8px 14px; border-top: 1px solid var(--c-border); background: var(--c-surface-2); color: var(--c-text-4); font-size: 10px; }
.editor-status b { margin-left: auto; color: var(--c-success); font-weight: 600; }

/* Sidebar */
.editor-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
  height: 100%;
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
.sidebar-block-title .check-count { float: right; color: var(--c-success); }
.publish-checks { display: flex; flex-direction: column; gap: 8px; }
.publish-checks p { display: flex; align-items: center; gap: 7px; color: var(--c-warning); font-size: 11px; }
.publish-checks p.done { color: var(--c-text-3); }
.publish-checks i { display: grid; width: 17px; height: 17px; place-items: center; border-radius: 50%; background: var(--c-warning-soft); font-size: 10px; font-style: normal; font-weight: 800; }
.publish-checks p.done i { background: var(--c-success-soft); color: var(--c-success); }

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
  .editor-layout { grid-template-columns: 1fr 260px; }
  .outline-panel { display: none; }
  .editor-sidebar { position: static; }
  .editor-modes { order: 3; width: 100%; justify-content: center; }
  .editor-header { flex-wrap: wrap; }
}
@media (max-width: 680px) {
  .save-article { height: 100%; overflow-y: auto; }
  .editor-layout { display: block; }
  .editor-main { height: 720px; margin-bottom: 14px; }
  .editor-layout { grid-template-columns: 1fr; }
  .editor-modes { overflow-x: auto; }
  .editor-actions .btn-secondary { display: none; }
}
</style>

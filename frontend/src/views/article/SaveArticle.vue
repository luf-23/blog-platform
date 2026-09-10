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
        <button type="button" :class="{ active: editorMode === 'edit' }" @click="setEditorMode('edit')">✎ 编辑</button>
        <button type="button" :class="{ active: editorMode === 'split' }" @click="setEditorMode('split')">▣ 分屏预览</button>
        <button type="button" :class="{ active: editorMode === 'preview' }" @click="setEditorMode('preview')">◉ 预览</button>
      </div>
      <div class="editor-actions">
        <button
          class="btn btn-ghost settings-toggle"
          :aria-expanded="!settingsCollapsed"
          :title="settingsCollapsed ? '展开文章设置' : '收起文章设置'"
          @click="settingsCollapsed = !settingsCollapsed"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 6h16M4 12h16M4 18h16"/><circle cx="9" cy="6" r="2" fill="var(--c-surface)"/><circle cx="15" cy="12" r="2" fill="var(--c-surface)"/><circle cx="11" cy="18" r="2" fill="var(--c-surface)"/></svg>
          <span>{{ settingsCollapsed ? '展开设置' : '收起设置' }}</span>
        </button>
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

    <div :class="['editor-layout', { 'settings-collapsed': settingsCollapsed }]">
      <aside class="outline-panel surface-card">
        <header><strong>文章大纲</strong><span>{{ outline.length }} 节</span></header>
        <nav v-if="outline.length">
          <button
            v-for="(item, index) in outline"
            :key="item.offset"
            :class="['level-' + item.level, { active: activeOutlineIndex === index }]"
            :title="item.text"
            @click="jumpToOutline(item, index)"
          ><i></i><span>{{ item.text }}</span></button>
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
          rows="1"
        ></textarea>
        <div class="editor-divider"></div>
        <MdEditor
          ref="mdEditorRef"
          class="markdown-editor"
          v-model="form.content"
          :theme="isDark ? 'dark' : 'light'"
          :toolbars="toolbars"
          :footers="[]"
          :preview="false"
          placeholder="输入文章正文"
          @on-upload-img="uploadContentImages"
        />
        <footer class="editor-status"><span>Markdown</span><span>字数：{{ wordCount }}</span><span>预计阅读：{{ readingMinutes }} 分钟</span><b>{{ lastSavedAt ? '全部更改已保存' : '正在编辑' }}</b></footer>
      </div>

      <!-- Settings sidebar -->
      <aside v-show="!settingsCollapsed" class="editor-sidebar">
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
              <TagTaxonomyPicker v-model="form.tagNames" :tags="availableTags" :loading="tagsLoading" :limit="5" />
              <div class="tag-help">选择最能描述文章内容的标签，最多 5 个</div>
            </el-form-item>
          </el-form>
        </div>

        <!-- Cover image -->
        <div class="surface-card sidebar-block">
          <div class="sidebar-block-title">封面图</div>
          <div class="cover-preview">
            <img :src="form.coverImage || defaultCover" alt="封面"/>
            <button v-if="form.coverImage" class="cover-remove" @click="form.coverImage = ''" aria-label="移除当前封面">×</button>
          </div>
          <button class="btn btn-secondary btn-sm cover-upload" @click="coverDialogVisible = true">
            {{ form.coverImage ? '更换封面' : '上传封面' }}
          </button>
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

    <UploadImageDialog
      v-model:visible="coverDialogVisible"
      title="选择文章封面"
      default-ratio="16:9"
      :loading="uploadingImage"
      hint="支持 JPG、PNG、WEBP，图片大小不超过 5MB"
      @confirm="uploadCover"
    />
    <UploadImageDialog
      :visible="contentCropVisible"
      :initial-file="contentCropFile"
      :title="contentCropTitle"
      @update:visible="visible => { if (!visible) finishContentCrop(null) }"
      @confirm="finishContentCrop"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { MdEditor } from 'md-editor-v3'
import TagTaxonomyPicker from '../../components/article/TagTaxonomyPicker.vue'
import UploadImageDialog from '../../components/common/UploadImageDialog.vue'
import 'md-editor-v3/lib/style.css'
import { addArticleService, updateArticleService, getMyArticleDetailService } from '../../api/article.js'
import { getCategoryListService } from '../../api/category.js'
import { getAllTagsService } from '../../api/tag.js'
import { useTheme } from '../../composables/useTheme.js'
import { useUserInfoStore } from '../../store/userInfo.js'
import { OSSClient, uploadImageToOss } from '../../utils/oss/index.js'
import { DEFAULT_ARTICLE_COVER_URL as defaultCover } from '../../constants/assets.js'

const route = useRoute()
const router = useRouter()
const { isDark } = useTheme()
const userInfoStore = useUserInfoStore()

const isEdit = computed(() => !!route.params.id)
const saving = ref(false)
const categories = ref([])
const availableTags = ref([])
const tagsLoading = ref(false)
const editorMode = ref('edit')
const mdEditorRef = ref(null)
const settingsCollapsed = ref(false)
const activeOutlineIndex = ref(-1)
const lastSavedAt = ref('')
const coverDialogVisible = ref(false)
const contentCropVisible = ref(false)
const contentCropFile = ref(null)
const contentCropTitle = ref('裁剪正文图片')
let resolveContentCrop = null
let contentUploadQueue = Promise.resolve()
let editorDisposed = false
const uploadingImage = ref(false)
let autosaveTimer = null
let localPersistenceEnabled = true

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
  'prettier'
]

const wordCount = computed(() => form.content.replace(/[#>*_\-\[\]()]/g, '').replace(/\s+/g, '').length)
const readingMinutes = computed(() => Math.max(1, Math.ceil(wordCount.value / 400)))
const outline = computed(() => {
  const items = []
  let offset = 0
  let insideCodeFence = false
  form.content.split('\n').forEach(line => {
    if (/^\s*(```|~~~)/.test(line)) {
      insideCodeFence = !insideCodeFence
      offset += line.length + 1
      return
    }
    const match = insideCodeFence ? null : line.match(/^\s*(#{1,3})\s+(.+)/)
    if (match) {
      items.push({
        level: match[1].length,
        text: cleanOutlineText(match[2]),
        offset
      })
    }
    offset += line.length + 1
  })
  return items
})
const completedChecks = computed(() => [form.title.trim(), form.content.trim(), form.categoryId, form.summary.trim()].filter(Boolean).length)
const draftKey = computed(() => `bp-editor-draft-${userInfoStore.userInfo?.userId || 'anonymous'}-new`)

function cleanOutlineText(text) {
  return text
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
    .replace(/[`*_~]/g, '')
    .trim()
}

async function jumpToOutline(item, index) {
  activeOutlineIndex.value = index
  if (editorMode.value === 'preview') await setEditorMode('edit')
  await nextTick()
  mdEditorRef.value?.focus({ cursorPos: item.offset })
}

async function setEditorMode(mode) {
  editorMode.value = mode
  await nextTick()
  const editor = mdEditorRef.value
  if (!editor) return

  if (mode === 'edit') {
    editor.togglePreviewOnly(false)
    editor.togglePreview(false)
    return
  }
  if (mode === 'split') {
    editor.togglePreviewOnly(false)
    editor.togglePreview(true)
    return
  }
  editor.togglePreviewOnly(true)
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
    localPersistenceEnabled = false
    localStorage.removeItem(draftKey.value)
    localStorage.removeItem('moyu-editor-draft-new')
    ElMessage.success('已保存草稿')
    if (!isEdit.value && form.articleId) {
      await router.replace(`/article/edit/${form.articleId}`)
    }
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
    localPersistenceEnabled = false
    localStorage.removeItem(draftKey.value)
    localStorage.removeItem('moyu-editor-draft-new')
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
  if (!localPersistenceEnabled || isEdit.value) return
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

async function uploadCover(file) {
  uploadingImage.value = true
  try {
    form.coverImage = await uploadImageToOss(
      file,
      OSSClient.IMAGE_TYPE.ARTICLE_COVER,
      userInfoStore.userInfo?.userId
    )
    coverDialogVisible.value = false
    ElMessage.success('封面已更新')
  } catch (error) {
    ElMessage.error(error?.message || '封面上传失败')
  } finally {
    uploadingImage.value = false
  }
}

function finishContentCrop(file) {
  const resolve = resolveContentCrop
  resolveContentCrop = null
  contentCropVisible.value = false
  contentCropFile.value = null
  resolve?.(file)
}

function uploadContentImages(files, callback) {
  // Serialize paste/drop batches so only one crop dialog is active at a time.
  contentUploadQueue = contentUploadQueue.then(async () => {
    if (editorDisposed) return
    uploadingImage.value = true
    try {
      const croppedFiles = []
      for (const [index, file] of Array.from(files).entries()) {
        contentCropTitle.value = `裁剪正文图片（${index + 1}/${files.length}）`
        const croppedFile = await new Promise(resolve => {
          resolveContentCrop = resolve
          contentCropFile.value = file
          contentCropVisible.value = true
        })
        if (!croppedFile || editorDisposed) return
        croppedFiles.push(croppedFile)
        // Let the previous dialog state close before loading the next image.
        await nextTick()
      }
      const urls = await Promise.all(croppedFiles.map(file => uploadImageToOss(
        file,
        OSSClient.IMAGE_TYPE.ARTICLE_CONTENT,
        userInfoStore.userInfo?.userId
      )))
      if (!editorDisposed) callback(urls)
    } catch (error) {
      if (!editorDisposed) ElMessage.error(error?.message || '正文图片上传失败')
    } finally {
      uploadingImage.value = false
    }
  })
  return contentUploadQueue
}

async function loadAvailableTags() {
  tagsLoading.value = true
  try {
    const result = await getAllTagsService()
    availableTags.value = result.data || []
  } finally {
    tagsLoading.value = false
  }
}

onMounted(async () => {
  localStorage.removeItem('moyu-editor-draft-new')
  const categoryTask = getCategoryListService().then(r => { categories.value = r.data || [] }).catch(() => {})
  await loadAvailableTags()
  await loadArticle()
  restoreLocalDraft()
  const allowedNames = new Set(availableTags.value.filter(tag => tag.parentId != null).map(tag => tag.tagName))
  form.tagNames = form.tagNames.filter(name => allowedNames.has(name)).slice(0, 5)
  await categoryTask
  autosaveTimer = window.setInterval(saveLocalDraft, 12000)
})
onUnmounted(() => {
  window.clearInterval(autosaveTimer)
  editorDisposed = true
  finishContentCrop(null)
})
watch(() => [form.title, form.summary, form.content, form.categoryId, form.coverImage, form.tagNames.join(',')], () => {
  lastSavedAt.value = ''
})
</script>

<style scoped>
.save-article { display: flex; width: calc(100% - 24px); height: 100%; min-height: 0; margin: 0 auto; padding: 10px 0; flex-direction: column; overflow: hidden; }

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 48px;
  flex: 0 0 auto;
  margin-bottom: 8px;
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
  grid-template-columns: 216px minmax(0, 1fr) 320px;
  gap: 10px;
  align-items: stretch;
}
.editor-layout.settings-collapsed { grid-template-columns: 216px minmax(0, 1fr); }

.editor-main { display: flex; min-height: 0; flex-direction: column; overflow: hidden; }
.outline-panel { display: flex; min-height: 0; flex-direction: column; padding: 16px 12px 12px; overflow: hidden; border-radius: var(--radius-sm); }
.outline-panel header { display: flex; align-items: center; justify-content: space-between; padding: 0 8px 13px; border-bottom: 1px solid var(--c-border); }
.outline-panel header strong { color: var(--c-text); font-size: 15px; font-weight: 700; }
.outline-panel header span { color: var(--c-text-4); font-size: 12px; }
.outline-panel nav { position: relative; display: flex; flex: 1; flex-direction: column; gap: 2px; padding: 10px 0; overflow: auto; }
.outline-panel nav::before { position: absolute; top: 12px; bottom: 12px; left: 12px; width: 1px; background: var(--c-border); content: ''; }
.outline-panel nav button { position: relative; display: flex; min-height: 34px; align-items: center; gap: 8px; padding: 7px 10px 7px 24px; overflow: hidden; border: 0; border-radius: 4px; background: transparent; color: var(--c-text-3); font-size: 13px; line-height: 1.45; text-align: left; }
.outline-panel nav button i { position: absolute; z-index: 1; left: 9px; width: 7px; height: 7px; border: 2px solid var(--c-surface); border-radius: 50%; background: var(--c-border-strong); }
.outline-panel nav button span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.outline-panel nav button:hover { background: var(--c-surface-2); color: var(--c-text); }
.outline-panel nav button.active { background: var(--c-primary-soft); color: var(--c-primary); font-weight: 600; }
.outline-panel nav button.active i { background: var(--c-primary); }
.outline-panel nav .level-2 { padding-left: 36px; }
.outline-panel nav .level-2 i { left: 21px; }
.outline-panel nav .level-3 { padding-left: 48px; }
.outline-panel nav .level-3 i { left: 33px; width: 6px; height: 6px; }
.outline-empty { padding: 22px 9px; color: var(--c-text-4); font-size: 12px; line-height: 1.7; }
.outline-panel footer { display: flex; justify-content: space-between; padding: 11px 8px 0; border-top: 1px solid var(--c-border); color: var(--c-text-4); font-size: 11px; }

.title-input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 22px;
  font-weight: 700;
  padding: 16px 28px 8px;
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
  padding: 0 28px 12px;
  background: transparent;
  color: var(--c-text-3);
  font-family: inherit;
  resize: none;
  line-height: 1.6;
}
.summary-input::placeholder { color: var(--c-text-4); }

.editor-divider { height: 1px; background: var(--c-border); }
.markdown-editor { min-height: 0; flex: 1; border: 0; border-radius: 0; }
.markdown-editor :deep(.cm-scroller) {
  font-family: "JetBrains Mono", "SFMono-Regular", Consolas, "Liberation Mono", monospace;
  font-size: 15px;
  line-height: 1.72;
}
.markdown-editor :deep(.cm-content) { padding: 14px 16px 28px; caret-color: var(--c-primary); }
.markdown-editor :deep(.cm-line) { padding-inline: 4px; }
.markdown-editor :deep(.md-editor-preview) {
  padding: 28px 34px 48px;
  color: var(--c-text-2);
  font-family: Inter, ui-sans-serif, -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  font-size: 16px;
  line-height: 1.8;
}
.markdown-editor :deep(.md-editor-preview h1),
.markdown-editor :deep(.md-editor-preview h2),
.markdown-editor :deep(.md-editor-preview h3),
.markdown-editor :deep(.md-editor-preview h4) { color: var(--c-text); font-family: inherit; letter-spacing: -.01em; }
.markdown-editor :deep(.md-editor-preview h2) { margin-top: 2.8em; padding-top: 1em; border-top: 1px solid var(--c-border); }
.markdown-editor :deep(.md-editor-preview p) { margin: 1em 0; }
.editor-status { display: flex; align-items: center; gap: 18px; padding: 8px 14px; border-top: 1px solid var(--c-border); background: var(--c-surface-2); color: var(--c-text-4); font-size: 10px; }
.editor-status b { margin-left: auto; color: var(--c-success); font-weight: 600; }
.settings-toggle { border-color: var(--c-border); background: var(--c-surface); }
.settings-toggle:hover { border-color: var(--c-border-strong); background: var(--c-surface-2); }

/* Sidebar */
.editor-sidebar {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
  height: 100%;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-right: 2px;
}

.sidebar-block { padding: 16px; border-radius: 8px; }
.sidebar-block-title {
  margin-bottom: 14px;
  color: var(--c-text);
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0;
}
.editor-sidebar :deep(.el-form-item) { margin-bottom: 18px; }
.editor-sidebar :deep(.el-form-item:last-child) { margin-bottom: 0; }
.editor-sidebar :deep(.el-form-item__label) { margin-bottom: 7px; color: var(--c-text-2); font-size: 13px; font-weight: 600; line-height: 1.4; }
.sidebar-block-title .check-count { float: right; color: var(--c-success); }
.publish-checks { display: flex; flex-direction: column; gap: 8px; }
.publish-checks p { display: flex; align-items: center; gap: 7px; color: var(--c-warning); font-size: 11px; }
.publish-checks p.done { color: var(--c-text-3); }
.publish-checks i { display: grid; width: 17px; height: 17px; place-items: center; border-radius: 50%; background: var(--c-warning-soft); font-size: 10px; font-style: normal; font-weight: 800; }
.publish-checks p.done i { background: var(--c-success-soft); color: var(--c-success); }

/* Managed tag selector */
.tag-help { margin-top: 8px; color: var(--c-text-4); font-size: 11px; line-height: 1.5; }

@media (max-width: 1280px) {
  .editor-layout { grid-template-columns: 200px minmax(0, 1fr) 300px; }
  .editor-layout.settings-collapsed { grid-template-columns: 200px minmax(0, 1fr); }
}
@media (max-width: 1180px) {
  .editor-layout { grid-template-columns: minmax(0, 1fr) 300px; }
  .editor-layout.settings-collapsed { grid-template-columns: minmax(0, 1fr); }
  .outline-panel { display: none; }
}

/* Cover */
.cover-preview {
  position: relative;
  border-radius: var(--radius);
  overflow: hidden;
  aspect-ratio: 16/9;
}
.cover-preview img { width: 100%; height: 100%; object-fit: contain; background: var(--c-surface-2); }
.cover-upload { width: 100%; margin-top: 9px; }
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
  .editor-layout.settings-collapsed { grid-template-columns: 1fr; }
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
  .settings-toggle { display: inline-flex !important; width: 38px; padding: 0; }
  .settings-toggle span { display: none; }
}
</style>

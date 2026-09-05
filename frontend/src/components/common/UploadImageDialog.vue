<script setup>
import { computed, onBeforeUnmount, ref, shallowRef, useId, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { UploadFilled } from '@element-plus/icons-vue';
import { cropOutputSize, exportCroppedImage, fitCrop, isFullCrop, moveCrop, resizeCrop } from '../../utils/imageCrop.js';

const props = defineProps({
  visible: { type: Boolean, required: true },
  title: { type: String, default: '上传图片' },
  loading: { type: Boolean, default: false },
  hint: { type: String, default: '支持 JPG / PNG / GIF / WEBP，最大 5MB' },
  defaultRatio: { type: String, default: 'original' },
  initialFile: { type: Object, default: null }
});
const emit = defineEmits(['update:visible', 'confirm']);
const ratioId = useId();
const input = ref(null);
const board = ref(null);
const previewUrl = ref('');
const selectedFile = shallowRef(null);
const sourceImage = shallowRef(null);
const dimensions = ref({ width: 0, height: 0 });
const crop = ref({ x: 0, y: 0, width: 0, height: 0 });
const aspect = ref(props.defaultRatio);
const decoding = ref(false);
const exporting = ref(false);
const busy = computed(() => props.loading || decoding.value || exporting.value);
let selectionVersion = 0;
let gesture = null;
const corners = [
  { key: 'nw', label: '左上角' }, { key: 'ne', label: '右上角' },
  { key: 'sw', label: '左下角' }, { key: 'se', label: '右下角' }
];
const ratio = computed(() => {
  if (aspect.value === 'free') return null;
  if (aspect.value === 'original') return dimensions.value.width / dimensions.value.height;
  const [width, height] = aspect.value.split(':').map(Number);
  return width / height;
});
const wholeImage = computed(() => isFullCrop(crop.value, dimensions.value));
const outputSize = computed(() => wholeImage.value ? dimensions.value : cropOutputSize(crop.value));
const boardStyle = computed(() => {
  const imageRatio = dimensions.value.width / dimensions.value.height || 1;
  // Let layout size the board even before a dialog's resize notifications arrive.
  return {
    width: `min(100%, calc(var(--crop-stage-height) * ${imageRatio}))`,
    aspectRatio: String(imageRatio)
  };
});
const selectionStyle = computed(() => ({
  left: `${crop.value.x / dimensions.value.width * 100}%`,
  top: `${crop.value.y / dimensions.value.height * 100}%`,
  width: `${crop.value.width / dimensions.value.width * 100}%`,
  height: `${crop.value.height / dimensions.value.height * 100}%`
}));

function resetCrop() {
  endGesture();
  crop.value = fitCrop(dimensions.value.width, dimensions.value.height, ratio.value);
}

function restoreWholeImage() {
  aspect.value = 'original';
  resetCrop();
}

function reset() {
  selectionVersion++;
  endGesture();
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value);
  previewUrl.value = '';
  selectedFile.value = null;
  sourceImage.value = null;
  dimensions.value = { width: 0, height: 0 };
  decoding.value = false;
  if (input.value) input.value.value = '';
}

async function pickFile(file) {
  if (!file || props.loading || exporting.value) return;
  if (!file.type.startsWith('image/')) return ElMessage.error('只能上传图片文件');
  if (file.size > 5 * 1024 * 1024) return ElMessage.error('图片大小不能超过 5MB');
  const version = ++selectionVersion;
  const url = URL.createObjectURL(file);
  decoding.value = true;
  try {
    const image = new Image();
    image.src = url;
    await image.decode();
    if (version !== selectionVersion) { URL.revokeObjectURL(url); return; }
    if (!image.naturalWidth || !image.naturalHeight) throw new Error('Invalid image');
    if (previewUrl.value) URL.revokeObjectURL(previewUrl.value);
    previewUrl.value = url;
    sourceImage.value = image;
    selectedFile.value = file;
    dimensions.value = { width: image.naturalWidth, height: image.naturalHeight };
    aspect.value = props.defaultRatio;
    resetCrop();
  } catch {
    URL.revokeObjectURL(url);
    if (version === selectionVersion) ElMessage.error('无法读取这张图片，请选择有效的 JPG、PNG、GIF 或 WEBP 图片');
  } finally {
    if (version === selectionVersion) decoding.value = false;
  }
}

watch(() => [props.visible, props.initialFile], ([visible, file]) => {
  if (!visible) reset();
  else if (file) pickFile(file);
}, { immediate: true });
onBeforeUnmount(reset);

function onChange(event) {
  pickFile(event.target.files?.[0]);
  event.target.value = '';
}

function startGesture(event, corner = null) {
  if (busy.value || (event.pointerType === 'mouse' && event.button !== 0)) return;
  event.preventDefault();
  endGesture();
  const rect = board.value.getBoundingClientRect();
  gesture = { pointerId: event.pointerId, target: event.currentTarget, x: event.clientX, y: event.clientY, rect, crop: { ...crop.value }, corner };
  event.currentTarget.focus({ preventScroll: true });
  event.currentTarget.setPointerCapture(event.pointerId);
}

function drag(event) {
  if (!gesture || event.pointerId !== gesture.pointerId || busy.value) return;
  const dx = (event.clientX - gesture.x) * dimensions.value.width / gesture.rect.width;
  const dy = (event.clientY - gesture.y) * dimensions.value.height / gesture.rect.height;
  crop.value = gesture.corner
    ? resizeCrop(gesture.crop, gesture.corner, dx, dy, dimensions.value, ratio.value)
    : moveCrop(gesture.crop, dx, dy, dimensions.value);
}

function endGesture() {
  const previous = gesture;
  gesture = null;
  if (previous?.target.hasPointerCapture(previous.pointerId)) previous.target.releasePointerCapture(previous.pointerId);
}

function nudge(event, corner = null) {
  const directions = { ArrowLeft: [-1, 0], ArrowRight: [1, 0], ArrowUp: [0, -1], ArrowDown: [0, 1] };
  if (busy.value || !directions[event.key]) return;
  event.preventDefault();
  const [x, y] = directions[event.key];
  const step = event.shiftKey ? 10 : 1;
  crop.value = corner
    ? resizeCrop(crop.value, corner, x * step, y * step, dimensions.value, ratio.value)
    : moveCrop(crop.value, x * step, y * step, dimensions.value);
}

function close() {
  if (!props.loading && !exporting.value) emit('update:visible', false);
}

async function confirm() {
  if (!selectedFile.value || busy.value) return;
  exporting.value = true;
  const version = selectionVersion;
  endGesture();
  try {
    const file = await exportCroppedImage(sourceImage.value, selectedFile.value, { ...crop.value });
    if (version === selectionVersion && props.visible) emit('confirm', file);
  } catch (error) {
    if (version === selectionVersion) ElMessage.error(error.message || '图片裁剪失败');
  } finally {
    exporting.value = false;
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="value => { if (!value) close(); }"
    :title="title"
    width="min(720px, calc(100vw - 24px))"
    :close-on-click-modal="false"
    :close-on-press-escape="!loading && !exporting"
    :show-close="!loading && !exporting"
    append-to-body
    align-center
    class="image-crop-dialog"
  >
    <div class="image-crop" :aria-busy="busy" @drop.prevent="pickFile($event.dataTransfer?.files?.[0])" @dragover.prevent>
      <input ref="input" type="file" accept="image/*" class="file-input" :disabled="busy" @change="onChange" />
      <button v-if="!previewUrl" type="button" class="upload-zone" :disabled="busy" @click="input?.click()">
        <el-icon :size="36"><UploadFilled /></el-icon>
        <strong>{{ decoding ? '正在读取图片…' : '点击或拖拽上传图片' }}</strong>
        <span>{{ hint }}</span>
        <span>上传前可调整裁剪范围</span>
      </button>
      <template v-else>
        <div class="crop-toolbar">
          <label :for="ratioId">裁剪比例</label>
          <select :id="ratioId" v-model="aspect" :disabled="busy" @change="resetCrop">
            <option value="original">原图比例</option>
            <option value="free">自由裁剪</option>
            <option value="1:1">1:1 正方形</option>
            <option value="4:3">4:3 横向</option>
            <option value="16:9">16:9 横向</option>
            <option value="3:1">3:1 宽幅</option>
            <option value="3:4">3:4 竖向</option>
            <option value="9:16">9:16 竖向</option>
          </select>
          <el-button :disabled="busy" @click="restoreWholeImage">保留整图</el-button>
          <el-button :disabled="busy" @click="input?.click()">更换图片</el-button>
        </div>
        <div class="crop-stage">
          <div ref="board" class="crop-board" :style="boardStyle">
            <img :src="previewUrl" class="crop-image" alt="完整原图，亮色框内为保留范围" draggable="false" />
            <div
              class="crop-selection" :style="selectionStyle" tabindex="0" role="group" aria-label="裁剪范围，拖动或使用方向键移动"
              @pointerdown.stop="startGesture($event)" @pointermove="drag" @pointerup="endGesture" @pointercancel="endGesture" @lostpointercapture="endGesture"
              @keydown.stop="nudge($event)"
            >
              <span class="crop-grid" aria-hidden="true"></span>
              <button
                v-for="corner in corners" :key="corner.key" type="button" :class="['crop-handle', `crop-handle--${corner.key}`]"
                :aria-label="`调整裁剪框${corner.label}，可使用方向键`" :disabled="busy"
                @pointerdown.stop="startGesture($event, corner.key)" @keydown.stop="nudge($event, corner.key)"
              ></button>
            </div>
          </div>
        </div>
        <div class="crop-description">
          <span>拖动选框移动，拖动四角调整大小；框外内容不会上传。</span>
          <span class="crop-size">{{ outputSize.width }} × {{ outputSize.height }} px</span>
        </div>
        <p v-if="selectedFile?.type === 'image/gif' && !wholeImage" class="crop-note">裁剪 GIF 将保存为静态 PNG；选择“保留整图”可保留动画。</p>
      </template>
    </div>
    <template #footer>
      <el-button :disabled="loading || exporting" @click="close">取消</el-button>
      <el-button type="primary" :loading="loading || exporting" :disabled="!selectedFile || decoding" @click="confirm">
        {{ wholeImage ? '使用整张图片' : '裁剪并使用' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.image-crop { max-height: calc(100dvh - 180px); overflow-y: auto; }
.file-input { display: none; }
.upload-zone { width: 100%; min-height: 240px; padding: 24px; border: 1.5px dashed var(--c-border-strong); border-radius: var(--radius-sm); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; background: var(--c-surface-2); color: var(--c-text-3); cursor: pointer; }
.upload-zone:hover { border-color: var(--c-primary); }
.upload-zone strong, .upload-zone .el-icon { color: var(--c-primary); }
.upload-zone span { font-size: 12px; }
.crop-toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; padding-bottom: 14px; }
.crop-toolbar label { color: var(--c-text-2); font-size: 13px; }
.crop-toolbar select { min-height: 32px; max-width: 100%; padding: 4px 10px; border: 1px solid var(--c-border); border-radius: var(--radius-sm); background: var(--c-surface); color: var(--c-text); font: inherit; font-size: 13px; }
.crop-toolbar .el-button + .el-button { margin-left: 0; }
.crop-stage { --crop-stage-height: clamp(180px, 44dvh, 420px); height: var(--crop-stage-height); width: 100%; display: flex; align-items: center; justify-content: center; background: var(--c-surface-3); border-radius: var(--radius-sm); }
.crop-board { position: relative; overflow: hidden; flex-shrink: 0; user-select: none; }
.crop-image { position: absolute; inset: 0; display: block; width: 100%; height: 100%; object-fit: contain; pointer-events: none; }
.crop-selection { position: absolute; box-sizing: border-box; border: 2px solid white; box-shadow: 0 0 0 9999px rgb(0 0 0 / 48%); cursor: move; touch-action: none; }
.crop-selection:focus-visible { outline: 2px solid var(--c-primary); outline-offset: -4px; }
.crop-grid { position: absolute; inset: 0; pointer-events: none; background: linear-gradient(to right, transparent 33.1%, rgb(255 255 255 / 45%) 33.1%, rgb(255 255 255 / 45%) 33.5%, transparent 33.5%, transparent 66.4%, rgb(255 255 255 / 45%) 66.4%, rgb(255 255 255 / 45%) 66.8%, transparent 66.8%), linear-gradient(to bottom, transparent 33.1%, rgb(255 255 255 / 45%) 33.1%, rgb(255 255 255 / 45%) 33.5%, transparent 33.5%, transparent 66.4%, rgb(255 255 255 / 45%) 66.4%, rgb(255 255 255 / 45%) 66.8%, transparent 66.8%); }
.crop-handle { position: absolute; width: 26px; height: 26px; padding: 0; border: 0; background: transparent; touch-action: none; }
.crop-handle::after { content: ''; position: absolute; width: 10px; height: 10px; border: 2px solid white; background: var(--c-primary); border-radius: 2px; }
.crop-handle--nw { top: -2px; left: -2px; cursor: nwse-resize; }
.crop-handle--ne { top: -2px; right: -2px; cursor: nesw-resize; }
.crop-handle--sw { bottom: -2px; left: -2px; cursor: nesw-resize; }
.crop-handle--se { bottom: -2px; right: -2px; cursor: nwse-resize; }
.crop-handle--nw::after { top: 0; left: 0; }
.crop-handle--ne::after { top: 0; right: 0; }
.crop-handle--sw::after { bottom: 0; left: 0; }
.crop-handle--se::after { bottom: 0; right: 0; }
.crop-description { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 8px; padding-top: 12px; color: var(--c-text-3); font-size: 12px; }
.crop-size { white-space: nowrap; font-variant-numeric: tabular-nums; }
.crop-note { margin: 10px 0 0; font-size: 12px; color: var(--c-text-3); }
@media (max-width: 480px) { .crop-toolbar { gap: 6px; } .crop-toolbar label { width: 100%; } .crop-toolbar .el-button { padding: 8px 10px; } }
</style>

<script setup>
import { ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { UploadFilled } from "@element-plus/icons-vue";

const props = defineProps({
  visible: { type: Boolean, required: true },
  title: { type: String, default: "上传图片" },
  loading: { type: Boolean, default: false },
  hint: { type: String, default: "支持 JPG / PNG / GIF / WEBP，最大 5MB" }
});

const emit = defineEmits(["update:visible", "confirm"]);

const previewUrl = ref("");
const selectedFile = ref(null);

watch(
  () => props.visible,
  (val) => {
    if (!val) reset();
  }
);

function reset() {
  if (previewUrl.value && previewUrl.value.startsWith("blob:")) {
    URL.revokeObjectURL(previewUrl.value);
  }
  previewUrl.value = "";
  selectedFile.value = null;
}

function pickFile(file) {
  if (!file) return;
  if (!file.type.startsWith("image/")) {
    ElMessage.error("只能上传图片文件");
    return;
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error("图片大小不能超过 5MB");
    return;
  }
  selectedFile.value = file;
  if (previewUrl.value && previewUrl.value.startsWith("blob:")) {
    URL.revokeObjectURL(previewUrl.value);
  }
  previewUrl.value = URL.createObjectURL(file);
}

function onDrop(e) {
  const file = e.dataTransfer?.files?.[0];
  if (file) pickFile(file);
}

function onChange(e) {
  const file = e.target.files?.[0];
  if (file) pickFile(file);
}

function close() {
  emit("update:visible", false);
}

function confirm() {
  if (!selectedFile.value) {
    ElMessage.warning("请先选择图片");
    return;
  }
  emit("confirm", selectedFile.value);
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="(v) => emit('update:visible', v)"
    :title="title"
    width="440px"
    :close-on-click-modal="false"
    @close="close"
    align-center
  >
    <div class="upload-zone" @drop.prevent="onDrop" @dragover.prevent>
      <input
        type="file"
        accept="image/*"
        class="upload-zone__input"
        @change="onChange"
      />
      <template v-if="previewUrl">
        <img :src="previewUrl" class="upload-zone__preview" />
        <div class="upload-zone__overlay">
          <el-icon size="22"><UploadFilled /></el-icon>
          <span>点击或拖拽更换</span>
        </div>
      </template>
      <template v-else>
        <el-icon class="upload-zone__icon" size="36">
          <UploadFilled />
        </el-icon>
        <div class="upload-zone__title">点击或拖拽上传</div>
        <div class="upload-zone__hint">{{ hint }}</div>
      </template>
    </div>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="loading" @click="confirm">
        确认上传
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.upload-zone {
  position: relative;
  height: 220px;
  border-radius: var(--bp-radius-md);
  border: 1.5px dashed var(--bp-color-border-strong);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s ease, background 0.2s ease;
  background: var(--bp-color-bg-soft);
}

.upload-zone:hover {
  border-color: var(--bp-color-primary);
  background: var(--bp-color-primary-soft);
}

.upload-zone__input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.upload-zone__preview {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-zone__overlay {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.upload-zone:hover .upload-zone__overlay {
  opacity: 1;
}

.upload-zone__icon {
  color: var(--bp-color-primary);
}

.upload-zone__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--bp-color-text-primary);
}

.upload-zone__hint {
  font-size: 12px;
  color: var(--bp-color-text-tertiary);
}
</style>

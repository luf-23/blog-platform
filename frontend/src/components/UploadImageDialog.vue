<script setup>
import { ref } from "vue";
import { ElMessage } from "element-plus";
import { Upload } from "@element-plus/icons-vue";
//子组件不能修改props,只能通知父组件进行修改
const props = defineProps({
  visible: {
    type: Boolean,
    required: true
  },
  title: {
    type: String,
    default: "上传图片"
  },
  loading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(["update:visible", "close", "file-change", "confirm"]);

const imageUrl = ref("");
const selectedFile = ref(null);

// 处理文件上传前的验证
const beforeUpload = (file) => {
  const isImage = file.type.startsWith("image/");
  const isLt5M = file.size / 1024 / 1024 < 5;

  if (!isImage) {
    ElMessage.error("只能上传图片文件!");
    return false;
  }
  if (!isLt5M) {
    ElMessage.error("图片大小不能超过 5MB!");
    return false;
  }
  return true;
};

// 处理文件变更
const handleFileChange = (file) => {
  if (file && beforeUpload(file)) {
    selectedFile.value = file;
    imageUrl.value = URL.createObjectURL(file);
    //emit("file-change", file);
  }
};

// 处理拖拽文件
const handleDrop = (e) => {
  e.preventDefault();
  const file = e.dataTransfer.files[0];
  handleFileChange(file);
};

// 关闭对话框
const handleClose = () => {
  emit("update:visible", false);
  //emit("close");
  imageUrl.value = "";
  selectedFile.value = null;
};

// 确认上传
const handleConfirm = () => {
  if (!selectedFile.value) {
    ElMessage.warning("请先选择图片");
    return;
  }
  emit("confirm", selectedFile.value); // 触发确认上传事件，父组件通过@confirm 监听
};
</script>

<template>
  <el-dialog
    :modelValue="visible"
    @update:modelValue="(val) => emit('update:visible', val)"
    :title="title"
    width="400px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="upload-container" @drop.prevent="handleDrop" @dragover.prevent>
      <div class="upload-area" :class="{ 'has-image': imageUrl }">
        <template v-if="imageUrl">
          <img :src="imageUrl" class="preview-image" />
          <div class="image-overlay">
            <el-icon><Upload /></el-icon>
            <span>点击或拖拽更换图片</span>
          </div>
        </template>
        <template v-else>
          <el-icon><Upload /></el-icon>
          <div class="upload-text">
            <span>点击或拖拽上传图片</span>
            <p class="upload-hint">支持 jpg、png 格式，大小不超过 5MB</p>
          </div>
        </template>
        <input
          type="file"
          class="file-input"
          accept="image/*"
          @change="(e) => handleFileChange(e.target.files[0])"
        />
      </div>
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleConfirm">
          确认上传
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<style scoped>
.upload-container {
  padding: 20px;
}

.upload-area {
  position: relative;
  width: 100%;
  height: 200px;
  border: 2px dashed #dcdfe6;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s;
  overflow: hidden;
}

.upload-area:hover {
  border-color: var(--el-color-primary);
}

.upload-area.has-image {
  border-style: solid;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s;
  color: white;
}

.upload-area.has-image:hover .image-overlay {
  opacity: 1;
}

.upload-text {
  text-align: center;
  color: #606266;
}

.upload-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.file-input {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  opacity: 0;
  cursor: pointer;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}
</style>
]]>

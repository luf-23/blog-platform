<script setup>
import { reactive, ref, onMounted, onUnmounted } from "vue";
import { useUserInfoStore } from "../store/userInfo.js";
import { storeToRefs } from "pinia";
import { ElMessage } from "element-plus";
import { useRoute } from "vue-router";
import {
  getUserInfoByNameService,
  updateUserInfoService
} from "../api/user.js";
import { ossClient } from "../utils/oss";
import UploadImageDialog from "../components/UploadImageDialog.vue";

const route = useRoute();
const author = route.query.author ? route.query.author : null;
const userInfoStore = useUserInfoStore();
const { userInfo } = storeToRefs(userInfoStore);
const isAuthor = ref(userInfo.value.username === author);
if (!author) {
  isAuthor.value = true; // 如果没有提供作者，则视为当前用户
}
//其他用户数据
const userInfoData = ref({
  username: "",
  nickname: "",
  signature: "",
  avatarImage: "",
  backgroundImage: ""
});
const getUserInfoData = async () => {
  const result = await getUserInfoByNameService({
    username: author
  });
  userInfoData.value = {
    username: result.data.username,
    nickname: result.data.nickname,
    signature: result.data.signature,
    avatarImage: result.data.avatarImage,
    backgroundImage: result.data.backgroundImage
  };
};
if (author && author !== userInfo.value.username) {
  getUserInfoData();
}

// 图片上传相关
const uploadDialogVisible = ref(false);
const isAvatar = ref(false);
const uploadLoading = ref(false);

const openAvatarDialog = () => {
  isAvatar.value = true;
  uploadDialogVisible.value = true;
};

const openBackgroundDialog = () => {
  isAvatar.value = false;
  uploadDialogVisible.value = true;
};

const handleUpload = async (file) => {
  if (!file) {
    ElMessage.warning("请先选择图片");
    return;
  }

  uploadLoading.value = true;
  try {
    await ossClient.init();
    const extension = file.name.split(".").pop();
    const type = isAvatar.value
      ? ossClient.constructor.IMAGE_TYPE.AVATAR
      : ossClient.constructor.IMAGE_TYPE.BACKGROUND;
    const fileName = ossClient.generateFileName(
      userInfo.value.userId,
      type,
      extension
    );
    const fileUrl = ossClient.generateFileUrl(fileName);
    await ossClient.uploadFile(fileName, file);

    if (isAvatar.value) {
      formData.avatarImage = fileUrl;
    } else {
      formData.backgroundImage = fileUrl;
    }

    uploadDialogVisible.value = false;
    ElMessage.success("上传成功");
  } catch (error) {
    console.error("上传失败:", error);
    ElMessage.error("上传失败，请重试");
  } finally {
    uploadLoading.value = false;
  }
};

const dialogVisible = ref(false);
const formData = reactive({
  nickname: "",
  signature: "",
  avatarImage: "",
  backgroundImage: ""
});

const openDialog = function () {
  dialogVisible.value = true;
  formData.nickname = userInfo.value.nickname;
  formData.signature = userInfo.value.signature;
  formData.avatarImage = userInfo.value.avatarImage;
  formData.backgroundImage = userInfo.value.backgroundImage;
};

const closeDialog = function () {
  dialogVisible.value = false;
};

const formRef = ref(null);

const rules = {
  nickname: [{ required: true, message: "昵称不能为空", trigger: "blur" }]
};

const submitForm = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
    await updateUserInfo(formData);
    ElMessage.success("更新成功");
    closeDialog();
  } catch (error) {
    console.error("表单验证失败:", error);
    ElMessage.error("请检查表单输入是否正确");
  }
};

const updateUserInfo = async (data) => {
  userInfo.value.nickname = data.nickname;
  userInfo.value.signature = data.signature;
  userInfo.value.avatarImage = data.avatarImage;
  userInfo.value.backgroundImage = data.backgroundImage;
  await updateUserInfoService({
    nickname: data.nickname,
    signature: data.signature,
    avatarImage: data.avatarImage,
    backgroundImage: data.backgroundImage
  });
};

const windowWidth = ref(window?.innerWidth || 768);

const updateWindowWidth = () => {
  windowWidth.value = window.innerWidth;
};

onMounted(() => {
  updateWindowWidth();
  window.addEventListener("resize", updateWindowWidth);
});

onUnmounted(() => {
  window.removeEventListener("resize", updateWindowWidth);
});

const handleAvatarClick = () => {
  openDialog();
};
</script>

<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <div class="background-image">
        <img
          :src="
            isAuthor ? userInfo.backgroundImage : userInfoData.backgroundImage
          "
          alt="Background"
        />
      </div>

      <div class="user-info">
        <el-avatar
          :size="100"
          :src="isAuthor ? userInfo.avatarImage : userInfoData.avatarImage"
          class="avatar"
          style="cursor: pointer"
          @click="handleAvatarClick"
        />

        <div class="info-content">
          <h2 class="nickname">
            {{ isAuthor ? userInfo.nickname : userInfoData.nickname }}
          </h2>
          <div class="username">
            @{{ isAuthor ? userInfo.username : userInfoData.username }}
          </div>
          <div class="signature">
            {{ isAuthor ? userInfo.signature : userInfoData.signature }}
          </div>
        </div>

        <div class="actions">
          <el-button
            type="primary"
            size="small"
            v-on:click="openDialog"
            v-if="isAuthor"
          >
            编辑资料</el-button
          >
        </div>
      </div>

      <el-tabs class="user-tabs">
        <el-tab-pane label="基本信息">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户名">{{
              isAuthor ? userInfo.username : userInfoData.username
            }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{
              isAuthor ? userInfo.nickname : userInfoData.nickname
            }}</el-descriptions-item>
            <el-descriptions-item label="个性签名">{{
              isAuthor ? userInfo.signature : userInfoData.signature
            }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑资料"
      :width="windowWidth <= 768 ? '90%' : '30%'"
      :modal="true"
      :model="formData"
      :close-on-click-modal="false"
      class="edit-profile-dialog"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="80px"
        class="edit-form"
      >
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="formData.nickname"></el-input>
        </el-form-item>
        <el-form-item label="个性签名">
          <el-input
            v-model.number="formData.signature"
            type="textarea"
            :rows="3"
          ></el-input>
        </el-form-item>
        <el-form-item label="头像">
          <div class="avatar-input-group">
            <el-input v-model="formData.avatarImage"></el-input>
            <el-button @click="openAvatarDialog">选择头像</el-button>
          </div>
        </el-form-item>
        <el-form-item label="背景图片">
          <div class="background-input-group">
            <el-input v-model="formData.backgroundImage"></el-input>
            <el-button @click="openBackgroundDialog">选择背景</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeDialog">取消</el-button>
          <el-button type="primary" @click="submitForm">提交</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 图片上传对话框 -->
    <UploadImageDialog
      v-model:visible="uploadDialogVisible"
      :title="isAvatar ? '选择头像' : '选择背景图片'"
      :loading="uploadLoading"
      @confirm="handleUpload"
    />
  </div>
</template>

<style scoped>
.profile-container {
  padding: 0;
  min-height: 100%;
}

.profile-card {
  max-width: 1000px;
  margin: 0 auto;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  overflow: hidden;
  background: transparent;
  position: relative;
}

.background-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.background-image::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0.4) 0%,
    rgba(0, 0, 0, 0.6) 50%,
    rgba(0, 0, 0, 0.7) 100%
  );
  z-index: 2;
}

.background-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-info {
  position: relative;
  padding: 30px;
  z-index: 3;
  color: #fff;
}

.avatar {
  border: 4px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  transition: transform 0.3s ease;
}

.avatar:hover {
  transform: scale(1.05);
}

.info-content {
  margin-top: 15px;
}

.nickname {
  font-size: 28px;
  margin: 0;
  color: #fff;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.username {
  color: rgba(255, 255, 255, 0.9);
  margin: 8px 0;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.signature {
  color: rgba(255, 255, 255, 0.9);
  margin: 10px 0;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.actions {
  position: absolute;
  top: 20px;
  right: 20px;
}

.user-tabs {
  position: relative;
  z-index: 3;
  margin-top: 20px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  margin: 20px;
}

:deep(.el-descriptions__label) {
  font-weight: 500;
}

:deep(.el-descriptions__content) {
  line-height: 1.6;
}

.edit-profile-dialog {
  padding: 20px;
}

.edit-form {
  margin-top: 10px;
}

.avatar-input-group,
.background-input-group {
  display: flex;
  gap: 10px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 移动端适配样式 */
@media screen and (max-width: 768px) {
  .profile-container {
    padding: 10px;
  }

  .user-info {
    padding: 20px 15px;
  }

  .actions {
    position: absolute;
    top: 15px;
    right: 15px;
    margin-top: 0;
    text-align: right;
  }

  .nickname {
    font-size: 22px;
    padding-right: 60px;
  }

  .edit-form {
    padding: 0 10px;
  }

  :deep(.el-form-item__label) {
    float: none;
    display: block;
    text-align: left;
    margin-bottom: 8px;
  }

  .avatar-input-group,
  .background-input-group {
    flex-direction: column;
  }

  .avatar-input-group .el-button,
  .background-input-group .el-button {
    margin-top: 8px;
    width: 100%;
  }

  :deep(.el-dialog__body) {
    padding: 15px;
  }

  :deep(.el-dialog__footer) {
    padding: 10px 15px 15px;
  }
}
</style>

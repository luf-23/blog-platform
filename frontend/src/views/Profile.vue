<script setup>
import { reactive, ref, computed, onMounted, watch } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { EditPen, Picture, UserFilled, Calendar } from "@element-plus/icons-vue";
import { storeToRefs } from "pinia";

import { useUserInfoStore } from "../store/userInfo.js";
import {
  getUserInfoByNameService,
  updateUserInfoService
} from "../api/user.js";
import { ossClient } from "../utils/oss/index.js";
import UploadImageDialog from "../components/common/UploadImageDialog.vue";

const route = useRoute();
const userInfoStore = useUserInfoStore();
const { userInfo } = storeToRefs(userInfoStore);

const queryAuthor = computed(() => route.query.author || null);
const isAuthor = computed(
  () => !queryAuthor.value || queryAuthor.value === userInfo.value?.username
);

const otherUser = ref({});

async function loadOther() {
  if (!queryAuthor.value || queryAuthor.value === userInfo.value?.username) return;
  const result = await getUserInfoByNameService({ username: queryAuthor.value });
  otherUser.value = result.data || {};
}

watch(queryAuthor, loadOther, { immediate: true });

const display = computed(() => (isAuthor.value ? userInfo.value : otherUser.value));

const dialogVisible = ref(false);
const formRef = ref(null);
const formData = reactive({
  nickname: "",
  signature: "",
  avatarImage: "",
  backgroundImage: ""
});

const rules = {
  nickname: [{ required: true, message: "昵称不能为空", trigger: "blur" }]
};

function openEdit() {
  Object.assign(formData, {
    nickname: userInfo.value?.nickname || "",
    signature: userInfo.value?.signature || "",
    avatarImage: userInfo.value?.avatarImage || "",
    backgroundImage: userInfo.value?.backgroundImage || ""
  });
  dialogVisible.value = true;
}

async function submitEdit() {
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  userInfoStore.setUserInfo({ ...userInfo.value, ...formData });
  await updateUserInfoService(formData);
  ElMessage.success("已更新个人资料");
  dialogVisible.value = false;
}

const uploadVisible = ref(false);
const uploadLoading = ref(false);
const uploadType = ref("avatar");

function openUpload(type) {
  uploadType.value = type;
  uploadVisible.value = true;
}

async function handleUpload(file) {
  uploadLoading.value = true;
  try {
    await ossClient.init();
    const extension = file.name.split(".").pop();
    const ossType =
      uploadType.value === "avatar"
        ? ossClient.constructor.IMAGE_TYPE.AVATAR
        : ossClient.constructor.IMAGE_TYPE.BACKGROUND;
    const fileName = ossClient.generateFileName(
      userInfo.value?.userId || "user",
      ossType,
      extension
    );
    await ossClient.uploadFile(fileName, file);
    const url = ossClient.generateFileUrl(fileName);
    if (uploadType.value === "avatar") {
      formData.avatarImage = url;
    } else {
      formData.backgroundImage = url;
    }
    ElMessage.success("上传成功");
    uploadVisible.value = false;
  } catch {
    ElMessage.error("上传失败");
  } finally {
    uploadLoading.value = false;
  }
}

function formatDate(value) {
  if (!value) return "暂无";
  return new Date(value).toLocaleDateString("zh-CN");
}

onMounted(loadOther);
</script>

<template>
  <div class="bp-page profile-page">
    <section class="profile-hero bp-card">
      <div
        class="profile-hero__bg"
        :style="{
          backgroundImage: display?.backgroundImage
            ? `url(${display.backgroundImage})`
            : 'none'
        }"
      />
      <div class="profile-hero__content">
        <el-avatar
          :size="96"
          :src="display?.avatarImage || '/avatar/avatar1.png'"
          class="profile-hero__avatar"
        />
        <div class="profile-hero__info">
          <h1>{{ display?.nickname || display?.username || "未命名用户" }}</h1>
          <p class="profile-hero__username">@{{ display?.username }}</p>
          <p class="profile-hero__signature">
            "{{ display?.signature || "这个人很懒，还没有签名" }}"
          </p>
        </div>
        <div v-if="isAuthor" class="profile-hero__actions">
          <el-button type="primary" :icon="EditPen" @click="openEdit">
            编辑资料
          </el-button>
        </div>
      </div>
    </section>

    <section class="info-grid">
      <div class="info-card bp-card">
        <h3>账号信息</h3>
        <ul>
          <li>
            <span class="info-card__label">
              <el-icon><UserFilled /></el-icon> 用户名
            </span>
            <span>{{ display?.username }}</span>
          </li>
          <li>
            <span class="info-card__label">
              <el-icon><EditPen /></el-icon> 昵称
            </span>
            <span>{{ display?.nickname || "未设置" }}</span>
          </li>
          <li>
            <span class="info-card__label">
              <el-icon><Calendar /></el-icon> 注册时间
            </span>
            <span>{{ formatDate(display?.createTime) }}</span>
          </li>
        </ul>
      </div>

      <div class="info-card bp-card">
        <h3>个性签名</h3>
        <p class="signature-text">
          {{ display?.signature || "尚未填写个性签名" }}
        </p>
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      title="编辑资料"
      width="520px"
      align-center
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-position="top"
      >
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="formData.nickname" placeholder="给自己起个名字" />
        </el-form-item>
        <el-form-item label="个性签名">
          <el-input
            v-model="formData.signature"
            type="textarea"
            :rows="3"
            placeholder="描述一下你自己"
          />
        </el-form-item>
        <el-form-item label="头像">
          <div class="dual-input">
            <el-input v-model="formData.avatarImage" placeholder="头像 URL" />
            <el-button :icon="Picture" @click="openUpload('avatar')">
              上传
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="背景图">
          <div class="dual-input">
            <el-input v-model="formData.backgroundImage" placeholder="背景 URL" />
            <el-button :icon="Picture" @click="openUpload('background')">
              上传
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <UploadImageDialog
      v-model:visible="uploadVisible"
      :title="uploadType === 'avatar' ? '更换头像' : '更换背景'"
      :loading="uploadLoading"
      @confirm="handleUpload"
    />
  </div>
</template>

<style scoped>
.profile-hero {
  position: relative;
  overflow: hidden;
  isolation: isolate;
}

.profile-hero__bg {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  filter: blur(8px);
  opacity: 0.5;
  transform: scale(1.1);
}

.profile-hero__bg::after {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(
    180deg,
    rgba(0, 0, 0, 0.1) 0%,
    var(--bp-color-bg-elevated) 100%
  );
}

.profile-hero__content {
  position: relative;
  padding: 36px clamp(20px, 3vw, 32px);
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 24px;
  align-items: center;
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0) 0%,
    var(--bp-color-bg-elevated) 100%
  );
}

:root[data-theme="dark"] .profile-hero__content {
  background: linear-gradient(
    180deg,
    rgba(0, 0, 0, 0) 0%,
    var(--bp-color-bg-elevated) 100%
  );
}

.profile-hero__avatar {
  border: 4px solid var(--bp-color-bg-elevated);
  box-shadow: var(--bp-shadow-md);
}

.profile-hero__info h1 {
  font-size: 24px;
  font-weight: 700;
}

.profile-hero__username {
  margin-top: 4px;
  color: var(--bp-color-text-tertiary);
  font-size: 13px;
}

.profile-hero__signature {
  margin-top: 12px;
  color: var(--bp-color-text-secondary);
  font-style: italic;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.info-card {
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-card h3 {
  font-size: 15px;
}

.info-card ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-card li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  color: var(--bp-color-text-secondary);
}

.info-card__label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--bp-color-text-tertiary);
  font-size: 13px;
}

.signature-text {
  font-style: italic;
  color: var(--bp-color-text-secondary);
  line-height: 1.7;
}

.dual-input {
  display: flex;
  gap: 8px;
  width: 100%;
}

@media (max-width: 720px) {
  .profile-hero__content {
    grid-template-columns: 1fr;
    text-align: center;
    justify-items: center;
  }
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>

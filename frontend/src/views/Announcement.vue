<script setup>
import { computed, ref } from "vue";
import { storeToRefs } from "pinia";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Plus,
  Delete,
  InfoFilled,
  CircleCheckFilled,
  WarningFilled,
  CircleCloseFilled
} from "@element-plus/icons-vue";

import PageHeader from "../components/common/PageHeader.vue";
import EmptyState from "../components/common/EmptyState.vue";
import {
  getAnnouncementListService,
  addAnnouncementService,
  deleteAnnouncementService
} from "../api/admin.js";
import { useUserInfoStore } from "../store/userInfo.js";

const userInfoStore = useUserInfoStore();
const { userInfo } = storeToRefs(userInfoStore);
const isAdmin = computed(() => userInfo.value?.username === "admin");

const announcements = ref([]);
const loading = ref(false);

async function fetchList() {
  loading.value = true;
  try {
    const result = await getAnnouncementListService();
    announcements.value = (result.data || []).map((item) => ({
      id: item.id,
      title: item.title,
      content: item.content,
      date: item.date,
      type: item.type || "info"
    }));
    announcements.value.sort((a, b) => new Date(b.date) - new Date(a.date));
  } finally {
    loading.value = false;
  }
}

fetchList();

const TYPE_MAP = {
  info: { label: "通知", icon: InfoFilled, color: "var(--bp-color-info)" },
  success: { label: "公告", icon: CircleCheckFilled, color: "var(--bp-color-success)" },
  warning: { label: "提醒", icon: WarningFilled, color: "var(--bp-color-warning)" },
  danger: { label: "重要", icon: CircleCloseFilled, color: "var(--bp-color-danger)" }
};

function typeInfo(type) {
  return TYPE_MAP[type] || TYPE_MAP.info;
}

function formatDate(value) {
  if (!value) return "";
  return new Date(value).toLocaleString("zh-CN", { hour12: false });
}

function remove(id) {
  ElMessageBox.confirm("确定删除该公告吗？", "删除公告", {
    confirmButtonText: "删除",
    cancelButtonText: "取消",
    type: "warning"
  })
    .then(async () => {
      await deleteAnnouncementService({ id });
      ElMessage.success("删除成功");
      fetchList();
    })
    .catch(() => {});
}

const dialogVisible = ref(false);
const formRef = ref(null);
const formData = ref({ title: "", content: "", type: "info" });
const rules = {
  title: [{ required: true, message: "请填写标题", trigger: "blur" }],
  content: [{ required: true, message: "请填写内容", trigger: "blur" }]
};

function openCreate() {
  formData.value = { title: "", content: "", type: "info" };
  dialogVisible.value = true;
}

async function submit() {
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  await addAnnouncementService(formData.value);
  ElMessage.success("公告已发布");
  dialogVisible.value = false;
  fetchList();
}
</script>

<template>
  <div class="bp-page bp-page--compact">
    <PageHeader title="系统公告" compact>
      <template #actions>
        <el-button v-if="isAdmin" type="primary" :icon="Plus" @click="openCreate">
          发布公告
        </el-button>
      </template>
    </PageHeader>

    <div v-loading="loading" class="announcement-list">
      <article
        v-for="item in announcements"
        :key="item.id"
        class="announcement bp-card"
      >
        <div
          class="announcement__icon"
          :style="{
            background: `color-mix(in srgb, ${typeInfo(item.type).color} 18%, transparent)`,
            color: typeInfo(item.type).color
          }"
        >
          <el-icon size="18">
            <component :is="typeInfo(item.type).icon" />
          </el-icon>
        </div>
        <div class="announcement__body">
          <header class="announcement__head">
            <div>
              <span
                class="announcement__tag"
                :style="{
                  color: typeInfo(item.type).color,
                  background: `color-mix(in srgb, ${typeInfo(item.type).color} 12%, transparent)`
                }"
              >
                {{ typeInfo(item.type).label }}
              </span>
              <h3>{{ item.title }}</h3>
            </div>
            <el-button
              v-if="isAdmin"
              size="small"
              type="danger"
              plain
              :icon="Delete"
              @click="remove(item.id)"
            >
              删除
            </el-button>
          </header>
          <p class="announcement__content">{{ item.content }}</p>
          <span class="announcement__date">{{ formatDate(item.date) }}</span>
        </div>
      </article>

      <EmptyState
        v-if="!loading && !announcements.length"
        title="暂无公告"
        description="目前没有需要关注的系统消息"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      title="发布公告"
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
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="5"
            placeholder="公告内容"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="formData.type" style="width: 100%">
            <el-option label="通知" value="info" />
            <el-option label="公告" value="success" />
            <el-option label="提醒" value="warning" />
            <el-option label="重要" value="danger" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.announcement-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.announcement {
  display: flex;
  gap: 16px;
  padding: 18px 20px;
}

.announcement__icon {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.announcement__body {
  flex: 1;
  min-width: 0;
}

.announcement__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.announcement__head h3 {
  margin-top: 6px;
  font-size: 16px;
}

.announcement__tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.announcement__content {
  margin-top: 10px;
  color: var(--bp-color-text-secondary);
  line-height: 1.7;
  white-space: pre-wrap;
}

.announcement__date {
  display: inline-block;
  margin-top: 10px;
  font-size: 12px;
  color: var(--bp-color-text-tertiary);
}
</style>

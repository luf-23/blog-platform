<script setup>
import { ref } from "vue";
import {
  getAnnouncementListService,
  deleteAnnouncementService,
  addAnnouncementService
} from "../api/admin";
import { useUserInfoStore } from "../store/userInfo.js";
import { storeToRefs } from "pinia";
import { ElMessageBox, ElMessage } from "element-plus";
import {
  InfoFilled,
  CircleCheckFilled,
  WarningFilled,
  CircleCloseFilled
} from "@element-plus/icons-vue";
import { onMounted, onUnmounted } from "vue";

const userInfoStore = useUserInfoStore();
const { userInfo } = storeToRefs(userInfoStore);
const isadmin = userInfo.value.username === "admin";
const announcements = ref([]);
const getAnnouncementList = async () => {
  try {
    const result = await getAnnouncementListService();
    announcements.value = result.data.map((item) => ({
      id: item.id,
      title: item.title,
      content: item.content,
      date: item.date,
      type: item.type // 'success', 'info', 'warning', 'danger'
    }));
    // 按日期降序排序
    announcements.value.sort((a, b) => new Date(b.date) - new Date(a.date));
  } catch (error) {
    console.error("获取公告列表失败:", error);
  }
};
getAnnouncementList();

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm("确定要删除该公告吗？", "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning"
    });
    await deleteAnnouncementService({
      id: id
    });
    ElMessage.success("删除成功");
    getAnnouncementList();
  } catch (error) {
    if (error !== "cancel") {
      console.error("删除公告失败:", error);
      ElMessage.error("删除失败");
    }
  }
};
const dialogVisible = ref(false);
const dialogWidth = ref(window.innerWidth < 768 ? "90%" : "40%");
// 添加窗口大小变化监听函数
const updateDialogWidth = () => {
  dialogWidth.value = window.innerWidth > 768 ? "40%" : "90%";
};

// 在组件挂载时添加监听器
onMounted(() => {
  window.addEventListener("resize", updateDialogWidth);
});

// 在组件卸载时移除监听器
onUnmounted(() => {
  window.removeEventListener("resize", updateDialogWidth);
});
const handleCreate = () => {
  dialogVisible.value = true;
};

const formData = ref({
  title: "",
  content: "",
  type: "info"
});

const handleSubmit = async () => {
  try {
    await addAnnouncementService({
      title: formData.value.title,
      content: formData.value.content,
      type: formData.value.type
    });
    ElMessage.success("创建成功");
    dialogVisible.value = false;
    formData.value = { title: "", content: "", type: "info" };
    getAnnouncementList();
  } catch (error) {
    console.error("创建公告失败:", error);
    ElMessage.error("创建失败");
  }
};
</script>

<template>
  <div class="announcement-container">
    <div class="header-container">
      <h2 class="page-title">系统公告</h2>
      <el-button v-if="isadmin" type="primary" @click="handleCreate"
        >新建公告</el-button
      >
    </div>
    <template v-if="announcements.length > 0">
      <el-timeline style="padding-left: 5px">
        <el-timeline-item
          v-for="item in announcements"
          :key="item.id"
          :type="item.type"
          :timestamp="item.date"
          size="large"
        >
          <el-card class="announcement-card">
            <template #header>
              <div class="card-header">
                <h3>{{ item.title }}</h3>
                <el-button
                  v-if="isadmin"
                  type="danger"
                  size="small"
                  @click="handleDelete(item.id)"
                >
                  删除
                </el-button>
              </div>
            </template>
            <p class="announcement-content">{{ item.content }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </template>
    <el-empty v-else description="暂无系统公告" :image-size="200" />
    <!-- 新建公告对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="新建公告"
      :width="dialogWidth"
      :close-on-click-modal="false"
    >
      <el-form :model="formData" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="formData.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="4"
            placeholder="请输入公告内容"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="formData.type" placeholder="请选择公告类型">
            <el-option label="普通" value="info">
              <template #default>
                <el-icon class="info-icon"><InfoFilled /></el-icon>
                <span>普通</span>
              </template>
            </el-option>
            <el-option label="成功" value="success">
              <template #default>
                <el-icon class="success-icon"><CircleCheckFilled /></el-icon>
                <span>成功</span>
              </template>
            </el-option>
            <el-option label="警告" value="warning">
              <template #default>
                <el-icon class="warning-icon"><WarningFilled /></el-icon>
                <span>警告</span>
              </template>
            </el-option>
            <el-option label="危险" value="danger">
              <template #default>
                <el-icon class="danger-icon"><CircleCloseFilled /></el-icon>
                <span>危险</span>
              </template>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.announcement-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-title {
  margin-bottom: 0;
  color: #303133;
  text-align: center;
}

.announcement-card {
  margin-bottom: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  color: #303133;
  font-size: 16px;
}

.announcement-content {
  color: #606266;
  line-height: 1.6;
  margin: 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.info-icon {
  color: #909399;
  margin-right: 8px;
}

.success-icon {
  color: #67c23a;
  margin-right: 8px;
}

.warning-icon {
  color: #e6a23c;
  margin-right: 8px;
}

.danger-icon {
  color: #f56c6c;
  margin-right: 8px;
}
</style>

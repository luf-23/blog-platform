<script setup>
import { ref, onMounted } from "vue";
import { markRaw } from "vue";
import { useTokenStore } from "../store/token";
import { useUserInfoStore } from "../store/userInfo";
import { storeToRefs } from "pinia";
import { useRouter } from "vue-router";
import {
  User,
  Document,
  Bell,
  ChatDotSquare,
  ChatDotRound,
  HotWater
} from "@element-plus/icons-vue";

const router = useRouter();
const tokenStore = useTokenStore();
const userInfoStore = useUserInfoStore();
const token = tokenStore.token;
const { userInfo } = storeToRefs(userInfoStore);
const loading = ref(false);

// 获取时间相关数据
const currentDate = ref(new Date());
const timeOfDay = ref("");

const updateDateTime = () => {
  currentDate.value = new Date();
  const hour = currentDate.value.getHours();
  if (hour < 6) timeOfDay.value = "凌晨好";
  else if (hour < 9) timeOfDay.value = "早上好";
  else if (hour < 12) timeOfDay.value = "上午好";
  else if (hour < 14) timeOfDay.value = "中午好";
  else if (hour < 17) timeOfDay.value = "下午好";
  else if (hour < 19) timeOfDay.value = "傍晚好";
  else timeOfDay.value = "晚上好";
};

// 更新时间
setInterval(updateDateTime, 1000);

// 快捷入口数据
const quickActions = ref([
  {
    title: "社区讨论",
    icon: markRaw(ChatDotSquare),
    path: "/community",
    color: "#60A5FA",
    description: "参与社区讨论，分享你的想法"
  },
  {
    title: "AI助手",
    icon: markRaw(ChatDotRound),
    path: "/ai/chat",
    color: "#A855F7",
    description: "智能AI助手"
  },
  {
    title: "文章管理",
    icon: markRaw(Document),
    path: "/article/category",
    color: "#F56C6C",
    description: "管理你的文章内容"
  },
  {
    title: "个人信息",
    icon: markRaw(User),
    path: "/profile",
    color: "#409EFF",
    description: "查看和编辑个人资料"
  },
  {
    title: "系统公告",
    icon: markRaw(Bell),
    path: "/announcement",
    color: "#67C23A",
    description: "查看最新系统公告"
  }
]);
if (userInfo.value.username === "admin") {
  quickActions.value.push({
    title: "管理员入口",
    icon: markRaw(HotWater),
    path: "/admin/home",
    color: "#666666",
    description: "提供管理员相关接口"
  });
}
onMounted(() => {
  if (!token) {
    router.push("/login");
    return;
  }
  updateDateTime();
});

const handleWelcomeClick = () => {
  router.push({
    path: "/profile"
  });
};
</script>

<template>
  <div class="home-page">
    <!-- 顶部欢迎区域 -->
    <div
      class="welcome-section"
      :style="{
        backgroundImage: `url(${
          userInfo.backgroundImage || '/background/background1.jpg'
        })`
      }"
      @click="handleWelcomeClick"
    >
      <div class="welcome-content">
        <el-avatar
          :size="64"
          :src="userInfo.avatarImage || '/avatar/avatar1.png'"
        />
        <div class="welcome-text">
          <h2>{{ timeOfDay }}，{{ userInfo.nickname || "亲爱的用户" }}</h2>
          <p class="signature">
            {{ userInfo.signature || "这个人很懒，还没有设置签名" }}
          </p>
        </div>
        <div class="date-time">
          <p>
            {{ currentDate.toLocaleString("zh-CN", { hour12: false }) }}
          </p>
        </div>
      </div>
    </div>

    <!-- 快捷入口区域 -->
    <div class="quick-actions">
      <h3>快捷入口</h3>
      <div class="actions-grid">
        <el-card
          v-for="action in quickActions"
          :key="action.title"
          class="action-card"
          :body-style="{ padding: '20px' }"
          @click="router.push(action.path)"
        >
          <div class="action-content">
            <el-icon :size="30" :color="action.color">
              <component :is="action.icon" />
            </el-icon>
            <h4>{{ action.title }}</h4>
            <p>{{ action.description }}</p>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  padding: 0;
  min-height: calc(100vh - 60px);
}

.welcome-section {
  background-size: cover;
  background-position: center;
  border-radius: 16px;
  height: 220px;
  margin-bottom: 32px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  transition: all 0.3s ease;
  cursor: pointer;
}

.welcome-section:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.welcome-section::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.4), rgba(0, 0, 0, 0.2));
  backdrop-filter: blur(1px);
}

.welcome-content {
  position: relative;
  padding: 40px;
  color: white;
  display: flex;
  align-items: center;
  gap: 24px;
  height: 100%;
}

.welcome-text h2 {
  margin: 0;
  font-size: 28px;
  margin-bottom: 8px;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.signature {
  margin: 0;
  font-size: 16px;
  opacity: 0.95;
  font-style: italic;
}

.date-time {
  margin-left: auto;
  text-align: right;
  font-size: 16px;
  font-weight: 500;
}

.quick-actions {
  padding: 32px;
  background: linear-gradient(135deg, #ffffff, #f8fafc);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.quick-actions h3 {
  margin: 0 0 24px 0;
  color: #1e293b;
  font-size: 20px;
  font-weight: 600;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
}

.action-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 12px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  overflow: hidden;
}

.action-card:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.12);
  border-color: #e2e8f0;
}

.action-content {
  text-align: center;
  padding: 8px 0;
}

.action-content h4 {
  margin: 16px 0 8px 0;
  color: #1e293b;
  font-size: 18px;
  font-weight: 600;
}

.action-content p {
  margin: 0;
  font-size: 14px;
  color: #64748b;
  line-height: 1.5;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .home-page {
    padding: 0;
    margin: 0; /* 确保没有外边距 */
  }

  .welcome-section {
    height: auto;
    min-height: 160px; /* 减少高度 */
    margin-bottom: 16px; /* 减少底部间距 */
    border-radius: 12px;
  }

  .welcome-content {
    flex-direction: column;
    text-align: center;
    padding: 20px; /* 减少内边距 */
    gap: 12px; /* 减少间距 */
  }

  .welcome-text h2 {
    font-size: 20px;
  }

  .signature {
    font-size: 14px;
  }

  .date-time {
    width: 100%;
    margin: 0;
    text-align: center;
    font-size: 14px;
  }

  .quick-actions {
    padding: 16px; /* 减少内边距 */
    margin: 0; /* 确保没有外边距 */
  }

  .actions-grid {
    grid-template-columns: 1fr;
    gap: 12px; /* 减少间距 */
  }

  .action-card:hover {
    transform: translateY(-4px) scale(1.01);
  }
}

@media (max-width: 480px) {
  .welcome-content {
    padding: 20px;
  }

  .welcome-text h2 {
    font-size: 18px;
  }

  .quick-actions {
    padding: 16px;
  }

  .quick-actions h3 {
    font-size: 18px;
  }
}
</style>

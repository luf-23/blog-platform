<script setup>
import { check } from "../../utils/admin/check";
import { useRouter } from "vue-router";
import { markRaw } from "vue";
import {
  User,
  Document,
  Bell,
  Setting,
  TrendCharts,
  CaretTop,
  CaretBottom,
  Operation
} from "@element-plus/icons-vue";

const router = useRouter();
check();

// 定义快捷操作卡片数据
const quickActions = [
  {
    title: "用户管理",
    description: "查看和管理用户信息",
    icon: markRaw(User),
    color: "#409EFF",
    path: { name: "UserManager" }
  },
  {
    title: "文章管理",
    description: "管理文章内容和分类",
    icon: markRaw(Document),
    color: "#67C23A",
    path: { name: "ArticleListManager" }
  },
  {
    title: "发布公告",
    description: "发布系统公告",
    icon: markRaw(Bell),
    color: "#E6A23C",
    path: { name: "Announcement" }
  },
  {
    title: "系统设置",
    description: "管理系统配置",
    icon: markRaw(Setting),
    color: "#909399",
    path: "/admin/settings"
  }
];

// 系统概览数据（示例数据）
const systemOverview = [
  {
    title: "总用户数",
    value: "1,234",
    increase: "+12%",
    color: "#409EFF",
    icon: User
  },
  {
    title: "文章总数",
    value: "856",
    increase: "+8%",
    color: "#67C23A",
    icon: Document
  },
  {
    title: "系统公告",
    value: "12",
    increase: "+2",
    color: "#E6A23C",
    icon: Bell
  },
  {
    title: "活跃度",
    value: "89%",
    increase: "+5%",
    color: "#909399",
    icon: TrendCharts
  }
];
</script>

<template>
  <div class="admin-container">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <div class="welcome-content">
        <div class="welcome-text">
          <h1>管理员控制台</h1>
          <p>欢迎回来，管理员</p>
        </div>
        <div class="welcome-time">{{ new Date().toLocaleTimeString() }}</div>
      </div>
      <div class="welcome-decoration"></div>
    </div>

    <!-- 系统概览 -->
    <div class="overview-section">
      <h2 class="section-title">
        <el-icon><TrendCharts /></el-icon>
        系统概览
      </h2>
      <div class="overview-grid">
        <el-card
          v-for="item in systemOverview"
          :key="item.title"
          class="overview-card"
          :class="{ 'positive-trend': item.increase.includes('+') }"
        >
          <div class="overview-content">
            <div class="overview-info">
              <h3>{{ item.title }}</h3>
              <div class="overview-value">{{ item.value }}</div>
              <div class="overview-increase" :style="{ color: item.color }">
                <el-icon v-if="item.increase.includes('+')" color="#67C23A"
                  ><CaretTop
                /></el-icon>
                <el-icon v-else color="#F56C6C"><CaretBottom /></el-icon>
                {{ item.increase }}
              </div>
            </div>
            <div
              class="overview-icon"
              :style="{ backgroundColor: item.color + '15' }"
            >
              <el-icon :size="32" :color="item.color">
                <component :is="item.icon" />
              </el-icon>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 快捷操作区域 -->
    <div class="quick-actions">
      <h2 class="section-title">
        <el-icon><Operation /></el-icon>
        快捷操作
      </h2>
      <div class="actions-grid">
        <el-card
          v-for="action in quickActions"
          :key="action.title"
          class="action-card"
          shadow="hover"
          :body-style="{ padding: '24px' }"
          @click="router.push(action.path)"
        >
          <div class="action-content">
            <div
              class="action-icon"
              :style="{ backgroundColor: action.color + '15' }"
            >
              <el-icon :size="32" :color="action.color">
                <component :is="action.icon" />
              </el-icon>
            </div>
            <h3>{{ action.title }}</h3>
            <p>{{ action.description }}</p>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-container {
  padding: 24px;
  background-color: #f8fafc;
  min-height: calc(100vh - 48px);
}

.welcome-section {
  background: linear-gradient(135deg, #4158d0 0%, #c850c0 46%, #ffcc70 100%);
  border-radius: 16px;
  padding: 32px;
  color: white;
  margin-bottom: 32px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(200, 80, 192, 0.2);
}

.welcome-content {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.welcome-text h1 {
  margin: 0 0 12px 0;
  font-size: 32px;
  font-weight: 600;
  letter-spacing: -0.5px;
}

.welcome-text p {
  margin: 0;
  font-size: 16px;
  opacity: 0.9;
  font-weight: 300;
}

.welcome-time {
  font-size: 24px;
  font-weight: 300;
  opacity: 0.9;
}

.welcome-decoration {
  position: absolute;
  right: -100px;
  top: -100px;
  width: 300px;
  height: 300px;
  background: radial-gradient(
    circle at center,
    rgba(255, 255, 255, 0.2) 0%,
    rgba(255, 255, 255, 0) 70%
  );
  border-radius: 50%;
  animation: rotate 20s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.section-title {
  margin: 0 0 24px 0;
  font-size: 22px;
  color: #1f2937;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

.overview-card {
  border-radius: 12px;
  transition: all 0.3s ease;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.overview-card.positive-trend {
  background: linear-gradient(to right bottom, #ffffff, #f0fdf4);
}

.overview-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.overview-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.overview-info {
  flex: 1;
}

.overview-info h3 {
  margin: 0 0 12px 0;
  font-size: 15px;
  color: #64748b;
  font-weight: 500;
}

.overview-value {
  font-size: 32px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.overview-increase {
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}

.overview-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
}

.action-card {
  border-radius: 12px;
  transition: all 0.3s ease;
  border: none;
  background: #ffffff;
  cursor: pointer;
}

.action-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.action-content {
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.action-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.action-content h3 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
  font-weight: 600;
}

.action-content p {
  margin: 0;
  font-size: 14px;
  color: #64748b;
  line-height: 1.5;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .admin-container {
    padding: 16px;
  }

  .welcome-section {
    padding: 24px;
    margin-bottom: 24px;
  }

  .welcome-text h1 {
    font-size: 24px;
  }

  .welcome-time {
    font-size: 18px;
  }

  .section-title {
    font-size: 20px;
    margin-bottom: 20px;
  }

  .overview-grid {
    gap: 16px;
    margin-bottom: 24px;
  }

  .actions-grid {
    gap: 16px;
  }

  .overview-value {
    font-size: 28px;
  }
}

@media (max-width: 480px) {
  .admin-container {
    padding: 12px;
  }

  .welcome-section {
    padding: 20px;
    margin-bottom: 20px;
  }

  .welcome-content {
    flex-direction: column;
    gap: 16px;
  }

  .welcome-text h1 {
    font-size: 22px;
  }

  .welcome-time {
    font-size: 16px;
  }

  .overview-grid,
  .actions-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .overview-value {
    font-size: 24px;
  }
}
</style>

<script setup>
import { ref, markRaw, computed } from "vue";
import { useRouter } from "vue-router";
import {
  User,
  Document,
  Bell,
  TrendCharts,
  Setting,
  Operation,
  ArrowRight
} from "@element-plus/icons-vue";
import PageHeader from "../../components/common/PageHeader.vue";

const router = useRouter();

const now = ref(new Date());

const stats = [
  {
    title: "总用户数",
    value: "1,234",
    delta: "+12%",
    icon: markRaw(User),
    accent: "var(--bp-color-primary)"
  },
  {
    title: "文章总数",
    value: "856",
    delta: "+8%",
    icon: markRaw(Document),
    accent: "var(--bp-color-success)"
  },
  {
    title: "系统公告",
    value: "12",
    delta: "+2",
    icon: markRaw(Bell),
    accent: "var(--bp-color-warning)"
  },
  {
    title: "用户活跃度",
    value: "89%",
    delta: "+5%",
    icon: markRaw(TrendCharts),
    accent: "var(--bp-color-info)"
  }
];

const tools = [
  {
    title: "用户管理",
    desc: "查看与管理平台用户",
    icon: markRaw(User),
    accent: "var(--bp-color-primary)",
    path: { name: "UserManager" }
  },
  {
    title: "文章审核",
    desc: "审核与下架社区文章",
    icon: markRaw(Document),
    accent: "var(--bp-color-success)",
    path: { name: "ArticleListManager" }
  },
  {
    title: "发布公告",
    desc: "向所有用户发布系统通知",
    icon: markRaw(Bell),
    accent: "var(--bp-color-warning)",
    path: "/announcement"
  },
  {
    title: "系统设置",
    desc: "调整平台运行参数",
    icon: markRaw(Setting),
    accent: "var(--bp-color-text-secondary)",
    path: "/admin/home"
  }
];

const timeText = computed(() =>
  now.value.toLocaleTimeString("zh-CN", { hour12: false })
);
const dateText = computed(() =>
  now.value.toLocaleDateString("zh-CN", { dateStyle: "full" })
);

function go(path) {
  router.push(path);
}
</script>

<template>
  <div class="bp-page">
    <PageHeader title="管理后台" subtitle="平台数据与运营操作总览" />

    <section class="hero bp-card">
      <div class="hero__bg" />
      <div class="hero__content">
        <div>
          <p class="hero__date">{{ dateText }}</p>
          <h2>欢迎回来，管理员</h2>
          <p class="hero__desc">
            这里是平台的核心控制中心。请谨慎使用每一项操作。
          </p>
        </div>
        <div class="hero__time">{{ timeText }}</div>
      </div>
    </section>

    <section>
      <h3 class="section-title">
        <el-icon><TrendCharts /></el-icon> 数据概览
      </h3>
      <div class="stat-grid">
        <div v-for="stat in stats" :key="stat.title" class="stat-card bp-card">
          <div
            class="stat-card__icon"
            :style="{
              background: `color-mix(in srgb, ${stat.accent} 14%, transparent)`,
              color: stat.accent
            }"
          >
            <el-icon size="20"><component :is="stat.icon" /></el-icon>
          </div>
          <div class="stat-card__body">
            <span class="stat-card__title">{{ stat.title }}</span>
            <strong class="stat-card__value">{{ stat.value }}</strong>
            <span
              class="stat-card__delta"
              :style="{ color: stat.accent }"
            >{{ stat.delta }}</span>
          </div>
        </div>
      </div>
    </section>

    <section>
      <h3 class="section-title">
        <el-icon><Operation /></el-icon> 快捷操作
      </h3>
      <div class="tool-grid">
        <button
          v-for="tool in tools"
          :key="tool.title"
          class="tool-card bp-card bp-card-hover"
          @click="go(tool.path)"
        >
          <span
            class="tool-card__icon"
            :style="{
              background: `color-mix(in srgb, ${tool.accent} 14%, transparent)`,
              color: tool.accent
            }"
          >
            <el-icon size="20"><component :is="tool.icon" /></el-icon>
          </span>
          <div class="tool-card__body">
            <strong>{{ tool.title }}</strong>
            <p>{{ tool.desc }}</p>
          </div>
          <el-icon class="tool-card__arrow"><ArrowRight /></el-icon>
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.hero {
  position: relative;
  padding: 28px;
  overflow: hidden;
  isolation: isolate;
}

.hero__bg {
  position: absolute;
  inset: 0;
  background: var(--bp-gradient-hero);
  opacity: 0.12;
  z-index: -1;
}

.hero__content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.hero__date {
  font-size: 12px;
  color: var(--bp-color-text-tertiary);
}

.hero h2 {
  font-size: clamp(20px, 2.4vw, 26px);
  margin: 4px 0;
}

.hero__desc {
  color: var(--bp-color-text-secondary);
  font-size: 13px;
  max-width: 540px;
}

.hero__time {
  font-size: clamp(24px, 3vw, 32px);
  font-weight: 600;
  color: var(--bp-color-text-primary);
  font-variant-numeric: tabular-nums;
}

.section-title {
  font-size: 16px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 14px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.stat-card {
  padding: 18px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card__title {
  font-size: 12px;
  color: var(--bp-color-text-tertiary);
}

.stat-card__value {
  display: block;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.02em;
  margin: 2px 0;
}

.stat-card__delta {
  font-size: 12px;
  font-weight: 600;
}

.tool-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.tool-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  background: var(--bp-color-bg-elevated);
  border: 1px solid var(--bp-color-border);
  border-radius: var(--bp-radius-md);
  cursor: pointer;
  text-align: left;
  color: inherit;
}

.tool-card__icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tool-card__body {
  flex: 1;
  min-width: 0;
}

.tool-card__body strong {
  display: block;
  font-size: 14px;
}

.tool-card__body p {
  margin-top: 4px;
  font-size: 12.5px;
  color: var(--bp-color-text-tertiary);
}

.tool-card__arrow {
  color: var(--bp-color-text-tertiary);
}
</style>

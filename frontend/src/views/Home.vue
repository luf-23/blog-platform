<script setup>
import { computed, onMounted, onUnmounted, ref, markRaw } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import {
  Document,
  Bell,
  ChatDotSquare,
  ChatDotRound,
  User,
  Setting,
  ArrowRight,
  Star,
  Reading,
  Promotion
} from "@element-plus/icons-vue";

import { useUserInfoStore } from "../store/userInfo.js";

const router = useRouter();
const userInfoStore = useUserInfoStore();
const { userInfo } = storeToRefs(userInfoStore);

const now = ref(new Date());
let timer = null;

onMounted(() => {
  timer = setInterval(() => {
    now.value = new Date();
  }, 1000);
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});

const greeting = computed(() => {
  const hour = now.value.getHours();
  if (hour < 6) return "凌晨好";
  if (hour < 12) return "上午好";
  if (hour < 14) return "中午好";
  if (hour < 18) return "下午好";
  if (hour < 22) return "晚上好";
  return "夜深了";
});

const dateText = computed(() =>
  now.value.toLocaleDateString("zh-CN", {
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "long"
  })
);

const timeText = computed(() =>
  now.value.toLocaleTimeString("zh-CN", { hour12: false })
);

const isAdmin = computed(() => userInfo.value?.username === "admin");

const quickActions = computed(() => {
  const base = [
    {
      title: "我的文章",
      desc: "管理你的分类和文章",
      icon: markRaw(Document),
      gradient: "linear-gradient(135deg, #6366f1, #8b5cf6)",
      path: "/article/category"
    },
    {
      title: "社区广场",
      desc: "看看大家都在分享什么",
      icon: markRaw(ChatDotSquare),
      gradient: "linear-gradient(135deg, #06b6d4, #0ea5e9)",
      path: "/community"
    },
    {
      title: "AI 助手",
      desc: "和 AI 进行流畅对话",
      icon: markRaw(ChatDotRound),
      gradient: "linear-gradient(135deg, #ec4899, #f43f5e)",
      path: "/ai/chat"
    },
    {
      title: "系统公告",
      desc: "查看平台最新动态",
      icon: markRaw(Bell),
      gradient: "linear-gradient(135deg, #f59e0b, #ef4444)",
      path: "/announcement"
    },
    {
      title: "个人主页",
      desc: "完善你的个人资料",
      icon: markRaw(User),
      gradient: "linear-gradient(135deg, #10b981, #34d399)",
      path: "/profile"
    }
  ];
  if (isAdmin.value) {
    base.push({
      title: "管理后台",
      desc: "管理用户与内容审核",
      icon: markRaw(Setting),
      gradient: "linear-gradient(135deg, #64748b, #334155)",
      path: "/admin/home"
    });
  }
  return base;
});

const stats = [
  { label: "持续创作", value: "Inspire", icon: markRaw(Star) },
  { label: "畅享阅读", value: "Explore", icon: markRaw(Reading) },
  { label: "高效互动", value: "Engage", icon: markRaw(Promotion) }
];

function navigate(path) {
  router.push(path);
}
</script>

<template>
  <div class="bp-page home-page">
    <section class="hero">
      <div
        class="hero__background"
        :style="{
          backgroundImage: userInfo?.backgroundImage
            ? `url(${userInfo.backgroundImage})`
            : 'none'
        }"
      />
      <div class="hero__content">
        <div class="hero__greeting">
          <el-avatar
            :size="64"
            :src="userInfo?.avatarImage || '/avatar/avatar1.png'"
            class="hero__avatar"
            @click="navigate('/profile')"
          />
          <div>
            <p class="hero__date">{{ dateText }} · {{ timeText }}</p>
            <h1 class="hero__title">
              {{ greeting }}，{{ userInfo?.nickname || userInfo?.username || "朋友" }} 👋
            </h1>
            <p class="hero__sub">
              {{ userInfo?.signature || "在这里记录所有值得分享的灵感" }}
            </p>
          </div>
        </div>

        <div class="hero__stats">
          <div v-for="s in stats" :key="s.label" class="hero__stat">
            <el-icon :size="18"><component :is="s.icon" /></el-icon>
            <div>
              <strong>{{ s.value }}</strong>
              <span>{{ s.label }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section>
      <div class="section-head">
        <div>
          <h2 class="section-title">快捷入口</h2>
          <p class="bp-page-subtitle">从最常用的功能开始你的一天</p>
        </div>
      </div>

      <div class="actions-grid">
        <button
          v-for="action in quickActions"
          :key="action.title"
          class="action-card bp-card bp-card-hover"
          @click="navigate(action.path)"
        >
          <span
            class="action-card__icon"
            :style="{ background: action.gradient }"
          >
            <el-icon size="22"><component :is="action.icon" /></el-icon>
          </span>
          <div class="action-card__body">
            <h3>{{ action.title }}</h3>
            <p>{{ action.desc }}</p>
          </div>
          <el-icon class="action-card__arrow"><ArrowRight /></el-icon>
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-page {
  gap: 28px;
}

.hero {
  position: relative;
  padding: 36px clamp(20px, 3vw, 36px);
  border-radius: var(--bp-radius-lg);
  background: var(--bp-color-bg-elevated);
  border: 1px solid var(--bp-color-border);
  overflow: hidden;
  isolation: isolate;
}

.hero__background {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  filter: blur(8px) saturate(1.1);
  opacity: 0.35;
  transform: scale(1.1);
  z-index: -2;
}

.hero::before {
  content: "";
  position: absolute;
  inset: 0;
  background: var(--bp-gradient-soft);
  z-index: -1;
}

.hero__content {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 24px;
  align-items: center;
}

.hero__greeting {
  display: flex;
  align-items: center;
  gap: 18px;
}

.hero__avatar {
  cursor: pointer;
  border: 3px solid var(--bp-color-bg-elevated);
  box-shadow: var(--bp-shadow-md);
  transition: transform 0.2s ease;
}

.hero__avatar:hover {
  transform: scale(1.05);
}

.hero__date {
  font-size: 12px;
  color: var(--bp-color-text-tertiary);
  margin-bottom: 4px;
}

.hero__title {
  font-size: clamp(22px, 2.6vw, 30px);
  font-weight: 700;
  letter-spacing: -0.02em;
}

.hero__sub {
  margin-top: 6px;
  font-size: 14px;
  color: var(--bp-color-text-secondary);
  max-width: 480px;
}

.hero__stats {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.hero__stat {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  border-radius: 14px;
  background: var(--bp-color-bg-elevated);
  border: 1px solid var(--bp-color-border);
  color: var(--bp-color-text-secondary);
}

.hero__stat strong {
  display: block;
  font-size: 14px;
  color: var(--bp-color-text-primary);
}

.hero__stat span {
  font-size: 11px;
  color: var(--bp-color-text-tertiary);
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.action-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  text-align: left;
  border: 1px solid var(--bp-color-border);
  border-radius: var(--bp-radius-md);
  background: var(--bp-color-bg-elevated);
  color: inherit;
  cursor: pointer;
}

.action-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
  box-shadow: var(--bp-shadow-sm);
}

.action-card__body {
  flex: 1;
  min-width: 0;
}

.action-card__body h3 {
  font-size: 15px;
  font-weight: 600;
}

.action-card__body p {
  margin-top: 4px;
  font-size: 12.5px;
  color: var(--bp-color-text-tertiary);
}

.action-card__arrow {
  color: var(--bp-color-text-tertiary);
  transition: transform 0.2s ease, color 0.2s ease;
}

.action-card:hover .action-card__arrow {
  transform: translateX(2px);
  color: var(--bp-color-primary);
}

@media (max-width: 720px) {
  .hero__content {
    grid-template-columns: 1fr;
  }
  .hero__stats {
    flex-wrap: wrap;
  }
}
</style>

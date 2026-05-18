<script setup>
import { Sunny, Moon } from "@element-plus/icons-vue";
import { useTheme } from "../composables/useTheme.js";

const { isDark, toggle } = useTheme();
</script>

<template>
  <div class="auth-shell">
    <div class="auth-shell__decor">
      <span class="blob blob--a"></span>
      <span class="blob blob--b"></span>
      <span class="blob blob--c"></span>
    </div>

    <header class="auth-shell__top">
      <div class="auth-brand">
        <span class="auth-brand__dot" />
        <strong>Blog Platform</strong>
      </div>
      <button class="auth-theme-toggle" @click="toggle">
        <el-icon>
          <component :is="isDark ? Sunny : Moon" />
        </el-icon>
      </button>
    </header>

    <main class="auth-shell__body">
      <router-view />
    </main>

    <footer class="auth-shell__footer">
      <span>© {{ new Date().getFullYear() }} Blog Platform · Crafted with Vue 3</span>
    </footer>
  </div>
</template>

<style scoped>
.auth-shell {
  position: relative;
  min-height: 100vh;
  width: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bp-color-bg);
  overflow: hidden;
}

.auth-shell__decor {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  filter: blur(60px);
  opacity: 0.7;
}

.blob {
  position: absolute;
  border-radius: 999px;
}

.blob--a {
  width: 480px;
  height: 480px;
  left: -120px;
  top: -100px;
  background: radial-gradient(
    circle,
    rgba(99, 102, 241, 0.55),
    rgba(99, 102, 241, 0)
  );
}

.blob--b {
  width: 460px;
  height: 460px;
  right: -120px;
  top: 30%;
  background: radial-gradient(
    circle,
    rgba(236, 72, 153, 0.4),
    rgba(236, 72, 153, 0)
  );
}

.blob--c {
  width: 380px;
  height: 380px;
  left: 30%;
  bottom: -160px;
  background: radial-gradient(
    circle,
    rgba(14, 165, 233, 0.4),
    rgba(14, 165, 233, 0)
  );
}

.auth-shell__top {
  position: relative;
  z-index: 1;
  padding: 24px clamp(16px, 4vw, 48px);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  letter-spacing: 0.02em;
  color: var(--bp-color-text-primary);
}

.auth-brand__dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: var(--bp-gradient-hero);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}

.auth-theme-toggle {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  border: 1px solid var(--bp-color-border);
  background: var(--bp-color-bg-elevated);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--bp-color-text-secondary);
  transition: background 0.2s ease, color 0.2s ease;
}

.auth-theme-toggle:hover {
  background: var(--bp-color-primary-soft);
  color: var(--bp-color-primary);
}

.auth-shell__body {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(16px, 4vw, 48px);
}

.auth-shell__footer {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 16px;
  color: var(--bp-color-text-tertiary);
  font-size: 12px;
}
</style>

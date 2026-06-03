<template>
  <div class="auth-layout">
    <div class="auth-bg">
      <div class="auth-bg-pattern"></div>
    </div>
    <div class="auth-container">
      <div class="auth-brand">
        <router-link to="/home" class="brand-logo">
          <svg width="36" height="36" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="8" fill="var(--c-primary)"/>
            <path d="M7 9h14M7 14h10M7 19h12" stroke="white" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <span>Blog Platform</span>
        </router-link>
      </div>
      <div class="auth-card card">
        <router-view />
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-layout {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: var(--c-bg);
  position: relative;
  overflow: auto;
  overscroll-behavior: contain;
}

.auth-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.auth-bg-pattern {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 30%, rgba(79, 70, 229, 0.08) 0%, transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(79, 70, 229, 0.06) 0%, transparent 50%);
}

.auth-container {
  width: 100%;
  max-width: 440px;
  position: relative;
  z-index: 1;
}

.auth-brand {
  text-align: center;
  margin-bottom: 24px;
}

.brand-logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 700;
  color: var(--c-text);
  text-decoration: none;
}

.auth-card {
  padding: 40px;
}

@media (max-width: 480px) {
  .auth-card { padding: 28px 24px; }
}
</style>

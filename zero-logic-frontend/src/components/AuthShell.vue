<template>
  <div class="auth-shell">
    <RouterLink to="/" class="brand-link" aria-label="返回首页">
      <img class="brand-logo" src="@/assets/ZeroLogic_logo.png" alt="ZeroLogic" />
      <span>ZeroLogic</span>
    </RouterLink>

    <section class="brand-panel">
      <div class="brand-copy">
        <h1>ZeroLogic</h1>
        <p class="slogan">定义下一代智能应用构建方式</p>
      </div>
    </section>

    <section class="auth-card" :class="`auth-card-${variant}`">
      <div class="auth-card-header">
        <h2>{{ title }}</h2>
        <p>{{ subtitle }}</p>
      </div>
      <slot />
    </section>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    subtitle: string
    variant?: 'login' | 'register'
  }>(),
  {
    variant: 'login',
  },
)
</script>

<style scoped>
.auth-shell {
  position: relative;
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(360px, 0.95fr);
  align-items: center;
  gap: 64px;
  padding: 72px clamp(28px, 6vw, 96px);
  overflow: auto;
  background:
    linear-gradient(135deg, rgba(248, 250, 252, 0.96) 0%, rgba(239, 246, 255, 0.84) 48%, rgba(236, 253, 245, 0.78) 100%),
    linear-gradient(rgba(15, 23, 42, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(15, 23, 42, 0.035) 1px, transparent 1px);
  background-size:
    auto,
    36px 36px,
    36px 36px;
}

.auth-shell::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(120deg, rgba(22, 119, 255, 0.12), transparent 26%),
    linear-gradient(300deg, rgba(16, 185, 129, 0.1), transparent 24%);
  pointer-events: none;
}

.brand-link {
  position: absolute;
  top: 24px;
  left: clamp(24px, 5vw, 64px);
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 16px;
  color: #1677ff;
  font-size: 18px;
  font-weight: 650;
  background: rgba(255, 255, 255, 0.68);
  border: 1px solid rgba(148, 163, 184, 0.24);
  backdrop-filter: blur(14px);
}

.brand-logo {
  width: 38px;
  height: 38px;
}

.brand-panel,
.auth-card {
  position: relative;
  z-index: 1;
}

.brand-copy {
  max-width: 680px;
}

.brand-copy h1 {
  margin: 0;
  color: #0b1220;
  font-size: clamp(68px, 9vw, 132px);
  line-height: 0.92;
  font-weight: 850;
  letter-spacing: 0;
  text-shadow: 0 18px 48px rgba(22, 119, 255, 0.14);
}

.slogan {
  margin: 24px 0 0;
  color: #334155;
  font-size: clamp(20px, 2.2vw, 28px);
  line-height: 1.5;
  font-weight: 650;
}

.auth-card {
  width: min(440px, 100%);
  justify-self: center;
  padding: 34px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(148, 163, 184, 0.24);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.14);
  backdrop-filter: blur(18px);
}

.auth-card-header {
  text-align: center;
  margin-bottom: 26px;
}

.auth-card h2 {
  margin: 0;
  color: #0f172a;
  font-size: 28px;
  font-weight: 760;
}

.auth-card-header p:last-child {
  margin: 10px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.auth-card :deep(.ant-form-item) {
  margin-bottom: 18px;
}

.auth-card :deep(.ant-input-affix-wrapper),
.auth-card :deep(.ant-input) {
  min-height: 44px;
  border-radius: 12px;
}

.auth-card :deep(.ant-input-affix-wrapper) {
  border-color: rgba(148, 163, 184, 0.36);
  box-shadow: none;
}

.auth-card :deep(.ant-input-affix-wrapper:hover),
.auth-card :deep(.ant-input-affix-wrapper-focused) {
  border-color: rgba(22, 119, 255, 0.55);
  box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.08);
}

.auth-card :deep(.ant-btn-primary) {
  height: 46px;
  border-radius: 12px;
  font-weight: 700;
  box-shadow: 0 12px 24px rgba(22, 119, 255, 0.22);
}

@media (max-width: 980px) {
  .auth-shell {
    grid-template-columns: 1fr;
    gap: 28px;
    padding: 92px 24px 32px;
  }

  .brand-panel {
    max-width: 720px;
  }

  .brand-copy {
    max-width: 100%;
  }

  .auth-card {
    justify-self: stretch;
    width: 100%;
  }
}

@media (max-width: 560px) {
  .auth-shell {
    padding: 86px 16px 24px;
  }

  .brand-link {
    left: 16px;
    top: 16px;
  }

  .brand-copy h1 {
    font-size: 54px;
  }

  .auth-card {
    padding: 24px;
    border-radius: 20px;
  }
}
</style>

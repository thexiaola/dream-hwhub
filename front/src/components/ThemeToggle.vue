<template>
  <button
    class="theme-toggle"
    :title="isDark ? '切换到亮色模式' : '切换到暗色模式'"
    :aria-label="isDark ? '切换到亮色模式' : '切换到暗色模式'"
    @click="toggle"
  >
    <span class="theme-toggle-icon" :class="{ 'is-dark': isDark }">
      <Sun class="icon icon-sun" :size="18" aria-hidden="true" />
      <Moon class="icon icon-moon" :size="18" aria-hidden="true" />
    </span>
  </button>
</template>

<script setup lang="ts">
import { Sun, Moon } from '@lucide/vue'
import { useTheme } from '@/composables/useTheme'

const { isDark, toggle } = useTheme()
</script>

<style scoped>
.theme-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  margin-right: 8px;
  background: transparent;
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 8px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
  cursor: pointer;
  transition: background 0.3s, color 0.3s, border-color 0.3s;
}

.theme-toggle:hover {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.05);
  color: var(--fg);
}

/* 太阳与月亮叠放，主题切换时一方旋转放大展开、另一方旋转缩小收起 */
.theme-toggle-icon {
  position: relative;
  display: block;
  width: 18px;
  height: 18px;
}

.theme-toggle-icon :deep(.icon) {
  position: absolute;
  inset: 0;
  transition: opacity 0.32s ease, transform 0.32s cubic-bezier(0.4, 0, 0.2, 1);
}

.theme-toggle-icon :deep(.icon-moon) {
  opacity: 1;
  transform: rotate(0deg) scale(1);
}

.theme-toggle-icon :deep(.icon-sun) {
  opacity: 0;
  transform: rotate(-90deg) scale(0.4);
}

.theme-toggle-icon.is-dark :deep(.icon-moon) {
  opacity: 0;
  transform: rotate(90deg) scale(0.4);
}

.theme-toggle-icon.is-dark :deep(.icon-sun) {
  opacity: 1;
  transform: rotate(0deg) scale(1);
}

/* 系统要求减少动画时直接互换图标 */
@media (prefers-reduced-motion: reduce) {
  .theme-toggle-icon :deep(.icon) {
    transition: none;
  }
}
</style>

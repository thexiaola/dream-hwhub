<template>
  <button
    class="theme-toggle"
    :title="isDark ? '切换到亮色模式' : '切换到暗色模式'"
    :aria-label="isDark ? '切换到亮色模式' : '切换到暗色模式'"
    @click="toggle"
  >
    <Sun v-if="isDark" :size="18" />
    <Moon v-else :size="18" />
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
  border: 1px solid rgba(var(--rgb-fg), 0.1);
  border-radius: 8px;
  color: rgba(var(--rgb-fg), 0.7);
  cursor: pointer;
  transition: background 0.3s, color 0.3s, border-color 0.3s;
}

.theme-toggle:hover {
  background: rgba(var(--rgb-fg), 0.05);
  color: var(--fg);
}
</style>

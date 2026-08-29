import { ref, computed, watch } from 'vue'

export type Theme = 'light' | 'dark'

const STORAGE_KEY = 'theme'
const TRANSITION_MS = 320

const initial: Theme =
  typeof localStorage !== 'undefined' && localStorage.getItem(STORAGE_KEY) === 'dark'
    ? 'dark'
    : 'light'

const theme = ref<Theme>(initial)
const isDark = computed(() => theme.value === 'dark')

function applyToDom(value: Theme, withTransition: boolean): void {
  if (typeof document === 'undefined') return
  const root = document.documentElement
  if (withTransition) {
    root.classList.add('theme-transitioning')
    root.dataset.theme = value
    window.setTimeout(() => root.classList.remove('theme-transitioning'), TRANSITION_MS)
  } else {
    root.dataset.theme = value
  }
}

// 模块加载即同步写入 data-theme，在应用挂载前生效，避免首屏闪烁
applyToDom(theme.value, false)

watch(theme, (value) => {
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem(STORAGE_KEY, value)
  }
  applyToDom(value, true)
})

function toggle(): void {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
}

export function useTheme() {
  return { theme, isDark, toggle }
}

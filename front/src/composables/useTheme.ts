import { ref, computed, watch } from 'vue'

export type Theme = 'light' | 'dark'

const STORAGE_KEY = 'theme'
/* 回退路径移除 .theme-transitioning 的延时，略长于 style.css 中该类的 0.2s 过渡 */
const TRANSITION_MS = 220
const REDUCED_MOTION_QUERY = '(prefers-reduced-motion: reduce)'

type ViewTransitionHandle = {
  finished: Promise<void>
  skipTransition?: () => void
}
type ViewTransitionDocument = Document & {
  startViewTransition?: (update: () => void) => ViewTransitionHandle
}

const initial: Theme =
  typeof localStorage !== 'undefined' && localStorage.getItem(STORAGE_KEY) === 'dark'
    ? 'dark'
    : 'light'

const theme = ref<Theme>(initial)
const isDark = computed(() => theme.value === 'dark')

/* 进行中的整页过渡；新的切换会先结束它，避免动画叠加 */
let activeViewTransition: ViewTransitionHandle | null = null
/* 回退路径中移除 .theme-transitioning 的定时器，连续切换时重新计时 */
let fallbackTimer: number | null = null

/** 系统是否要求减少动画 */
function prefersReducedMotion(): boolean {
  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia(REDUCED_MOTION_QUERY).matches
  )
}

/**
 * 启动一次整页主题过渡；环境不支持或启动失败时返回 null，由调用方走回退路径。
 * update 在过渡开始后执行，其中读取 theme.value 以确保写入的是最新主题。
 */
function beginViewTransition(update: () => void): ViewTransitionHandle | null {
  const startViewTransition = (document as ViewTransitionDocument).startViewTransition
  if (typeof startViewTransition !== 'function') return null
  try {
    activeViewTransition?.skipTransition?.()
    const transition = startViewTransition.call(document, update)
    if (!transition || typeof transition.finished?.then !== 'function') return null
    return transition
  } catch {
    return null
  }
}

function applyToDom(value: Theme, withTransition: boolean): void {
  if (typeof document === 'undefined') return
  const root = document.documentElement

  if (!withTransition || prefersReducedMotion()) {
    root.dataset.theme = value
    return
  }

  /* 整页过渡：新主题整体淡入，只有一次样式重算，动画在合成层完成 */
  const transition = beginViewTransition(() => {
    root.dataset.theme = theme.value
  })
  if (transition) {
    activeViewTransition = transition
    transition.finished
      .catch(() => {})
      .then(() => {
        if (activeViewTransition === transition) activeViewTransition = null
        /* 过渡结束仍未写入主题时兜底，避免 DOM 与 theme 状态分歧 */
        if (root.dataset.theme !== theme.value) root.dataset.theme = theme.value
      })
    return
  }

  /* 无 View Transitions 支持的浏览器：切换瞬间对全站启用颜色过渡 */
  root.classList.add('theme-transitioning')
  root.dataset.theme = value
  if (fallbackTimer !== null) window.clearTimeout(fallbackTimer)
  fallbackTimer = window.setTimeout(() => {
    fallbackTimer = null
    root.classList.remove('theme-transitioning')
  }, TRANSITION_MS)
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

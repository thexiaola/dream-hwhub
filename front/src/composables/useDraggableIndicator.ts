import { computed, onUnmounted, ref, watch, type Ref } from 'vue'

/** tab 相对定位容器的几何信息 */
export interface TabGeometry {
  key: string
  left: number
  top: number
  width: number
  height: number
}

/** 指示器方向：x-水平（顶部导航、分段控件），y-竖直（侧边栏导航），
    both-自由二维（换行成多行的页签，可上下左右拖动） */
export type IndicatorAxis = 'x' | 'y' | 'both'

export interface UseDraggableIndicatorOptions {
  /** 定位容器：指示器与 tab 坐标都相对它计算，也是命中测试的坐标系 */
  container: Ref<HTMLElement | null>
  /** 当前可见的 tab（key + 元素），按视觉顺序返回 */
  getTabs: () => { key: string; el: HTMLElement }[]
  /** 当前激活项 key；未拖拽时指示器吸附到它 */
  activeKey: () => string
  /** 主轴方向，默认 'x'；可传函数以按视口等运行时条件解析 */
  axis?: IndicatorAxis | (() => IndicatorAxis)
  /** 拖拽中滑块中心进入某个 tab 时实时回调（用于实时切换激活项） */
  onCross?: (key: string) => void
  /** 松开鼠标并吸附到最近 tab 时回调 */
  onSettle?: (key: string) => void
}

/** 按下后位移超过该像素才算拖拽，用于区分点击与拖动 */
const DRAG_THRESHOLD = 3
/** 拖拽结束后短暂屏蔽 click，避免拖动同时触发原 tab 的点击 */
const CLICK_SUPPRESS_MS = 250

const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), max)

/**
 * 可拖拽的激活指示器：按住高亮块拖动使其跟随指针，松开后吸附到最近 tab。
 * 文字保持不动，只有指示器位移；由使用方根据 position 渲染指示器。
 * 通过 axis 支持水平/竖直/自由二维三种排布。
 */
export function useDraggableIndicator(options: UseDraggableIndicatorOptions) {
  // 每次读取而非缓存：拖拽时按当前视口解析方向（如单行用 x、换行多行用 both）
  const getMode = (): IndicatorAxis =>
    typeof options.axis === 'function' ? options.axis() : options.axis ?? 'x'

  const dragging = ref(false)
  // 静止位置（跟随 activeKey）与拖拽中的位置分开存放，避免拖拽被 activeKey 变化打断
  const resting = ref<TabGeometry | null>(null)
  const dragGeo = ref<TabGeometry | null>(null)

  const position = computed(() => (dragging.value ? dragGeo.value : resting.value))
  const visible = computed(() => !!position.value)

  // 主轴上的起点与尺寸（y 取 top/height，x 与 both 取 left/width）
  const mainStart = (g: TabGeometry) => (getMode() === 'y' ? g.top : g.left)
  const mainSize = (g: TabGeometry) => (getMode() === 'y' ? g.height : g.width)

  // 容器当前的「布局 → 视觉」缩放比：页面进出场动画含 scale，getBoundingClientRect
  // 会返回缩放后的值，而 CSS 的 px 是布局值，故需按 offsetWidth 换算回布局坐标系
  const layoutScale = (container: HTMLElement): number => {
    const visualWidth = container.getBoundingClientRect().width
    const layoutWidth = container.offsetWidth
    return layoutWidth > 0 ? visualWidth / layoutWidth : 1
  }

  // 统一换算到容器坐标系，兼容 tab 的定位祖先各不相同的情况
  const measureTabs = (): TabGeometry[] => {
    const container = options.container.value
    if (!container) return []
    const base = container.getBoundingClientRect()
    const scale = layoutScale(container)
    return options.getTabs().map(({ key, el }) => {
      const rect = el.getBoundingClientRect()
      return {
        key,
        left: (rect.left - base.left) / scale,
        top: (rect.top - base.top) / scale,
        width: rect.width / scale,
        height: rect.height / scale
      }
    })
  }

  /** 让指示器吸附回当前激活项；拖拽进行中不改动位置 */
  const syncToActive = () => {
    if (dragging.value) return
    resting.value = measureTabs().find(t => t.key === options.activeKey()) ?? null
  }

  let pressed = false
  let moved = false
  let startX = 0
  let startY = 0
  // 指针相对滑块起点的抓取偏移：拖动时保持该相对位置
  let grabOffsetX = 0
  let grabOffsetY = 0
  let crossedKey: string | null = null
  let suppressClick = false
  let suppressTimer: number | null = null

  /** 单轴：按主轴中心距离取最近 tab */
  const nearestTabSingle = (pointerMain: number, tabs: TabGeometry[]) => {
    let best = tabs[0]
    let bestDist = Infinity
    for (const tab of tabs) {
      const dist = Math.abs(mainStart(tab) + mainSize(tab) / 2 - pointerMain)
      if (dist < bestDist) {
        bestDist = dist
        best = tab
      }
    }
    return best
  }

  /** 二维：按中心欧氏距离取最近 tab */
  const nearestTabBoth = (px: number, py: number, tabs: TabGeometry[]) => {
    let best = tabs[0]
    let bestDist = Infinity
    for (const tab of tabs) {
      const dist = Math.hypot(tab.left + tab.width / 2 - px, tab.top + tab.height / 2 - py)
      if (dist < bestDist) {
        bestDist = dist
        best = tab
      }
    }
    return best
  }

  /** 指针所在的 tab；不在任何 tab 内时取中心最近者 */
  const tabAtPointer = (px: number, py: number, tabs: TabGeometry[]) => {
    if (getMode() === 'both') {
      return tabs.find(
        t => px >= t.left && px <= t.left + t.width && py >= t.top && py <= t.top + t.height
      ) ?? nearestTabBoth(px, py, tabs)
    }
    const pointerMain = getMode() === 'y' ? py : px
    return tabs.find(t => pointerMain >= mainStart(t) && pointerMain <= mainStart(t) + mainSize(t))
      ?? nearestTabSingle(pointerMain, tabs)
  }

  const onMove = (event: PointerEvent) => {
    if (!pressed) return
    const dx = event.clientX - startX
    const dy = event.clientY - startY
    if (!moved && Math.hypot(dx, dy) < DRAG_THRESHOLD) return
    moved = true
    dragging.value = true

    const container = options.container.value
    const tabs = measureTabs()
    if (!container || tabs.length === 0) return

    // 指针位置换算到容器坐标系
    const base = container.getBoundingClientRect()
    const scale = layoutScale(container)
    const pointerX = (event.clientX - base.left) / scale
    const pointerY = (event.clientY - base.top) / scale

    // 尺寸与位置都随指针所在 tab 变化，而非固定大小
    const target = tabAtPointer(pointerX, pointerY, tabs)

    if (getMode() === 'both') {
      // 自由二维（换行成多行的页签）：
      // 纵向夹在首行与末行之间——上方/下方没有页签就拖不过去；
      // 横向只夹在「指针当前所在行」的范围内，避免拖进某行末尾没有页签的空单元格。
      const rowTabs = tabs.filter(t => Math.abs(t.top - target.top) < 1)
      const rowMinLeft = Math.min(...rowTabs.map(t => t.left))
      const rowMaxLeft = Math.max(...rowTabs.map(t => t.left + t.width)) - target.width
      const minTop = Math.min(...tabs.map(t => t.top))
      const maxTop = Math.max(...tabs.map(t => t.top + t.height)) - target.height
      dragGeo.value = {
        key: target.key,
        left: clamp(pointerX - grabOffsetX, rowMinLeft, Math.max(rowMinLeft, rowMaxLeft)),
        top: clamp(pointerY - grabOffsetY, minTop, Math.max(minTop, maxTop)),
        width: target.width,
        height: target.height
      }
    } else {
      const pointerMain = getMode() === 'y' ? pointerY : pointerX
      const grabOffsetMain = getMode() === 'y' ? grabOffsetY : grabOffsetX
      const targetSize = mainSize(target)
      const first = tabs[0]
      const last = tabs[tabs.length - 1]
      const minMain = mainStart(first)
      const maxMain = Math.max(minMain, mainStart(last) + mainSize(last) - targetSize)
      const mainPos = clamp(pointerMain - grabOffsetMain, minMain, maxMain)
      dragGeo.value = getMode() === 'y'
        ? { key: target.key, left: target.left, top: mainPos, width: target.width, height: targetSize }
        : { key: target.key, left: mainPos, top: target.top, width: targetSize, height: target.height }
    }

    // 实时切换：指针进入某个 tab 时切换过去
    if (options.onCross && target.key !== crossedKey) {
      crossedKey = target.key
      options.onCross(target.key)
    }
  }

  const stopListeners = () => {
    window.removeEventListener('pointermove', onMove)
    window.removeEventListener('pointerup', onUp)
    window.removeEventListener('pointercancel', onCancel)
    window.removeEventListener('keydown', onKey)
  }

  const release = (settle: boolean) => {
    if (!pressed) return
    const wasDragging = dragging.value && moved
    const geo = dragGeo.value
    pressed = false
    stopListeners()

    if (wasDragging) {
      // 拖拽结束紧跟着会派发一次 click，需屏蔽以免又切回原 tab
      suppressClick = true
      if (suppressTimer !== null) window.clearTimeout(suppressTimer)
      suppressTimer = window.setTimeout(() => {
        suppressTimer = null
        suppressClick = false
      }, CLICK_SUPPRESS_MS)
    }

    if (wasDragging && settle && geo) {
      const tabs = measureTabs()
      if (tabs.length > 0) {
        const key = getMode() === 'both'
          ? nearestTabBoth(geo.left + geo.width / 2, geo.top + geo.height / 2, tabs).key
          : nearestTabSingle(mainStart(geo) + mainSize(geo) / 2, tabs).key
        options.onSettle?.(key)
      }
    }

    dragging.value = false
    dragGeo.value = null
    crossedKey = null
    syncToActive()
  }

  const onUp = () => release(true)
  const onCancel = () => release(false)
  const onKey = (event: KeyboardEvent) => {
    if (event.key === 'Escape') release(false)
  }

  /** 在容器的 pointerdown 上调用；只有按在指示器范围内才启动拖拽 */
  const startDrag = (event: PointerEvent) => {
    if (event.button !== 0 || pressed) return
    const el = options.container.value
    const geo = resting.value
    if (!el || !geo) return
    const base = el.getBoundingClientRect()
    const pointerX = event.clientX - base.left
    const pointerY = event.clientY - base.top
    const insideIndicator =
      pointerX >= geo.left &&
      pointerX <= geo.left + geo.width &&
      pointerY >= geo.top &&
      pointerY <= geo.top + geo.height
    if (!insideIndicator) return

    pressed = true
    moved = false
    startX = event.clientX
    startY = event.clientY
    // 记录抓取偏移：拖动时保持指针落在滑块的同一相对位置
    grabOffsetX = pointerX - geo.left
    grabOffsetY = pointerY - geo.top
    crossedKey = options.activeKey()
    window.addEventListener('pointermove', onMove)
    window.addEventListener('pointerup', onUp)
    window.addEventListener('pointercancel', onCancel)
    window.addEventListener('keydown', onKey)
  }

  /** 拖拽后紧跟的 click 需要吞掉，绑定到容器的 click 捕获阶段 */
  const onClickCapture = (event: MouseEvent) => {
    if (!suppressClick) return
    suppressClick = false
    if (suppressTimer !== null) {
      window.clearTimeout(suppressTimer)
      suppressTimer = null
    }
    event.stopPropagation()
    event.preventDefault()
  }

  // 拖拽期间禁止选中文字并显示抓取光标
  watch(dragging, value => {
    document.body.classList.toggle('indicator-dragging', value)
  })

  onUnmounted(() => {
    if (pressed) {
      pressed = false
      stopListeners()
      dragging.value = false
    }
    if (suppressTimer !== null) {
      window.clearTimeout(suppressTimer)
      suppressTimer = null
    }
    document.body.classList.remove('indicator-dragging')
  })

  return { dragging, position, visible, syncToActive, startDrag, onClickCapture }
}

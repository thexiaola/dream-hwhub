import { computed, onUnmounted, ref, watch, type Ref } from 'vue'

/** tab 相对定位容器的几何信息 */
export interface TabGeometry {
  key: string
  left: number
  top: number
  width: number
  height: number
}

export interface UseDraggableIndicatorOptions {
  /** 定位容器：指示器与 tab 坐标都相对它计算，也是命中测试的坐标系 */
  container: Ref<HTMLElement | null>
  /** 当前可见的 tab（key + 元素），按视觉顺序返回 */
  getTabs: () => { key: string; el: HTMLElement }[]
  /** 当前激活项 key；未拖拽时指示器吸附到它 */
  activeKey: () => string
  /** 拖拽中滑块中心进入某个 tab 时实时回调（用于实时切换激活项） */
  onCross?: (key: string) => void
  /** 松开鼠标并吸附到最近 tab 时回调 */
  onSettle?: (key: string) => void
}

/** 按下后位移超过该像素才算拖拽，用于区分点击与拖动 */
const DRAG_THRESHOLD = 3
/** 拖拽结束后短暂屏蔽 click，避免拖动同时触发原 tab 的点击 */
const CLICK_SUPPRESS_MS = 250

/**
 * 可拖拽的激活指示器：按住高亮块拖动使其跟随指针，松开后吸附到最近 tab。
 * 文字保持不动，只有指示器位移；由使用方根据 position 渲染指示器。
 */
export function useDraggableIndicator(options: UseDraggableIndicatorOptions) {
  const dragging = ref(false)
  // 静止位置（跟随 activeKey）与拖拽中的位置分开存放，避免拖拽被 activeKey 变化打断
  const resting = ref<TabGeometry | null>(null)
  const dragGeo = ref<TabGeometry | null>(null)

  const position = computed(() => (dragging.value ? dragGeo.value : resting.value))
  const visible = computed(() => !!position.value)

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
  // 指针在滑块内的抓取偏移：拖动时保持指针落在滑块的同一相对位置
  let grabOffset = 0
  let crossedKey: string | null = null
  let suppressClick = false
  let suppressTimer: number | null = null

  const nearestTab = (center: number, tabs: TabGeometry[]) => {
    let best = tabs[0]
    let bestDist = Infinity
    for (const tab of tabs) {
      const dist = Math.abs(tab.left + tab.width / 2 - center)
      if (dist < bestDist) {
        bestDist = dist
        best = tab
      }
    }
    return best
  }

  /** 指针所在的 tab；不在任何 tab 内时取中心最近者 */
  const tabAtPointer = (pointerX: number, tabs: TabGeometry[]) =>
    tabs.find(t => pointerX >= t.left && pointerX <= t.left + t.width) ?? nearestTab(pointerX, tabs)

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

    // 指针位置换算到容器坐标系，滑块跟随指针（保持抓取偏移）
    const base = container.getBoundingClientRect()
    const pointerX = event.clientX - base.left

    // 尺寸随指针所在 tab 动态变化，而非固定大小
    const target = tabAtPointer(pointerX, tabs)
    const width = target.width
    const first = tabs[0]
    const last = tabs[tabs.length - 1]
    const minLeft = first.left
    const maxLeft = Math.max(minLeft, last.left + last.width - width)
    const left = Math.min(Math.max(pointerX - grabOffset, minLeft), maxLeft)

    dragGeo.value = {
      key: target.key,
      left,
      top: target.top,
      width,
      height: target.height
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
        options.onSettle?.(nearestTab(geo.left + geo.width / 2, tabs).key)
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
    // 记录抓取偏移：指针相对滑块左边缘的位置，拖动时保持该相对位置
    grabOffset = pointerX - geo.left
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

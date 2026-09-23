<template>
  <div
    ref="trackRef"
    class="slide-segmented"
    role="radiogroup"
    :aria-label="ariaLabel"
    @keydown.left.prevent="moveSelection(-1)"
    @keydown.right.prevent="moveSelection(1)"
    @pointerdown="startDrag"
    @click.capture="onClickCapture"
  >
    <!-- 选中项背景块：随选项滑动，也可按住拖拽，松开吸附到最近的选项 -->
    <span
      v-show="indicatorVisible"
      class="slide-segmented__indicator"
      :class="{ 'is-ready': ready, 'is-dragging': indicatorDragging }"
      :style="indicatorStyle"
    />
    <button
      v-for="option in options"
      :key="String(option.value)"
      type="button"
      class="slide-segmented__item"
      :class="{ 'is-active': isActive(option), 'is-disabled': option.disabled }"
      role="radio"
      :aria-checked="isActive(option)"
      :disabled="option.disabled"
      :tabindex="isActive(option) ? 0 : -1"
      @click="select(option)"
    >
      {{ option.label }}
    </button>
  </div>
</template>

<script setup lang="ts" generic="T extends string | number | boolean">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useDraggableIndicator } from '@/composables/useDraggableIndicator'

const props = withDefaults(
  defineProps<{
    modelValue: T
    options: { label: string; value: T; disabled?: boolean }[]
    ariaLabel?: string
  }>(),
  { ariaLabel: undefined }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: T): void
  (e: 'change', value: T): void
}>()

const trackRef = ref<HTMLElement | null>(null)
// 首次可见落位后开启过渡，避免初始/恢复显示时从错误位置滑入
const ready = ref(false)
let readyFrame: number | null = null
let resizeObserver: ResizeObserver | null = null

const isActive = (option: { value: T }) => option.value === props.modelValue

// 直接按索引取按钮节点，避免模板内联 ref 回调在每次渲染时重建
const getItemElement = (index: number): HTMLElement | undefined =>
  trackRef.value?.querySelectorAll<HTMLElement>('.slide-segmented__item')?.[index]

const activeIndex = computed(() => props.options.findIndex(isActive))

// 拖拽命中用的选项集合（key 与 options 索引一致）
const getTabs = () => {
  const result: { key: string; el: HTMLElement }[] = []
  props.options.forEach((_, index) => {
    const el = getItemElement(index)
    if (el) result.push({ key: String(index), el })
  })
  return result
}

// 按索引切换选项（拖拽经过/松手、键盘移动共用）
const selectByIndex = (index: number) => {
  const option = props.options[index]
  if (!option || option.disabled || option.value === props.modelValue) return
  emit('update:modelValue', option.value)
  emit('change', option.value)
}

const select = (option: { value: T; disabled?: boolean }) => {
  if (option.disabled || option.value === props.modelValue) return
  emit('update:modelValue', option.value)
  emit('change', option.value)
}

const {
  dragging: indicatorDragging,
  position: indicatorPosition,
  visible: indicatorPositioned,
  syncToActive,
  startDrag,
  onClickCapture
} = useDraggableIndicator({
  container: trackRef,
  getTabs,
  activeKey: () => String(activeIndex.value),
  // 拖拽经过选项即实时切换
  onCross: key => selectByIndex(Number(key)),
  // 松开吸附到最近的选项
  onSettle: key => selectByIndex(Number(key))
})

const indicatorVisible = computed(() => indicatorPositioned.value)

const indicatorStyle = computed(() => {
  const pos = indicatorPosition.value
  if (!pos) return {}
  return {
    transform: `translate(${pos.left}px, ${pos.top}px)`,
    width: `${pos.width}px`,
    height: `${pos.height}px`
  }
})

// 延后一帧再开启过渡，确保首次落位当帧不参与动画
const scheduleReady = () => {
  if (readyFrame !== null) cancelAnimationFrame(readyFrame)
  readyFrame = requestAnimationFrame(() => {
    readyFrame = null
    ready.value = true
  })
}

const refresh = () => {
  // 容器不可见（如未激活的 el-tab-pane）时尺寸为 0，此时候量不到有效位置，
  // 关闭过渡、等可见后再无动画落位
  if (!trackRef.value?.offsetWidth) {
    ready.value = false
    return
  }
  syncToActive()
  if (!ready.value) scheduleReady()
}

// 切换选项、选项增减或容器尺寸变化后重新定位
watch(() => props.modelValue, () => nextTick(refresh))
watch(() => props.options, () => nextTick(refresh), { deep: true })

// 键盘左右方向键在可用选项之间循环移动焦点
const moveSelection = (step: number) => {
  const usable = props.options.filter(option => !option.disabled)
  if (usable.length === 0) {
    return
  }
  const current = usable.findIndex(isActive)
  const nextIndex = current < 0 ? 0 : (current + step + usable.length) % usable.length
  const targetIndex = props.options.indexOf(usable[nextIndex])
  selectByIndex(targetIndex)
  nextTick(() => getItemElement(targetIndex)?.focus())
}

onMounted(async () => {
  await nextTick()
  refresh()

  resizeObserver = new ResizeObserver(() => nextTick(refresh))
  if (trackRef.value) {
    resizeObserver.observe(trackRef.value)
  }
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
  if (readyFrame !== null) {
    cancelAnimationFrame(readyFrame)
    readyFrame = null
  }
})
</script>

<style scoped>
/* 分段控件：外层凹槽轨道 + 内层贴身按钮 + 可拖拽的滑动激活块 */
.slide-segmented {
  position: relative;
  display: inline-flex;
  align-items: stretch;
  flex: 0 0 auto;
  gap: 2px;
  padding: 3px;
  border-radius: 11px;
  background: var(--segment-track);
}

.slide-segmented__indicator {
  position: absolute;
  top: 0;
  left: 0;
  box-sizing: border-box;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  box-shadow: 0 2px 8px -2px rgba(102, 126, 234, 0.65);
  pointer-events: none;
}

/* 仅首次落位后启用过渡，避免初始定位时播放入场滑动；拖拽中由下方规则接管 */
.slide-segmented__indicator.is-ready:not(.is-dragging) {
  transition:
    transform 0.38s cubic-bezier(0.22, 0.61, 0.36, 1),
    width 0.38s cubic-bezier(0.22, 0.61, 0.36, 1);
}

/* 拖拽中：位移紧跟指针（transform 不设过渡），仅尺寸平滑变化 */
.slide-segmented__indicator.is-dragging {
  transition:
    width 0.18s ease,
    height 0.18s ease;
}

.slide-segmented__item {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 7px 16px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.62);
  font-family: inherit;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
  white-space: nowrap;
  cursor: pointer;
  transition: color 0.18s ease;
}

.slide-segmented__item:not(.is-active):not(:disabled):hover {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.06);
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.9);
}

.slide-segmented__item.is-active {
  color: var(--fg-on-accent);
  font-weight: 600;
}

/* 键盘聚焦时保留可见的焦点指示 */
.slide-segmented__item:focus-visible {
  outline: none;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.55);
}

.slide-segmented__item:disabled {
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.25);
  cursor: not-allowed;
}

@media (prefers-reduced-motion: reduce) {
  .slide-segmented__indicator,
  .slide-segmented__indicator.is-ready,
  .slide-segmented__indicator.is-dragging {
    transition: none;
  }
}

@media (max-width: 768px) {
  .slide-segmented__item {
    padding: 6px 12px;
    font-size: 12px;
  }
}
</style>

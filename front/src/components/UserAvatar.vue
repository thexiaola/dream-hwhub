<template>
  <span
    class="user-avatar"
    :class="{ 'is-clickable': clickable }"
    :style="boxStyle"
    @click="clickable && $emit('click')"
  >
    <img v-if="objectUrl" class="user-avatar__img" :src="objectUrl" :alt="alt" />
    <User v-else class="user-avatar__fallback" :size="fallbackSize" aria-hidden="true" />
  </span>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { User } from '@lucide/vue'
import { loadAvatarBlob } from '@/utils/attachment'

const props = withDefaults(
  defineProps<{
    /** 头像文件相对路径，空则显示默认头像 */
    avatar?: string | null
    /** 头像边长（像素） */
    size?: number
    /** 是否可点击（用于触发上传等操作） */
    clickable?: boolean
    alt?: string
  }>(),
  { avatar: null, size: 64, clickable: false, alt: '用户头像' }
)

defineEmits<{ (e: 'click'): void }>()

const objectUrl = ref<string | null>(null)
let ownedUrl: string | null = null

const release = () => {
  if (ownedUrl) {
    URL.revokeObjectURL(ownedUrl)
    ownedUrl = null
  }
  objectUrl.value = null
}

const load = async (path?: string | null) => {
  release()
  if (!path) return
  const blob = await loadAvatarBlob(path)
  // 异步返回时路径可能已变化，丢弃过期结果
  if (!blob || props.avatar !== path) return
  const url = URL.createObjectURL(blob)
  ownedUrl = url
  objectUrl.value = url
}

watch(() => props.avatar, value => load(value), { immediate: true })
onBeforeUnmount(release)

const boxStyle = computed(() => ({ width: `${props.size}px`, height: `${props.size}px` }))
const fallbackSize = computed(() => Math.round(props.size * 0.58))
</script>

<style scoped>
.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: var(--fg-on-accent);
}

.user-avatar.is-clickable {
  cursor: pointer;
}

.user-avatar__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
</style>

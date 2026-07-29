<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'

const handleAuthExpired = () => {
  ElMessage.warning('登录已过期，请重新登录')
}

onMounted(() => {
  window.addEventListener('auth-expired', handleAuthExpired)
})

onUnmounted(() => {
  window.removeEventListener('auth-expired', handleAuthExpired)
})
</script>

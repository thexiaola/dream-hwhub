<template>
  <router-view />
  <!-- 敏感操作身份二次验证弹窗（全局单例） -->
  <SensitiveVerifyDialog />
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import SensitiveVerifyDialog from '@/components/SensitiveVerifyDialog.vue'

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

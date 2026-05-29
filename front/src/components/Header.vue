<template>
  <header class="header">
    <div class="header-content">
      <div class="header-left">
        <h1>{{ pageTitle }}</h1>
      </div>
      
      <div class="header-right">
        <div class="user-info">
          <span>{{ userStore.user?.username }}</span>
          <el-dropdown>
            <span class="dropdown-trigger">
              <component :is="componentMap['ChevronDown']" />
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goToProfile">个人中心</el-dropdown-item>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ChevronDown } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const componentMap = {
  ChevronDown
}

const pageTitleMap = {
  '/dashboard': '首页',
  '/works': '作业管理',
  '/works/create': '创建作业',
  '/works/edit': '编辑作业',
  '/classes': '班级管理',
  '/classes/create': '创建班级',
  '/classes/edit': '编辑班级',
  '/submissions': '作业提交',
  '/submit': '提交作业',
  '/profile': '个人中心'
}

const pageTitle = computed(() => {
  return pageTitleMap[route.path] || pageTitleMap[route.path.split('/').slice(0, -1).join('/')] || '作业管理系统'
})

function goToProfile() {
  router.push('/profile')
}

async function handleLogout() {
  try {
    await userStore.logout()
    ElMessage.success('退出成功')
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.message || '退出失败')
  }
}
</script>

<style scoped>
.header {
  background: #fff;
  border-bottom: 1px solid #eee;
  padding: 0 24px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 64px;
}

.header-left h1 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  margin-right: 20px;
}

.user-info span {
  margin-right: 8px;
  color: #666;
}

.dropdown-trigger {
  cursor: pointer;
  color: #909399;
}

.dropdown-trigger svg {
  font-size: 16px;
}
</style>
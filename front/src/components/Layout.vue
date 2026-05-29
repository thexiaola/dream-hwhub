<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="logo">
        <BookOpen class="logo-icon" :size="28" />
        <span class="logo-text">作业管理系统</span>
      </div>
      <el-menu :default-active="activeMenu" mode="vertical" class="menu">
        <el-menu-item index="/dashboard">
          <LayoutDashboard :size="20" />
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/work">
          <FileText :size="20" />
          <span>作业管理</span>
        </el-menu-item>
        <el-menu-item index="/class">
          <Users :size="20" />
          <span>班级管理</span>
        </el-menu-item>
        <el-menu-item index="/submission">
          <CheckCircle :size="20" />
          <span>作业提交</span>
        </el-menu-item>
      </el-menu>
    </aside>
    <main class="main-content">
      <header class="header">
        <div class="header-left">
          <h2>{{ pageTitle }}</h2>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <User :size="20" />
              <span>{{ userStore.userInfo?.username }}</span>
              <ChevronDown :size="16" />
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <User :size="16" />
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout">
                  <LogOut :size="16" />
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <div class="content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { BookOpen, LayoutDashboard, FileText, Users, CheckCircle, User, ChevronDown, LogOut } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const pageTitle = computed(() => {
  const titles: Record<string, string> = {
    '/dashboard': '首页仪表盘',
    '/work': '作业管理',
    '/class': '班级管理',
    '/submission': '作业提交',
    '/profile': '个人中心'
  }
  return titles[route.path] || '作业管理系统'
})

const handleCommand = (command: string) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
}

.sidebar {
  width: 240px;
  background: rgba(255, 255, 255, 0.03);
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  margin-bottom: 30px;
}

.logo-icon {
  color: #667eea;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.menu {
  flex: 1;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 64px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.03);
}

.header-left h2 {
  font-size: 18px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.3s;
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.05);
}

.content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
</style>

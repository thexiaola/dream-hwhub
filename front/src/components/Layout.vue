<template>
  <div class="layout">
    <header class="header">
      <div class="header-left">
        <div class="logo">
          <BookOpen class="logo-icon" :size="24" />
          <span class="logo-text">作业管理系统</span>
        </div>
        <div class="nav-tabs">
          <button 
            class="nav-tab" 
            :class="{ active: activeTab === 'student' }"
            @click="switchTab('student')"
          >
            <GraduationCap :size="18" />
            我听的课
          </button>
          <button 
            class="nav-tab" 
            :class="{ active: activeTab === 'teacher' }"
            @click="switchTab('teacher')"
          >
            <Presentation :size="18" />
            我教的课
          </button>
        </div>
      </div>
      <div class="header-right">
        <el-dropdown @command="handleCommand">
          <div class="user-info">
            <User :size="18" />
            <span>{{ userStore.userInfo?.username }}</span>
            <ChevronDown :size="14" />
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
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { BookOpen, GraduationCap, Presentation, User, ChevronDown, LogOut } from '@lucide/vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeTab = ref<'student' | 'teacher'>('student')

const switchTab = (tab: 'student' | 'teacher') => {
  activeTab.value = tab
  if (tab === 'student') {
    router.push('/student/courses')
  } else {
    router.push('/teacher/courses')
  }
}

const handleCommand = (command: string) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

onMounted(() => {
  if (route.path.startsWith('/teacher')) {
    activeTab.value = 'teacher'
  } else {
    activeTab.value = 'student'
  }
})
</script>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 64px;
  background: rgba(255, 255, 255, 0.03);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 32px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  color: #667eea;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav-tabs {
  display: flex;
  gap: 8px;
}

.nav-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.nav-tab:hover {
  background: rgba(255, 255, 255, 0.05);
  color: white;
}

.nav-tab.active {
  background: rgba(102, 126, 234, 0.2);
  border-color: rgba(102, 126, 234, 0.4);
  color: #667eea;
}

.header-right {
  display: flex;
  align-items: center;
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

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
</style>
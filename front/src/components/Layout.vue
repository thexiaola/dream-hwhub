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
          <button
            v-if="isAdmin"
            class="nav-tab"
            :class="{ active: activeTab === 'admin' }"
            @click="switchTab('admin')"
          >
            <Shield :size="18" />
            管理面板
          </button>
        </div>
      </div>
      <div class="header-right">
        <ThemeToggle />
        <el-dropdown @command="handleCommand">
          <div class="user-info">
            <User :size="18" />
            <span class="user-name">{{ userStore.userInfo?.username }}</span>
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

    <!-- 手机端底部导航 -->
    <nav class="mobile-tab-bar">
      <button
        class="mobile-tab"
        :class="{ active: activeTab === 'student' }"
        @click="switchTab('student')"
      >
        <GraduationCap :size="22" />
        <span>我的课</span>
      </button>
      <button
        class="mobile-tab"
        :class="{ active: activeTab === 'teacher' }"
        @click="switchTab('teacher')"
      >
        <Presentation :size="22" />
        <span>我教的</span>
      </button>
      <button
        v-if="isAdmin"
        class="mobile-tab"
        :class="{ active: activeTab === 'admin' }"
        @click="switchTab('admin')"
      >
        <Shield :size="22" />
        <span>管理</span>
      </button>
      <button
        class="mobile-tab"
        :class="{ active: route.path.startsWith('/profile') }"
        @click="router.push('/profile')"
      >
        <User :size="22" />
        <span>我的</span>
      </button>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import ThemeToggle from '@/components/ThemeToggle.vue'
import { BookOpen, GraduationCap, Presentation, User, ChevronDown, LogOut, Shield } from '@lucide/vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isAdmin = computed(() => (userStore.userInfo?.permission ?? 0) >= 100)
const activeTab = ref<'student' | 'teacher' | 'admin'>('student')

const switchTab = (tab: 'student' | 'teacher' | 'admin') => {
  activeTab.value = tab
  if (tab === 'student') {
    router.push('/student/courses')
  } else if (tab === 'teacher') {
    router.push('/teacher/courses')
  } else {
    router.push('/admin/panel')
  }
}

const handleCommand = async (command: string) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    await userStore.logout()
    router.push('/login')
  }
}

onMounted(() => {
  if (route.path.startsWith('/teacher')) {
    activeTab.value = 'teacher'
  } else if (route.path.startsWith('/admin')) {
    activeTab.value = 'admin'
  } else {
    activeTab.value = 'student'
  }
})
</script>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 64px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
  border-bottom: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  flex-shrink: 0;
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
  white-space: nowrap;
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
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 8px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.nav-tab:hover {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.05);
  color: var(--fg);
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
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.88);
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
}

.user-info:hover {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.08);
  color: var(--fg);
}

.user-name {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.user-info .el-icon) {
  color: inherit;
}

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

/* 手机端底部导航 */
.mobile-tab-bar {
  display: none;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 64px;
  background: var(--bg-elevated);
  border-top: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  z-index: 999;
  padding-bottom: env(safe-area-inset-bottom);
}

.mobile-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  background: transparent;
  border: none;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.5);
  font-size: 11px;
  cursor: pointer;
  transition: color 0.2s;
}

.mobile-tab .lucide {
  stroke-width: 2;
}

.mobile-tab.active {
  color: #667eea;
}

/* 手机端适配 */
@media (max-width: 768px) {
  .header {
    padding: 0 12px;
    height: 56px;
  }

  .header-left {
    gap: 12px;
    flex: 1;
    min-width: 0;
  }

  .logo-text {
    font-size: 14px;
  }

  .nav-tabs {
    display: none;
  }

  .user-name {
    display: none;
  }

  .user-info {
    padding: 6px 10px;
  }

  .main-content {
    padding: 12px;
    padding-bottom: 80px;
  }

  .mobile-tab-bar {
    display: flex;
  }
}
</style>

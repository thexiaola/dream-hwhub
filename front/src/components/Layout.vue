<template>
  <div class="layout">
    <header class="header">
      <div class="header-left">
        <div class="logo">
          <BookOpen class="logo-icon" :size="24" />
          <span class="logo-text">作业管理系统</span>
        </div>
        <div class="nav-tabs" ref="navTabsRef">
          <!-- 激活项背景块：随选中项在选项卡之间平滑滑动 -->
          <span
            v-show="indicator.visible"
            class="nav-indicator"
            :style="{ transform: `translateX(${indicator.left}px)`, width: `${indicator.width}px` }"
          />
          <button
            :ref="tabRefSetters.student"
            class="nav-tab"
            :class="{ active: activeTab === 'student' }"
            @click="switchTab('student')"
          >
            <GraduationCap :size="18" />
            我听的课
          </button>
          <button
            v-if="canEnterTeacherArea"
            :ref="tabRefSetters.teacher"
            class="nav-tab"
            :class="{ active: activeTab === 'teacher' }"
            @click="switchTab('teacher')"
          >
            <Presentation :size="18" />
            我教的课
          </button>
          <button
            :ref="tabRefSetters.school"
            class="nav-tab"
            :class="{ active: activeTab === 'school' }"
            @click="switchTab('school')"
          >
            <School :size="18" />
            我的学校
          </button>
          <button
            v-if="isAdmin"
            :ref="tabRefSetters.admin"
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
      <!-- 页面切换：淡出淡入配合位移与缩放，接近 PowerPoint 平滑过渡的观感 -->
      <router-view v-slot="{ Component, route: currentRoute }">
        <transition name="page-morph" mode="out-in">
          <component :is="Component" :key="currentRoute.path" />
        </transition>
      </router-view>
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
        v-if="canEnterTeacherArea"
        class="mobile-tab"
        :class="{ active: activeTab === 'teacher' }"
        @click="switchTab('teacher')"
      >
        <Presentation :size="22" />
        <span>我教的</span>
      </button>
      <button
        class="mobile-tab"
        :class="{ active: activeTab === 'school' }"
        @click="switchTab('school')"
      >
        <School :size="22" />
        <span>学校</span>
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
import { ref, computed, onMounted, onUnmounted, reactive, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useSchoolStore } from '@/stores/school'
import ThemeToggle from '@/components/ThemeToggle.vue'
import { BookOpen, GraduationCap, Presentation, User, ChevronDown, LogOut, Shield, School } from '@lucide/vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const schoolStore = useSchoolStore()

const isAdmin = computed(() => userStore.isAdmin)
const canEnterTeacherArea = computed(() => userStore.isOp || schoolStore.isSchoolTeacher)
const activeTab = ref<'student' | 'teacher' | 'school' | 'admin'>('student')

// 导航激活块的定位：随选中选项卡移动
const navTabsRef = ref<HTMLElement | null>(null)
const tabElements = new Map<string, HTMLElement>()
const indicator = reactive({ left: 0, width: 0, visible: false })
let resizeObserver: ResizeObserver | null = null

const setTabRef = (key: string, el: unknown) => {
  if (el instanceof HTMLElement) {
    tabElements.set(key, el)
  } else {
    tabElements.delete(key)
  }
}

// 固定的 ref 回调，避免模板内联函数在每次渲染时重建
const tabRefSetters = {
  student: (el: unknown) => setTabRef('student', el),
  teacher: (el: unknown) => setTabRef('teacher', el),
  school: (el: unknown) => setTabRef('school', el),
  admin: (el: unknown) => setTabRef('admin', el)
}

const updateIndicator = () => {
  const el = tabElements.get(activeTab.value)
  if (!el) {
    indicator.visible = false
    return
  }
  indicator.left = el.offsetLeft
  indicator.width = el.offsetWidth
  indicator.visible = true
}

// 切换选项卡或隐藏/显示入口（如学校身份变化）后重新定位
watch(activeTab, () => nextTick(updateIndicator))
watch(canEnterTeacherArea, () => nextTick(updateIndicator))
watch(isAdmin, () => nextTick(updateIndicator))

const handleResize = () => nextTick(updateIndicator)
const switchTab = (tab: 'student' | 'teacher' | 'school' | 'admin') => {
  activeTab.value = tab
  if (tab === 'student') {
    router.push('/student/courses')
  } else if (tab === 'teacher') {
    router.push('/teacher/courses')
  } else if (tab === 'school') {
    router.push('/school')
  } else {
    router.push('/admin/panel')
  }
}

const handleCommand = async (command: string) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    const msg = await userStore.logout()
    if (msg) {
      ElMessage.success(msg)
      router.push('/login')
    } else {
      ElMessage.error('登出失败，请重试')
    }
  }
}

onMounted(async () => {
  // 先同步定位一次，避免等身份请求期间导航没有激活块
  await nextTick()
  updateIndicator()

  // 容器尺寸变化（窗口缩放、字体加载、选项卡增减）时重新定位
  resizeObserver = new ResizeObserver(handleResize)
  if (navTabsRef.value) {
    resizeObserver.observe(navTabsRef.value)
  }

  // 非教师路径先同步确定高亮，避免等网络请求造成闪烁
  if (route.path.startsWith('/school')) {
    activeTab.value = 'school'
  } else if (route.path.startsWith('/admin')) {
    activeTab.value = 'admin'
  } else if (!route.path.startsWith('/teacher')) {
    activeTab.value = 'student'
  }

  // 拉取学校身份：决定「我教的课」入口是否展示，以及教师页面的高亮
  await schoolStore.fetchMySchools()
  if (route.path.startsWith('/teacher') && canEnterTeacherArea.value) {
    activeTab.value = 'teacher'
  }
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
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
  position: relative;
  display: flex;
  gap: 8px;
}

/* 激活项背景块：位置与宽度过渡交给 transform/width 动画 */
.nav-indicator {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  box-sizing: border-box;
  border: 1px solid rgba(102, 126, 234, 0.4);
  border-radius: 8px;
  background: rgba(102, 126, 234, 0.2);
  pointer-events: none;
  transition:
    transform 0.38s cubic-bezier(0.22, 0.61, 0.36, 1),
    width 0.38s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.nav-tab {
  position: relative;
  z-index: 1;
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
  border-color: transparent;
  background: transparent;
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

/* ===== 页面过渡（配合 router-view 的 transition name="page-morph"）===== */
.page-morph-enter-active {
  transition:
    opacity 0.3s ease,
    transform 0.3s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.page-morph-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.page-morph-enter-from {
  opacity: 0;
  transform: translateY(14px) scale(0.99);
}

.page-morph-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.995);
}

@media (prefers-reduced-motion: reduce) {
  .page-morph-enter-active,
  .page-morph-leave-active,
  .nav-indicator {
    transition: none;
  }

  .page-morph-enter-from,
  .page-morph-leave-to {
    opacity: 1;
    transform: none;
  }
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

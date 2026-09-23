<template>
  <div class="layout">
    <!-- ===== 顶部导航栏：全宽、优先级最高（学校选择 / 主题 / 个人信息） ===== -->
    <header class="header">
      <div class="header-left">
        <div class="logo">
          <BookOpen class="logo-icon" :size="24" />
          <span class="logo-text">作业管理系统</span>
        </div>
        <!-- 当前学校 + 本人在该校身份（如「北京大学（教师）」）；点击可切换学校 -->
        <el-select
          v-if="schoolOptions.length > 0"
          :model-value="schoolStore.currentSchoolId"
          class="header-school-select"
          placeholder="选择学校"
          @update:model-value="schoolStore.setCurrentSchool"
        >
          <el-option
            v-for="school in schoolOptions"
            :key="school.id"
            :label="schoolLabel(school)"
            :value="school.id"
          />
        </el-select>
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

    <!-- ===== 主体：左侧导航栏 + 内容区 ===== -->
    <div class="layout-body">
      <aside class="sidebar" :class="{ 'is-collapsed': collapsed }">
        <!-- 顶部：用户头像 + 昵称 + 身份 -->
        <div class="sidebar-user">
          <UserAvatar
            :avatar="userStore.userInfo?.avatar"
            :size="40"
            :alt="userStore.userInfo?.username"
          />
          <div v-if="!collapsed" class="sidebar-user-meta">
            <span class="sidebar-user-name">{{ userStore.userInfo?.username || '未登录' }}</span>
            <span class="sidebar-user-role">{{ roleText }}</span>
          </div>
        </div>

        <!-- 导航项：激活高亮块可按住拖动，松开吸附到最近项 -->
        <div
          class="side-nav"
          ref="navTabsRef"
          @pointerdown="startDrag"
          @click.capture="onClickCapture"
        >
          <span
            v-show="indicatorVisible"
            class="side-indicator"
            :class="{ 'is-dragging': indicatorDragging }"
            :style="indicatorStyle"
          />
          <button
            :ref="tabRefSetters.student"
            class="side-nav-item"
            :class="{ active: activeTab === 'student' }"
            :title="collapsed ? '我听的课' : undefined"
            @click="switchTab('student')"
          >
            <GraduationCap :size="18" />
            <span v-if="!collapsed" class="side-nav-label">我听的课</span>
          </button>
          <button
            v-if="canEnterTeacherArea"
            :ref="tabRefSetters.teacher"
            class="side-nav-item"
            :class="{ active: activeTab === 'teacher' }"
            :title="collapsed ? '我教的课' : undefined"
            @click="switchTab('teacher')"
          >
            <Presentation :size="18" />
            <span v-if="!collapsed" class="side-nav-label">我教的课</span>
          </button>
          <button
            :ref="tabRefSetters.school"
            class="side-nav-item"
            :class="{ active: activeTab === 'school' }"
            :title="collapsed ? '我的学校' : undefined"
            @click="switchTab('school')"
          >
            <School :size="18" />
            <span v-if="!collapsed" class="side-nav-label">我的学校</span>
          </button>
          <button
            v-if="isAdmin"
            :ref="tabRefSetters.admin"
            class="side-nav-item"
            :class="{ active: activeTab === 'admin' }"
            :title="collapsed ? '管理面板' : undefined"
            @click="switchTab('admin')"
          >
            <Shield :size="18" />
            <span v-if="!collapsed" class="side-nav-label">管理面板</span>
          </button>
        </div>

        <!-- 底部：收起 / 展开导航 -->
        <button
          class="sidebar-collapse"
          :title="collapsed ? '展开导航' : '收起导航'"
          @click="collapsed = !collapsed"
        >
          <component :is="collapsed ? ChevronsRight : ChevronsLeft" :size="18" />
        </button>
      </aside>

      <main class="main-content">
        <!-- 页面切换：淡出淡入配合位移与缩放，接近 PowerPoint 平滑过渡的观感 -->
        <router-view v-slot="{ Component, route: currentRoute }">
          <transition name="page-morph" mode="out-in">
            <component :is="Component" :key="currentRoute.meta.viewKey || currentRoute.path" />
          </transition>
        </router-view>
      </main>
    </div>

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
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useSchoolStore } from '@/stores/school'
import ThemeToggle from '@/components/ThemeToggle.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { useDraggableIndicator } from '@/composables/useDraggableIndicator'
import {
  BookOpen, GraduationCap, Presentation, User, ChevronDown, LogOut, Shield, School,
  ChevronsLeft, ChevronsRight
} from '@lucide/vue'

type NavTab = 'student' | 'teacher' | 'school' | 'admin'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const schoolStore = useSchoolStore()

const isAdmin = computed(() => userStore.isAdmin)
const canEnterTeacherArea = computed(() => userStore.isOp || schoolStore.isSchoolTeacher)
const activeTab = ref<NavTab>('student')

// 侧边栏收起状态：收起后只留图标，为内容区让出宽度
const collapsed = ref(false)

// 顶部「选择学校」下拉的选项：我加入的学校（教职工与学生共用）
const schoolOptions = computed(() => schoolStore.mySchools ?? [])

// 学校选项文案：学校全称 + 本人在该校身份，如「北京大学（教师）」
// 按角色码映射而非直接用后端 myRole，使措辞与侧边栏、个人中心统一为「教师」
const SCHOOL_ROLE_TEXT: Record<number, string> = { 2: '学校管理员', 1: '教师', 0: '学生' }

const schoolLabel = (school: { schoolName: string; myRoleCode?: number | null }) => {
  const role = school.myRoleCode != null ? SCHOOL_ROLE_TEXT[school.myRoleCode] : undefined
  return role ? `${school.schoolName}（${role}）` : school.schoolName
}

// 侧边栏展示的身份标签
const roleText = computed(() => {
  if (userStore.isOp) return '平台管理员'
  if (schoolStore.isSchoolTeacher) return '教师'
  return '学生'
})

// 导航激活块的定位：随选中项移动，可按住拖动
const navTabsRef = ref<HTMLElement | null>(null)
const tabElements = new Map<string, HTMLElement>()
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

// 当前可见导航项的视觉顺序，拖拽吸附与命中测试都以此为准
const tabOrder = computed<NavTab[]>(() => {
  const keys: NavTab[] = ['student']
  if (canEnterTeacherArea.value) keys.push('teacher')
  keys.push('school')
  if (isAdmin.value) keys.push('admin')
  return keys
})

const getTabs = () => {
  const result: { key: string; el: HTMLElement }[] = []
  for (const key of tabOrder.value) {
    const el = tabElements.get(key)
    if (el) result.push({ key, el })
  }
  return result
}

// 各导航项对应的目标路径
const navPath = (tab: NavTab): string =>
  tab === 'student'
    ? '/student/courses'
    : tab === 'teacher'
      ? '/teacher/courses'
      : tab === 'school'
        ? '/school'
        : '/admin/panel'

// 当前是否已处于该项对应的路由下。
// 用前缀匹配而非全等：如管理面板子路由为 /admin/panel/:tab，仍应视为已在该项
const isOnTab = (tab: NavTab): boolean => route.path === navPath(tab) || route.path.startsWith(`/${tab}`)

// 点击导航项：写入历史，便于后退
const switchTab = (tab: NavTab) => {
  activeTab.value = tab
  if (!isOnTab(tab)) {
    router.push(navPath(tab))
  }
}

// 拖拽经过导航项：实时跳转；用 replace 避免快速掠过时刷出多条历史
const previewTab = (tab: NavTab) => {
  if (activeTab.value === tab) return
  activeTab.value = tab
  if (!isOnTab(tab)) {
    router.replace(navPath(tab))
  }
}

const { dragging: indicatorDragging, position: indicatorPosition, visible: indicatorVisible, syncToActive, startDrag, onClickCapture } =
  useDraggableIndicator({
    container: navTabsRef,
    getTabs,
    activeKey: () => activeTab.value,
    // 侧边栏为竖直排布
    axis: 'y',
    // 拖拽经过即实时跳转
    onCross: key => previewTab(key as NavTab),
    // 松开吸附到最近的导航项
    onSettle: key => switchTab(key as NavTab)
  })

const indicatorStyle = computed(() => {
  const pos = indicatorPosition.value
  if (!pos) return {}
  return {
    transform: `translate(${pos.left}px, ${pos.top}px)`,
    width: `${pos.width}px`,
    height: `${pos.height}px`
  }
})

// 切换导航项或隐藏/显示入口（如学校身份变化）后重新定位
watch(activeTab, () => nextTick(syncToActive))
watch(canEnterTeacherArea, () => nextTick(syncToActive))
watch(isAdmin, () => nextTick(syncToActive))

const handleResize = () => nextTick(syncToActive)

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
  syncToActive()

  // 容器尺寸变化（窗口缩放、字体加载、导航项增减）时重新定位
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

  // 拉取学校列表：顶部「选择学校」下拉需要它（教职工与学生共用），
  // 「我教的课」入口是否展示也取决于此。请求在 store 内做了并发去重。
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

/* ===== 顶部导航栏：全宽、优先级最高 ===== */
.header {
  position: relative;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 64px;
  flex-shrink: 0;
  background: var(--bg-elevated);
  border-bottom: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
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

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 顶部「当前学校（身份）」下拉：显示学校全称 + 身份，名称过长时省略 */
.header-school-select {
  width: 240px;
  max-width: 40vw;
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

/* ===== 主体：左导航 + 内容 ===== */
.layout-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

/* 左侧导航栏：品牌色渐变，形式参照超星平台 */
.sidebar {
  position: relative;
  z-index: 1;
  width: 220px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 16px 12px 12px;
  background: linear-gradient(180deg, var(--primary-color), var(--secondary-color));
  transition: width 0.25s ease;
}

.sidebar.is-collapsed {
  width: 68px;
  padding-left: 8px;
  padding-right: 8px;
}

/* 顶部：用户头像 + 昵称 + 身份 */
.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
  margin-bottom: 12px;
}

.sidebar.is-collapsed .sidebar-user {
  justify-content: center;
  padding: 6px;
}

/* 头像在渐变底上加深色环，保证边界清晰 */
.sidebar-user :deep(.user-avatar) {
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.6);
}

.sidebar-user-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.sidebar-user-name {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-user-role {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.side-nav {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* 激活项背景块：位置与尺寸过渡交给 transform/width/height 动画 */
.side-indicator {
  position: absolute;
  top: 0;
  left: 0;
  box-sizing: border-box;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.24);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.35);
  pointer-events: none;
  transition:
    transform 0.38s cubic-bezier(0.22, 0.61, 0.36, 1),
    width 0.38s cubic-bezier(0.22, 0.61, 0.36, 1),
    height 0.38s cubic-bezier(0.22, 0.61, 0.36, 1);
}

/* 拖拽中：位移紧跟指针（transform 不设过渡），仅尺寸平滑过渡 */
.side-indicator.is-dragging {
  transition:
    width 0.18s ease,
    height 0.18s ease;
}

.side-nav-item {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 11px 12px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.82);
  font-size: 14px;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

.sidebar.is-collapsed .side-nav-item {
  justify-content: center;
  padding-left: 0;
  padding-right: 0;
}

.side-nav-item:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
}

.side-nav-item.active {
  color: #ffffff;
  font-weight: 600;
}

.side-nav-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 底部：收起 / 展开导航 */
.sidebar-collapse {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.sidebar-collapse:hover {
  background: rgba(255, 255, 255, 0.22);
}

/* ===== 内容区 ===== */
.main-content {
  flex: 1;
  min-width: 0;
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
  .side-indicator,
  .side-indicator.is-dragging {
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

/* 手机端适配：隐藏侧边栏，改用底部导航 */
@media (max-width: 768px) {
  .sidebar {
    display: none;
  }

  .header {
    padding: 0 12px;
    height: 56px;
  }

  /* 顶部栏空间紧张：logo 收成图标，让左侧「当前学校（身份）」下拉占据剩余宽度 */
  .header-left {
    flex: 1;
    min-width: 0;
    gap: 8px;
  }

  .logo-text {
    display: none;
  }

  .header-right {
    flex: 0 0 auto;
    gap: 8px;
    justify-content: flex-end;
  }

  .header-school-select {
    flex: 1;
    min-width: 0;
    width: auto;
    max-width: none;
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

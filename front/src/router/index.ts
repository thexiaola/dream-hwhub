import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useSchoolStore } from '@/stores/school'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/courses/student'
  },
  {
    path: '/dashboard',
    redirect: '/courses/student'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue')
  },
  {
    path: '/retrieve',
    name: 'Retrieve',
    component: () => import('@/views/Retrieve.vue')
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/components/Layout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        // 「课程」合并了「我听的课 / 我教的课」两个选项卡，由 :tab 决定当前选项卡。
        // viewKey 固定，使切换 :tab 时组件不重挂载（避免整页重播过渡动画）
        path: 'courses/:tab?',
        name: 'Courses',
        component: () => import('@/views/Courses.vue'),
        meta: { viewKey: 'courses' }
      },
      {
        // 旧链接兼容：原「我听的课」入口
        path: 'student/courses',
        redirect: '/courses/student'
      },
      {
        // 旧链接兼容：原「我教的课」入口（保留教师区守卫）
        path: 'teacher/courses',
        redirect: '/courses/teacher',
        meta: { requiresSchoolTeacher: true }
      },
      {
        path: 'student/course/:id',
        name: 'StudentCourseDetail',
        component: () => import('@/views/student/StudentCourseDetail.vue')
      },
      {
        path: 'student/work/:id',
        name: 'StudentWork',
        component: () => import('@/views/student/StudentWork.vue')
      },
      {
        // 考试作答页：与作业详情分离，因需承载反作弊（强制全屏/切屏检测/字体映射等）
        path: 'student/exam/:id',
        name: 'StudentExam',
        component: () => import('@/views/student/ExamWork.vue')
      },
      {
        // 不设 requiresSchoolTeacher：班级详情需允许「已失去教师身份的原创建者」
        // 进入查看冻结状态（写入操作在后端已全部拦截），准入由后端 getClassDetail
        // 与页面内 loadCourse 共同判定（成员/管理员/冻结班级的本校老师）
        path: 'teacher/course/:id',
        name: 'TeacherCourseDetail',
        component: () => import('@/views/teacher/TeacherCourseDetail.vue')
      },
      {
        path: 'teacher/work/:id/edit',
        name: 'TeacherWorkEdit',
        component: () => import('@/views/teacher/EditWork.vue'),
        meta: { requiresSchoolTeacher: true }
      },
      {
        path: 'teacher/work/:id/submissions',
        name: 'TeacherWorkSubmissions',
        component: () => import('@/views/teacher/WorkSubmissions.vue'),
        meta: { requiresSchoolTeacher: true }
      },
      {
        path: 'school',
        name: 'SchoolCenter',
        component: () => import('@/views/school/SchoolCenter.vue')
      },
      {
        path: 'school/:id',
        name: 'SchoolDetail',
        component: () => import('@/views/school/SchoolDetail.vue')
      },
      {
        // 学校管理独立页：仅平台管理员或该校学校管理员可进入（页面内二次校验）。
        // 从「管理面板 → 学校管理」或「我的学校」的管理按钮跳转而来，避免在抽屉里
        // 右侧窄栏呈现管理控制台造成的拥挤。
        path: 'school/:id/manage',
        name: 'SchoolManagePage',
        component: () => import('@/views/school/SchoolManagePage.vue')
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue')
      },
      {
        // 站内信（按学校隔离，支持筛选）
        path: 'messages',
        name: 'MessageCenter',
        component: () => import('@/views/message/MessageCenter.vue')
      },
      {
        // 好友（按学校隔离，可添加同校用户）
        path: 'friends',
        name: 'FriendCenter',
        component: () => import('@/views/message/FriendCenter.vue')
      },
      {
        // 私信（按学校隔离，非好友也可发，受陌生私信配额约束）
        path: 'private-messages',
        name: 'PrivateMessageCenter',
        component: () => import('@/views/message/PrivateMessageCenter.vue')
      },
      {
        // 管理面板各模块用独立链接：/admin/panel/:tab（tab 省略时回退到首个有权限的模块）
        // viewKey 固定，切换模块时复用同一页面实例，不整页重挂载
        path: 'admin/panel/:tab?',
        name: 'AdminPanel',
        component: () => import('@/views/admin/AdminPanel.vue'),
        meta: { requiresAdmin: true, viewKey: 'admin-panel' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth) {
    if (!userStore.isLoggedIn) {
      next('/login')
      return
    }
    
    if (!userStore.userInfo) {
      try {
        await userStore.getUserInfo()
      } catch {
        userStore.clearLocal()
        next('/login')
        return
      }

      // 拿不到用户信息说明登录态已失效（Token 无效 / 账号不存在），不允许进入
      if (!userStore.userInfo) {
        userStore.clearLocal()
        next('/login')
        return
      }
    }

    if (to.meta.requiresAdmin && !userStore.isAdmin) {
      next('/courses/student')
      return
    }

    // 教师相关页面对平台管理员与学校老师（含学校管理员）开放
    if (to.meta.requiresSchoolTeacher) {
      const schoolStore = useSchoolStore()
      // 强制刷新，避免身份被收回后仍用旧的缓存放行
      await schoolStore.fetchMySchools(true)
      if (!userStore.isOp && !schoolStore.isSchoolTeacher) {
        next('/courses/student')
        return
      }
    }
    next()
  } else {
    // 已登录用户访问登录页时，确认登录态仍然有效后再回到主页
    if (to.path === '/login' && userStore.isLoggedIn) {
      await userStore.getUserInfo()
      if (userStore.userInfo) {
        next('/courses/student')
        return
      }
      userStore.clearLocal()
    }
    next()
  }
})

export default router
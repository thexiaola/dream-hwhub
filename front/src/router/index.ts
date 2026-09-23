import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useSchoolStore } from '@/stores/school'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/student/courses'
  },
  {
    path: '/dashboard',
    redirect: '/student/courses'
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
        path: 'student/courses',
        name: 'StudentCourses',
        component: () => import('@/views/student/MyCourses.vue')
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
        path: 'teacher/courses',
        name: 'TeacherCourses',
        component: () => import('@/views/teacher/TeachingCourses.vue'),
        meta: { requiresSchoolTeacher: true }
      },
      {
        path: 'teacher/course/:id',
        name: 'TeacherCourseDetail',
        component: () => import('@/views/teacher/TeacherCourseDetail.vue'),
        meta: { requiresSchoolTeacher: true }
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
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue')
      },
      {
        path: 'admin/panel',
        name: 'AdminPanel',
        component: () => import('@/views/admin/AdminPanel.vue'),
        meta: { requiresAdmin: true }
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
      next('/student/courses')
      return
    }

    // 教师相关页面对平台管理员与学校老师（含学校管理员）开放
    if (to.meta.requiresSchoolTeacher) {
      const schoolStore = useSchoolStore()
      // 强制刷新，避免身份被收回后仍用旧的缓存放行
      await schoolStore.fetchMySchools(true)
      if (!userStore.isOp && !schoolStore.isSchoolTeacher) {
        next('/student/courses')
        return
      }
    }
    next()
  } else {
    // 已登录用户访问登录页时，确认登录态仍然有效后再回到主页
    if (to.path === '/login' && userStore.isLoggedIn) {
      await userStore.getUserInfo()
      if (userStore.userInfo) {
        next('/student/courses')
        return
      }
      userStore.clearLocal()
    }
    next()
  }
})

export default router
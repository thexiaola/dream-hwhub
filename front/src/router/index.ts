import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

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
        path: 'teacher/courses',
        name: 'TeacherCourses',
        component: () => import('@/views/teacher/TeachingCourses.vue')
      },
      {
        path: 'teacher/course/:id',
        name: 'TeacherCourseDetail',
        component: () => import('@/views/teacher/TeacherCourseDetail.vue')
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
        userStore.logout()
        next('/login')
        return
      }
    }

    if (to.meta.requiresAdmin && (userStore.userInfo?.permission ?? 0) < 100) {
      next('/student/courses')
      return
    }
    next()
  } else {
    if (userStore.isLoggedIn && to.path === '/login') {
      next('/student/courses')
      return
    }
    next()
  }
})

export default router
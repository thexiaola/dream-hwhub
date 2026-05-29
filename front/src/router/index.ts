import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard'
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
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue')
      },
      {
        path: 'work',
        name: 'WorkList',
        component: () => import('@/views/work/WorkList.vue')
      },
      {
        path: 'work/create',
        name: 'CreateWork',
        component: () => import('@/views/work/CreateWork.vue')
      },
      {
        path: 'work/:id',
        name: 'WorkDetail',
        component: () => import('@/views/work/WorkDetail.vue')
      },
      {
        path: 'class',
        name: 'ClassList',
        component: () => import('@/views/class/ClassList.vue')
      },
      {
        path: 'class/create',
        name: 'CreateClass',
        component: () => import('@/views/class/CreateClass.vue')
      },
      {
        path: 'class/:id',
        name: 'ClassDetail',
        component: () => import('@/views/class/ClassDetail.vue')
      },
      {
        path: 'submission',
        name: 'SubmissionList',
        component: () => import('@/views/submission/SubmissionList.vue')
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
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
    next()
  } else {
    if (userStore.isLoggedIn && to.path === '/login') {
      next('/dashboard')
      return
    }
    next()
  }
})

export default router

import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '../utils/http'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/dashboard'
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/Login.vue')
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('../views/Register.vue')
    },
    {
      path: '/retrieve',
      name: 'Retrieve',
      component: () => import('../views/Retrieve.vue')
    },
    {
      path: '/dashboard',
      name: 'Dashboard',
      component: () => import('../views/Dashboard.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('../views/Profile.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/works',
      name: 'WorkList',
      component: () => import('../views/work/WorkList.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/works/create',
      name: 'CreateWork',
      component: () => import('../views/work/CreateWork.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/works/:id',
      name: 'WorkDetail',
      component: () => import('../views/work/WorkDetail.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/works/:id/edit',
      name: 'EditWork',
      component: () => import('../views/work/EditWork.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/classes',
      name: 'ClassList',
      component: () => import('../views/class/ClassList.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/classes/create',
      name: 'CreateClass',
      component: () => import('../views/class/CreateClass.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/classes/:id',
      name: 'ClassDetail',
      component: () => import('../views/class/ClassDetail.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/classes/:id/edit',
      name: 'EditClass',
      component: () => import('../views/class/EditClass.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/submissions',
      name: 'SubmissionList',
      component: () => import('../views/submission/SubmissionList.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/submissions/:id',
      name: 'SubmissionDetail',
      component: () => import('../views/submission/SubmissionDetail.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/submit/:workId',
      name: 'SubmitWork',
      component: () => import('../views/submission/SubmitWork.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register' || to.path === '/retrieve') && isLoggedIn()) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
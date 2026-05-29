<template>
  <aside class="sidebar">
    <div class="sidebar-header">
      <div class="logo">
        <component :is="componentMap['GraduationCap']" />
        <span>作业管理系统</span>
      </div>
    </div>
    
    <nav class="sidebar-nav">
      <router-link 
        v-for="item in menuItems" 
        :key="item.path"
        :to="item.path"
        class="nav-item"
        :class="{ active: $route.path === item.path }"
      >
        <component :is="componentMap[item.icon]" />
        <span>{{ item.label }}</span>
      </router-link>
    </nav>
  </aside>
</template>

<script setup>
import { GraduationCap, LayoutDashboard, BookOpen, Users, FileUp, User } from 'lucide-vue-next'

const componentMap = {
  GraduationCap,
  LayoutDashboard,
  BookOpen,
  Users,
  FileUp,
  User
}

const menuItems = [
  { path: '/dashboard', label: '首页', icon: 'LayoutDashboard' },
  { path: '/works', label: '作业管理', icon: 'BookOpen' },
  { path: '/classes', label: '班级管理', icon: 'Users' },
  { path: '/submissions', label: '作业提交', icon: 'FileUp' },
  { path: '/profile', label: '个人中心', icon: 'User' }
]
</script>

<style scoped>
.sidebar {
  width: 200px;
  background: #2d3748;
  color: #fff;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 24px;
  border-bottom: 1px solid #4a5568;
}

.logo {
  display: flex;
  align-items: center;
  font-size: 18px;
  font-weight: 600;
}

.logo svg {
  margin-right: 8px;
  font-size: 24px;
}

.sidebar-nav {
  flex: 1;
  padding: 20px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 12px 24px;
  color: #a0aec0;
  text-decoration: none;
  transition: all 0.3s;
}

.nav-item:hover {
  background: #4a5568;
  color: #fff;
}

.nav-item.active {
  background: #667eea;
  color: #fff;
}

.nav-item svg {
  margin-right: 12px;
  font-size: 18px;
}
</style>
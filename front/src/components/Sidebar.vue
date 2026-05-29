<template>
  <aside class="sidebar">
    <nav class="sidebar-nav">
      <div class="nav-section">
        <div 
          v-for="item in menuItems" 
          :key="item.path"
          :class="['nav-item', { active: currentPath === item.path }]"
          @click="navigateTo(item.path)"
        >
          <component :is="componentMap[item.icon]" class="nav-icon" />
          <span class="nav-text">{{ item.label }}</span>
        </div>
      </div>
      
      <div class="nav-divider"></div>
      
      <div class="nav-section">
        <div 
          v-for="item in manageItems" 
          :key="item.path"
          :class="['nav-item', { active: currentPath.startsWith(item.path) }]"
          @click="navigateTo(item.path)"
        >
          <component :is="componentMap[item.icon]" class="nav-icon" />
          <span class="nav-text">{{ item.label }}</span>
        </div>
      </div>
    </nav>
  </aside>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, onBeforeRouteUpdate } from 'vue-router'
import { 
  LayoutDashboard, 
  FileText, 
  Users, 
  CheckCircle, 
  Plus,
  List
} from 'lucide-vue-next'

const componentMap = {
  LayoutDashboard,
  FileText,
  Users,
  CheckCircle,
  Plus,
  List
}

const router = useRouter()
const currentPath = ref('/dashboard')

const menuItems = [
  { path: '/dashboard', label: '首页', icon: 'LayoutDashboard' },
  { path: '/works', label: '作业管理', icon: 'FileText' },
  { path: '/classes', label: '班级管理', icon: 'Users' },
  { path: '/submissions', label: '作业提交', icon: 'CheckCircle' }
]

const manageItems = [
  { path: '/works/create', label: '创建作业', icon: 'Plus' },
  { path: '/classes/create', label: '创建班级', icon: 'Plus' }
]

onMounted(() => {
  currentPath.value = router.currentRoute.value.path
})

onBeforeRouteUpdate((to) => {
  currentPath.value = to.path
})

function navigateTo(path) {
  router.push(path)
}
</script>

<style scoped>
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  width: 256px;
  height: 100vh;
  background: rgba(10, 10, 26, 0.98);
  border-right: 1px solid rgba(102, 126, 234, 0.2);
  padding-top: 80px;
  z-index: 200;
}

.sidebar-nav {
  padding: 16px;
}

.nav-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.nav-item:hover {
  background: rgba(102, 126, 234, 0.1);
}

.nav-item.active {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.2) 0%, rgba(118, 75, 162, 0.2) 100%);
  border-left: 3px solid #667eea;
}

.nav-icon {
  font-size: 18px;
  color: #a0a0c0;
  transition: color 0.3s ease;
}

.nav-item:hover .nav-icon,
.nav-item.active .nav-icon {
  color: #667eea;
}

.nav-text {
  font-size: 14px;
  color: #a0a0c0;
  transition: color 0.3s ease;
}

.nav-item:hover .nav-text,
.nav-item.active .nav-text {
  color: white;
}

.nav-divider {
  height: 1px;
  background: rgba(102, 126, 234, 0.2);
  margin: 20px 0;
}
</style>
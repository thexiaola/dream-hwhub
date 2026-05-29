<template>
  <header class="header">
    <div class="header-left">
      <div class="logo">
        <component :is="componentMap['GraduationCap']" class="logo-icon" />
        <span class="logo-text">作业管理系统</span>
      </div>
    </div>
    
    <div class="header-right">
      <div class="user-info">
        <span class="user-name">{{ userInfo.username }}</span>
        <component :is="componentMap['User']" class="user-avatar" />
      </div>
      
      <div class="header-actions">
        <el-dropdown>
          <span class="dropdown-trigger">
            <component :is="componentMap['Settings']" class="action-icon" />
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="goToProfile">
                <component :is="componentMap['User']" /> 个人中心
              </el-dropdown-item>
              <el-dropdown-item @click="logout">
                <component :is="componentMap['LogOut']" /> 退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import { BookOpen, User, Settings, LogOut } from 'lucide-vue-next'

const componentMap = {
  BookOpen: BookOpen,
  User,
  Settings,
  LogOut
}

const router = useRouter()
const userStore = useUserStore()

const userInfo = ref({
  username: ''
})

onMounted(async () => {
  await loadUserInfo()
})

async function loadUserInfo() {
  const response = await userStore.getUserInfo()
  if (response.code === 200) {
    userInfo.value = response.data
  }
}

function goToProfile() {
  router.push('/profile')
}

async function logout() {
  const response = await userStore.logout()
  if (response.code === 200) {
    ElMessage.success('退出成功')
    router.push('/login')
  } else {
    ElMessage.error('退出失败')
  }
}
</script>

<style scoped>
.header {
  position: fixed;
  top: 0;
  left: 256px;
  right: 0;
  height: 64px;
  background: rgba(18, 18, 42, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(102, 126, 234, 0.2);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: white;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 20px;
}

.user-name {
  font-size: 14px;
  color: white;
}

.user-avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: white;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dropdown-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.dropdown-trigger:hover {
  background: rgba(102, 126, 234, 0.2);
}

.action-icon {
  font-size: 18px;
  color: #a0a0c0;
}

.el-dropdown-menu {
  background: rgba(18, 18, 42, 0.95);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 12px;
}

.el-dropdown-item {
  color: #a0a0c0;
  transition: all 0.3s ease;
}

.el-dropdown-item:hover {
  background: rgba(102, 126, 234, 0.2);
  color: white;
}
</style>
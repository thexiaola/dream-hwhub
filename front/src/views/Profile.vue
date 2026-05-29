<template>
  <div class="profile-page">
    <div class="page-header">
      <h2>个人中心</h2>
    </div>
    <el-card class="profile-card">
      <div class="profile-header">
        <div class="avatar">
          <User :size="48" />
        </div>
        <div class="user-info">
          <h3>{{ userStore.userInfo?.username }}</h3>
          <p>{{ userStore.userInfo?.account }}</p>
        </div>
      </div>
      <div class="info-section">
        <h4>基本信息</h4>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">学号/工号</span>
            <span class="value">{{ userStore.userInfo?.userNo }}</span>
          </div>
          <div class="info-item">
            <span class="label">邮箱</span>
            <span class="value">{{ userStore.userInfo?.email }}</span>
          </div>
          <div class="info-item">
            <span class="label">角色</span>
            <span class="value">{{ userStore.userInfo?.role === 'teacher' ? '教师' : '学生' }}</span>
          </div>
        </div>
      </div>
      <div class="actions-section">
        <h4>账号设置</h4>
        <div class="action-list">
          <button class="action-item" @click="goToModifyPassword">
            <Key :size="18" />
            <span>修改密码</span>
            <ChevronRight :size="18" />
          </button>
          <button class="action-item" @click="goToModifyEmail">
            <Mail :size="18" />
            <span>修改邮箱</span>
            <ChevronRight :size="18" />
          </button>
          <button class="action-item danger" @click="logout">
            <LogOut :size="18" />
            <span>退出登录</span>
          </button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { User, Key, Mail, ChevronRight, LogOut } from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()

const goToModifyPassword = () => {
  ElMessage.info('功能开发中')
}

const goToModifyEmail = () => {
  ElMessage.info('功能开发中')
}

const logout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.profile-page {
  padding-bottom: 24px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 24px;
}

.profile-card {
  max-width: 600px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  margin-bottom: 24px;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.user-info h3 {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 4px;
}

.user-info p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.info-section, .actions-section {
  margin-bottom: 24px;
}

.info-section:last-child, .actions-section:last-child {
  margin-bottom: 0;
}

.info-section h4, .actions-section h4 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
}

.info-item .label {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.info-item .value {
  font-size: 14px;
  font-weight: 500;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.action-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.action-item.danger:hover {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.2);
}
</style>

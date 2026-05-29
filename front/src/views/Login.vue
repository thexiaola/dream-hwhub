<template>
  <div class="login-page">
    <div class="background-effects">
      <div class="grid-bg"></div>
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="stars">
        <span v-for="n in 50" :key="n" class="star" :style="getStarStyle(n)"></span>
      </div>
    </div>
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <BookOpen :size="48" class="login-icon" />
          <h1 class="login-title">作业管理系统</h1>
          <p class="login-subtitle">欢迎回来，请登录</p>
        </div>
        <el-form :model="form" :rules="rules" ref="formRef" class="login-form">
          <el-form-item prop="account">
            <el-input 
              v-model="form.account" 
              placeholder="账号"
              :prefix-icon="UserIcon"
              class="input-field"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input 
              v-model="form.password" 
              type="password" 
              placeholder="密码"
              :prefix-icon="LockIcon"
              class="input-field"
            />
          </el-form-item>
          <el-form-item class="form-actions">
            <router-link to="/retrieve" class="forgot-link">忘记密码?</router-link>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="login-btn" @click="handleLogin" :loading="loading">
              登录
            </el-button>
          </el-form-item>
        </el-form>
        <div class="register-link">
          <span>还没有账号?</span>
          <router-link to="/register">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, h } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { BookOpen, User, Lock } from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()

const form = ref({
  account: '',
  password: ''
})

const formRef = ref()
const loading = ref(false)

const rules = {
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const UserIcon = () => h(User, { size: 18 })
const LockIcon = () => h(Lock, { size: 18 })

const getStarStyle = (n: number) => ({
  left: `${Math.random() * 100}%`,
  top: `${Math.random() * 100}%`,
  animationDelay: `${Math.random() * 3}s`,
  animationDuration: `${3 + Math.random() * 4}s`
})

const handleLogin = async () => {
  if (!form.value.account || !form.value.password) {
    ElMessage.error('请填写账号和密码')
    return
  }
  
  loading.value = true
  
  try {
    const result = await userStore.login(form.value.account, form.value.password)
    if (result.code === 200) {
      ElMessage.success('登录成功')
      router.push('/dashboard')
    } else {
      ElMessage.error(result.message)
      form.value.password = ''
    }
  } catch (error) {
    ElMessage.error('登录失败')
    form.value.password = ''
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
}

.background-effects {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.grid-bg {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(rgba(102, 126, 234, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(102, 126, 234, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
}

.orb-1 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  top: -100px;
  right: -100px;
  animation: float1 6s ease-in-out infinite;
}

.orb-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #764ba2, #f093fb);
  bottom: -50px;
  left: -50px;
  animation: float2 8s ease-in-out infinite;
}

@keyframes float1 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-20px, 20px); }
}

@keyframes float2 {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(20px, -20px); }
}

.stars {
  position: absolute;
  inset: 0;
}

.star {
  position: absolute;
  width: 2px;
  height: 2px;
  background: white;
  border-radius: 50%;
  animation: twinkle 3s ease-in-out infinite;
}

@keyframes twinkle {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}

.login-container {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 420px;
  padding: 0 20px;
}

.login-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 40px;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-icon {
  color: #667eea;
  margin-bottom: 16px;
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
}

.login-subtitle {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.login-form {
  margin-bottom: 24px;
}

.input-field {
  width: 100%;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 0;
}

.forgot-link {
  color: #667eea;
  font-size: 14px;
  text-decoration: none;
}

.forgot-link:hover {
  text-decoration: underline;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
}

.register-link {
  text-align: center;
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.register-link a {
  color: #667eea;
  margin-left: 8px;
  text-decoration: none;
}

.register-link a:hover {
  text-decoration: underline;
}
</style>

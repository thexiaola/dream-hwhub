<template>
  <div class="register-page">
    <div class="background-effects">
      <div class="grid-bg"></div>
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
    </div>
    <div class="register-container">
      <div class="register-card">
        <div class="register-header">
          <BookOpen :size="48" class="register-icon" />
          <h1 class="register-title">创建账号</h1>
          <p class="register-subtitle">加入我们的作业管理系统</p>
        </div>
        <el-form :model="form" :rules="rules" ref="formRef" class="register-form">
          <el-form-item prop="username">
            <el-input 
              v-model="form.username" 
              placeholder="姓名"
              :prefix-icon="UserIcon"
              class="input-field"
            />
          </el-form-item>
          <el-form-item prop="userNo">
            <el-input 
              v-model="form.userNo" 
              placeholder="学号/工号"
              :prefix-icon="CreditCardIcon"
              class="input-field"
            />
          </el-form-item>
          <el-form-item prop="email">
            <el-input 
              v-model="form.email" 
              type="email" 
              placeholder="邮箱"
              :prefix-icon="MailIcon"
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
          <el-form-item prop="code">
            <el-row :gutter="12">
              <el-col :span="16">
                <el-input 
                  v-model="form.code" 
                  placeholder="验证码"
                  :prefix-icon="KeyIcon"
                  class="input-field"
                />
              </el-col>
              <el-col :span="8">
                <el-button 
                  type="primary" 
                  class="code-btn"
                  :disabled="countdown > 0"
                  @click="sendVerifyCode"
                >
                  {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                </el-button>
              </el-col>
            </el-row>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="register-btn" @click="handleRegister" :loading="loading">
              立即注册
            </el-button>
          </el-form-item>
        </el-form>
        <div class="login-link">
          <span>已有账号?</span>
          <router-link to="/login">立即登录</router-link>
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
import { BookOpen, User, CreditCard, Mail, Lock, Key } from '@lucide/vue'

const router = useRouter()
const userStore = useUserStore()

const form = ref({
  username: '',
  userNo: '',
  email: '',
  password: '',
  code: ''
})

const loading = ref(false)
const countdown = ref(0)

const rules = {
  username: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  userNo: [
    { required: true, message: '请输入学号/工号', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' },
    { max: 32, message: '密码长度不能超过32位', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

const UserIcon = () => h(User, { size: 18 })
const CreditCardIcon = () => h(CreditCard, { size: 18 })
const MailIcon = () => h(Mail, { size: 18 })
const LockIcon = () => h(Lock, { size: 18 })
const KeyIcon = () => h(Key, { size: 18 })

const sendVerifyCode = async () => {
  if (!form.value.email) {
    ElMessage.error('请先输入邮箱')
    return
  }
  if (!form.value.userNo) {
    ElMessage.error('请先输入学号/工号')
    return
  }
  if (!form.value.username) {
    ElMessage.error('请先输入姓名')
    return
  }
  
  const result = await userStore.sendCode(form.value.email, form.value.userNo, form.value.username)
  if (result.code === 200) {
    ElMessage.success('验证码已发送')
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } else {
    ElMessage.error(result.message)
  }
}

const handleRegister = async () => {
  loading.value = true
  
  try {
    const result = await userStore.register({
      username: form.value.username,
      userNo: form.value.userNo,
      email: form.value.email,
      password: form.value.password,
      code: form.value.code
    })
    
    if (result.code === 200) {
      ElMessage.success('注册成功')
      router.push('/login')
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    ElMessage.error('注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
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

.register-container {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 480px;
  padding: 0 20px;
}

.register-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 40px;
}

.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.register-icon {
  color: #667eea;
  margin-bottom: 16px;
}

.register-title {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
}

.register-subtitle {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.register-form {
  margin-bottom: 24px;
}

.input-field {
  width: 100%;
}

.code-btn {
  width: 100%;
  height: 40px;
}

.register-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
}

.login-link {
  text-align: center;
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.login-link a {
  color: #667eea;
  margin-left: 8px;
  text-decoration: none;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>

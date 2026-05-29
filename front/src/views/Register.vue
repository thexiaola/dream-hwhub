<template>
  <div class="register-container">
    <div class="bg-decoration">
      <div class="grid-lines"></div>
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="stars"></div>
    </div>
    
    <div class="register-card">
      <div class="register-header">
        <div class="logo">
          <div class="logo-icon">
            <component :is="componentMap['GraduationCap']" />
          </div>
          <span class="logo-text">作业管理系统</span>
        </div>
        <p class="subtitle">创建您的账号，开始学习之旅</p>
      </div>
      
      <el-form ref="formRef" :model="form" class="register-form">
        <el-form-item prop="username">
          <div class="input-wrapper">
            <component :is="componentMap['User']" class="input-icon" />
            <el-input 
              v-model="form.username" 
              placeholder="请输入用户名"
              class="custom-input"
            />
          </div>
        </el-form-item>
        
        <el-form-item prop="userNo">
          <div class="input-wrapper">
            <component :is="componentMap['Postcard']" class="input-icon" />
            <el-input 
              v-model="form.userNo" 
              placeholder="请输入学号/工号"
              class="custom-input"
            />
          </div>
        </el-form-item>
        
        <el-form-item prop="email">
          <div class="input-wrapper">
            <component :is="componentMap['Message']" class="input-icon" />
            <el-input 
              v-model="form.email" 
              type="email"
              placeholder="请输入邮箱"
              class="custom-input"
            />
          </div>
        </el-form-item>
        
        <el-form-item prop="password">
          <div class="input-wrapper">
            <component :is="componentMap['Lock']" class="input-icon" />
            <el-input 
              v-model="form.password" 
              type="password" 
              placeholder="请输入密码（8-32位，需包含大小写字母和数字）"
              class="custom-input"
              show-password
            />
          </div>
        </el-form-item>
        
        <el-form-item prop="confirmPassword">
          <div class="input-wrapper">
            <component :is="componentMap['Lock']" class="input-icon" />
            <el-input 
              v-model="form.confirmPassword" 
              type="password" 
              placeholder="请确认密码"
              class="custom-input"
              show-password
            />
          </div>
        </el-form-item>
        
        <el-form-item prop="code">
          <div class="code-row">
            <div class="input-wrapper code-input">
              <component :is="componentMap['Key']" class="input-icon" />
              <el-input 
                v-model="form.code" 
                placeholder="请输入验证码"
                class="custom-input"
              />
            </div>
            <el-button 
              type="primary" 
              @click="sendCode" 
              :disabled="!canSendCode"
              :loading="sendingCode"
              class="code-btn"
            >
              {{ codeButtonText }}
            </el-button>
          </div>
        </el-form-item>
        
        <el-form-item class="register-actions">
          <el-button type="primary" @click="handleRegister" :loading="loading" class="register-btn">
            注 册
          </el-button>
        </el-form-item>
        
        <div class="register-links">
          <router-link to="/login" class="link">已有账号，立即登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import { User, CreditCard, Mail, Lock, Key, BookOpen } from 'lucide-vue-next'

const componentMap = {
  User,
  Postcard: CreditCard,
  Message: Mail,
  Lock,
  Key,
  BookOpen
}

const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  username: '',
  userNo: '',
  email: '',
  password: '',
  confirmPassword: '',
  code: ''
})

const loading = ref(false)
const sendingCode = ref(false)
const codeCountdown = ref(0)

const canSendCode = computed(() => {
  return form.email && form.userNo && form.username && codeCountdown.value === 0
})

const codeButtonText = computed(() => {
  if (codeCountdown.value > 0) {
    return `${codeCountdown.value}s`
  }
  return '发送验证码'
})

async function sendCode() {
  if (!canSendCode.value) {
    ElMessage.warning('请先填写邮箱、学号和用户名')
    return
  }
  
  sendingCode.value = true
  
  try {
    const response = await userStore.sendRegisterCode({
      email: form.email,
      userNo: form.userNo,
      username: form.username
    })
    
    if (response.code === 200) {
      ElMessage.success('验证码已发送')
      codeCountdown.value = 60
      startCountdown()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(error.message || '发送失败')
  } finally {
    sendingCode.value = false
  }
}

function startCountdown() {
  const timer = setInterval(() => {
    codeCountdown.value--
    if (codeCountdown.value === 0) {
      clearInterval(timer)
    }
  }, 1000)
}

async function handleRegister() {
  if (!form.username || !form.userNo || !form.email || !form.password || !form.code) {
    ElMessage.error('请填写所有必填项')
    return
  }
  
  if (form.password !== form.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  
  const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d]{8,32}$/
  if (!passwordRegex.test(form.password)) {
    ElMessage.error('密码需包含大小写字母和数字，长度8-32位')
    return
  }
  
  loading.value = true
  
  try {
    const response = await userStore.register({
      username: form.username,
      userNo: form.userNo,
      email: form.email,
      password: form.password,
      code: form.code
    })
    
    if (response.code === 200) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(error.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(180deg, #0a0a1a 0%, #1a1a3a 50%, #0a0a1a 100%);
  position: relative;
  overflow: hidden;
}

.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.grid-lines {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    linear-gradient(rgba(102, 126, 234, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(102, 126, 234, 0.05) 1px, transparent 1px);
  background-size: 60px 60px;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.6;
}

.orb-1 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #667eea 0%, transparent 100%);
  top: -100px;
  right: -100px;
  animation: float 8s ease-in-out infinite;
}

.orb-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #764ba2 0%, transparent 100%);
  bottom: -50px;
  left: -50px;
  animation: float 6s ease-in-out infinite reverse;
}

.stars {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    radial-gradient(2px 2px at 20px 30px, rgba(255, 255, 255, 0.4), transparent),
    radial-gradient(2px 2px at 40px 70px, rgba(255, 255, 255, 0.3), transparent),
    radial-gradient(1px 1px at 90px 40px, rgba(255, 255, 255, 0.5), transparent),
    radial-gradient(2px 2px at 130px 80px, rgba(255, 255, 255, 0.3), transparent),
    radial-gradient(1px 1px at 160px 120px, rgba(255, 255, 255, 0.4), transparent);
  background-repeat: repeat;
  background-size: 450px 200px;
}

.register-card {
  width: 480px;
  padding: 48px;
  background: rgba(18, 18, 42, 0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 20px;
  box-shadow: 
    0 20px 60px rgba(0, 0, 0, 0.5),
    0 0 40px rgba(102, 126, 234, 0.1);
  position: relative;
  z-index: 10;
}

.register-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
}

.logo-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.logo-text {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  color: #a0a0c0;
  font-size: 14px;
}

.register-form {
  margin-top: 20px;
}

.input-wrapper {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 12px;
  padding: 0 16px;
  transition: all 0.3s ease;
}

.input-wrapper:focus-within {
  border-color: #667eea;
  box-shadow: 0 0 20px rgba(102, 126, 234, 0.2);
}

.input-icon {
  color: #667eea;
  font-size: 18px;
  margin-right: 12px;
}

.custom-input {
  flex: 1;
  background: transparent;
  border: none;
  color: white;
  font-size: 14px;
  padding: 14px 0;
}

.custom-input::placeholder {
  color: #606080;
}

.code-row {
  display: flex;
  gap: 12px;
}

.code-input {
  flex: 1;
}

.code-btn {
  width: 130px;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  color: white;
  transition: all 0.3s ease;
}

.code-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
}

.register-actions {
  margin-bottom: 24px;
}

.register-btn {
  width: 100%;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  color: white;
  transition: all 0.3s ease;
}

.register-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
}

.register-links {
  text-align: center;
}

.link {
  color: #a0a0c0;
  font-size: 14px;
  text-decoration: none;
  transition: color 0.3s ease;
}

.link:hover {
  color: #667eea;
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-20px); }
}
</style>
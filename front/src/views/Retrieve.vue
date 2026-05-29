<template>
  <div class="retrieve-container">
    <div class="bg-decoration">
      <div class="grid-lines"></div>
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="stars"></div>
    </div>
    
    <div class="retrieve-card">
      <div class="retrieve-header">
        <div class="logo">
          <div class="logo-icon">
            <component :is="componentMap['GraduationCap']" />
          </div>
          <span class="logo-text">作业管理系统</span>
        </div>
        <p class="subtitle">找回密码</p>
      </div>
      
      <div class="step-indicator">
        <div :class="['step', { active: currentStep >= 1 }, { done: currentStep > 1 }]">
          <span class="step-num">1</span>
          <span class="step-label">验证身份</span>
        </div>
        <div class="step-line"></div>
        <div :class="['step', { active: currentStep >= 2 }, { done: currentStep > 2 }]">
          <span class="step-num">2</span>
          <span class="step-label">设置新密码</span>
        </div>
      </div>
      
      <el-form v-if="currentStep === 1" :model="form" class="retrieve-form">
        <el-form-item prop="account">
          <div class="input-wrapper">
            <component :is="componentMap['User']" class="input-icon" />
            <el-input 
              v-model="form.account" 
              placeholder="请输入账号（学号/工号或邮箱）"
              class="custom-input"
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
              :disabled="!form.account"
              :loading="sendingCode"
              class="code-btn"
            >
              {{ codeButtonText }}
            </el-button>
          </div>
        </el-form-item>
        
        <el-form-item class="retrieve-actions">
          <el-button type="primary" @click="verifyAccount" :loading="loading" class="retrieve-btn">
            下一步
          </el-button>
        </el-form-item>
      </el-form>
      
      <el-form v-else :model="form" class="retrieve-form">
        <el-form-item prop="password">
          <div class="input-wrapper">
            <component :is="componentMap['Lock']" class="input-icon" />
            <el-input 
              v-model="form.password" 
              type="password" 
              placeholder="请输入新密码（8-32位，需包含大小写字母和数字）"
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
              placeholder="请确认新密码"
              class="custom-input"
              show-password
            />
          </div>
        </el-form-item>
        
        <el-form-item class="retrieve-actions">
          <el-button type="primary" @click="resetPassword" :loading="loading" class="retrieve-btn">
            确认修改
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="retrieve-links">
        <router-link to="/login" class="link">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import { User, Lock, Key, BookOpen } from 'lucide-vue-next'

const componentMap = {
  User,
  Lock,
  Key,
  BookOpen
}

const router = useRouter()
const userStore = useUserStore()

const currentStep = ref(1)
const loading = ref(false)
const sendingCode = ref(false)
const codeCountdown = ref(0)

const form = reactive({
  account: '',
  code: '',
  password: '',
  confirmPassword: ''
})

const codeButtonText = computed(() => {
  if (codeCountdown.value > 0) {
    return `${codeCountdown.value}s`
  }
  return '发送验证码'
})

async function sendCode() {
  if (!form.account) {
    ElMessage.warning('请先填写账号')
    return
  }
  
  sendingCode.value = true
  
  try {
    const response = await userStore.sendRetrieveCode({ account: form.account })
    
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

async function verifyAccount() {
  if (!form.account || !form.code) {
    ElMessage.error('请填写账号和验证码')
    return
  }
  
  loading.value = true
  
  try {
    const response = await userStore.verifyRetrieveCode({
      account: form.account,
      code: form.code
    })
    
    if (response.code === 200) {
      currentStep.value = 2
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(error.message || '验证失败')
  } finally {
    loading.value = false
  }
}

async function resetPassword() {
  if (!form.password || !form.confirmPassword) {
    ElMessage.error('请填写密码')
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
    const response = await userStore.retrievePassword({
      account: form.account,
      code: form.code,
      password: form.password
    })
    
    if (response.code === 200) {
      ElMessage.success('密码修改成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(error.message || '修改失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.retrieve-container {
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

.retrieve-card {
  width: 450px;
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

.retrieve-header {
  text-align: center;
  margin-bottom: 32px;
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

.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 32px;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.step-num {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  border: 2px solid rgba(102, 126, 234, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: #606080;
  transition: all 0.3s ease;
}

.step.active .step-num {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: #667eea;
  color: white;
}

.step.done .step-num {
  background: #28a745;
  border-color: #28a745;
  color: white;
}

.step-label {
  font-size: 12px;
  color: #606080;
}

.step.active .step-label,
.step.done .step-label {
  color: #a0a0c0;
}

.step-line {
  width: 60px;
  height: 2px;
  background: rgba(102, 126, 234, 0.3);
}

.retrieve-form {
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

.retrieve-actions {
  margin-bottom: 24px;
}

.retrieve-btn {
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

.retrieve-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
}

.retrieve-links {
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
<template>
  <div class="retrieve-page">
    <div class="background-effects">
      <div class="grid-bg"></div>
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
    </div>
    <div class="retrieve-container">
      <div class="retrieve-card">
        <div class="retrieve-header">
          <BookOpen :size="48" class="retrieve-icon" />
          <h1 class="retrieve-title">找回密码</h1>
          <p class="retrieve-subtitle">请按照提示步骤操作</p>
        </div>
        <div class="steps">
          <div :class="['step', { active: currentStep === 1 }, { completed: currentStep > 1 }]">
            <span class="step-number">1</span>
            <span class="step-text">输入账号</span>
          </div>
          <div class="step-line"></div>
          <div :class="['step', { active: currentStep === 2 }, { completed: currentStep > 2 }]">
            <span class="step-number">2</span>
            <span class="step-text">验证身份</span>
          </div>
          <div class="step-line"></div>
          <div :class="['step', { active: currentStep === 3 }]">
            <span class="step-number">3</span>
            <span class="step-text">重置密码</span>
          </div>
        </div>
        <el-form v-if="currentStep === 1" :model="step1Form" class="retrieve-form">
          <el-form-item>
            <el-input 
              v-model="step1Form.account" 
              placeholder="请输入账号"
              :prefix-icon="UserIcon"
              class="input-field"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="retrieve-btn" @click="goToStep2" :loading="loading">
              下一步
            </el-button>
          </el-form-item>
        </el-form>
        <el-form v-if="currentStep === 2" :model="step2Form" class="retrieve-form">
          <el-form-item>
            <el-input 
              v-model="step2Form.email" 
              placeholder="请输入绑定的邮箱"
              :prefix-icon="MailIcon"
              class="input-field"
            />
          </el-form-item>
          <el-form-item>
            <el-row :gutter="12">
              <el-col :span="16">
                <el-input 
                  v-model="step2Form.verifyCode" 
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
          <el-form-item class="form-actions">
            <el-button @click="goToStep1">上一步</el-button>
            <el-button type="primary" @click="goToStep3" :loading="loading">下一步</el-button>
          </el-form-item>
        </el-form>
        <el-form v-if="currentStep === 3" :model="step3Form" class="retrieve-form">
          <el-form-item>
            <el-input 
              v-model="step3Form.password" 
              type="password" 
              placeholder="请输入新密码"
              :prefix-icon="LockIcon"
              class="input-field"
            />
          </el-form-item>
          <el-form-item>
            <el-input 
              v-model="step3Form.confirmPassword" 
              type="password" 
              placeholder="请确认新密码"
              :prefix-icon="LockIcon"
              class="input-field"
            />
          </el-form-item>
          <el-form-item class="form-actions">
            <el-button @click="goToStep2">上一步</el-button>
            <el-button type="primary" @click="resetPassword" :loading="loading">确认重置</el-button>
          </el-form-item>
        </el-form>
        <div class="login-link">
          <router-link to="/login">返回登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, h } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { BookOpen, User, Mail, Key, Lock } from '@lucide/vue'
import { post } from '@/utils/http'

const router = useRouter()

const currentStep = ref(1)
const loading = ref(false)
const countdown = ref(0)

const step1Form = ref({
  account: ''
})

const step2Form = ref({
  email: '',
  verifyCode: ''
})

const step3Form = ref({
  password: '',
  confirmPassword: ''
})

const UserIcon = () => h(User, { size: 18 })
const MailIcon = () => h(Mail, { size: 18 })
const KeyIcon = () => h(Key, { size: 18 })
const LockIcon = () => h(Lock, { size: 18 })

const goToStep1 = () => {
  currentStep.value = 1
}

const goToStep2 = () => {
  if (!step1Form.value.account) {
    ElMessage.error('请输入账号')
    return
  }
  currentStep.value = 2
}

const goToStep3 = () => {
  if (!step2Form.value.email || !step2Form.value.verifyCode) {
    ElMessage.error('请填写完整信息')
    return
  }
  currentStep.value = 3
}

const sendVerifyCode = async () => {
  if (!step2Form.value.email) {
    ElMessage.error('请先输入邮箱')
    return
  }
  
  const result = await post('/users/retrieve-password/code', { 
    account: step1Form.value.account,
    email: step2Form.value.email 
  })
  
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

const resetPassword = async () => {
  if (!step3Form.value.password || !step3Form.value.confirmPassword) {
    ElMessage.error('请填写完整信息')
    return
  }
  
  if (step3Form.value.password !== step3Form.value.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  
  loading.value = true
  
  try {
    const result = await post('/users/retrieve-password/modify', {
      account: step1Form.value.account,
      email: step2Form.value.email,
      verifyCode: step2Form.value.verifyCode,
      password: step3Form.value.password
    })
    
    if (result.code === 200) {
      ElMessage.success('密码重置成功')
      router.push('/login')
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    ElMessage.error('密码重置失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.retrieve-page {
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

.retrieve-container {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 480px;
  padding: 0 20px;
}

.retrieve-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 40px;
}

.retrieve-header {
  text-align: center;
  margin-bottom: 32px;
}

.retrieve-icon {
  color: #667eea;
  margin-bottom: 16px;
}

.retrieve-title {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
}

.retrieve-subtitle {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.steps {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32px;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.step-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
}

.step.active .step-number {
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.step.completed .step-number {
  background: rgba(102, 126, 234, 0.3);
}

.step-text {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.step-line {
  width: 40px;
  height: 2px;
  background: rgba(255, 255, 255, 0.1);
  margin: 0 8px;
}

.retrieve-form {
  margin-bottom: 24px;
}

.input-field {
  width: 100%;
}

.code-btn {
  width: 100%;
  height: 40px;
}

.retrieve-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
}

.form-actions {
  display: flex;
  justify-content: space-between;
}

.login-link {
  text-align: center;
}

.login-link a {
  color: #667eea;
  font-size: 14px;
  text-decoration: none;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>

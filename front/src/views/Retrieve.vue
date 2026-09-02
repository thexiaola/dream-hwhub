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
          <p class="retrieve-subtitle">请输入用户名或邮箱，验证码将自动发送至绑定的邮箱</p>
        </div>
        <div class="steps">
          <div :class="['step', { active: currentStep === 1 }, { completed: currentStep > 1 }]">
            <span class="step-number">1</span>
            <span class="step-text">输入账号</span>
          </div>
          <div class="step-line"></div>
          <div :class="['step', { active: currentStep === 2 }]">
            <span class="step-number">2</span>
            <span class="step-text">重置密码</span>
          </div>
        </div>
        <el-form v-if="currentStep === 1" class="retrieve-form" @submit.prevent="sendCode">
          <el-form-item>
            <el-input
              v-model="account"
              placeholder="请输入用户名或邮箱"
              :prefix-icon="UserIcon"
              class="input-field"
              @keyup.enter="sendCode"
            />
          </el-form-item>
          <p class="form-hint">验证码将自动发送至该账号绑定的邮箱，请先登录邮箱查收</p>
          <el-form-item>
            <el-button type="primary" class="retrieve-btn" :loading="sending" @click="sendCode">
              {{ countdown > 0 ? `验证码已发送，${countdown}s` : '获取验证码' }}
            </el-button>
          </el-form-item>
        </el-form>
        <el-form v-else class="retrieve-form" @submit.prevent="resetPassword">
          <p class="form-hint">
            <Mail :size="14" class="form-hint-icon" />
            验证码已发送至账号「{{ account }}」绑定的邮箱
          </p>
          <el-form-item>
            <div class="code-row">
              <el-input
                v-model="code"
                placeholder="请输入邮箱中的验证码"
                :prefix-icon="KeyIcon"
                class="input-field"
                @keyup.enter="resetPassword"
              />
              <el-button
                type="primary"
                class="code-btn"
                :disabled="countdown > 0 || sending"
                :loading="sending"
                @click="sendCode"
              >
                {{ countdown > 0 ? `${countdown}s` : '重新发送' }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="password"
              type="password"
              placeholder="请输入新密码"
              :prefix-icon="LockIcon"
              class="input-field"
              @keyup.enter="resetPassword"
            />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="confirmPassword"
              type="password"
              placeholder="请确认新密码"
              :prefix-icon="LockIcon"
              class="input-field"
              @keyup.enter="resetPassword"
            />
          </el-form-item>
          <el-form-item class="form-actions">
            <el-button @click="goToStep1">上一步</el-button>
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
import { post, put } from '@/utils/http'

const router = useRouter()

const currentStep = ref(1)
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const startCountdown = (seconds: number) => {
  countdown.value = seconds
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer!)
      countdownTimer = null
    }
  }, 1000)
}

const account = ref('')
const code = ref('')
const password = ref('')
const confirmPassword = ref('')

const UserIcon = () => h(User, { size: 18 })
const KeyIcon = () => h(Key, { size: 18 })
const LockIcon = () => h(Lock, { size: 18 })

const goToStep1 = () => {
  currentStep.value = 1
}

/**
 * 向该账号绑定的邮箱发送验证码（首次发送成功后进入重置密码步骤）
 */
const sendCode = async () => {
  if (sending.value || countdown.value > 0) {
    return
  }
  if (!account.value.trim()) {
    ElMessage.error('请输入用户名或邮箱')
    return
  }

  sending.value = true
  try {
    const result = await Promise.race([
      post('/users/retrieve/sendcode', {
        account: account.value.trim()
      }).catch(() => ({ code: -1, message: '网络请求失败', data: null })),
      // 10 秒内未收到后端回复则自动释放等待状态
      new Promise<{ code: number; message: string; data: null }>((resolve) =>
        setTimeout(() => resolve({ code: -1, message: '请求超时，请稍后重试', data: null }), 10000)
      )
    ])
    // 冷却时长由后端返回：成功时为配置值，冷却期内为剩余秒数
    if (typeof result.data === 'number' && result.data > 0) {
      startCountdown(result.data)
    }
    if (result.code === 200) {
      ElMessage.success(result.message || '验证码已发送')
      if (currentStep.value === 1) {
        currentStep.value = 2
      }
    } else {
      ElMessage.error(result.message || '发送失败，请稍后重试')
    }
  } catch (error) {
    ElMessage.error('发送失败，请检查网络后重试')
  } finally {
    sending.value = false
  }
}

const resetPassword = async () => {
  if (!code.value || !password.value || !confirmPassword.value) {
    ElMessage.error('请填写完整信息')
    return
  }

  if (password.value !== confirmPassword.value) {
    ElMessage.error('两次输入的密码不一致')
    return
  }

  if (password.value.length < 4 || password.value.length > 48) {
    ElMessage.error('新密码长度需在 4-48 位之间')
    return
  }

  const pattern = /^[0-9a-zA-Z!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]+$/
  if (!pattern.test(password.value)) {
    ElMessage.error('新密码只能包含字母、数字和常用特殊字符')
    return
  }

  loading.value = true

  try {
    const result = await put('/users/retrieve/resetpassword', {
      account: account.value.trim(),
      code: code.value,
      newPassword: password.value
    })

    if (result.code === 200) {
      ElMessage.success('密码重置成功')
      router.push('/login')
    } else {
      ElMessage.error(result.message || '密码重置失败')
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
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.05);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
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
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
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
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
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
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.6);
}

.step-line {
  width: 40px;
  height: 2px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  margin: 0 8px;
}

.retrieve-form {
  margin-bottom: 24px;
}

.input-field {
  width: 100%;
}

.form-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: center;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.55);
  font-size: 13px;
  line-height: 1.6;
  margin: -8px 0 16px;
  text-align: center;
}

.form-hint-icon {
  flex-shrink: 0;
}

.code-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.code-row > :deep(.el-input) {
  flex: 1;
}

.code-btn {
  width: 120px;
  flex-shrink: 0;
}

.retrieve-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
}

.form-actions {
  margin-bottom: 0;
}

.form-actions :deep(.el-form-item__content) {
  display: flex;
  justify-content: center;
  gap: 32px;
}

.form-actions :deep(.el-button) {
  margin-left: 0;
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

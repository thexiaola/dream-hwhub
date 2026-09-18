<template>
  <AuthShell badge="重置密码" title="找回密码" subtitle="验证绑定邮箱后即可设置新密码">
    <div class="auth-steps">
      <div :class="['auth-step', { active: currentStep === 1, completed: currentStep > 1 }]">
        <span class="auth-step-number">1</span>
        <span class="auth-step-text">输入账号</span>
      </div>
      <div class="auth-step-line"></div>
      <div :class="['auth-step', { active: currentStep === 2 }]">
        <span class="auth-step-number">2</span>
        <span class="auth-step-text">重置密码</span>
      </div>
    </div>

    <el-form v-if="currentStep === 1" class="auth-form" @submit.prevent="sendCode">
      <el-form-item>
        <el-input
          v-model="account"
          size="large"
          placeholder="用户名 / 邮箱"
          class="auth-input"
        >
          <template #prefix>
            <span class="auth-input-icon">
              <User :size="16" />
            </span>
          </template>
        </el-input>
      </el-form-item>

      <p class="auth-hint">验证码将发送至该账号绑定的邮箱，请先登录邮箱查收</p>

      <el-form-item>
        <el-button
          type="primary"
          class="auth-submit"
          :loading="sending"
          @click="sendCode"
        >
          <span class="auth-submit-text">
            {{ countdown > 0 ? `验证码已发送 ${countdown}s` : '获取验证码' }}
          </span>
        </el-button>
      </el-form-item>
    </el-form>

    <el-form v-else class="auth-form" @submit.prevent="resetPassword">
      <p class="auth-hint">
        <Mail :size="14" />
        验证码已发送至账号「{{ account }}」绑定的邮箱
      </p>

      <el-form-item>
        <div class="auth-code-row">
          <el-input
            v-model="code"
            size="large"
            placeholder="邮箱验证码"
            class="auth-input"
            @keyup.enter="resetPassword"
          >
            <template #prefix>
              <span class="auth-input-icon">
                <Key :size="16" />
              </span>
            </template>
          </el-input>
          <el-button
            type="primary"
            class="auth-code-btn"
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
          size="large"
          placeholder="新密码"
          show-password
          class="auth-input"
          @keyup.enter="resetPassword"
        >
          <template #prefix>
            <span class="auth-input-icon">
              <Lock :size="16" />
            </span>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item>
        <el-input
          v-model="confirmPassword"
          type="password"
          size="large"
          placeholder="确认新密码"
          show-password
          class="auth-input"
          @keyup.enter="resetPassword"
        >
          <template #prefix>
            <span class="auth-input-icon">
              <Lock :size="16" />
            </span>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item class="auth-form-actions">
        <el-button @click="goToStep1">上一步</el-button>
        <el-button type="primary" :loading="loading" @click="resetPassword">确认重置</el-button>
      </el-form-item>
    </el-form>

    <div class="auth-links auth-links--center">
      <router-link to="/login" class="auth-link">返回登录</router-link>
    </div>
  </AuthShell>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Mail, Key, Lock } from '@lucide/vue'
import { post, put } from '@/utils/http'
import AuthShell from '@/components/AuthShell.vue'

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
  // 回车提交与按钮点击共用此函数，避免请求期间重复触发
  if (loading.value) {
    return
  }

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

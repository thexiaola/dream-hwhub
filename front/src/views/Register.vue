<template>
  <AuthShell badge="新用户注册" title="注 册" subtitle="填写以下信息即可创建账号">
    <el-form ref="formRef" :model="form" :rules="rules" class="auth-form" @submit.prevent="handleRegister">
      <el-form-item prop="username">
        <el-input
          v-model="form.username"
          size="large"
          placeholder="用户名"
          maxlength="16"
          class="auth-input"
        >
          <template #prefix>
            <span class="auth-input-icon">
              <User :size="16" />
            </span>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="userNo">
        <el-input
          v-model="form.userNo"
          size="large"
          placeholder="学号 / 工号"
          class="auth-input"
        >
          <template #prefix>
            <span class="auth-input-icon">
              <CreditCard :size="16" />
            </span>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="email">
        <el-input
          v-model="form.email"
          type="email"
          size="large"
          placeholder="邮箱"
          class="auth-input"
        >
          <template #prefix>
            <span class="auth-input-icon">
              <Mail :size="16" />
            </span>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="password">
        <el-input
          v-model="form.password"
          type="password"
          size="large"
          placeholder="密码"
          show-password
          class="auth-input"
        >
          <template #prefix>
            <span class="auth-input-icon">
              <Lock :size="16" />
            </span>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="code">
        <div class="auth-code-row">
          <el-input
            ref="codeInputRef"
            v-model="form.code"
            size="large"
            placeholder="邮箱验证码"
            class="auth-input"
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
            @click="sendVerifyCode"
          >
            {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
          </el-button>
        </div>
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          class="auth-submit"
          :loading="loading"
          @click="handleRegister"
        >
          <span class="auth-submit-text">立即注册</span>
        </el-button>
      </el-form-item>
    </el-form>

    <div class="auth-links auth-links--center">
      <span>已有账号？</span>
      <router-link to="/login" class="auth-link">立即登录</router-link>
    </div>
  </AuthShell>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { FormInstance, InputInstance } from 'element-plus'
import { User, CreditCard, Mail, Lock, Key } from '@lucide/vue'
import AuthShell from '@/components/AuthShell.vue'

const router = useRouter()
const userStore = useUserStore()

const form = ref({
  username: '',
  userNo: '',
  email: '',
  password: '',
  code: ''
})

const formRef = ref<FormInstance>()
const codeInputRef = ref<InputInstance>()
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

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 16, message: '用户名长度需为 3-16 位', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9].*[A-Za-z0-9]$/, message: '用户名不能以下划线开头或结尾', trigger: 'blur' }
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
    { min: 4, message: '密码长度不能少于4位', trigger: 'blur' },
    { max: 48, message: '密码长度不能超过48位', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

/**
 * 清除验证码输入框的校验提示（表单校验异步返回，稍后再清一次，避免提示被写回）
 */
const clearCodeValidate = () => {
  formRef.value?.clearValidate('code')
  setTimeout(() => formRef.value?.clearValidate('code'), 0)
}

/** 将焦点移到验证码输入框 */
const focusCodeInput = () => {
  nextTick(() => codeInputRef.value?.focus())
}

const sendVerifyCode = async () => {
  if (sending.value || countdown.value > 0) {
    return
  }
  clearCodeValidate()
  // 聚焦验证码输入框，方便直接录入验证码
  focusCodeInput()
  if (!form.value.email) {
    ElMessage.error('请先输入邮箱')
    return
  }
  if (!form.value.userNo) {
    ElMessage.error('请先输入学号/工号')
    return
  }
  if (!form.value.username) {
    ElMessage.error('请先输入用户名')
    return
  }

  sending.value = true
  try {
    const result = await Promise.race([
      userStore.sendCode(form.value.email, form.value.userNo, form.value.username)
        .catch(() => ({ code: -1, message: '网络请求失败', data: null })),
      // 10 秒内未收到后端回复则自动释放等待状态
      new Promise<{ code: number; message: string; data: unknown }>((resolve) =>
        setTimeout(() => resolve({ code: -1, message: '请求超时，请稍后重试', data: null }), 10000)
      )
    ])
    if (result.code === -1) {
      ElMessage.error(result.message)
      return
    }
    if (result.code === 200) {
      ElMessage.success('验证码已发送')
    } else {
      ElMessage.error(result.message)
    }
    // 冷却时长由后端返回：成功时为配置值，冷却期内为剩余秒数
    if (typeof result.data === 'number' && result.data > 0) {
      startCountdown(result.data)
    }
  } finally {
    sending.value = false
  }
}

const handleRegister = async () => {
  // 回车提交与按钮点击共用此函数，避免请求期间重复触发
  if (loading.value) {
    return
  }

  loading.value = true

  try {
    const result = await userStore.register({
      username: form.value.username,
      userNo: form.value.userNo,
      email: form.value.email,
      password: form.value.password,
      emailCode: form.value.code
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

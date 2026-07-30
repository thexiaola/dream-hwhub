<template>
  <div class="profile-page">
    <div class="page-header">
      <h2>个人中心</h2>
    </div>

    <el-card class="profile-card" shadow="never">
      <template #header>
        <div class="profile-header">
          <div class="avatar">
            <User :size="48" />
          </div>
          <div class="user-info">
            <h3>{{ userStore.userInfo?.username }}</h3>
            <p class="account-row">
              <span>{{ userStore.userInfo?.userNo }}</span>
              <el-tag size="small" :type="roleTagType" effect="dark" round>
                {{ roleText }}
              </el-tag>
            </p>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="profile-tabs" stretch>
        <!-- ========================= 个人信息 ========================= -->
        <el-tab-pane label="修改个人信息" name="info">
          <el-form
            ref="infoFormRef"
            :model="infoForm"
            :rules="infoRules"
            label-width="100px"
            class="profile-form"
          >
            <el-form-item label="用户昵称" prop="username">
              <el-input v-model="infoForm.username" placeholder="请输入用户昵称" maxlength="64" show-word-limit />
            </el-form-item>
            <el-form-item label="学号/工号" prop="userNo">
              <el-input v-model="infoForm.userNo" placeholder="只允许数字" maxlength="24" show-word-limit />
            </el-form-item>
            <el-form-item label="真实姓名" prop="idName">
              <el-input v-model="infoForm.idName" placeholder="字母/汉字及空格、中点、连字符" maxlength="32" show-word-limit />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="infoForm.phone" placeholder="可选，数字 / + - ( ) 空格" maxlength="20" />
            </el-form-item>
            <el-form-item label="当前邮箱">
              <el-input v-model="currentEmail" disabled />
            </el-form-item>
            <el-form-item label="账号角色">
              <el-input :model-value="roleText" disabled />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="infoLoading" @click="submitInfo">
                <Save :size="14" />
                &nbsp;保存修改
              </el-button>
              <el-button @click="resetInfoForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- ========================= 修改邮箱 ========================= -->
        <el-tab-pane label="修改邮箱" name="email">
          <el-form
            ref="emailFormRef"
            :model="emailForm"
            :rules="emailRules"
            label-width="110px"
            class="profile-form"
          >
            <el-alert
              title="修改邮箱需要分别验证旧邮箱与新邮箱，两步请在 15 分钟内完成"
              type="warning"
              :closable="false"
              show-icon
              class="profile-alert"
            />
            <el-form-item label="当前邮箱">
              <el-input v-model="maskedCurrentEmail" disabled />
            </el-form-item>
            <el-form-item label="旧邮箱验证码" prop="beforeCode">
              <div class="code-row">
                <el-input v-model="emailForm.beforeCode" placeholder="请输入 6 位数字验证码" maxlength="6" />
                <el-button
                  type="primary"
                  plain
                  :disabled="beforeCountdown > 0 || emailSubmitting"
                  @click="sendBeforeCode"
                >
                  {{ beforeCountdown > 0 ? `${beforeCountdown}s 后重发` : '发送验证码至旧邮箱' }}
                </el-button>
              </div>
            </el-form-item>
            <el-divider content-position="left">新邮箱</el-divider>
            <el-form-item label="新邮箱地址" prop="newEmail">
              <el-input v-model="emailForm.newEmail" placeholder="请输入新邮箱地址" maxlength="64" />
            </el-form-item>
            <el-form-item label="新邮箱验证码" prop="afterCode">
              <div class="code-row">
                <el-input v-model="emailForm.afterCode" placeholder="请输入 6 位数字验证码" maxlength="6" />
                <el-button
                  type="primary"
                  plain
                  :disabled="afterCountdown > 0 || emailSubmitting || !emailForm.newEmail"
                  @click="sendAfterCode"
                >
                  {{ afterCountdown > 0 ? `${afterCountdown}s 后重发` : '发送验证码至新邮箱' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="emailSubmitting" @click="submitEmail">
                <MailCheck :size="14" />
                &nbsp;确认修改邮箱
              </el-button>
              <el-button @click="resetEmailForm">清空</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- ========================= 修改密码 ========================= -->
        <el-tab-pane label="修改密码" name="password">
          <el-form
            ref="pwdFormRef"
            :model="pwdForm"
            :rules="pwdRules"
            label-width="110px"
            class="profile-form"
          >
            <el-form-item label="当前登录密码" prop="oldPassword">
              <el-input
                v-model="pwdForm.oldPassword"
                type="password"
                show-password
                autocomplete="current-password"
                placeholder="请输入当前登录密码"
                maxlength="48"
              />
            </el-form-item>
            <el-divider content-position="left">新密码</el-divider>
            <el-form-item label="新登录密码" prop="newPassword">
              <el-input
                v-model="pwdForm.newPassword"
                type="password"
                show-password
                autocomplete="new-password"
                placeholder="6-48 位，字母 / 数字 / 常用特殊字符"
                maxlength="48"
              />
            </el-form-item>
            <el-form-item label="再次输入新密码" prop="confirmPassword">
              <el-input
                v-model="pwdForm.confirmPassword"
                type="password"
                show-password
                autocomplete="new-password"
                placeholder="请再次输入新密码"
                maxlength="48"
              />
            </el-form-item>
            <el-alert
              title="修改成功后将自动退出登录，并跳转到登录页重新登录"
              type="warning"
              :closable="false"
              show-icon
              class="profile-alert"
            />
            <el-form-item>
              <el-button type="primary" :loading="pwdSubmitting" @click="submitPassword">
                <LockKeyhole :size="14" />
                &nbsp;确认修改密码
              </el-button>
              <el-button @click="resetPwdForm">清空</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-card class="profile-card danger-zone" shadow="never">
      <template #header>
        <div class="danger-header">
          <AlertTriangle :size="18" class="danger-header-icon" />
          <span>账号安全</span>
        </div>
      </template>
      <div class="action-list">
        <button class="action-item danger" @click="handleLogout">
          <LogOut :size="18" />
          <span>退出登录</span>
          <ChevronRight :size="18" />
        </button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import type { UserInfo } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { put, post } from '@/utils/http'
import {
  AlertTriangle,
  ChevronRight,
  LockKeyhole,
  LogOut,
  MailCheck,
  Save,
  User,
} from '@lucide/vue'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('info')

// ========== 公用 ==========
const currentEmail = computed(() => userStore.userInfo?.email ?? '')

const roleText = computed(() => {
  const r = userStore.userInfo?.role
  if (r === 'teacher') return '教师'
  if (r === 'student') return '学生'
  if (userStore.userInfo && userStore.userInfo.permission >= 9) return '管理员'
  return '普通用户'
})

const roleTagType = computed(() => {
  const r = userStore.userInfo?.role
  if (r === 'teacher') return 'primary'
  if (r === 'student') return 'success'
  if (userStore.userInfo && userStore.userInfo.permission >= 9) return 'danger'
  return 'info'
})

const maskEmail = (email: string): string => {
  if (!email) return ''
  const at = email.indexOf('@')
  if (at <= 1) return email
  const name = email.slice(0, at)
  const tail = email.slice(at)
  const first = name[0]
  const last = name[name.length - 1]
  const stars = '*'.repeat(Math.max(1, name.length - 2))
  return `${first}${stars}${last}${tail}`
}

const maskedCurrentEmail = computed(() => maskEmail(currentEmail.value))

// 倒计时
let beforeTimer: number | null = null
let afterTimer: number | null = null
const beforeCountdown = ref(0)
const afterCountdown = ref(0)

const startCountdown = (target: 'before' | 'after') => {
  const seconds = 60
  const ref_ = target === 'before' ? beforeCountdown : afterCountdown
  const timer_ = target === 'before' ? (t: number | null) => { beforeTimer = t } : (t: number | null) => { afterTimer = t }
  const clear_ = target === 'before' ? () => beforeTimer && clearInterval(beforeTimer) : () => afterTimer && clearInterval(afterTimer)
  ref_.value = seconds
  clear_()
  const id = window.setInterval(() => {
    ref_.value -= 1
    if (ref_.value <= 0) {
      ref_.value = 0
      clear_()
      timer_(null)
    }
  }, 1000)
  timer_(id as unknown as number)
}

onBeforeUnmount(() => {
  if (beforeTimer) clearInterval(beforeTimer)
  if (afterTimer) clearInterval(afterTimer)
})

// ========== 修改个人信息 ==========
const infoFormRef = ref<FormInstance | null>(null)
const infoLoading = ref(false)

const infoForm = reactive({
  username: '',
  userNo: '',
  idName: '',
  phone: '',
})

const fillInfoForm = () => {
  const u = userStore.userInfo
  infoForm.username = u?.username ?? ''
  infoForm.userNo = u?.userNo ?? ''
  infoForm.idName = u?.idName ?? ''
  infoForm.phone = u?.phone ?? ''
}

fillInfoForm()

const infoRules: FormRules = {
  username: [
    { required: true, message: '请输入用户昵称', trigger: 'blur' },
    { max: 64, message: '昵称长度不能超过 64 位', trigger: 'blur' },
    { pattern: /^[^\r\n\t\f\v]+$/, message: '昵称不能包含换行符、制表符等特殊字符', trigger: 'blur' },
  ],
  userNo: [
    { required: true, message: '请输入学号/工号', trigger: 'blur' },
    { max: 24, message: '长度不能超过 24 位', trigger: 'blur' },
    { pattern: /^[0-9]+$/, message: '学号/工号只能包含数字', trigger: 'blur' },
  ],
  idName: [
    { max: 32, message: '长度不能超过 32 位', trigger: 'blur' },
    { pattern: /^[\p{L}\s·-]+$/u, message: '真实姓名只能包含字母、汉字及空格、中点、连字符', trigger: 'blur' },
  ],
  phone: [
    { max: 20, message: '手机号长度不能超过 20 位', trigger: 'blur' },
    { pattern: /^$|^[+]?[0-9()\-\s]+$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
}

const resetInfoForm = () => {
  fillInfoForm()
  infoFormRef.value?.resetFields()
}

const submitInfo = async () => {
  if (!infoFormRef.value) return
  try {
    await infoFormRef.value.validate()
  } catch {
    return
  }
  infoLoading.value = true
  try {
    const payload = {
      username: infoForm.username,
      userNo: infoForm.userNo,
      idName: infoForm.idName || undefined,
      phone: infoForm.phone || undefined,
    } as Record<string, unknown>
    const res = await put<UserInfo>('/users/modify/info', payload)
    if (res.code === 200 && res.data) {
      userStore.setUserInfo(res.data)
      ElMessage.success(res.message || '个人信息修改成功')
    } else {
      ElMessage.error(res.message || '修改失败，请稍后再试')
    }
  } catch {
    ElMessage.error('修改失败，请稍后再试')
  } finally {
    infoLoading.value = false
  }
}

// ========== 修改邮箱 ==========
const emailFormRef = ref<FormInstance | null>(null)
const emailSubmitting = ref(false)

const emailForm = reactive({
  beforeCode: '',
  newEmail: '',
  afterCode: '',
})

const validateEmail = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (!value) return callback(new Error('请输入新邮箱'))
  if (!/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(value)) {
    return callback(new Error('邮箱格式不正确'))
  }
  if (value.length > 64) return callback(new Error('邮箱长度不能超过 64 位'))
  if (value === currentEmail.value) return callback(new Error('新邮箱不能与当前邮箱相同'))
  callback()
}

const validateCode = (len = 6) => (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (!value) return callback(new Error('请输入验证码'))
  if (value.length !== len) return callback(new Error(`验证码长度为 ${len} 位`))
  if (!/^[0-9]+$/.test(value)) return callback(new Error('验证码只能包含数字'))
  callback()
}

const emailRules: FormRules = {
  beforeCode: [{ validator: validateCode(6), trigger: 'blur' }],
  afterCode: [{ validator: validateCode(6), trigger: 'blur' }],
  newEmail: [{ validator: validateEmail, trigger: 'blur' }],
}

const resetEmailForm = () => {
  emailForm.beforeCode = ''
  emailForm.newEmail = ''
  emailForm.afterCode = ''
  emailFormRef.value?.resetFields()
}

const sendBeforeCode = async () => {
  try {
    const res = await post<void>('/users/modify/getmodifycode/before')
    if (res.code === 200) {
      ElMessage.success(res.message || '验证码已发送至旧邮箱')
      startCountdown('before')
    } else {
      ElMessage.error(res.message || '验证码发送失败')
    }
  } catch {
    ElMessage.error('验证码发送失败，请稍后再试')
  }
}

const sendAfterCode = async () => {
  if (!emailForm.newEmail) {
    ElMessage.warning('请先填写新邮箱')
    return
  }
  try {
    const res = await post<void>('/users/modify/getmodifycode/after', { newEmail: emailForm.newEmail })
    if (res.code === 200) {
      ElMessage.success(res.message || '验证码已发送至新邮箱')
      startCountdown('after')
    } else {
      ElMessage.error(res.message || '验证码发送失败')
    }
  } catch {
    ElMessage.error('验证码发送失败，请稍后再试')
  }
}

const submitEmail = async () => {
  if (!emailFormRef.value) return
  try {
    await emailFormRef.value.validate()
  } catch {
    return
  }
  emailSubmitting.value = true
  try {
    const res = await put<UserInfo>('/users/modify/email', {
      beforeCode: emailForm.beforeCode,
      newEmail: emailForm.newEmail,
      afterCode: emailForm.afterCode,
    })
    if (res.code === 200) {
      if (res.data) userStore.setUserInfo(res.data)
      await userStore.refreshUserInfo()
      ElMessage.success(res.message || '邮箱修改成功')
      resetEmailForm()
    } else {
      ElMessage.error(res.message || '邮箱修改失败')
    }
  } catch {
    ElMessage.error('邮箱修改失败，请稍后再试')
  } finally {
    emailSubmitting.value = false
  }
}

// ========== 修改密码 ==========
const pwdFormRef = ref<FormInstance | null>(null)
const pwdSubmitting = ref(false)

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateNewPwd = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (!value) return callback(new Error('请输入新密码'))
  if (value.length < 6 || value.length > 48) return callback(new Error('新密码长度需在 6-48 位之间'))
  const pattern = /^[0-9a-zA-Z!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]+$/
  if (!pattern.test(value)) return callback(new Error('新密码只能包含字母、数字和常用特殊字符'))
  if (value === pwdForm.oldPassword) return callback(new Error('新密码不能与旧密码相同'))
  callback()
}

const validateConfirmPwd = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (!value) return callback(new Error('请再次输入新密码'))
  if (value !== pwdForm.newPassword) return callback(new Error('两次输入的新密码不一致'))
  callback()
}

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前登录密码', trigger: 'blur' }],
  newPassword: [{ validator: validateNewPwd, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPwd, trigger: 'blur' }],
}

const resetPwdForm = () => {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdFormRef.value?.resetFields()
}

const submitPassword = async () => {
  if (!pwdFormRef.value) return
  try {
    await pwdFormRef.value.validate()
  } catch {
    return
  }
  try {
    await ElMessageBox.confirm('密码修改成功后将自动退出登录，确认提交新密码？', '修改密码二次确认', {
      confirmButtonText: '确认修改',
      cancelButtonText: '再想想',
      type: 'warning',
    })
  } catch {
    return
  }
  pwdSubmitting.value = true
  try {
    const res = await put<void>('/users/modify/password', {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    if (res.code === 200) {
      ElMessage.success(res.message || '密码修改成功，请重新登录')
      resetPwdForm()
      setTimeout(() => {
        userStore.logout()
        router.push('/login')
      }, 800)
    } else {
      ElMessage.error(res.message || '密码修改失败')
    }
  } catch {
    ElMessage.error('密码修改失败，请稍后再试')
  } finally {
    pwdSubmitting.value = false
  }
}

// ========== 退出登录 ==========
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确认退出当前账号？', '提示', {
      confirmButtonText: '确认退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await post<void>('/users/logout')
  } catch {
    /* ignore */
  }
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.profile-page {
  padding-bottom: 32px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 24px 0;
  color: rgba(255, 255, 255, 0.95);
}

.profile-card {
  max-width: 760px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  color: rgba(255, 255, 255, 0.9);
}

.profile-card + .profile-card {
  margin-top: 20px;
}

:deep(.profile-card .el-card__header) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.08) !important;
  background: transparent !important;
  padding: 20px 24px !important;
}

:deep(.profile-card .el-card__body) {
  background: transparent !important;
  padding: 24px !important;
  color: rgba(255, 255, 255, 0.9);
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  flex-shrink: 0;
}

.user-info h3 {
  margin: 0 0 6px 0;
  font-size: 20px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.95);
}

.account-row {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.55);
}

.profile-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(255, 255, 255, 0.08) !important;
}

.profile-tabs :deep(.el-tabs__item) {
  color: rgba(255, 255, 255, 0.55) !important;
  height: 40px;
  font-size: 14px;
  font-weight: 500;
}

.profile-tabs :deep(.el-tabs__item.is-active) {
  color: #ffffff !important;
}

.profile-tabs :deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #667eea, #9254de) !important;
  height: 3px;
  border-radius: 2px;
}

.profile-form {
  padding-top: 8px;
}

/* 表单元素：适配深色主题 */
:deep(.profile-form .el-form-item__label) {
  color: rgba(255, 255, 255, 0.88) !important;
  font-weight: 500;
  font-size: 14px;
}

:deep(.profile-form .el-form-item.is-error .el-form-item__label) {
  color: #fca5a5 !important;
}

:deep(.profile-form .el-form-item__error) {
  color: #fca5a5 !important;
}

:deep(.profile-form .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.04) !important;
  border: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: none !important;
  border-radius: 8px !important;
  transition: all 0.2s ease;
}

:deep(.profile-form .el-input__wrapper:hover) {
  background: rgba(255, 255, 255, 0.07) !important;
  border-color: rgba(102, 126, 234, 0.45) !important;
}

:deep(.profile-form .el-input__wrapper.is-focus),
:deep(.profile-form .el-input.is-focus .el-input__wrapper) {
  background: rgba(102, 126, 234, 0.08) !important;
  border-color: rgba(102, 126, 234, 0.75) !important;
}

:deep(.profile-form .el-input.is-disabled .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.025) !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
  cursor: not-allowed;
}

:deep(.profile-form .el-input__inner) {
  color: rgba(255, 255, 255, 0.95) !important;
}

:deep(.profile-form .el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.32) !important;
}

:deep(.profile-form .el-input.is-disabled .el-input__inner) {
  color: rgba(255, 255, 255, 0.55) !important;
  -webkit-text-fill-color: rgba(255, 255, 255, 0.55) !important;
}

:deep(.profile-form .el-textarea__inner) {
  background: rgba(255, 255, 255, 0.04) !important;
  border: 1px solid rgba(255, 255, 255, 0.14) !important;
  color: rgba(255, 255, 255, 0.95) !important;
  border-radius: 8px !important;
  box-shadow: none !important;
}

:deep(.profile-form .el-textarea__inner:hover) {
  border-color: rgba(102, 126, 234, 0.45) !important;
}

:deep(.profile-form .el-textarea__inner:focus) {
  border-color: rgba(102, 126, 234, 0.75) !important;
  background: rgba(102, 126, 234, 0.08) !important;
}

:deep(.profile-form .el-textarea__inner::placeholder) {
  color: rgba(255, 255, 255, 0.32) !important;
}

:deep(.profile-form .el-count) {
  color: rgba(255, 255, 255, 0.35) !important;
}

/* el-divider / el-alert 文本色适配 */
:deep(.profile-form .el-divider__text) {
  background: transparent !important;
  color: rgba(255, 255, 255, 0.75) !important;
  font-weight: 600;
}

:deep(.profile-form .el-divider) {
  --el-border-color: rgba(255, 255, 255, 0.08) !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
}

:deep(.profile-alert.el-alert) {
  background: rgba(202, 138, 4, 0.12) !important;
  border: 1px solid rgba(234, 179, 8, 0.3) !important;
}

:deep(.profile-alert .el-alert__title) {
  color: #fde68a !important;
}

:deep(.profile-alert .el-alert__description) {
  color: rgba(253, 230, 138, 0.8) !important;
}

:deep(.profile-alert .el-alert__icon) {
  color: #facc15 !important;
}

.profile-alert {
  margin-bottom: 20px;
  border-radius: 10px;
}

.code-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.code-row > :deep(.el-input) {
  flex: 1;
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
  padding: 16px 18px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.action-item:hover {
  background: rgba(255, 255, 255, 0.06);
}

.action-item.danger {
  border-color: rgba(239, 68, 68, 0.2);
  color: #fecaca;
}

.action-item.danger:hover {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.35);
}

.action-item > :nth-child(2) {
  flex: 1;
  margin: 0 12px;
  font-weight: 500;
}

.danger-zone .danger-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #fecaca;
}

.danger-header-icon {
  color: #ef4444;
}

/* 手机端适配 */
@media (max-width: 640px) {
  .code-row {
    flex-direction: column;
  }
  .profile-card {
    border-radius: 14px;
  }
  :deep(.profile-card .el-card__body) {
    padding: 18px !important;
  }
  :deep(.profile-card .el-card__header) {
    padding: 18px !important;
  }
  :deep(.el-form-item__label) {
    width: auto !important;
    text-align: left !important;
  }
}
</style>

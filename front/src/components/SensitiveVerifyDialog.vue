<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="440px"
    class="sensitive-verify-dialog"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @close="onClose"
  >
    <p class="sv-tip">
      为保障账号安全，{{ operationName ? `「${operationName}」` : '该操作' }}需要验证你的身份。
    </p>

    <!-- 仅在两种方式都开启时才展示方式切换；只开一种时直接用该方式 -->
    <el-radio-group v-if="canChooseMethod" v-model="method" class="sv-method">
      <el-radio-button value="password">登录密码</el-radio-button>
      <el-radio-button value="email_code">邮箱验证码</el-radio-button>
    </el-radio-group>

    <div v-if="method === 'password'" class="sv-field">
      <el-input
        v-model="password"
        type="password"
        placeholder="请输入当前账号的登录密码"
        show-password
        maxlength="48"
        @keyup.enter="onConfirm"
      />
    </div>

    <div v-else class="sv-field sv-code-field">
      <el-input
        v-model="emailCode"
        placeholder="请输入发送到绑定邮箱的验证码"
        maxlength="6"
        @keyup.enter="onConfirm"
      />
      <el-button
        :disabled="countdown > 0 || sending"
        :loading="sending"
        @click="onSendCode"
      >
        {{ countdown > 0 ? `${countdown}s 后重发` : '发送验证码' }}
      </el-button>
    </div>

    <template #footer>
      <el-button @click="onCancel">取消</el-button>
      <el-button type="primary" @click="onConfirm">确认</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  cancelVerification,
  confirmVerification,
  sendVerifyCode,
  useSensitiveVerificationState,
} from '@/composables/useSensitiveVerification'

const { visible, operationName, method, password, emailCode, sending, countdown, canChooseMethod } =
  useSensitiveVerificationState()

const title = computed(() => (operationName.value ? `身份验证 · ${operationName.value}` : '身份验证'))

const onSendCode = async () => {
  const res = await sendVerifyCode()
  if (res.ok) {
    ElMessage.success(res.message)
  } else {
    ElMessage.error(res.message)
  }
}

const onConfirm = () => {
  if (method.value === 'password' && !password.value) {
    ElMessage.warning('请输入登录密码')
    return
  }
  if (method.value === 'email_code' && !emailCode.value.trim()) {
    ElMessage.warning('请输入邮箱验证码')
    return
  }
  confirmVerification()
}

const onCancel = () => cancelVerification()

const onClose = () => cancelVerification()
</script>

<style scoped>
.sv-tip {
  margin: 0 0 14px;
  font-size: 13px;
  line-height: 1.6;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
}

.sv-method {
  margin-bottom: 14px;
}

.sv-field {
  margin-bottom: 4px;
}

.sv-code-field {
  display: flex;
  gap: 8px;
}

.sv-code-field :deep(.el-input) {
  flex: 1;
  min-width: 0;
}

.sv-code-field :deep(.el-button) {
  flex-shrink: 0;
}
</style>

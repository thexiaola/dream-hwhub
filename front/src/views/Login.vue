<template>
  <AuthShell badge="账号密码登录" title="登 录" subtitle="请输入账号与密码以继续">
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      class="auth-form"
      @submit.prevent="handleLogin"
    >
      <el-form-item prop="account">
        <el-input
          v-model="form.account"
          size="large"
          placeholder="用户名 / 邮箱"
          class="auth-input"
          @keyup.enter="handleLogin"
        >
          <template #prefix>
            <span class="auth-input-icon">
              <User :size="16" />
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
          @keyup.enter="handleLogin"
        >
          <template #prefix>
            <span class="auth-input-icon">
              <Lock :size="16" />
            </span>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          class="auth-submit"
          :loading="loading"
          @click="handleLogin"
        >
          <span class="auth-submit-text">登 录</span>
        </el-button>
      </el-form-item>
    </el-form>

    <div class="auth-links">
      <router-link to="/retrieve" class="auth-link">忘记密码？</router-link>
      <router-link to="/register" class="auth-link">立即注册</router-link>
    </div>
  </AuthShell>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@lucide/vue'
import AuthShell from '@/components/AuthShell.vue'

const router = useRouter()
const userStore = useUserStore()

const form = ref({
  account: '',
  password: ''
})

const loading = ref(false)

const rules = {
  account: [
    { required: true, message: '请输入用户名或邮箱', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  // 回车提交与按钮点击共用此函数，避免请求期间重复触发
  if (loading.value) {
    return
  }

  if (!form.value.account || !form.value.password) {
    ElMessage.error('请填写账号和密码')
    return
  }

  loading.value = true

  try {
    const result = await userStore.login(form.value.account, form.value.password)
    if (result.code === 200) {
      ElMessage.success('登录成功')
      router.push('/student/courses')
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

<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h2>作业管理系统</h2>
        <p>欢迎登录</p>
      </div>
      
      <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
        <el-form-item prop="account">
          <el-input 
            v-model="form.account" 
            placeholder="请输入账号（学号/工号或邮箱）"
            :prefix-icon="userIcon"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input 
            v-model="form.password" 
            type="password" 
            placeholder="请输入密码"
            :prefix-icon="lockIcon"
            show-password
          />
        </el-form-item>
        
        <el-form-item class="login-actions">
          <el-button type="primary" @click="handleLogin" :loading="loading">
            登录
          </el-button>
        </el-form-item>
        
        <div class="login-links">
          <router-link to="/register">注册账号</router-link>
          <router-link to="/retrieve">忘记密码</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, h } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const userIcon = () => h(User)
const lockIcon = () => h(Lock)

const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  account: '',
  password: ''
})

const loading = ref(false)

const rules = {
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

async function handleLogin() {
  if (!form.account || !form.password) {
    ElMessage.error('请填写账号和密码')
    return
  }
  
  loading.value = true
  
  try {
    const response = await userStore.login(form.account, form.password)
    if (response.code === 200) {
      ElMessage.success('登录成功')
      router.push('/dashboard')
    } else {
      ElMessage.error(response.message)
      form.password = ''
    }
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
    form.password = ''
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.login-header p {
  color: #909399;
  font-size: 14px;
}

.login-form {
  margin-top: 20px;
}

.login-actions {
  margin-bottom: 16px;
}

.login-actions button {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

.login-links {
  display: flex;
  justify-content: space-between;
  text-align: center;
}

.login-links a {
  color: #667eea;
  font-size: 14px;
}

.login-links a:hover {
  color: #764ba2;
}
</style>
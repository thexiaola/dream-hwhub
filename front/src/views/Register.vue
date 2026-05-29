<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <h2>作业管理系统</h2>
        <p>用户注册</p>
      </div>
      
      <el-form ref="formRef" :model="form" class="register-form">
        <el-form-item prop="username">
          <el-input 
            v-model="form.username" 
            placeholder="请输入用户名"
            :prefix-icon="userIcon"
          />
        </el-form-item>
        
        <el-form-item prop="userNo">
          <el-input 
            v-model="form.userNo" 
            placeholder="请输入学号/工号"
            :prefix-icon="idCardIcon"
          />
        </el-form-item>
        
        <el-form-item prop="email">
          <el-input 
            v-model="form.email" 
            type="email"
            placeholder="请输入邮箱"
            :prefix-icon="mailIcon"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input 
            v-model="form.password" 
            type="password" 
            placeholder="请输入密码（8-32位，需包含大小写字母和数字）"
            :prefix-icon="lockIcon"
            show-password
          />
        </el-form-item>
        
        <el-form-item prop="confirmPassword">
          <el-input 
            v-model="form.confirmPassword" 
            type="password" 
            placeholder="请确认密码"
            :prefix-icon="lockIcon"
            show-password
          />
        </el-form-item>
        
        <el-form-item prop="code">
          <el-row :gutter="10">
            <el-col :span="16">
              <el-input 
                v-model="form.code" 
                placeholder="请输入验证码"
                :prefix-icon="codeIcon"
              />
            </el-col>
            <el-col :span="8">
              <el-button 
                type="primary" 
                @click="sendCode" 
                :disabled="!canSendCode"
                :loading="sendingCode"
              >
                {{ codeButtonText }}
              </el-button>
            </el-col>
          </el-row>
        </el-form-item>
        
        <el-form-item class="register-actions">
          <el-button type="primary" @click="handleRegister" :loading="loading">
            注册
          </el-button>
        </el-form-item>
        
        <div class="register-links">
          <router-link to="/login">已有账号，立即登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, h } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import { User, Postcard, Message, Lock, Key } from '@element-plus/icons-vue'

const userIcon = () => h(User)
const idCardIcon = () => h(Postcard)
const mailIcon = () => h(Message)
const lockIcon = () => h(Lock)
const codeIcon = () => h(Key)

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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-card {
  width: 450px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.register-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.register-header p {
  color: #909399;
  font-size: 14px;
}

.register-form {
  margin-top: 20px;
}

.register-actions {
  margin-bottom: 16px;
}

.register-actions button {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

.register-links {
  text-align: center;
}

.register-links a {
  color: #667eea;
  font-size: 14px;
}

.register-links a:hover {
  color: #764ba2;
}
</style>
<template>
  <div class="retrieve-container">
    <div class="retrieve-card">
      <div class="retrieve-header">
        <h2>作业管理系统</h2>
        <p>找回密码</p>
      </div>
      
      <el-form :model="form" class="retrieve-form">
        <el-form-item>
          <el-input 
            v-model="form.account" 
            placeholder="请输入账号（学号/工号或邮箱）"
            :prefix-icon="userIcon"
          />
        </el-form-item>
        
        <el-form-item>
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
                :disabled="!form.account || codeCountdown > 0"
                :loading="sendingCode"
              >
                {{ codeCountdown > 0 ? `${codeCountdown}s` : '发送验证码' }}
              </el-button>
            </el-col>
          </el-row>
        </el-form-item>
        
        <el-form-item>
          <el-input 
            v-model="form.newPassword" 
            type="password" 
            placeholder="请输入新密码（8-32位，需包含大小写字母和数字）"
            :prefix-icon="lockIcon"
            show-password
          />
        </el-form-item>
        
        <el-form-item class="retrieve-actions">
          <el-button type="primary" @click="handleReset" :loading="loading">
            重置密码
          </el-button>
        </el-form-item>
        
        <div class="retrieve-links">
          <router-link to="/login">返回登录</router-link>
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
import { User, Lock, Key, Message } from '@element-plus/icons-vue'

const userIcon = () => h(User)
const lockIcon = () => h(Lock)
const codeIcon = () => h(Key)

const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  account: '',
  code: '',
  newPassword: ''
})

const loading = ref(false)
const sendingCode = ref(false)
const codeCountdown = ref(0)

async function sendCode() {
  if (!form.account) {
    ElMessage.warning('请先填写账号')
    return
  }
  
  sendingCode.value = true
  
  try {
    const response = await userStore.sendRetrieveCode(form.account)
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

async function handleReset() {
  if (!form.account || !form.code || !form.newPassword) {
    ElMessage.error('请填写所有必填项')
    return
  }
  
  const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d]{8,32}$/
  if (!passwordRegex.test(form.newPassword)) {
    ElMessage.error('密码需包含大小写字母和数字，长度8-32位')
    return
  }
  
  loading.value = true
  
  try {
    const response = await userStore.resetPassword({
      account: form.account,
      code: form.code,
      newPassword: form.newPassword
    })
    
    if (response.code === 200) {
      ElMessage.success('密码重置成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(error.message || '重置失败')
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.retrieve-card {
  width: 450px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.retrieve-header {
  text-align: center;
  margin-bottom: 30px;
}

.retrieve-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.retrieve-header p {
  color: #909399;
  font-size: 14px;
}

.retrieve-form {
  margin-top: 20px;
}

.retrieve-actions {
  margin-bottom: 16px;
}

.retrieve-actions button {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

.retrieve-links {
  text-align: center;
}

.retrieve-links a {
  color: #667eea;
  font-size: 14px;
}

.retrieve-links a:hover {
  color: #764ba2;
}
</style>
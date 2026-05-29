<template>
  <div class="profile-container">
    <Sidebar />
    
    <div class="profile-main">
      <Header />
      
      <div class="profile-content">
        <el-card class="profile-card">
          <div class="profile-header">
            <div class="avatar-section">
              <div class="avatar">
                <component :is="componentMap['User']" />
              </div>
              <h2>{{ userStore.user?.username }}</h2>
              <p>{{ userStore.user?.email }}</p>
            </div>
          </div>
          
          <div class="profile-tabs">
            <el-tabs v-model="activeTab" @tab-change="handleTabChange">
              <el-tab-pane label="基本信息" name="info">
                <div class="tab-content">
                  <el-form :model="infoForm" label-width="120px">
                    <el-form-item label="用户名">
                      <el-input v-model="infoForm.username" />
                    </el-form-item>
                    <el-form-item label="学号/工号">
                      <el-input v-model="infoForm.userNo" disabled />
                    </el-form-item>
                    <el-form-item label="邮箱">
                      <el-input v-model="infoForm.email" disabled />
                    </el-form-item>
                    <el-form-item>
                      <el-button type="primary" @click="saveInfo">保存修改</el-button>
                    </el-form-item>
                  </el-form>
                </div>
              </el-tab-pane>
              
              <el-tab-pane label="修改密码" name="password">
                <div class="tab-content">
                  <el-form :model="passwordForm" label-width="120px">
                    <el-form-item label="旧密码">
                      <el-input v-model="passwordForm.oldPassword" type="password" />
                    </el-form-item>
                    <el-form-item label="新密码">
                      <el-input v-model="passwordForm.newPassword" type="password" />
                    </el-form-item>
                    <el-form-item label="确认新密码">
                      <el-input v-model="passwordForm.confirmPassword" type="password" />
                    </el-form-item>
                    <el-form-item>
                      <el-button type="primary" @click="changePassword">修改密码</el-button>
                    </el-form-item>
                  </el-form>
                </div>
              </el-tab-pane>
              
              <el-tab-pane label="修改邮箱" name="email">
                <div class="tab-content">
                  <el-form :model="emailForm" label-width="120px">
                    <el-form-item label="当前邮箱">
                      <el-input :value="userStore.user?.email" disabled />
                    </el-form-item>
                    <el-form-item label="旧邮箱验证码">
                      <el-row :gutter="10">
                        <el-col :span="16">
                          <el-input v-model="emailForm.oldEmailCode" />
                        </el-col>
                        <el-col :span="8">
                          <el-button 
                            type="primary" 
                            @click="sendOldEmailCode"
                            :disabled="oldEmailCodeCountdown > 0"
                          >
                            {{ oldEmailCodeCountdown > 0 ? `${oldEmailCodeCountdown}s` : '获取验证码' }}
                          </el-button>
                        </el-col>
                      </el-row>
                    </el-form-item>
                    <el-form-item label="新邮箱">
                      <el-input v-model="emailForm.newEmail" />
                    </el-form-item>
                    <el-form-item label="新邮箱验证码">
                      <el-row :gutter="10">
                        <el-col :span="16">
                          <el-input v-model="emailForm.newEmailCode" />
                        </el-col>
                        <el-col :span="8">
                          <el-button 
                            type="primary" 
                            @click="sendNewEmailCode"
                            :disabled="!emailForm.newEmail || newEmailCodeCountdown > 0"
                          >
                            {{ newEmailCodeCountdown > 0 ? `${newEmailCodeCountdown}s` : '获取验证码' }}
                          </el-button>
                        </el-col>
                      </el-row>
                    </el-form-item>
                    <el-form-item>
                      <el-button type="primary" @click="changeEmail">修改邮箱</el-button>
                    </el-form-item>
                  </el-form>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import Sidebar from '../components/Sidebar.vue'
import Header from '../components/Header.vue'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import { User } from 'lucide-vue-next'

const userStore = useUserStore()

const componentMap = {
  User
}

const activeTab = ref('info')

const infoForm = reactive({
  username: '',
  userNo: '',
  email: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const emailForm = reactive({
  oldEmailCode: '',
  newEmail: '',
  newEmailCode: ''
})

const oldEmailCodeCountdown = ref(0)
const newEmailCodeCountdown = ref(0)

onMounted(() => {
  syncUserInfo()
})

watch(() => userStore.user, () => {
  syncUserInfo()
}, { deep: true })

function syncUserInfo() {
  if (userStore.user) {
    infoForm.username = userStore.user.username
    infoForm.userNo = userStore.user.userNo
    infoForm.email = userStore.user.email
  }
}

function handleTabChange() {
}

async function saveInfo() {
  if (!infoForm.username) {
    ElMessage.error('请输入用户名')
    return
  }
  
  try {
    const response = await userStore.modifyUserInfo(infoForm.username)
    if (response.code === 200) {
      ElMessage.success('信息修改成功')
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(error.message || '修改失败')
  }
}

async function changePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.error('请填写密码')
    return
  }
  
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  
  const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d]{8,32}$/
  if (!passwordRegex.test(passwordForm.newPassword)) {
    ElMessage.error('密码需包含大小写字母和数字，长度8-32位')
    return
  }
  
  try {
    const response = await userStore.modifyPassword(passwordForm.oldPassword, passwordForm.newPassword)
    if (response.code === 200) {
      ElMessage.success('密码修改成功')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(error.message || '修改失败')
  }
}

async function sendOldEmailCode() {
  try {
    const response = await userStore.sendOldEmailCode()
    if (response.code === 200) {
      ElMessage.success('验证码已发送')
      oldEmailCodeCountdown.value = 60
      startCountdown(oldEmailCodeCountdown)
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(error.message || '发送失败')
  }
}

async function sendNewEmailCode() {
  if (!emailForm.newEmail) {
    ElMessage.warning('请先输入新邮箱')
    return
  }
  
  try {
    const response = await userStore.sendNewEmailCode(emailForm.newEmail)
    if (response.code === 200) {
      ElMessage.success('验证码已发送')
      newEmailCodeCountdown.value = 60
      startCountdown(newEmailCodeCountdown)
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(error.message || '发送失败')
  }
}

function startCountdown(countdownRef) {
  const timer = setInterval(() => {
    countdownRef.value--
    if (countdownRef.value === 0) {
      clearInterval(timer)
    }
  }, 1000)
}

async function changeEmail() {
  if (!emailForm.oldEmailCode || !emailForm.newEmail || !emailForm.newEmailCode) {
    ElMessage.error('请填写所有信息')
    return
  }
  
  try {
    const response = await userStore.modifyEmail({
      oldEmailCode: emailForm.oldEmailCode,
      newEmail: emailForm.newEmail,
      newEmailCode: emailForm.newEmailCode
    })
    
    if (response.code === 200) {
      ElMessage.success('邮箱修改成功')
      emailForm.oldEmailCode = ''
      emailForm.newEmail = ''
      emailForm.newEmailCode = ''
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(error.message || '修改失败')
  }
}
</script>

<style scoped>
.profile-container {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

.profile-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.profile-content {
  padding: 24px;
}

.profile-card {
  max-width: 600px;
}

.profile-header {
  text-align: center;
  padding: 24px 0;
  border-bottom: 1px solid #eee;
  margin-bottom: 24px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 32px;
  margin-bottom: 16px;
}

.avatar-section h2 {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
}

.avatar-section p {
  color: #909399;
}

.profile-tabs {
  padding: 0 24px;
}

.tab-content {
  padding: 24px 0;
}
</style>
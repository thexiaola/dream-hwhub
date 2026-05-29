<template>
  <div class="work-container">
    <Sidebar />
    
    <div class="work-main">
      <Header />
      
      <div class="work-content">
        <el-card class="work-form-card">
          <div class="form-header">
            <h2>编辑作业</h2>
          </div>
          
          <el-form :model="form" class="work-form">
            <el-form-item label="作业标题" required>
              <el-input 
                v-model="form.title" 
                placeholder="请输入作业标题"
                :maxlength="128"
              />
            </el-form-item>
            
            <el-form-item label="作业描述">
              <el-textarea 
                v-model="form.description" 
                placeholder="请输入作业描述"
                :rows="4"
                :maxlength="1024"
              />
            </el-form-item>
            
            <el-form-item label="截止时间" required>
              <el-date-picker
                v-model="form.deadline"
                type="datetime"
                placeholder="请选择截止时间"
                :min-date="new Date()"
              />
            </el-form-item>
            
            <el-form-item label="作业总分" required>
              <el-input 
                v-model.number="form.totalScore" 
                type="number" 
                placeholder="请输入作业总分"
                :min="1"
                :max="1000"
              />
            </el-form-item>
            
            <el-form-item label="允许逾期提交">
              <el-switch v-model="form.allowLateSubmit" />
            </el-form-item>
            
            <el-form-item class="form-actions">
              <el-button type="primary" @click="handleSubmit">保存修改</el-button>
              <el-button @click="goBack">取消</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Sidebar from '../../components/Sidebar.vue'
import Header from '../../components/Header.vue'
import { useWorkStore } from '../../stores/work'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const workStore = useWorkStore()

const workId = ref(parseInt(route.params.id))

const form = reactive({
  title: '',
  description: '',
  deadline: '',
  totalScore: 100,
  allowLateSubmit: true
})

onMounted(async () => {
  await loadWorkDetail()
})

async function loadWorkDetail() {
  const response = await workStore.getWorkDetail(workId.value)
  if (response.code === 200) {
    const work = response.data
    form.title = work.title
    form.description = work.description || ''
    form.deadline = new Date(work.deadline)
    form.totalScore = work.totalScore
    form.allowLateSubmit = work.allowLateSubmit
  }
}

async function handleSubmit() {
  if (!form.title || !form.deadline || !form.totalScore) {
    ElMessage.error('请填写必填项')
    return
  }
  
  const formData = new FormData()
  formData.append('id', workId.value)
  formData.append('title', form.title)
  formData.append('description', form.description || '')
  formData.append('deadline', new Date(form.deadline).toISOString())
  formData.append('totalScore', form.totalScore)
  formData.append('allowLateSubmit', form.allowLateSubmit)
  
  const response = await workStore.updateWork(workId.value, formData)
  
  if (response.code === 200) {
    ElMessage.success('修改成功')
    router.push(`/works/${workId.value}`)
  } else {
    ElMessage.error(response.message)
  }
}

function goBack() {
  router.push(`/works/${workId.value}`)
}
</script>

<style scoped>
.work-container {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

.work-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.work-content {
  padding: 24px;
}

.work-form-card {
  max-width: 600px;
}

.form-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}

.form-header h2 {
  font-size: 18px;
  font-weight: 600;
}

.work-form {
  padding: 0 24px;
}

.form-actions {
  display: flex;
  gap: 12px;
}
</style>
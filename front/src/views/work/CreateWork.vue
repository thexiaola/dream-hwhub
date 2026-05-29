<template>
  <div class="work-container">
    <Sidebar />
    
    <div class="work-main">
      <Header />
      
      <div class="work-content">
        <el-card class="work-form-card">
          <div class="form-header">
            <h2>创建作业</h2>
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
            
            <el-form-item label="所属班级" required>
              <el-select v-model="form.classId" placeholder="请选择班级">
                <el-option 
                  v-for="cls in classList" 
                  :key="cls.id" 
                  :label="cls.className" 
                  :value="cls.id" 
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="截止时间" required>
              <el-date-picker
                v-model="form.deadline"
                type="datetime"
                placeholder="请选择截止时间"
                :min-date="new Date()"
              />
            </el-form-item>
            
            <el-form-item label="发布时间">
              <el-date-picker
                v-model="form.publishTime"
                type="datetime"
                placeholder="请选择发布时间（不填则即时发布）"
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
            
            <el-form-item label="附件">
              <el-upload
                class="upload-demo"
                action="/api/works/upload"
                :on-success="handleUploadSuccess"
                :on-error="handleUploadError"
                multiple
              >
                <el-button size="small" type="primary">点击上传</el-button>
              </el-upload>
            </el-form-item>
            
            <el-form-item class="form-actions">
              <el-button type="primary" @click="handleSubmit">创建作业</el-button>
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
import { useRouter } from 'vue-router'
import Sidebar from '../../components/Sidebar.vue'
import Header from '../../components/Header.vue'
import { useWorkStore } from '../../stores/work'
import { useClassStore } from '../../stores/class'
import { ElMessage } from 'element-plus'

const router = useRouter()
const workStore = useWorkStore()
const classStore = useClassStore()

const form = reactive({
  title: '',
  description: '',
  classId: '',
  deadline: '',
  publishTime: '',
  totalScore: 100,
  allowLateSubmit: true
})

const classList = ref([])

onMounted(async () => {
  await loadClasses()
})

async function loadClasses() {
  const response = await classStore.getMyClasses()
  if (response.code === 200) {
    classList.value = response.data.records
  }
}

async function handleSubmit() {
  if (!form.title || !form.classId || !form.deadline || !form.totalScore) {
    ElMessage.error('请填写必填项')
    return
  }
  
  const formData = new FormData()
  formData.append('title', form.title)
  formData.append('description', form.description || '')
  formData.append('classId', form.classId)
  formData.append('deadline', new Date(form.deadline).toISOString())
  if (form.publishTime) {
    formData.append('publishTime', new Date(form.publishTime).toISOString())
  }
  formData.append('totalScore', form.totalScore)
  formData.append('allowLateSubmit', form.allowLateSubmit)
  
  const response = await workStore.createWork(formData)
  
  if (response.code === 200) {
    ElMessage.success('创建成功')
    router.push('/works')
  } else {
    ElMessage.error(response.message)
  }
}

function handleUploadSuccess(response, file, fileList) {
  ElMessage.success('文件上传成功')
}

function handleUploadError(err, file, fileList) {
  ElMessage.error('文件上传失败')
}

function goBack() {
  router.push('/works')
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
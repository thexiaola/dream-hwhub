<template>
  <div class="submit-container">
    <Sidebar />
    
    <div class="submit-main">
      <Header />
      
      <div class="submit-content">
        <div class="submit-header">
          <el-button @click="goBack">返回</el-button>
        </div>
        
        <el-card v-if="work" class="submit-card">
          <div class="work-info">
            <h2>{{ work.title }}</h2>
            <div class="work-meta">
              <span>班级: {{ work.className }}</span>
              <span>截止时间: {{ formatDate(work.deadline) }}</span>
              <span>总分: {{ work.totalScore }} 分</span>
            </div>
          </div>
          
          <div class="work-description">
            <h3>作业描述</h3>
            <p>{{ work.description || '暂无描述' }}</p>
          </div>
          
          <div v-if="work.attachments && work.attachments.length > 0" class="work-attachments">
            <h3>附件</h3>
            <ul>
              <li v-for="attachment in work.attachments" :key="attachment.id">
                <component :is="componentMap['FileText']" />
                <span>{{ attachment.fileName }}</span>
                <span class="file-size">{{ formatFileSize(attachment.fileSize) }}</span>
              </li>
            </ul>
          </div>
          
          <el-divider />
          
          <div class="submit-form">
            <h3>提交作业</h3>
            
            <el-form :model="form">
              <el-form-item label="提交内容">
                <el-input 
                  v-model="form.submissionContent" 
                  type="textarea" 
                  :rows="6"
                  placeholder="请输入提交内容（可选）"
                  :maxlength="2048"
                />
                <div class="char-count">{{ form.submissionContent?.length || 0 }} / 2048</div>
              </el-form-item>
              
              <el-form-item label="上传附件">
                <el-upload
                  ref="uploadRef"
                  class="upload-demo"
                  :auto-upload="false"
                  :on-change="handleFileChange"
                  :on-remove="handleFileRemove"
                  multiple
                >
                  <el-button size="small" type="primary">选择文件</el-button>
                  <template #tip>
                    <div class="upload-tip">支持多文件上传，单个文件最大 50MB</div>
                  </template>
                </el-upload>
              </el-form-item>
              
              <el-form-item>
                <el-button type="primary" @click="handleSubmit" :loading="submitting">
                  提交作业
                </el-button>
              </el-form-item>
            </el-form>
          </div>
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
import { useSubmissionStore } from '../../stores/submission'
import { ElMessage } from 'element-plus'
import { FileText } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const workStore = useWorkStore()
const submissionStore = useSubmissionStore()

const componentMap = {
  FileText
}

const workId = ref(parseInt(route.params.workId))
const work = ref(null)
const submitting = ref(false)
const fileList = ref([])

const form = reactive({
  submissionContent: ''
})

onMounted(async () => {
  await loadWorkDetail()
})

async function loadWorkDetail() {
  const response = await workStore.getWorkDetail(workId.value)
  if (response.code === 200) {
    work.value = response.data
  } else {
    ElMessage.error('作业不存在')
    router.push('/works')
  }
}

function handleFileChange(file, files) {
  fileList.value = files
}

function handleFileRemove(file, files) {
  fileList.value = files
}

async function handleSubmit() {
  submitting.value = true
  
  try {
    const formData = new FormData()
    formData.append('workId', workId.value)
    if (form.submissionContent) {
      formData.append('submissionContent', form.submissionContent)
    }
    
    for (let i = 0; i < fileList.value.length; i++) {
      const file = fileList.value[i]
      if (file.raw) {
        formData.append('attachments', file.raw)
      }
    }
    
    const response = await submissionStore.submitWork(formData)
    
    if (response.code === 200) {
      ElMessage.success('提交成功')
      router.push('/submissions')
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(error.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

function formatDate(dateStr) {
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function goBack() {
  router.back()
}
</script>

<style scoped>
.submit-container {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

.submit-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.submit-content {
  padding: 24px;
}

.submit-card {
  max-width: 800px;
}

.work-info {
  margin-bottom: 24px;
}

.work-info h2 {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 12px;
}

.work-meta {
  display: flex;
  gap: 24px;
  color: #666;
  font-size: 14px;
}

.work-description {
  margin-bottom: 24px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.work-description h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
}

.work-description p {
  color: #666;
  white-space: pre-wrap;
}

.work-attachments {
  margin-bottom: 24px;
}

.work-attachments h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.work-attachments ul {
  list-style: none;
  padding: 0;
}

.work-attachments li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f9fafb;
  margin-bottom: 8px;
  border-radius: 8px;
}

.file-size {
  margin-left: auto;
  color: #909399;
  font-size: 14px;
}

.submit-form {
  margin-top: 24px;
}

.submit-form h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
}

.char-count {
  text-align: right;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
<template>
  <div class="submit-work-page">
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack" class="back-btn">
          <ArrowLeft :size="18" />
        </el-button>
        <h2>提交作业</h2>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="submitWork" :loading="loading">提交</el-button>
      </div>
    </div>
    <el-card class="content-card">
      <div v-if="work" class="work-info">
        <h3>{{ work.title }}</h3>
        <p>{{ work.description }}</p>
        <div class="work-meta">
          <span>截止时间：{{ formatDate(work.deadline) }}</span>
          <span>满分：{{ work.score }}分</span>
        </div>
      </div>
      <el-form :model="form" class="submit-form">
        <el-form-item label="作业内容">
          <el-textarea 
            v-model="form.content" 
            placeholder="请输入作业内容"
            :rows="8"
            class="form-input"
          />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useWorkStore } from '@/stores/work'
import { useSubmissionStore } from '@/stores/submission'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@lucide/vue'

const route = useRoute()
const router = useRouter()
const workStore = useWorkStore()
const submissionStore = useSubmissionStore()

const work = ref<any>(null)
const form = ref({
  content: ''
})
const loading = ref(false)

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const goBack = () => {
  const workId = route.query.workId
  if (workId) {
    router.push(`/work/${workId}`)
  } else {
    router.push('/submission')
  }
}

const submitWork = async () => {
  if (!form.value.content.trim()) {
    ElMessage.error('请输入作业内容')
    return
  }
  
  loading.value = true
  
  try {
    const workId = Number(route.query.workId) || 0
    const result = await submissionStore.submitWork(workId, form.value.content)
    
    if (result.code === 200) {
      ElMessage.success('提交成功')
      goBack()
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    ElMessage.error('提交失败')
  } finally {
    loading.value = false
  }
}

const loadData = async () => {
  const workId = Number(route.query.workId)
  if (workId) {
    work.value = await workStore.getWorkById(workId)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.submit-work-page {
  padding-bottom: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  padding: 8px;
}

.header-left h2 {
  font-size: 24px;
  font-weight: 600;
}

.content-card {
  max-width: 800px;
}

.work-info {
  padding: 20px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  margin-bottom: 24px;
}

.work-info h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}

.work-info p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.6;
  margin-bottom: 12px;
}

.work-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.submit-form {
  padding: 20px 0;
}

.form-input {
  width: 100%;
}
</style>

<template>
  <div class="create-work-page">
    <div class="page-header">
      <div class="header-left">
        <h2>创建作业</h2>
        <p class="subtitle">填写作业信息</p>
      </div>
      <div class="header-right">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="loading">提交</el-button>
      </div>
    </div>
    <el-card class="content-card">
      <el-form :model="form" :rules="rules" ref="formRef" class="create-form">
        <el-form-item label="作业标题" prop="title">
          <el-input 
            v-model="form.title" 
            placeholder="请输入作业标题"
            class="form-input"
          />
        </el-form-item>
        <el-form-item label="作业描述" prop="description">
          <el-textarea 
            v-model="form.description" 
            placeholder="请输入作业描述"
            :rows="4"
            class="form-input"
          />
        </el-form-item>
        <el-form-item label="所属班级" prop="classId">
          <el-select v-model="form.classId" placeholder="请选择班级" class="form-input">
            <el-option v-for="cls in classOptions" :key="cls.id" :label="cls.className" :value="cls.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="满分" prop="score">
          <el-input 
            v-model.number="form.score" 
            type="number" 
            placeholder="请输入满分"
            class="form-input"
          />
        </el-form-item>
        <el-form-item label="截止时间" prop="deadline">
          <el-date-picker 
            v-model="form.deadline" 
            type="datetime" 
            placeholder="请选择截止时间"
            class="form-input"
          />
        </el-form-item>
        <el-form-item label="置顶作业">
          <el-switch v-model="form.isPinned" />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useWorkStore } from '@/stores/work'
import { useClassStore } from '@/stores/class'
import { ElMessage } from 'element-plus'

const router = useRouter()
const workStore = useWorkStore()
const classStore = useClassStore()

const form = ref({
  title: '',
  description: '',
  classId: '',
  score: 100,
  deadline: '',
  isPinned: false
})

const formRef = ref()
const loading = ref(false)
const classOptions = ref<any[]>([])

const rules = {
  title: [
    { required: true, message: '请输入作业标题', trigger: 'blur' }
  ],
  classId: [
    { required: true, message: '请选择班级', trigger: 'change' }
  ],
  score: [
    { required: true, message: '请输入满分', trigger: 'blur' },
    { type: 'number', min: 1, max: 1000, message: '分数范围1-1000', trigger: 'blur' }
  ],
  deadline: [
    { required: true, message: '请选择截止时间', trigger: 'change' }
  ]
}

const goBack = () => {
  router.push('/work')
}

const submitForm = async () => {
  loading.value = true
  
  try {
    const result = await workStore.createWork({
      title: form.value.title,
      description: form.value.description,
      classId: form.value.classId,
      score: form.value.score,
      deadline: form.value.deadline,
      isPinned: form.value.isPinned
    })
    
    if (result.code === 200) {
      ElMessage.success('创建成功')
      router.push('/work')
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    ElMessage.error('创建失败')
  } finally {
    loading.value = false
  }
}

const loadData = async () => {
  await classStore.getClasses()
  classOptions.value = classStore.classes
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.create-work-page {
  padding-bottom: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left h2 {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 4px;
}

.subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.header-right {
  display: flex;
  gap: 12px;
}

.content-card {
  max-width: 600px;
}

.create-form {
  padding: 20px 0;
}

.form-input {
  width: 100%;
}

.el-form-item {
  margin-bottom: 20px;
}
</style>

<template>
  <div class="create-class-page">
    <div class="page-header">
      <div class="header-left">
        <h2>创建班级</h2>
        <p class="subtitle">填写班级信息</p>
      </div>
      <div class="header-right">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="loading">提交</el-button>
      </div>
    </div>
    <el-card class="content-card">
      <el-form :model="form" :rules="rules" ref="formRef" class="create-form">
        <el-form-item label="班级名称" prop="className">
          <el-input 
            v-model="form.className" 
            placeholder="请输入班级名称"
            class="form-input"
          />
        </el-form-item>
        <el-form-item label="班级描述" prop="description">
          <el-textarea 
            v-model="form.description" 
            placeholder="请输入班级描述"
            :rows="4"
            class="form-input"
          />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useClassStore } from '@/stores/class'
import { ElMessage } from 'element-plus'

const router = useRouter()
const classStore = useClassStore()

const form = ref({
  className: '',
  description: ''
})

const loading = ref(false)

const rules = {
  className: [
    { required: true, message: '请输入班级名称', trigger: 'blur' }
  ]
}

const goBack = () => {
  router.push('/class')
}

const submitForm = async () => {
  loading.value = true
  
  try {
    const result = await classStore.createClass({
      className: form.value.className,
      description: form.value.description
    })
    
    if (result.code === 200) {
      ElMessage.success('创建成功')
      router.push('/class')
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    ElMessage.error('创建失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.create-class-page {
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
  color: rgba(255, 255, 255, 0.95);
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

.create-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06) !important;
  border-color: rgba(255, 255, 255, 0.25) !important;
}

.create-form :deep(.el-input__wrapper:focus-within) {
  border-color: var(--primary-color) !important;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2) !important;
}

.create-form :deep(.el-input__inner) {
  color: rgba(255, 255, 255, 0.95) !important;
  background: transparent !important;
}

.create-form :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4);
}

.create-form :deep(.el-textarea__inner) {
  background: rgba(255, 255, 255, 0.06) !important;
  border-color: rgba(255, 255, 255, 0.25) !important;
  color: rgba(255, 255, 255, 0.95) !important;
}

.create-form :deep(.el-textarea__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4);
}

.create-form :deep(.el-textarea__inner:focus) {
  border-color: var(--primary-color) !important;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2) !important;
}

.create-form :deep(.el-form-item__label) {
  color: rgba(255, 255, 255, 0.8);
}

.el-form-item {
  margin-bottom: 20px;
}
</style>

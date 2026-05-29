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

const formRef = ref()
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

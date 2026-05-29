<template>
  <div class="class-container">
    <Sidebar />
    
    <div class="class-main">
      <Header />
      
      <div class="class-content">
        <el-card class="class-form-card">
          <div class="form-header">
            <h2>编辑班级信息</h2>
          </div>
          
          <el-form :model="form" class="class-form">
            <el-form-item label="班级名称" required>
              <el-input 
                v-model="form.className" 
                placeholder="请输入班级名称"
                :maxlength="100"
              />
            </el-form-item>
            
            <el-form-item label="班级描述">
              <el-textarea 
                v-model="form.description" 
                placeholder="请输入班级描述"
                :rows="4"
                :maxlength="500"
              />
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
import { useClassStore } from '../../stores/class'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const classStore = useClassStore()

const classId = ref(parseInt(route.params.id))

const form = reactive({
  className: '',
  description: ''
})

onMounted(async () => {
  await loadClassDetail()
})

async function loadClassDetail() {
  const response = await classStore.getClassDetail(classId.value)
  if (response.code === 200) {
    const cls = response.data
    form.className = cls.className
    form.description = cls.description || ''
  }
}

async function handleSubmit() {
  if (!form.className) {
    ElMessage.error('请填写班级名称')
    return
  }
  
  const response = await classStore.updateClass({
    classId: classId.value,
    className: form.className,
    description: form.description || ''
  })
  
  if (response.code === 200) {
    ElMessage.success('修改成功')
    router.push(`/classes/${classId.value}`)
  } else {
    ElMessage.error(response.message)
  }
}

function goBack() {
  router.push(`/classes/${classId.value}`)
}
</script>

<style scoped>
.class-container {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

.class-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.class-content {
  padding: 24px;
}

.class-form-card {
  max-width: 500px;
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

.class-form {
  padding: 0 24px;
}

.form-actions {
  display: flex;
  gap: 12px;
}
</style>
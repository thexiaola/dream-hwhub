<template>
  <div class="class-detail-page">
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack" class="back-btn">
          <ArrowLeft :size="18" />
        </el-button>
        <h2>{{ cls?.className }}</h2>
      </div>
      <div class="header-right">
        <el-button @click="deleteClass">删除班级</el-button>
      </div>
    </div>
    <div v-if="cls" class="class-detail">
      <el-card class="detail-card">
        <div class="detail-section">
          <h3>基本信息</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">创建人</span>
              <span class="value">{{ cls.teacherName }}</span>
            </div>
            <div class="info-item">
              <span class="label">创建时间</span>
              <span class="value">{{ formatDate(cls.createdAt) }}</span>
            </div>
          </div>
        </div>
        <div class="detail-section">
          <h3>班级描述</h3>
          <p class="description">{{ cls.description }}</p>
        </div>
      </el-card>
      <el-card class="members-section">
        <template #header>
          <h3>班级成员</h3>
        </template>
        <div class="members-list">
          <div v-for="member in members" :key="member.id" class="member-item">
            <div class="member-icon">
              <User :size="16" />
            </div>
            <div class="member-info">
              <h4>{{ member.username }}</h4>
              <p>{{ member.userNo }}</p>
            </div>
          </div>
        </div>
        <div v-if="members.length === 0" class="empty-state">
          <Users :size="32" />
          <p>暂无成员</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useClassStore } from '@/stores/class'
import { ElMessage, ElConfirm } from 'element-plus'
import { ArrowLeft, User, Users } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const classStore = useClassStore()

const cls = ref<any>(null)
const members = ref<any[]>([])

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const goBack = () => {
  router.push('/class')
}

const deleteClass = async () => {
  await ElConfirm('确认删除此班级？', '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消'
  })
  
  const result = await classStore.deleteClass(Number(route.params.id))
  if (result.code === 200) {
    ElMessage.success('删除成功')
    router.push('/class')
  } else {
    ElMessage.error(result.message)
  }
}

const loadData = async () => {
  cls.value = await classStore.getClassById(Number(route.params.id))
  members.value = [
    { id: 1, username: '张三', userNo: '2021001' },
    { id: 2, username: '李四', userNo: '2021002' },
    { id: 3, username: '王五', userNo: '2021003' }
  ]
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.class-detail-page {
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

.detail-card, .members-section {
  margin-bottom: 20px;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.detail-section h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
}

.info-item .label {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.info-item .value {
  font-size: 14px;
  font-weight: 500;
}

.description {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.6;
  padding: 16px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
}

.members-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
}

.member-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: rgba(102, 126, 234, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #667eea;
}

.member-info h4 {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 2px;
}

.member-info p {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: rgba(255, 255, 255, 0.4);
}

.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}
</style>

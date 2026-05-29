<template>
  <div class="class-list-page">
    <div class="page-header">
      <div class="header-left">
        <h2>班级管理</h2>
        <p class="subtitle">管理和查看所有班级</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="goToCreate">
          <Plus :size="18" />
          创建班级
        </el-button>
      </div>
    </div>
    <el-card class="content-card">
      <div class="class-grid">
        <div 
          v-for="cls in classStore.classes" 
          :key="cls.id" 
          class="class-card"
          @click="goToDetail(cls.id)"
        >
          <div class="card-header">
            <div class="class-icon">
              <Users :size="24" />
            </div>
            <h3>{{ cls.className }}</h3>
          </div>
          <p class="description">{{ cls.description }}</p>
          <div class="card-info">
            <div class="info-item">
              <User :size="14" />
              <span>{{ cls.teacherName }}</span>
            </div>
          </div>
          <div class="card-actions">
            <button class="action-btn" @click.stop="joinClass(cls.id)">
              <Users :size="14" />
              加入
            </button>
            <button class="action-btn" @click.stop="goToDetail(cls.id)">
              <FileText :size="14" />
              详情
            </button>
          </div>
        </div>
      </div>
      <div v-if="classStore.classes.length === 0" class="empty-state">
        <Users :size="48" />
        <p>暂无班级</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useClassStore } from '@/stores/class'
import { ElMessage } from 'element-plus'
import { Plus, Users, User, FileText } from '@lucide/vue'

const router = useRouter()
const classStore = useClassStore()

const goToCreate = () => {
  router.push('/class/create')
}

const goToDetail = (id: number) => {
  router.push(`/class/${id}`)
}

const joinClass = async (classId: number) => {
  const result = await classStore.joinClass(classId)
  if (result.code === 200) {
    ElMessage.success('加入成功')
  } else {
    ElMessage.error(result.message)
  }
}

const loadData = async () => {
  await classStore.getClasses()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.class-list-page {
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

.class-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.class-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 20px;
  transition: all 0.3s;
  cursor: pointer;
}

.class-card:hover {
  background: rgba(255, 255, 255, 0.05);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.class-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(102, 126, 234, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #667eea;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
}

.description {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 16px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-info {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.card-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: white;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
}

.action-btn:hover {
  background: rgba(102, 126, 234, 0.2);
  border-color: rgba(102, 126, 234, 0.3);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: rgba(255, 255, 255, 0.4);
}

.empty-state p {
  margin-top: 16px;
  font-size: 14px;
}
</style>

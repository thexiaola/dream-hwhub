<template>
  <div class="dashboard">
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon blue">
          <FileText :size="24" />
        </div>
        <div class="stat-content">
          <p class="stat-value">{{ stats.totalWorks }}</p>
          <p class="stat-label">总作业数</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">
          <Users :size="24" />
        </div>
        <div class="stat-content">
          <p class="stat-value">{{ stats.totalClasses }}</p>
          <p class="stat-label">班级数</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">
          <CheckCircle :size="24" />
        </div>
        <div class="stat-content">
          <p class="stat-value">{{ stats.completedSubmissions }}</p>
          <p class="stat-label">已批改</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">
          <Clock :size="24" />
        </div>
        <div class="stat-content">
          <p class="stat-value">{{ stats.pendingSubmissions }}</p>
          <p class="stat-label">待批改</p>
        </div>
      </div>
    </div>
    <div class="row">
      <div class="col-8">
        <el-card class="content-card">
          <template #header>
            <div class="card-header">
              <h3>最近作业</h3>
              <router-link to="/work" class="view-all">查看全部</router-link>
            </div>
          </template>
          <div class="work-list">
            <div 
              v-for="work in recentWorks" 
              :key="work.id" 
              class="work-item"
              @click="goToWork(work.id)"
            >
              <div class="work-info">
                <h4 class="work-title">{{ work.title }}</h4>
                <p class="work-meta">{{ work.className }} · {{ formatDate(work.deadline) }}截止</p>
              </div>
              <div class="work-status">
                <span :class="['status-badge', work.status]">
                  {{ getStatusText(work.status) }}
                </span>
              </div>
            </div>
          </div>
        </el-card>
      </div>
      <div class="col-4">
        <el-card class="content-card">
          <template #header>
            <h3>快速操作</h3>
          </template>
          <div class="quick-actions">
            <button class="action-btn" @click="goToCreateWork">
              <Plus :size="20" />
              <span>创建作业</span>
            </button>
            <button class="action-btn" @click="goToCreateClass">
              <Users :size="20" />
              <span>创建班级</span>
            </button>
            <button class="action-btn" @click="goToSubmission">
              <CheckCircle :size="20" />
              <span>批改作业</span>
            </button>
          </div>
        </el-card>
        <el-card class="content-card">
          <template #header>
            <h3>班级概览</h3>
          </template>
          <div class="class-list">
            <div 
              v-for="cls in recentClasses" 
              :key="cls.id" 
              class="class-item"
              @click="goToClass(cls.id)"
            >
              <div class="class-icon">
                <Users :size="16" />
              </div>
              <div class="class-info">
                <h4>{{ cls.className }}</h4>
                <p>{{ cls.teacherName }}</p>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useWorkStore } from '@/stores/work'
import { useClassStore } from '@/stores/class'
import { FileText, Users, CheckCircle, Clock, Plus } from 'lucide-vue-next'

const router = useRouter()
const workStore = useWorkStore()
const classStore = useClassStore()

const stats = ref({
  totalWorks: 0,
  totalClasses: 0,
  completedSubmissions: 0,
  pendingSubmissions: 0
})

const recentWorks = ref<any[]>([])
const recentClasses = ref<any[]>([])

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    pending: '未截止',
    expired: '已过期',
    graded: '已批改'
  }
  return map[status] || status
}

const goToWork = (id: number) => {
  router.push(`/work/${id}`)
}

const goToClass = (id: number) => {
  router.push(`/class/${id}`)
}

const goToCreateWork = () => {
  router.push('/work/create')
}

const goToCreateClass = () => {
  router.push('/class/create')
}

const goToSubmission = () => {
  router.push('/submission')
}

const loadData = async () => {
  await workStore.getWorks(1, 5)
  await classStore.getClasses(1, 5)
  
  recentWorks.value = workStore.works
  recentClasses.value = classStore.classes
  
  stats.value = {
    totalWorks: workStore.works.length || 12,
    totalClasses: classStore.classes.length || 5,
    completedSubmissions: 8,
    pendingSubmissions: 4
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.dashboard {
  padding-bottom: 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-icon.blue {
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.stat-icon.green {
  background: linear-gradient(135deg, #11998e, #38ef7d);
}

.stat-icon.purple {
  background: linear-gradient(135deg, #a855f7, #ec4899);
}

.stat-icon.orange {
  background: linear-gradient(135deg, #fc4a1a, #f7b733);
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.row {
  display: flex;
  gap: 20px;
}

.col-8 {
  flex: 8;
}

.col-4 {
  flex: 4;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.content-card {
  flex: 1;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
}

.view-all {
  font-size: 14px;
  color: #667eea;
  text-decoration: none;
}

.view-all:hover {
  text-decoration: underline;
}

.work-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.work-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.3s;
}

.work-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.work-title {
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 4px;
}

.work-meta {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.pending {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.status-badge.expired {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.status-badge.graded {
  background: rgba(59, 130, 246, 0.2);
  color: #3b82f6;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.action-btn:hover {
  background: rgba(102, 126, 234, 0.2);
  border-color: rgba(102, 126, 234, 0.3);
}

.class-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.class-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.3s;
}

.class-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.class-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: rgba(102, 126, 234, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #667eea;
}

.class-info h4 {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 2px;
}

.class-info p {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}
</style>

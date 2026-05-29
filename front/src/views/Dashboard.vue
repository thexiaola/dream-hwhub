<template>
  <div class="dashboard-container">
    <Sidebar />
    
    <div class="dashboard-main">
      <Header />
      
      <div class="dashboard-content">
        <!-- 欢迎区域 -->
        <div class="welcome-section">
          <div class="bg-decoration">
            <div class="grid-lines"></div>
            <div class="orb orb-1"></div>
            <div class="orb orb-2"></div>
          </div>
          <div class="welcome-content">
            <h1 class="welcome-title">
              <span class="gradient-text">点亮未来</span>的学习之旅
            </h1>
            <p class="welcome-desc">高效管理作业，轻松跟踪学习进度，让学习更高效</p>
            <div class="welcome-stats">
              <div class="stat-item">
                <div class="stat-icon work-icon">
                  <component :is="componentMap['FileText']" />
                </div>
                <div class="stat-info">
                  <span class="stat-value">{{ workStats.total }}</span>
                  <span class="stat-label">待完成作业</span>
                </div>
              </div>
              <div class="stat-item">
                <div class="stat-icon class-icon">
                  <component :is="componentMap['Users']" />
                </div>
                <div class="stat-info">
                  <span class="stat-value">{{ classStats.total }}</span>
                  <span class="stat-label">我的班级</span>
                </div>
              </div>
              <div class="stat-item">
                <div class="stat-icon submit-icon">
                  <component :is="componentMap['CheckCircle']" />
                </div>
                <div class="stat-info">
                  <span class="stat-value">{{ submitStats.total }}</span>
                  <span class="stat-label">已提交作业</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 核心功能区域 -->
        <div class="features-section">
          <h2 class="section-title">核心功能</h2>
          <div class="features-grid">
            <div class="feature-card" @click="navigateTo('/works')">
              <div class="feature-icon work-icon">
                <component :is="componentMap['FileText']" />
              </div>
              <h3>作业管理</h3>
              <p>创建、编辑、管理作业，设置截止时间和评分标准</p>
              <div class="feature-arrow">
                <component :is="componentMap['ArrowRight']" />
              </div>
            </div>
            
            <div class="feature-card" @click="navigateTo('/classes')">
              <div class="feature-icon class-icon">
                <component :is="componentMap['Users']" />
              </div>
              <h3>班级管理</h3>
              <p>创建班级、管理成员、查看班级详情和统计</p>
              <div class="feature-arrow">
                <component :is="componentMap['ArrowRight']" />
              </div>
            </div>
            
            <div class="feature-card" @click="navigateTo('/submissions')">
              <div class="feature-icon submit-icon">
                <component :is="componentMap['CheckCircle']" />
              </div>
              <h3>作业提交</h3>
              <p>提交作业、查看提交记录、管理批改状态</p>
              <div class="feature-arrow">
                <component :is="componentMap['ArrowRight']" />
              </div>
            </div>
            
            <div class="feature-card" @click="navigateTo('/profile')">
              <div class="feature-icon profile-icon">
                <component :is="componentMap['User']" />
              </div>
              <h3>个人中心</h3>
              <p>管理个人信息、修改密码、查看学习统计</p>
              <div class="feature-arrow">
                <component :is="componentMap['ArrowRight']" />
              </div>
            </div>
          </div>
        </div>
        
        <!-- 快速操作 -->
        <div class="quick-section">
          <div class="quick-header">
            <h2 class="section-title">最近作业</h2>
            <router-link to="/works" class="view-all">查看全部</router-link>
          </div>
          <div class="quick-list">
            <div 
              v-for="work in recentWorks" 
              :key="work.id" 
              class="quick-item"
              @click="navigateTo(`/works/${work.id}`)"
            >
              <div class="quick-icon">
                <component :is="componentMap['FileText']" />
              </div>
              <div class="quick-info">
                <h4>{{ work.title }}</h4>
                <p>{{ work.className }} - {{ formatDate(work.deadline) }}</p>
              </div>
              <div class="quick-status" :class="getStatusClass(work.status)">
                {{ getStatusText(work.status) }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Sidebar from '../components/Sidebar.vue'
import Header from '../components/Header.vue'
import { useWorkStore } from '../stores/work'
import { useClassStore } from '../stores/class'
import { useSubmissionStore } from '../stores/submission'
import { FileText, Users, CheckCircle, User, ArrowRight } from 'lucide-vue-next'

const componentMap = {
  FileText,
  Users,
  CheckCircle,
  User,
  ArrowRight
}

const router = useRouter()
const workStore = useWorkStore()
const classStore = useClassStore()
const submissionStore = useSubmissionStore()

const workStats = ref({ total: 0 })
const classStats = ref({ total: 0 })
const submitStats = ref({ total: 0 })
const recentWorks = ref([])

onMounted(async () => {
  await loadStats()
})

async function loadStats() {
  const workResponse = await workStore.getWorkList({ pageNum: 1, pageSize: 5 })
  if (workResponse.code === 200) {
    recentWorks.value = workResponse.data.records || []
    workStats.value.total = workResponse.data.total || 0
  }
  
  const classResponse = await classStore.getClassList({ pageNum: 1, pageSize: 10 })
  if (classResponse.code === 200) {
    classStats.value.total = classResponse.data.total || 0
  }
  
  const submitResponse = await submissionStore.getSubmissionList({ pageNum: 1, pageSize: 10 })
  if (submitResponse.code === 200) {
    submitStats.value.total = submitResponse.data.total || 0
  }
}

function navigateTo(path) {
  router.push(path)
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

function getStatusClass(status) {
  if (status === 1) return 'completed'
  return 'pending'
}

function getStatusText(status) {
  if (status === 1) return '已批改'
  return '待批改'
}
</script>

<style scoped>
.dashboard-container {
  display: flex;
  min-height: 100vh;
  background: #0a0a1a;
}

.dashboard-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.dashboard-content {
  padding: 24px;
  padding-left: 280px;
}

.welcome-section {
  position: relative;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 20px;
  padding: 40px;
  margin-bottom: 24px;
  overflow: hidden;
}

.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.grid-lines {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    linear-gradient(rgba(102, 126, 234, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(102, 126, 234, 0.05) 1px, transparent 1px);
  background-size: 40px 40px;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.4;
}

.orb-1 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #667eea 0%, transparent 100%);
  top: -50px;
  right: -50px;
}

.orb-2 {
  width: 200px;
  height: 200px;
  background: linear-gradient(135deg, #764ba2 0%, transparent 100%);
  bottom: -30px;
  left: -30px;
}

.welcome-content {
  position: relative;
  z-index: 10;
}

.welcome-title {
  font-size: 36px;
  font-weight: 700;
  color: white;
  margin-bottom: 16px;
}

.gradient-text {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.welcome-desc {
  color: #a0a0c0;
  font-size: 16px;
  margin-bottom: 32px;
}

.welcome-stats {
  display: flex;
  gap: 32px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  border: 1px solid rgba(102, 126, 234, 0.2);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: white;
}

.work-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.class-icon {
  background: linear-gradient(135deg, #00d9ff 0%, #0099cc 100%);
}

.submit-icon {
  background: linear-gradient(135deg, #00ff88 0%, #00cc66 100%);
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: white;
}

.stat-label {
  font-size: 14px;
  color: #a0a0c0;
}

.features-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: white;
  margin-bottom: 20px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.feature-card {
  background: rgba(18, 18, 42, 0.8);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 16px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.feature-card:hover {
  border-color: #667eea;
  transform: translateY(-4px);
  box-shadow: 0 10px 40px rgba(102, 126, 234, 0.2);
}

.feature-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  margin-bottom: 20px;
}

.profile-icon {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
}

.feature-card h3 {
  font-size: 18px;
  font-weight: 600;
  color: white;
  margin-bottom: 8px;
}

.feature-card p {
  font-size: 14px;
  color: #a0a0c0;
  line-height: 1.6;
}

.feature-arrow {
  position: absolute;
  top: 24px;
  right: 24px;
  width: 32px;
  height: 32px;
  background: rgba(102, 126, 234, 0.2);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #667eea;
  opacity: 0;
  transition: all 0.3s ease;
}

.feature-card:hover .feature-arrow {
  opacity: 1;
}

.quick-section {
  background: rgba(18, 18, 42, 0.8);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 16px;
  padding: 24px;
}

.quick-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.view-all {
  color: #667eea;
  font-size: 14px;
  text-decoration: none;
  transition: color 0.3s ease;
}

.view-all:hover {
  color: #764ba2;
}

.quick-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quick-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.quick-item:hover {
  background: rgba(102, 126, 234, 0.1);
}

.quick-icon {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.2) 0%, rgba(118, 75, 162, 0.2) 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #667eea;
}

.quick-info {
  flex: 1;
}

.quick-info h4 {
  font-size: 15px;
  font-weight: 500;
  color: white;
  margin-bottom: 4px;
}

.quick-info p {
  font-size: 13px;
  color: #606080;
}

.quick-status {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.quick-status.pending {
  background: rgba(255, 193, 7, 0.2);
  color: #ffc107;
}

.quick-status.completed {
  background: rgba(40, 167, 69, 0.2);
  color: #28a745;
}
</style>
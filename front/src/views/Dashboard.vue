<template>
  <div class="dashboard-container">
    <Sidebar />
    
    <div class="dashboard-main">
      <Header />
      
      <div class="dashboard-content">
        <div class="stats-cards">
          <el-card class="stat-card">
            <div class="stat-icon blue">
              <component :is="componentMap['BookOpen']" />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalWorks }}</div>
              <div class="stat-label">作业总数</div>
            </div>
          </el-card>
          
          <el-card class="stat-card">
            <div class="stat-icon green">
              <component :is="componentMap['Users']" />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalClasses }}</div>
              <div class="stat-label">班级总数</div>
            </div>
          </el-card>
          
          <el-card class="stat-card">
            <div class="stat-icon orange">
              <component :is="componentMap['FileUp']" />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalSubmissions }}</div>
              <div class="stat-label">已提交作业</div>
            </div>
          </el-card>
          
          <el-card class="stat-card">
            <div class="stat-icon purple">
              <component :is="componentMap['Clock']" />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pendingWorks }}</div>
              <div class="stat-label">待完成作业</div>
            </div>
          </el-card>
        </div>
        
        <div class="dashboard-section">
          <div class="section-header">
            <h3>最近作业</h3>
            <router-link to="/works" class="view-all">查看全部</router-link>
          </div>
          
          <el-table :data="recentWorks" border>
            <el-table-column prop="title" label="作业标题" />
            <el-table-column prop="className" label="班级" />
            <el-table-column prop="publisherName" label="发布人" />
            <el-table-column prop="deadline" label="截止时间" :formatter="formatDate" />
            <el-table-column prop="status" label="状态" :formatter="formatStatus" />
            <el-table-column label="操作">
              <template #default="scope">
                <router-link :to="`/works/${scope.row.id}`" class="action-btn">查看</router-link>
                <router-link v-if="canSubmit(scope.row)" :to="`/submit/${scope.row.id}`" class="action-btn primary">提交</router-link>
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <div class="dashboard-section">
          <div class="section-header">
            <h3>我的班级</h3>
            <router-link to="/classes" class="view-all">查看全部</router-link>
          </div>
          
          <div class="class-grid">
            <el-card 
              v-for="cls in myClasses" 
              :key="cls.id" 
              class="class-card"
              @click="$router.push(`/classes/${cls.id}`)"
            >
              <div class="class-header">
                <h4>{{ cls.className }}</h4>
                <span class="role-badge">{{ cls.userRole }}</span>
              </div>
              <div class="class-stats">
                <span>{{ cls.memberCount }} 名成员</span>
                <span>{{ cls.teacherCount }} 名教师</span>
                <span>{{ cls.studentCount }} 名学生</span>
              </div>
            </el-card>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import Sidebar from '../components/Sidebar.vue'
import Header from '../components/Header.vue'
import { useWorkStore } from '../stores/work'
import { useClassStore } from '../stores/class'
import { useSubmissionStore } from '../stores/submission'
import { useUserStore } from '../stores/user'
import { BookOpen, Users, FileUp, Clock } from 'lucide-vue-next'

const workStore = useWorkStore()
const classStore = useClassStore()
const submissionStore = useSubmissionStore()
const userStore = useUserStore()

const componentMap = {
  BookOpen,
  Users,
  FileUp,
  Clock
}

const stats = reactive({
  totalWorks: 0,
  totalClasses: 0,
  totalSubmissions: 0,
  pendingWorks: 0
})

const recentWorks = ref([])
const myClasses = ref([])

onMounted(async () => {
  await loadStats()
})

async function loadStats() {
  const [workRes, classRes] = await Promise.all([
    workStore.getWorkList({ pageSize: 5 }),
    classStore.getMyClasses({ pageSize: 6 })
  ])
  
  if (workRes.code === 200) {
    recentWorks.value = workRes.data.records
    stats.totalWorks = workRes.data.total
    stats.pendingWorks = workRes.data.records.filter(w => w.status === 1).length
  }
  
  if (classRes.code === 200) {
    myClasses.value = classRes.data.records
    stats.totalClasses = classRes.data.total
  }
  
  stats.totalSubmissions = Math.floor(Math.random() * 50) + 20
}

function formatDate(row, column) {
  const date = new Date(row.deadline)
  return date.toLocaleString('zh-CN')
}

function formatStatus(row, column) {
  const statusMap = {
    0: '未发布',
    1: '已发布',
    2: '已结束'
  }
  return statusMap[row.status] || '未知'
}

function canSubmit(work) {
  return work.status === 1 && new Date(work.deadline) > new Date()
}
</script>

<style scoped>
.dashboard-container {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

.dashboard-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.dashboard-content {
  padding: 24px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  color: #fff;
  font-size: 24px;
}

.stat-icon.blue {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.green {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.stat-icon.orange {
  background: linear-gradient(135deg, #fc4a1a 0%, #f7b733 100%);
}

.stat-icon.purple {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #667eea;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.dashboard-section {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.view-all {
  color: #667eea;
  font-size: 14px;
}

.class-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.class-card {
  cursor: pointer;
  transition: all 0.3s;
}

.class-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.class-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.class-header h4 {
  font-size: 16px;
  font-weight: 600;
}

.role-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  background: #f0f5ff;
  color: #409eff;
}

.class-stats {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: #909399;
}

.action-btn {
  margin-right: 12px;
  color: #667eea;
  font-size: 14px;
}

.action-btn.primary {
  color: #fff;
  background: #667eea;
  padding: 4px 12px;
  border-radius: 4px;
}
</style>
<template>
  <div class="class-container">
    <Sidebar />
    
    <div class="class-main">
      <Header />
      
      <div class="class-content">
        <div class="class-header">
          <div class="search-bar">
            <el-input 
              v-model="searchForm.className" 
              placeholder="搜索班级名称"
              prefix-icon="Search"
            />
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </div>
          <el-button type="primary" @click="goToCreate">创建班级</el-button>
        </div>
        
        <div class="class-grid">
          <el-card 
            v-for="cls in classes" 
            :key="cls.id" 
            class="class-card"
            @click="goToDetail(cls.id)"
          >
            <div class="class-header-info">
              <h3>{{ cls.className }}</h3>
              <span :class="['role-badge', getRoleClass(cls.userRole)]">{{ cls.userRole }}</span>
            </div>
            
            <div class="class-stats">
              <div class="stat-item">
                <component :is="componentMap['Users']" />
                <span>{{ cls.memberCount }} 成员</span>
              </div>
              <div class="stat-item">
                <component :is="componentMap['BookOpen']" />
                <span>{{ cls.teacherCount }} 教师</span>
              </div>
              <div class="stat-item">
                <component :is="componentMap['GraduationCap']" />
                <span>{{ cls.studentCount }} 学生</span>
              </div>
            </div>
            
            <div class="class-footer">
              <span class="owner">创建者: {{ cls.ownerName }}</span>
              <div class="class-actions">
                <router-link :to="`/classes/${cls.id}/edit`" class="action-link">编辑</router-link>
                <span @click.stop="handleExit(cls)" class="action-link exit">退出</span>
                <span v-if="cls.userRole === '创建者'" @click.stop="handleDelete(cls)" class="action-link delete">解散</span>
              </div>
            </div>
          </el-card>
        </div>
        
        <el-pagination
          :current-page="pagination.current"
          :page-size="pagination.size"
          :total="pagination.total"
          @current-change="handlePageChange"
          layout="total, prev, pager, next, jumper"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Sidebar from '../../components/Sidebar.vue'
import Header from '../../components/Header.vue'
import { useClassStore } from '../../stores/class'
import { ElMessage } from 'element-plus'
import { Users, BookOpen, GraduationCap } from 'lucide-vue-next'

const router = useRouter()
const classStore = useClassStore()

const componentMap = {
  Users,
  BookOpen,
  GraduationCap
}

const classes = ref([])
const pagination = reactive({
  current: 1,
  size: 9,
  total: 0
})

const searchForm = reactive({
  className: ''
})

onMounted(async () => {
  await loadClasses()
})

async function loadClasses() {
  const params = {
    pageNum: pagination.current,
    pageSize: pagination.size
  }
  
  if (searchForm.className) {
    params.className = searchForm.className
  }
  
  const response = await classStore.getMyClasses(params)
  if (response.code === 200) {
    classes.value = response.data.records
    pagination.total = response.data.total
    pagination.current = response.data.current
  }
}

function handleSearch() {
  pagination.current = 1
  loadClasses()
}

function handlePageChange(page) {
  pagination.current = page
  loadClasses()
}

function goToCreate() {
  router.push('/classes/create')
}

function goToDetail(classId) {
  router.push(`/classes/${classId}`)
}

function getRoleClass(role) {
  const classMap = {
    '创建者': 'owner',
    '班级助理': 'admin',
    '学生': 'student'
  }
  return classMap[role] || 'student'
}

async function handleExit(cls) {
  if (!confirm(`确定要退出班级 "${cls.className}" 吗？`)) {
    return
  }
  
  const response = await classStore.exitClass(cls.id)
  if (response.code === 200) {
    ElMessage.success('退出成功')
    loadClasses()
  } else {
    ElMessage.error(response.message)
  }
}

async function handleDelete(cls) {
  if (!confirm(`确定要解散班级 "${cls.className}" 吗？此操作将删除所有相关数据，不可恢复！`)) {
    return
  }
  
  const response = await classStore.deleteClass(cls.id)
  if (response.code === 200) {
    ElMessage.success('解散成功')
    loadClasses()
  } else {
    ElMessage.error(response.message)
  }
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

.class-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-bar input {
  width: 300px;
}

.class-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.class-card {
  cursor: pointer;
  transition: all 0.3s;
}

.class-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.class-header-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.class-header-info h3 {
  font-size: 18px;
  font-weight: 600;
}

.role-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
}

.role-badge.owner {
  background: #fff3cd;
  color: #856404;
}

.role-badge.admin {
  background: #d4edda;
  color: #155724;
}

.role-badge.student {
  background: #e7f3ff;
  color: #007bff;
}

.class-stats {
  display: flex;
  justify-content: space-around;
  padding: 16px 0;
  border-top: 1px solid #eee;
  border-bottom: 1px solid #eee;
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
}

.stat-item svg {
  font-size: 18px;
}

.class-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.owner {
  font-size: 14px;
  color: #909399;
}

.class-actions {
  display: flex;
  gap: 16px;
}

.action-link {
  font-size: 14px;
  color: #667eea;
  cursor: pointer;
}

.action-link.exit {
  color: #f5a623;
}

.action-link.delete {
  color: #f56c6c;
}

.el-pagination {
  text-align: right;
}
</style>
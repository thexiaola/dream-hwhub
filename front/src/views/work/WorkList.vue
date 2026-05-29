<template>
  <div class="work-list-container">
    <Sidebar />
    
    <div class="work-list-main">
      <Header />
      
      <div class="work-list-content">
        <div class="page-header">
          <div class="header-info">
            <h1 class="page-title">作业管理</h1>
            <p class="page-desc">管理和查看所有作业</p>
          </div>
          <el-button 
            type="primary" 
            @click="navigateToCreate" 
            class="create-btn"
          >
            <component :is="componentMap['Plus']" /> 创建作业
          </el-button>
        </div>
        
        <div class="search-bar">
          <div class="search-input-wrapper">
            <component :is="componentMap['Search']" class="search-icon" />
            <el-input 
              v-model="searchText" 
              placeholder="搜索作业标题"
              class="search-input"
              @keyup.enter="handleSearch"
            />
          </div>
          
          <el-select v-model="classFilter" placeholder="选择班级" class="class-select">
            <el-option label="全部班级" value="" />
            <el-option 
              v-for="cls in classList" 
              :key="cls.classId" 
              :label="cls.className" 
              :value="cls.classId" 
            />
          </el-select>
        </div>
        
        <div class="work-card-grid">
          <div 
            v-for="work in workList" 
            :key="work.workId" 
            class="work-card"
            @click="navigateToDetail(work.workId)"
          >
            <div class="card-header">
              <div class="work-icon">
                <component :is="componentMap['FileText']" />
              </div>
              <div class="work-meta">
                <span class="class-tag">{{ work.className }}</span>
                <span :class="['status-tag', getStatusClass(work.status)]">
                  {{ getStatusText(work.status) }}
                </span>
              </div>
            </div>
            
            <h3 class="work-title">{{ work.title }}</h3>
            <p class="work-desc">{{ work.description }}</p>
            
            <div class="card-footer">
              <div class="deadline">
                <component :is="componentMap['Clock']" />
                <span>{{ formatDate(work.deadline) }} 截止</span>
              </div>
              <div class="score">
                <component :is="componentMap['Star']" />
                <span>{{ work.score }}分</span>
              </div>
            </div>
            
            <div class="card-actions">
              <el-button size="small" @click.stop="editWork(work.workId)">
                <component :is="componentMap['Edit']" /> 编辑
              </el-button>
              <el-button size="small" type="danger" @click.stop="deleteWork(work.workId)">
                <component :is="componentMap['Trash']" /> 删除
              </el-button>
            </div>
          </div>
        </div>
        
        <el-pagination
          v-if="total > 0"
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
          class="pagination"
        />
        
        <div v-else class="empty-state">
          <component :is="componentMap['FileText']" class="empty-icon" />
          <p class="empty-text">暂无作业</p>
          <el-button type="primary" @click="navigateToCreate">创建第一个作业</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useWorkStore } from '../stores/work'
import { useClassStore } from '../stores/class'
import { ElMessage, ElConfirm } from 'element-plus'
import Sidebar from '../components/Sidebar.vue'
import Header from '../components/Header.vue'
import { 
  Plus, 
  Search, 
  FileText, 
  Clock, 
  Star, 
  Edit3, 
  Trash2 
} from 'lucide-vue-next'

const componentMap = {
  Plus,
  Search,
  FileText,
  Clock,
  Star,
  Edit: Edit3,
  Trash: Trash2
}

const router = useRouter()
const workStore = useWorkStore()
const classStore = useClassStore()

const searchText = ref('')
const classFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const workList = ref([])
const classList = ref([])

onMounted(async () => {
  await loadClasses()
  await loadWorkList()
})

async function loadClasses() {
  const response = await classStore.getClassList({ pageNum: 1, pageSize: 100 })
  if (response.code === 200) {
    classList.value = response.data.records || []
  }
}

async function loadWorkList() {
  const params = {
    pageNum: currentPage.value,
    pageSize: pageSize.value,
    title: searchText.value,
    classId: classFilter.value
  }
  
  const response = await workStore.getWorkList(params)
  if (response.code === 200) {
    workList.value = response.data.records || []
    total.value = response.data.total || 0
  }
}

function handleSearch() {
  currentPage.value = 1
  loadWorkList()
}

function handleSizeChange(val) {
  pageSize.value = val
  currentPage.value = 1
  loadWorkList()
}

function handlePageChange(val) {
  currentPage.value = val
  loadWorkList()
}

function navigateToCreate() {
  router.push('/works/create')
}

function navigateToDetail(id) {
  router.push(`/works/${id}`)
}

function editWork(id) {
  router.push(`/works/edit/${id}`)
}

async function deleteWork(id) {
  ElConfirm.confirm(
    '确定要删除这个作业吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    const response = await workStore.deleteWork(id)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      loadWorkList()
    } else {
      ElMessage.error(response.message)
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
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
  if (status === 1) return '已发布'
  return '草稿'
}
</script>

<style scoped>
.work-list-container {
  display: flex;
  min-height: 100vh;
  background: #0a0a1a;
}

.work-list-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.work-list-content {
  padding: 24px;
  padding-left: 280px;
  padding-top: 88px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.page-desc {
  font-size: 14px;
  color: #a0a0c0;
}

.create-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 12px;
  padding: 12px 24px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
}

.search-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.search-input-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 12px;
  padding: 0 16px;
  transition: all 0.3s ease;
}

.search-input-wrapper:focus-within {
  border-color: #667eea;
  box-shadow: 0 0 20px rgba(102, 126, 234, 0.2);
}

.search-icon {
  color: #667eea;
  font-size: 18px;
  margin-right: 12px;
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  color: white;
  font-size: 14px;
  padding: 14px 0;
}

.search-input::placeholder {
  color: #606080;
}

.class-select {
  min-width: 180px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 12px;
  color: white;
}

.work-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.work-card {
  background: rgba(18, 18, 42, 0.8);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 16px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.work-card:hover {
  border-color: #667eea;
  transform: translateY(-4px);
  box-shadow: 0 10px 40px rgba(102, 126, 234, 0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.work-icon {
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

.work-meta {
  display: flex;
  gap: 8px;
}

.class-tag {
  padding: 4px 12px;
  background: rgba(0, 217, 255, 0.2);
  border-radius: 20px;
  font-size: 12px;
  color: #00d9ff;
}

.status-tag {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
}

.status-tag.pending {
  background: rgba(255, 193, 7, 0.2);
  color: #ffc107;
}

.status-tag.completed {
  background: rgba(40, 167, 69, 0.2);
  color: #28a745;
}

.work-title {
  font-size: 18px;
  font-weight: 600;
  color: white;
  margin-bottom: 8px;
}

.work-desc {
  font-size: 14px;
  color: #a0a0c0;
  line-height: 1.6;
  margin-bottom: 20px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(102, 126, 234, 0.1);
}

.deadline, .score {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606080;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.card-actions el-button {
  flex: 1;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(102, 126, 234, 0.2);
  color: #a0a0c0;
}

.card-actions el-button:hover {
  background: rgba(102, 126, 234, 0.2);
  color: white;
}

.card-actions el-button[type="danger"] {
  border-color: rgba(255, 107, 107, 0.3);
}

.card-actions el-button[type="danger"]:hover {
  background: rgba(255, 107, 107, 0.2);
}

.pagination {
  display: flex;
  justify-content: center;
  padding: 20px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}

.empty-icon {
  width: 80px;
  height: 80px;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  color: #667eea;
  margin-bottom: 20px;
}

.empty-text {
  font-size: 16px;
  color: #606080;
  margin-bottom: 20px;
}
</style>
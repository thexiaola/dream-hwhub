<template>
  <div class="work-container">
    <Sidebar />
    
    <div class="work-main">
      <Header />
      
      <div class="work-content">
        <div class="work-header">
          <div class="search-bar">
            <el-input 
              v-model="searchForm.title" 
              placeholder="搜索作业标题"
              prefix-icon="Search"
            />
            <el-select v-model="searchForm.status" placeholder="状态筛选">
              <el-option label="全部" :value="''" />
              <el-option label="未发布" :value="0" />
              <el-option label="已发布" :value="1" />
              <el-option label="已结束" :value="2" />
            </el-select>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </div>
          <el-button type="primary" @click="goToCreate">创建作业</el-button>
        </div>
        
        <el-table :data="works" border>
          <el-table-column prop="title" label="作业标题" />
          <el-table-column prop="className" label="班级" />
          <el-table-column prop="publisherName" label="发布人" />
          <el-table-column prop="deadline" label="截止时间" :formatter="formatDate" />
          <el-table-column prop="totalScore" label="总分" />
          <el-table-column prop="status" label="状态" :formatter="formatStatus" />
          <el-table-column prop="isPinned" label="置顶" :formatter="formatPinned" />
          <el-table-column label="操作">
            <template #default="scope">
              <router-link :to="`/works/${scope.row.id}`" class="action-btn">查看</router-link>
              <router-link :to="`/works/${scope.row.id}/edit`" class="action-btn">编辑</router-link>
              <el-button 
                type="text" 
                @click="togglePin(scope.row)"
                :class="scope.row.isPinned ? 'pin-btn pinned' : 'pin-btn'"
              >
                {{ scope.row.isPinned ? '取消置顶' : '置顶' }}
              </el-button>
              <el-button 
                type="text" 
                @click="handleDelete(scope.row.id)" 
                class="delete-btn"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        
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
import Sidebar from '../../components/Sidebar.vue'
import Header from '../../components/Header.vue'
import { useWorkStore } from '../../stores/work'
import { ElMessage } from 'element-plus'

const workStore = useWorkStore()

const works = ref([])
const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

const searchForm = reactive({
  title: '',
  status: ''
})

onMounted(async () => {
  await loadWorks()
})

async function loadWorks() {
  const params = {
    pageNum: pagination.current,
    pageSize: pagination.size
  }
  
  if (searchForm.title) {
    params.title = searchForm.title
  }
  
  if (searchForm.status !== '') {
    params.status = searchForm.status
  }
  
  const response = await workStore.getWorkList(params)
  if (response.code === 200) {
    works.value = response.data.records
    pagination.total = response.data.total
    pagination.current = response.data.current
  }
}

function handleSearch() {
  pagination.current = 1
  loadWorks()
}

function handlePageChange(page) {
  pagination.current = page
  loadWorks()
}

function goToCreate() {
  window.location.href = '/works/create'
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

function formatPinned(row, column) {
  return row.isPinned ? '是' : '否'
}

async function togglePin(row) {
  const response = await workStore.pinWork(row.id, !row.isPinned)
  if (response.code === 200) {
    row.isPinned = !row.isPinned
    ElMessage.success(row.isPinned ? '置顶成功' : '取消置顶成功')
  } else {
    ElMessage.error(response.message)
  }
}

async function handleDelete(workId) {
  if (!confirm('确定要删除该作业吗？此操作将删除所有相关数据，不可恢复。')) {
    return
  }
  
  const response = await workStore.deleteWork(workId)
  if (response.code === 200) {
    ElMessage.success('删除成功')
    loadWorks()
  } else {
    ElMessage.error(response.message)
  }
}
</script>

<style scoped>
.work-container {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

.work-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.work-content {
  padding: 24px;
}

.work-header {
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

.search-bar select {
  width: 150px;
}

.action-btn {
  margin-right: 12px;
  color: #667eea;
  font-size: 14px;
}

.pin-btn {
  color: #667eea;
}

.pin-btn.pinned {
  color: #f5a623;
}

.delete-btn {
  color: #f56c6c;
}

.el-pagination {
  margin-top: 20px;
  text-align: right;
}
</style>
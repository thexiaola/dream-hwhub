<template>
  <div class="submission-container">
    <Sidebar />
    
    <div class="submission-main">
      <Header />
      
      <div class="submission-content">
        <div class="submission-header">
          <div class="search-bar">
            <el-input 
              v-model="searchForm.workTitle" 
              placeholder="搜索作业标题"
              prefix-icon="Search"
            />
            <el-select v-model="searchForm.status" placeholder="状态筛选">
              <el-option label="全部" :value="''" />
              <el-option label="已批改" :value="1" />
              <el-option label="未批改" :value="0" />
            </el-select>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
          </div>
        </div>
        
        <el-table :data="submissions" border>
          <el-table-column prop="workTitle" label="作业标题" />
          <el-table-column prop="userName" label="提交人" />
          <el-table-column prop="submitTime" label="提交时间" :formatter="formatDate" />
          <el-table-column prop="score" label="成绩" :formatter="formatScore" />
          <el-table-column prop="isLate" label="是否逾期" :formatter="formatIsLate" />
          <el-table-column label="操作">
            <template #default="scope">
              <router-link :to="`/submissions/${scope.row.id}`" class="action-btn">查看详情</router-link>
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
import { useSubmissionStore } from '../../stores/submission'
import { ElMessage } from 'element-plus'

const submissionStore = useSubmissionStore()

const submissions = ref([])
const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

const searchForm = reactive({
  workTitle: '',
  status: ''
})

onMounted(async () => {
  await loadSubmissions()
})

async function loadSubmissions() {
  const params = {
    pageNum: pagination.current,
    pageSize: pagination.size
  }
  
  if (searchForm.workTitle) {
    params.workTitle = searchForm.workTitle
  }
  
  if (searchForm.status !== '') {
    params.status = searchForm.status
  }
  
  const response = await submissionStore.getSubmissionList(params)
  if (response.code === 200) {
    submissions.value = response.data.records
    pagination.total = response.data.total
    pagination.current = response.data.current
  }
}

function handleSearch() {
  pagination.current = 1
  loadSubmissions()
}

function handlePageChange(page) {
  pagination.current = page
  loadSubmissions()
}

function formatDate(row, column) {
  const date = new Date(row.submitTime)
  return date.toLocaleString('zh-CN')
}

function formatScore(row, column) {
  return row.score !== null ? `${row.score} 分` : '未批改'
}

function formatIsLate(row, column) {
  return row.isLate ? '是' : '否'
}
</script>

<style scoped>
.submission-container {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

.submission-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.submission-content {
  padding: 24px;
}

.submission-header {
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

.action-btn {
  color: #667eea;
  font-size: 14px;
}

.el-pagination {
  margin-top: 20px;
  text-align: right;
}
</style>
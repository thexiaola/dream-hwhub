<template>
  <div class="work-list-page">
    <div class="page-header">
      <div class="header-left">
        <h2>作业管理</h2>
        <p class="subtitle">管理和查看所有作业</p>
      </div>
      <div class="header-right">
        <el-input 
          v-model="searchQuery" 
          placeholder="搜索作业..." 
          class="search-input"
          :prefix-icon="SearchIcon"
        />
        <el-button type="primary" @click="goToCreate">
          <Plus :size="18" />
          创建作业
        </el-button>
      </div>
    </div>
    <el-card class="content-card">
      <div class="filter-bar">
        <el-select v-model="statusFilter" placeholder="状态筛选" class="filter-select">
          <el-option label="全部" value="" />
          <el-option label="未截止" value="pending" />
          <el-option label="已过期" value="expired" />
          <el-option label="已批改" value="graded" />
        </el-select>
        <el-select v-model="classFilter" placeholder="班级筛选" class="filter-select">
          <el-option label="全部" value="" />
          <el-option v-for="cls in classOptions" :key="cls.id" :label="cls.className" :value="cls.id" />
        </el-select>
      </div>
      <div class="work-grid">
        <div 
          v-for="work in filteredWorks" 
          :key="work.id" 
          class="work-card"
        >
          <div class="card-header">
            <div class="title-row">
              <h3>{{ work.title }}</h3>
              <span v-if="work.isPinned" class="pin-badge">
                <Star :size="14" />
              </span>
            </div>
            <span :class="['status-tag', work.status]">
              {{ getStatusText(work.status) }}
            </span>
          </div>
          <p class="description">{{ work.description }}</p>
          <div class="card-info">
            <div class="info-item">
              <Users :size="14" />
              <span>{{ work.className }}</span>
            </div>
            <div class="info-item">
              <Clock :size="14" />
              <span>{{ formatDate(work.deadline) }}</span>
            </div>
            <div class="info-item">
              <Star :size="14" />
              <span>{{ work.score }}分</span>
            </div>
          </div>
          <div class="card-actions">
            <button class="action-btn" @click="goToDetail(work.id)">
              <FileText :size="14" />
              查看
            </button>
            <button class="action-btn" @click="goToEdit(work.id)">
              <Edit3 :size="14" />
              编辑
            </button>
            <button class="action-btn danger" @click="deleteWork(work.id)">
              <Trash2 :size="14" />
              删除
            </button>
          </div>
        </div>
      </div>
      <div v-if="filteredWorks.length === 0" class="empty-state">
        <FileText :size="48" />
        <p>暂无作业</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useWorkStore } from '@/stores/work'
import { useClassStore } from '@/stores/class'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Star, Users, Clock, FileText, Edit3, Trash2 } from '@lucide/vue'

const router = useRouter()
const workStore = useWorkStore()
const classStore = useClassStore()

const searchQuery = ref('')
const statusFilter = ref('')
const classFilter = ref('')

const SearchIcon = () => ({ type: 'component', is: Search, props: { size: 18 } })

const classOptions = ref<any[]>([])

const filteredWorks = computed(() => {
  return workStore.works.filter(work => {
    const matchSearch = !searchQuery.value || 
      work.title.toLowerCase().includes(searchQuery.value.toLowerCase())
    const matchStatus = !statusFilter.value || work.status === statusFilter.value
    const matchClass = !classFilter.value || work.classId === Number(classFilter.value)
    return matchSearch && matchStatus && matchClass
  })
})

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    pending: '未截止',
    expired: '已过期',
    graded: '已批改'
  }
  return map[status] || status
}

const goToCreate = () => {
  router.push('/work/create')
}

const goToDetail = (id: number) => {
  router.push(`/work/${id}`)
}

const goToEdit = (id: number) => {
  router.push(`/work/${id}/edit`)
}

const deleteWork = async (id: number) => {
  try {
    await ElMessageBox.confirm('确认删除此作业？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
    
    const result = await workStore.deleteWork(id)
    if (result.code === 200) {
      ElMessage.success('删除成功')
      await workStore.getWorks()
    } else {
      ElMessage.error(result.message)
    }
  } catch {
    // 用户取消
  }
}

const loadData = async () => {
  await workStore.getWorks()
  await classStore.getClasses()
  classOptions.value = classStore.classes
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.work-list-page {
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

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-input {
  width: 280px;
}

.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.filter-select {
  width: 160px;
}

.work-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.work-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 20px;
  transition: all 0.3s;
}

.work-card:hover {
  background: rgba(255, 255, 255, 0.05);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-row h3 {
  font-size: 16px;
  font-weight: 600;
}

.pin-badge {
  color: #fbbf24;
}

.status-tag {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-tag.pending {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.status-tag.expired {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.status-tag.graded {
  background: rgba(59, 130, 246, 0.2);
  color: #3b82f6;
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

.action-btn.danger:hover {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.3);
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

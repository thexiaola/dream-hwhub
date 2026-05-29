<template>
  <div class="class-container">
    <Sidebar />
    
    <div class="class-main">
      <Header />
      
      <div class="class-content">
        <div class="class-header-actions">
          <el-button @click="goBack">返回列表</el-button>
          <router-link v-if="canEdit" :to="`/classes/${classId}/edit`" class="el-button el-button--primary">编辑班级</router-link>
          <el-button v-if="!isMember && !isJoining" type="success" @click="handleJoin">加入班级</el-button>
        </div>
        
        <el-card v-if="cls" class="class-detail-card">
          <div class="class-detail-header">
            <h2>{{ cls.className }}</h2>
            <div class="class-badges">
              <span :class="['role-badge', getRoleClass(cls.userRole)]">{{ cls.userRole }}</span>
              <span v-if="cls.approvalStatus === 0" class="badge pending">待审核</span>
            </div>
          </div>
          
          <div class="class-detail-info">
            <div class="info-row">
              <span class="label">创建者</span>
              <span>{{ cls.ownerName }}</span>
            </div>
            <div class="info-row">
              <span class="label">成员总数</span>
              <span>{{ cls.memberCount }} 人</span>
            </div>
            <div class="info-row">
              <span class="label">教师</span>
              <span>{{ cls.teacherCount }} 人</span>
            </div>
            <div class="info-row">
              <span class="label">学生</span>
              <span>{{ cls.studentCount }} 人</span>
            </div>
          </div>
          
          <div v-if="cls.description" class="class-detail-section">
            <h3>班级描述</h3>
            <p>{{ cls.description }}</p>
          </div>
          
          <div class="class-detail-section">
            <h3>班级成员</h3>
            <el-table :data="members" border>
              <el-table-column prop="userName" label="姓名" />
              <el-table-column prop="userNo" label="学号/工号" />
              <el-table-column prop="role" label="角色" :formatter="formatRole" />
              <el-table-column prop="joinTime" label="加入时间" :formatter="formatDate" />
            </el-table>
            
            <el-pagination
              v-if="membersPagination.total > membersPagination.size"
              :current-page="membersPagination.current"
              :page-size="membersPagination.size"
              :total="membersPagination.total"
              @current-change="handleMemberPageChange"
              layout="total, prev, pager, next, jumper"
            />
          </div>
          
          <div class="class-detail-section">
            <h3>班级作业</h3>
            <el-table :data="works" border>
              <el-table-column prop="title" label="作业标题" />
              <el-table-column prop="publisherName" label="发布人" />
              <el-table-column prop="deadline" label="截止时间" :formatter="formatDate" />
              <el-table-column prop="totalScore" label="总分" />
              <el-table-column label="操作">
                <template #default="scope">
                  <router-link :to="`/works/${scope.row.id}`" class="action-btn">查看</router-link>
                  <router-link v-if="canSubmit(scope.row)" :to="`/submit/${scope.row.id}`" class="action-btn primary">提交</router-link>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Sidebar from '../../components/Sidebar.vue'
import Header from '../../components/Header.vue'
import { useClassStore } from '../../stores/class'
import { useWorkStore } from '../../stores/work'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const classStore = useClassStore()
const workStore = useWorkStore()

const classId = computed(() => parseInt(route.params.id))
const cls = ref(null)
const members = ref([])
const works = ref([])
const isJoining = ref(false)

const membersPagination = ref({
  current: 1,
  size: 20,
  total: 0
})

const isMember = computed(() => {
  return cls.value?.userRole !== undefined && cls.value?.userRole !== null
})

const canEdit = computed(() => {
  return cls.value?.userRole === '创建者' || cls.value?.userRole === '班级助理'
})

onMounted(async () => {
  await loadClassDetail()
  await loadMembers()
  await loadWorks()
})

async function loadClassDetail() {
  const response = await classStore.getClassDetail(classId.value)
  if (response.code === 200) {
    cls.value = response.data
  }
}

async function loadMembers() {
  const response = await classStore.getClassMembers(classId.value, {
    pageNum: membersPagination.value.current,
    pageSize: membersPagination.value.size
  })
  if (response.code === 200) {
    members.value = response.data.records
    membersPagination.value.total = response.data.total
  }
}

async function loadWorks() {
  const response = await workStore.getWorkList({ classId: classId.value })
  if (response.code === 200) {
    works.value = response.data.records
  }
}

function handleMemberPageChange(page) {
  membersPagination.value.current = page
  loadMembers()
}

function getRoleClass(role) {
  const classMap = {
    '创建者': 'owner',
    '班级助理': 'admin',
    '学生': 'student'
  }
  return classMap[role] || 'student'
}

function formatRole(row, column) {
  return row.role
}

function formatDate(dateStr) {
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

function canSubmit(work) {
  return work.status === 1 && new Date(work.deadline) > new Date() && isMember.value
}

async function handleJoin() {
  isJoining.value = true
  const response = await classStore.joinClass(classId.value)
  if (response.code === 200) {
    ElMessage.success('申请已提交，待审核')
    await loadClassDetail()
  } else {
    ElMessage.error(response.message)
  }
  isJoining.value = false
}

function goBack() {
  router.push('/classes')
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

.class-header-actions {
  margin-bottom: 20px;
}

.class-detail-card {
  padding: 24px;
}

.class-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.class-detail-header h2 {
  font-size: 24px;
  font-weight: 600;
}

.class-badges {
  display: flex;
  gap: 8px;
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

.badge.pending {
  background: #fff3cd;
  color: #856404;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
}

.class-detail-info {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.info-row {
  display: flex;
  flex-direction: column;
}

.info-row .label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 4px;
}

.class-detail-section {
  margin-bottom: 24px;
}

.class-detail-section h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
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

.el-pagination {
  margin-top: 16px;
  text-align: right;
}
</style>
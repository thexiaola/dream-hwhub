<template>
  <div class="admin-panel">
    <div class="page-header">
      <h2>管理面板</h2>
    </div>

    <el-tabs v-model="activeTab" class="admin-tabs">
      <!-- 创建班级申请 -->
      <el-tab-pane label="创建班级申请" name="create">
        <div class="filter-bar">
          <el-radio-group v-model="createFilter" @change="loadCreateApplications">
            <el-radio-button :value="-1">全部</el-radio-button>
            <el-radio-button :value="0">待审核</el-radio-button>
            <el-radio-button :value="1">已通过</el-radio-button>
            <el-radio-button :value="2">已拒绝</el-radio-button>
          </el-radio-group>
        </div>

        <div class="application-list">
          <div v-for="app in createApplications" :key="app.id" class="application-card">
            <div class="app-header">
              <div class="app-title">
                <h4>{{ app.className }}</h4>
                <span :class="['status-tag', getStatusClass(app.status)]">
                  {{ getStatusText(app.status) }}
                </span>
              </div>
              <span class="app-time">申请时间：{{ formatDate(app.createTime) }}</span>
            </div>
            <div class="app-body">
              <div class="app-info">
                <span class="label">申请人ID：</span>
                <span class="value">{{ app.applicantId }}</span>
              </div>
              <div class="app-info" v-if="app.description">
                <span class="label">班级描述：</span>
                <span class="value">{{ app.description }}</span>
              </div>
              <div class="app-info" v-if="app.reviewComment">
                <span class="label">审核意见：</span>
                <span class="value">{{ app.reviewComment }}</span>
              </div>
              <div class="app-info" v-if="app.createdClassId">
                <span class="label">已创建班级ID：</span>
                <span class="value">{{ app.createdClassId }}</span>
              </div>
            </div>
            <div class="app-actions" v-if="app.status === 0">
              <el-button type="primary" size="small" @click="openReviewDialog('create', app.id, true)">
                通过
              </el-button>
              <el-button type="danger" size="small" @click="openReviewDialog('create', app.id, false)">
                拒绝
              </el-button>
            </div>
          </div>
          <div v-if="createApplications.length === 0" class="empty-state">
            <FileText :size="32" />
            <p>暂无创建班级申请</p>
          </div>
        </div>

        <div class="pagination" v-if="createTotal > 0">
          <el-pagination
            v-model:current-page="createPage"
            :page-size="10"
            :total="createTotal"
            layout="prev, pager, next"
            @current-change="loadCreateApplications"
          />
        </div>
      </el-tab-pane>

      <!-- 加入班级申请 -->
      <el-tab-pane label="加入班级申请" name="join">
        <div class="filter-bar">
          <el-radio-group v-model="joinFilter" @change="loadJoinApplications">
            <el-radio-button :value="-1">全部</el-radio-button>
            <el-radio-button :value="0">待审核</el-radio-button>
            <el-radio-button :value="1">已通过</el-radio-button>
            <el-radio-button :value="2">已拒绝</el-radio-button>
          </el-radio-group>
        </div>

        <div class="application-list">
          <div v-for="app in joinApplications" :key="app.id" class="application-card">
            <div class="app-header">
              <div class="app-title">
                <h4>申请加入班级 #{{ app.classId }}</h4>
                <span :class="['status-tag', getStatusClass(app.status)]">
                  {{ getStatusText(app.status) }}
                </span>
              </div>
              <span class="app-time">申请时间：{{ formatDate(app.createTime) }}</span>
            </div>
            <div class="app-body">
              <div class="app-info">
                <span class="label">申请人ID：</span>
                <span class="value">{{ app.applicantId }}</span>
              </div>
              <div class="app-info">
                <span class="label">班级ID：</span>
                <span class="value">{{ app.classId }}</span>
              </div>
              <div class="app-info" v-if="app.reviewComment">
                <span class="label">审核意见：</span>
                <span class="value">{{ app.reviewComment }}</span>
              </div>
            </div>
            <div class="app-actions" v-if="app.status === 0">
              <el-button type="primary" size="small" @click="openReviewDialog('join', app.id, true)">
                通过
              </el-button>
              <el-button type="danger" size="small" @click="openReviewDialog('join', app.id, false)">
                拒绝
              </el-button>
            </div>
          </div>
          <div v-if="joinApplications.length === 0" class="empty-state">
            <FileText :size="32" />
            <p>暂无加入班级申请</p>
          </div>
        </div>

        <div class="pagination" v-if="joinTotal > 0">
          <el-pagination
            v-model:current-page="joinPage"
            :page-size="10"
            :total="joinTotal"
            layout="prev, pager, next"
            @current-change="loadJoinApplications"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 审核对话框 -->
    <el-dialog v-model="reviewDialog.visible" :title="reviewDialog.approved ? '通过申请' : '拒绝申请'" width="450px" class="dark-dialog">
      <el-form label-width="80px">
        <el-form-item label="审核意见">
          <el-input
            v-model="reviewDialog.comment"
            type="textarea"
            :rows="3"
            placeholder="请输入审核意见（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialog.visible = false">取消</el-button>
        <el-button :type="reviewDialog.approved ? 'primary' : 'danger'" @click="submitReview">
          确认{{ reviewDialog.approved ? '通过' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { get, put } from '@/utils/http'
import { ElMessage } from 'element-plus'
import { FileText } from '@lucide/vue'

interface ClassCreateApplication {
  id: number
  applicantId: number
  className: string
  description: string
  status: number
  reviewerId: number | null
  reviewTime: string | null
  reviewComment: string | null
  createdClassId: number | null
  createTime: string
}

interface ClassJoinApplication {
  id: number
  classId: number
  applicantId: number
  status: number
  reviewerId: number | null
  reviewTime: string | null
  reviewComment: string | null
  createTime: string
}

interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

const activeTab = ref<'create' | 'join'>('create')

const createApplications = ref<ClassCreateApplication[]>([])
const createFilter = ref(-1)
const createPage = ref(1)
const createTotal = ref(0)

const joinApplications = ref<ClassJoinApplication[]>([])
const joinFilter = ref(-1)
const joinPage = ref(1)
const joinTotal = ref(0)

const reviewDialog = ref({
  visible: false,
  type: '' as 'create' | 'join',
  applicationId: 0,
  approved: false,
  comment: ''
})

const getStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '待审核', 1: '已通过', 2: '已拒绝' }
  return map[status] || '未知'
}

const getStatusClass = (status: number) => {
  const map: Record<number, string> = { 0: 'pending', 1: 'approved', 2: 'rejected' }
  return map[status] || 'pending'
}

const formatDate = (dateStr: string) => {
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const loadCreateApplications = async () => {
  const params: Record<string, unknown> = {
    pageNum: createPage.value,
    pageSize: 10
  }
  if (createFilter.value >= 0) {
    params.status = createFilter.value
  }
  const result = await get<PageResult<ClassCreateApplication>>('/class/applications/create/list', params)
  if (result.code === 200) {
    createApplications.value = result.data!.records
    createTotal.value = result.data!.total
  }
}

const loadJoinApplications = async () => {
  const params: Record<string, unknown> = {
    pageNum: joinPage.value,
    pageSize: 10
  }
  if (joinFilter.value >= 0) {
    params.status = joinFilter.value
  }
  const result = await get<PageResult<ClassJoinApplication>>('/class/applications/join/list', params)
  if (result.code === 200) {
    joinApplications.value = result.data!.records
    joinTotal.value = result.data!.total
  }
}

const openReviewDialog = (type: 'create' | 'join', applicationId: number, approved: boolean) => {
  reviewDialog.value = {
    visible: true,
    type,
    applicationId,
    approved,
    comment: ''
  }
}

const submitReview = async () => {
  const { type, applicationId, approved, comment } = reviewDialog.value
  const url = type === 'create'
    ? '/class/applications/create/approve'
    : '/class/applications/join/approve'
  const result = await put(url, { applicationId, approved, comment })
  if (result.code === 200) {
    ElMessage.success(approved ? '已通过' : '已拒绝')
    reviewDialog.value.visible = false
    if (type === 'create') {
      loadCreateApplications()
    } else {
      loadJoinApplications()
    }
  } else {
    ElMessage.error(result.message)
  }
}

onMounted(() => {
  loadCreateApplications()
  loadJoinApplications()
})
</script>

<style scoped>
.admin-panel {
  padding-bottom: 24px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
}

.filter-bar {
  margin-bottom: 20px;
}

.application-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.application-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px 20px;
  transition: border-color 0.3s;
}

.application-card:hover {
  border-color: rgba(102, 126, 234, 0.3);
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.app-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-title h4 {
  font-size: 16px;
  font-weight: 600;
}

.app-time {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
}

.status-tag {
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-tag.pending {
  background: rgba(251, 191, 36, 0.2);
  color: #fbbf24;
}

.status-tag.approved {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.status-tag.rejected {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.app-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.app-info {
  font-size: 14px;
}

.app-info .label {
  color: rgba(255, 255, 255, 0.5);
}

.app-info .value {
  color: rgba(255, 255, 255, 0.9);
}

.app-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px;
  color: rgba(255, 255, 255, 0.4);
}

.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>

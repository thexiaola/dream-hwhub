<template>
  <div class="message-page">
    <div class="page-header">
      <div class="header-left">
        <h2>站内信</h2>
        <p class="subtitle">班级布置新作业等消息会在这里通知你，按学校区分</p>
      </div>
      <div class="header-right">
        <el-button
          v-if="unreadTotal > 0"
          :disabled="loading"
          @click="markAllRead"
        >
          <CheckCheck :size="18" />
          全部已读
        </el-button>
      </div>
    </div>

    <el-card class="content-card">
      <!-- 筛选区：学校 / 类型 / 已读状态 / 关键字，全部由后端在数据库中筛选 -->
      <div class="message-filter">
        <el-select
          v-model="filters.schoolId"
          class="filter-item"
          placeholder="全部学校"
          clearable
          @change="reload"
        >
          <el-option
            v-for="school in schoolOptions"
            :key="school.id"
            :label="unreadOf(school.id) > 0 ? `${school.schoolName}（${unreadOf(school.id)}）` : school.schoolName"
            :value="school.id"
          />
        </el-select>
        <el-select
          v-model="filters.type"
          class="filter-item"
          placeholder="全部类型"
          @change="reload"
        >
          <el-option
            v-for="opt in MESSAGE_TYPE_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <SlideSegmented
          v-model="filters.readState"
          :options="MESSAGE_READ_OPTIONS"
          aria-label="站内信已读状态"
          @change="reload"
        />
        <div class="filter-keyword">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索标题 / 内容 / 班级"
            clearable
            @clear="reload"
            @keyup.enter="reload"
          >
            <template #prefix>
              <Search :size="16" />
            </template>
          </el-input>
          <el-button type="primary" @click="reload">搜索</el-button>
        </div>
      </div>

      <div class="message-list" v-loading="loading">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-item"
          :class="{ 'is-unread': !msg.isRead }"
          @click="openMessage(msg)"
        >
          <div class="message-icon" :class="msg.type">
            <FileText :size="20" />
          </div>
          <div class="message-main">
            <div class="message-title-row">
              <span class="message-title">{{ msg.title }}</span>
              <span v-if="!msg.isRead" class="unread-dot" aria-label="未读" />
              <span class="message-time">{{ formatDate(msg.createTime) }}</span>
            </div>
            <p class="message-content">{{ msg.content || '—' }}</p>
            <div class="message-meta">
              <span v-if="msg.schoolName" class="meta-tag">
                <School :size="12" />
                {{ msg.schoolName }}
              </span>
              <span v-if="msg.className" class="meta-tag">
                <Users :size="12" />
                {{ msg.className }}
              </span>
              <span class="meta-type">{{ typeText(msg.type) }}</span>
            </div>
          </div>
        </div>

        <div v-if="!loading && messages.length === 0" class="empty-state">
          <Inbox :size="48" />
          <p>暂无站内信</p>
          <p class="empty-tip">有新的班级作业通知时会显示在这里</p>
        </div>
      </div>

      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="loadMessages"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CheckCheck, FileText, Inbox, School, Search, Users } from '@lucide/vue'
import { get, put } from '@/utils/http'
import { useSchoolStore } from '@/stores/school'
import { useMessageStore } from '@/stores/message'
import SlideSegmented from '@/components/SlideSegmented.vue'
import type { PageResult } from '@/types'
import {
  MESSAGE_READ_OPTIONS,
  MESSAGE_TYPE_OPTIONS,
  MESSAGE_TYPE_WORK_PUBLISHED,
  type SiteMessageInfo
} from '@/types/message'
import { formatDateTime as formatDate } from '@/utils/format'

const router = useRouter()
const schoolStore = useSchoolStore()
const messageStore = useMessageStore()

const schoolOptions = computed(() => schoolStore.mySchools ?? [])
const unreadOf = (schoolId: number) => messageStore.unreadOf(schoolId)

const messages = ref<SiteMessageInfo[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const filters = reactive<{
  schoolId: number | null
  type: string
  readState: '' | 'unread' | 'read'
  keyword: string
}>({
  schoolId: schoolStore.currentSchoolId ?? null,
  type: '',
  readState: '',
  keyword: ''
})

// 「全部已读」按钮的未读数：限定当前学校筛选项
const unreadTotal = computed(() => messageStore.unreadOf(filters.schoolId))

const typeText = (type: string): string => {
  if (type === MESSAGE_TYPE_WORK_PUBLISHED) return '新作业'
  return type
}

const loadMessages = async () => {
  loading.value = true
  const params: Record<string, unknown> = { pageNum: page.value, pageSize: pageSize.value }
  if (filters.schoolId != null) params.schoolId = filters.schoolId
  if (filters.type) params.type = filters.type
  if (filters.readState === 'unread') params.isRead = false
  else if (filters.readState === 'read') params.isRead = true
  if (filters.keyword.trim()) params.keyword = filters.keyword.trim()

  const result = await get<PageResult<SiteMessageInfo>>('/messages', params)
  loading.value = false
  if (result.code === 200) {
    messages.value = result.data!.records
    total.value = result.data!.total
  } else {
    ElMessage.error(result.message)
  }
}

// 切换任一筛选条件后回到第一页
const reload = () => {
  page.value = 1
  loadMessages()
}

// 打开消息：标记已读并跳转到关联作业
const openMessage = async (msg: SiteMessageInfo) => {
  if (!msg.isRead) {
    const result = await put(`/messages/${msg.id}/read`)
    if (result.code === 200) {
      msg.isRead = true
      messageStore.decrementUnread(msg.schoolId)
    }
  }
  if (msg.type === MESSAGE_TYPE_WORK_PUBLISHED && msg.workId) {
    router.push(`/student/work/${msg.workId}`)
  }
}

const markAllRead = async () => {
  const params: Record<string, unknown> = {}
  if (filters.schoolId != null) params.schoolId = filters.schoolId
  const result = await put('/messages/read-all', undefined, params)
  if (result.code === 200) {
    ElMessage.success('已全部标记为已读')
    messageStore.clearUnread(filters.schoolId)
    loadMessages()
  } else {
    ElMessage.error(result.message)
  }
}

onMounted(async () => {
  // 学校列表用于筛选项；未读数用于角标与「全部已读」按钮
  if (!schoolStore.mySchools) {
    await schoolStore.fetchMySchools()
  }
  await messageStore.fetchUnread()
  loadMessages()
})
</script>

<style scoped>
.message-page {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.page-header h2 {
  margin: 0;
}

.subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.55);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.content-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.content-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* 筛选行：与页面其它筛选栏同调，采用中性内凹浅底 */
.message-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.08);
}

.filter-item {
  width: 180px;
}

/* 关键字搜索占满剩余宽度 */
.filter-keyword {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 220px;
}

.message-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message-item {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.message-item:hover {
  border-color: rgba(102, 126, 234, 0.5);
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.03);
}

/* 未读消息：左侧描边加粗 + 淡紫底，与已读区分 */
.message-item.is-unread {
  border-color: rgba(102, 126, 234, 0.35);
  background: rgba(102, 126, 234, 0.06);
  box-shadow: inset 3px 0 0 var(--primary-color);
}

.message-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(102, 126, 234, 0.15);
  color: #667eea;
}

.message-main {
  flex: 1;
  min-width: 0;
}

.message-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.message-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
}

.message-time {
  margin-left: auto;
  flex-shrink: 0;
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.45);
}

.message-content {
  margin: 6px 0 8px;
  font-size: 13px;
  line-height: 1.6;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12px;
}

.meta-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.06);
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
}

.meta-type {
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(102, 126, 234, 0.15);
  color: #667eea;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 60px 0;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

.empty-tip {
  font-size: 12px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .header-right .el-button {
    width: 100%;
    margin-left: 0;
  }

  .filter-item,
  .filter-keyword {
    width: 100%;
    min-width: 0;
  }
}
</style>

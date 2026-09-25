<template>
  <div class="friend-page">
    <div class="page-header">
      <div class="header-left">
        <h2>好友</h2>
        <p class="subtitle">好友按学校区分，可添加同一学校内的用户；未加好友也能发私信</p>
      </div>
      <div class="header-right">
        <el-select
          v-model="schoolId"
          class="school-select"
          placeholder="选择学校"
          @change="reloadAll"
        >
          <el-option
            v-for="school in schoolOptions"
            :key="school.id"
            :label="school.schoolName"
            :value="school.id"
          />
        </el-select>
      </div>
    </div>

    <el-card class="content-card">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane name="friends">
          <template #label>
            我的好友
            <span v-if="friends.length > 0" class="tab-count">{{ friends.length }}</span>
          </template>
          <div v-loading="loading.friends" class="friend-list">
            <div v-for="f in friends" :key="f.relationId" class="person-item">
              <UserAvatar :avatar="f.avatar" :size="40" :alt="f.username ?? ''" />
              <div class="person-main">
                <div class="person-name">
                  {{ f.realName || f.username }}
                  <span v-if="f.role" class="role-tag">{{ f.role }}</span>
                </div>
                <div class="person-meta">
                  <span>账号：{{ f.username }}</span>
                  <span v-if="f.staffNo">学工号：{{ f.staffNo }}</span>
                </div>
              </div>
              <div class="person-actions">
                <el-button size="small" type="primary" plain @click="openChat(f.userId)">
                  <MessageCircle :size="14" />
                  私信
                </el-button>
                <el-button size="small" type="danger" text @click="removeFriend(f)">
                  删除
                </el-button>
              </div>
            </div>
            <div v-if="!loading.friends && friends.length === 0" class="empty-state">
              <Users :size="48" />
              <p>还没有好友</p>
              <p class="empty-tip">切换到「添加好友」搜索并添加同校用户</p>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="requests">
          <template #label>
            好友申请
            <el-badge v-if="requests.length > 0" :value="requests.length" class="tab-badge" />
          </template>
          <div v-loading="loading.requests" class="friend-list">
            <div v-for="r in requests" :key="r.relationId" class="person-item">
              <UserAvatar :avatar="r.requesterAvatar" :size="40" :alt="r.requesterUsername ?? ''" />
              <div class="person-main">
                <div class="person-name">{{ r.requesterRealName || r.requesterUsername }}</div>
                <div class="person-meta">
                  <span>账号：{{ r.requesterUsername }}</span>
                  <span v-if="r.requesterStaffNo">学工号：{{ r.requesterStaffNo }}</span>
                  <span>{{ formatDate(r.createTime) }}</span>
                </div>
              </div>
              <div class="person-actions">
                <el-button size="small" type="primary" :loading="responding" @click="respond(r, true)">
                  同意
                </el-button>
                <el-button size="small" type="danger" plain :loading="responding" @click="respond(r, false)">
                  拒绝
                </el-button>
              </div>
            </div>
            <div v-if="!loading.requests && requests.length === 0" class="empty-state">
              <Inbox :size="48" />
              <p>暂无好友申请</p>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="add">
          <template #label>添加好友</template>
          <div class="filter-bar">
            <el-input
              v-model="keyword"
              placeholder="搜索用户名 / 姓名 / 学工号"
              clearable
              class="search-input"
              @clear="loadAddable"
              @keyup.enter="loadAddable"
            >
              <template #prefix><Search :size="16" /></template>
            </el-input>
            <el-button type="primary" @click="loadAddable">搜索</el-button>
          </div>
          <div v-loading="loading.addable" class="friend-list">
            <div v-for="u in addable" :key="u.userId" class="person-item">
              <UserAvatar :avatar="u.avatar" :size="40" :alt="u.username ?? ''" />
              <div class="person-main">
                <div class="person-name">
                  {{ u.realName || u.username }}
                  <span v-if="u.role" class="role-tag">{{ u.role }}</span>
                </div>
                <div class="person-meta">
                  <span>账号：{{ u.username }}</span>
                  <span v-if="u.staffNo">学工号：{{ u.staffNo }}</span>
                </div>
              </div>
              <div class="person-actions">
                <el-button v-if="u.relation === 'friend'" size="small" plain disabled>已是好友</el-button>
                <el-button v-else-if="u.relation === 'pending_out'" size="small" plain disabled>已申请</el-button>
                <el-button v-else-if="u.relation === 'pending_in'" size="small" type="warning" @click="switchToRequests">
                  待我处理
                </el-button>
                <template v-else>
                  <el-button size="small" type="primary" :loading="adding === u.userId" @click="addFriend(u)">
                    <UserPlus :size="14" />
                    加好友
                  </el-button>
                  <el-button size="small" plain @click="openChat(u.userId)">
                    <MessageCircle :size="14" />
                    私信
                  </el-button>
                </template>
              </div>
            </div>
            <div v-if="!loading.addable && addable.length === 0" class="empty-state">
              <Search :size="48" />
              <p>没有找到可添加的用户</p>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Inbox, MessageCircle, Search, UserPlus, Users } from '@lucide/vue'
import { del, get, post, put } from '@/utils/http'
import { useSchoolStore } from '@/stores/school'
import { useFriendStore } from '@/stores/friend'
import UserAvatar from '@/components/UserAvatar.vue'
import { formatDateTime as formatDate } from '@/utils/format'
import type { AddableUserInfo, FriendInfo, FriendRequestInfo } from '@/types/friend'

const router = useRouter()
const schoolStore = useSchoolStore()
const friendStore = useFriendStore()

const schoolOptions = computed(() => schoolStore.mySchools ?? [])
const schoolId = ref<number | null>(schoolStore.currentSchoolId ?? null)
const activeTab = ref<'friends' | 'requests' | 'add'>('friends')
const keyword = ref('')
const responding = ref(false)
const adding = ref<number | null>(null)

const friends = ref<FriendInfo[]>([])
const requests = ref<FriendRequestInfo[]>([])
const addable = ref<AddableUserInfo[]>([])
const loading = reactive({ friends: false, requests: false, addable: false })

const loadFriends = async () => {
  if (!schoolId.value) return
  loading.friends = true
  const result = await get<FriendInfo[]>('/friends', { schoolId: schoolId.value })
  loading.friends = false
  if (result.code === 200) friends.value = result.data ?? []
  else ElMessage.error(result.message)
}

const loadRequests = async () => {
  if (!schoolId.value) return
  loading.requests = true
  const result = await get<FriendRequestInfo[]>('/friends/requests', { schoolId: schoolId.value })
  loading.requests = false
  if (result.code === 200) {
    requests.value = result.data ?? []
    friendStore.setPendingCount(schoolId.value, requests.value.length)
  } else ElMessage.error(result.message)
}

const loadAddable = async () => {
  if (!schoolId.value) return
  loading.addable = true
  const params: Record<string, unknown> = { schoolId: schoolId.value }
  if (keyword.value.trim()) params.keyword = keyword.value.trim()
  const result = await get<AddableUserInfo[]>('/friends/search', params)
  loading.addable = false
  if (result.code === 200) addable.value = result.data ?? []
  else ElMessage.error(result.message)
}

const reloadAll = () => {
  loadFriends()
  loadRequests()
  if (activeTab.value === 'add') loadAddable()
}

const onTabChange = (name: string | number) => {
  if (name === 'add') loadAddable()
  else if (name === 'requests') loadRequests()
  else loadFriends()
}

const switchToRequests = () => {
  activeTab.value = 'requests'
}

const addFriend = async (u: AddableUserInfo) => {
  if (!schoolId.value) return
  adding.value = u.userId
  const result = await post('/friends/requests', { schoolId: schoolId.value, targetUserId: u.userId })
  adding.value = null
  if (result.code === 200) {
    ElMessage.success(result.message || '好友申请已发送')
    loadAddable()
  } else {
    ElMessage.error(result.message)
  }
}

const respond = async (r: FriendRequestInfo, accepted: boolean) => {
  responding.value = true
  const result = await put('/friends/requests', { relationId: r.relationId, accepted })
  responding.value = false
  if (result.code === 200) {
    ElMessage.success(result.message || '已处理')
    loadRequests()
    if (accepted) loadFriends()
  } else {
    ElMessage.error(result.message)
  }
}

const removeFriend = async (f: FriendInfo) => {
  try {
    await ElMessageBox.confirm(
      `确认删除好友「${f.realName || f.username}」？删除后你们将不再是好友，但仍可互发私信。`,
      '删除好友',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  const result = await del(`/friends/${f.relationId}`)
  if (result.code === 200) {
    ElMessage.success('已删除好友')
    loadFriends()
  } else {
    ElMessage.error(result.message)
  }
}

const openChat = (peerId: number) => {
  router.push({ path: '/private-messages', query: { peerId: String(peerId) } })
}

onMounted(async () => {
  if (!schoolStore.mySchools) {
    await schoolStore.fetchMySchools()
  }
  if (!schoolId.value) {
    schoolId.value = schoolStore.currentSchoolId ?? schoolOptions.value[0]?.id ?? null
  }
  reloadAll()
})

// 顶部「当前学校」切换时同步本页学校选择
watch(() => schoolStore.currentSchoolId, id => {
  if (id && id !== schoolId.value) {
    schoolId.value = id
    reloadAll()
  }
})
</script>

<style scoped>
.friend-page {
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

.school-select {
  width: 220px;
}

.content-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.tab-count,
.tab-badge {
  margin-left: 6px;
}

.filter-bar {
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

.search-input {
  width: 260px;
}

.friend-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 120px;
}

.person-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  border-radius: 12px;
}

.person-main {
  flex: 1;
  min-width: 0;
}

.person-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
}

.role-tag {
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 400;
  background: rgba(102, 126, 234, 0.15);
  color: #667eea;
}

.person-meta {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px 14px;
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.55);
}

.person-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
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

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .school-select,
  .search-input {
    width: 100%;
  }

  .person-item {
    flex-wrap: wrap;
  }

  .person-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>

<template>
  <div class="pm-page">
    <div class="page-header">
      <div class="header-left">
        <h2>私信</h2>
        <p class="subtitle">私信按学校区分；未加好友也能发消息（向陌生用户有发送条数限制）</p>
      </div>
      <div class="header-right">
        <el-select
          v-model="schoolId"
          class="school-select"
          placeholder="选择学校"
          @change="onSchoolChange"
        >
          <el-option
            v-for="school in schoolOptions"
            :key="school.id"
            :label="school.schoolName"
            :value="school.id"
          />
        </el-select>
        <el-button v-if="policy" plain @click="showPolicyTip">
          <Info :size="16" />
          陌生私信规则
        </el-button>
      </div>
    </div>

    <el-card class="content-card">
      <div class="pm-layout">
        <!-- 左：会话列表 -->
        <aside class="conv-pane">
          <div class="conv-search">
            <el-input
              v-model="peerKeyword"
              placeholder="搜索会话对方"
              clearable
            >
              <template #prefix><Search :size="16" /></template>
            </el-input>
          </div>
          <div v-loading="loading.conversations" class="conv-list">
            <div
              v-for="c in filteredConversations"
              :key="c.peerId"
              class="conv-item"
              :class="{ active: c.peerId === peerId }"
              @click="openConversation(c.peerId)"
            >
              <UserAvatar :avatar="c.peerAvatar" :size="38" :alt="c.peerUsername ?? ''" />
              <div class="conv-main">
                <div class="conv-title-row">
                  <span class="conv-name">{{ c.peerRealName || c.peerUsername }}</span>
                  <span v-if="c.friend" class="friend-dot" title="好友">友</span>
                  <span class="conv-time">{{ c.lastTime ? formatDate(c.lastTime) : '' }}</span>
                </div>
                <div class="conv-preview">{{ c.lastContent || '暂无消息' }}</div>
              </div>
              <span v-if="c.unreadCount > 0" class="conv-unread">{{ c.unreadCount > 99 ? '99+' : c.unreadCount }}</span>
            </div>
            <div v-if="!loading.conversations && filteredConversations.length === 0" class="conv-empty">
              <Inbox :size="36" />
              <p>暂无会话</p>
              <p class="empty-tip">可在「好友」中搜索同校用户并打开私信，或直接给陌生用户发消息</p>
            </div>
          </div>
        </aside>

        <!-- 右：聊天区 -->
        <section class="chat-pane">
          <template v-if="peerId">
            <div class="chat-header">
              <UserAvatar :avatar="peer?.peerAvatar" :size="34" :alt="peer?.peerUsername ?? ''" />
              <div class="chat-title">
                <span class="chat-name">{{ peer?.peerRealName || peer?.peerUsername || '用户' }}</span>
                <span v-if="peer?.peerRole" class="role-tag">{{ peer.peerRole }}</span>
              </div>
              <div class="chat-header-actions">
                <el-button v-if="peer && !peer.friend" size="small" type="primary" plain :loading="addingFriend" @click="addPeerAsFriend">
                  <UserPlus :size="14" />
                  加好友
                </el-button>
              </div>
            </div>

            <div ref="scrollRef" v-loading="loading.messages" class="chat-body">
              <div
                v-for="m in orderedMessages"
                :key="m.id"
                class="msg-row"
                :class="{ mine: m.mine }"
              >
                <div class="msg-bubble">
                  <div class="msg-content">{{ m.content }}</div>
                  <div class="msg-time">{{ formatDate(m.createTime) }}</div>
                </div>
              </div>
              <div v-if="!loading.messages && messages.length === 0" class="chat-empty">
                <MessageCircle :size="36" />
                <p>开始与 TA 的对话</p>
              </div>
            </div>

            <div class="chat-input">
              <el-input
                v-model="draft"
                type="textarea"
                :rows="2"
                maxlength="1000"
                show-word-limit
                resize="none"
                placeholder="输入消息，Enter 发送，Shift+Enter 换行"
                @keydown.enter.exact.prevent="send"
              />
              <el-button type="primary" :loading="sending" :disabled="!draft.trim()" @click="send">
                <Send :size="16" />
                发送
              </el-button>
            </div>

            <div v-if="!peer?.friend" class="stranger-hint">
              <Info :size="14" />
              <span>
                你们还不是好友。当前每 {{ policy?.resetHours ?? '—' }} 小时可向该陌生用户发送
                <b>{{ policy?.strangerLimit ?? '—' }}</b> 条消息；添加为好友后不受此限制。
              </span>
            </div>
          </template>
          <div v-else class="chat-placeholder">
            <MessageCircle :size="48" />
            <p>选择左侧会话开始聊天</p>
          </div>
        </section>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Info, Inbox, MessageCircle, Search, Send, UserPlus } from '@lucide/vue'
import { get, post, put } from '@/utils/http'
import { useSchoolStore } from '@/stores/school'
import { useFriendStore } from '@/stores/friend'
import UserAvatar from '@/components/UserAvatar.vue'
import type { PageResult } from '@/types'
import { formatDateTime as formatDate } from '@/utils/format'
import type { ConversationInfo, MessagePolicyInfo, PrivateMessageInfo } from '@/types/friend'

const route = useRoute()
const schoolStore = useSchoolStore()
const friendStore = useFriendStore()

const schoolOptions = computed(() => schoolStore.mySchools ?? [])
const schoolId = ref<number | null>(schoolStore.currentSchoolId ?? null)
const conversations = ref<ConversationInfo[]>([])
const peerId = ref<number | null>(null)
const messages = ref<PrivateMessageInfo[]>([])
const policy = ref<MessagePolicyInfo | null>(null)
const draft = ref('')
const peerKeyword = ref('')
const addingFriend = ref(false)
const sending = ref(false)
const loading = reactive({ conversations: false, messages: false })
const scrollRef = ref<HTMLElement | null>(null)

const peer = computed(() =>
  conversations.value.find(c => c.peerId === peerId.value) ?? null
)

// 消息按时间正序展示（接口按 id 倒序返回，前端反转）
const orderedMessages = computed(() => [...messages.value].reverse())

const filteredConversations = computed(() => {
  const kw = peerKeyword.value.trim().toLowerCase()
  if (!kw) return conversations.value
  return conversations.value.filter(c =>
    (c.peerRealName ?? '').toLowerCase().includes(kw)
    || (c.peerUsername ?? '').toLowerCase().includes(kw)
    || (c.peerStaffNo ?? '').toLowerCase().includes(kw)
  )
})

const loadPolicy = async () => {
  if (!schoolId.value) return
  const result = await get<MessagePolicyInfo>(`/message-policy/school/${schoolId.value}`)
  if (result.code === 200) policy.value = result.data
}

const loadConversations = async () => {
  if (!schoolId.value) return
  loading.conversations = true
  const result = await get<ConversationInfo[]>('/private-messages/conversations', { schoolId: schoolId.value })
  loading.conversations = false
  if (result.code === 200) {
    conversations.value = result.data ?? []
    // 若当前没有选中的会话，且地址栏带了 peerId，则补一个占位会话以便直接聊天
    ensurePeerConversation()
  } else {
    ElMessage.error(result.message)
  }
}

/** 地址栏指定了 peerId 但会话列表尚无该人时，补一个占位项 */
const ensurePeerConversation = () => {
  const target = peerId.value
  if (!target || !schoolId.value) return
  if (conversations.value.some(c => c.peerId === target)) return
  conversations.value.unshift({
    schoolId: schoolId.value,
    peerId: target,
    peerUsername: null,
    peerRealName: null,
    peerRole: null,
    friend: false,
    lastContent: null,
    lastTime: null,
    unreadCount: 0
  })
}

const loadMessages = async () => {
  if (!schoolId.value || !peerId.value) {
    messages.value = []
    return
  }
  loading.messages = true
  const result = await get<PageResult<PrivateMessageInfo>>('/private-messages', {
    schoolId: schoolId.value,
    peerId: peerId.value,
    pageNum: 1,
    pageSize: 200
  })
  loading.messages = false
  if (result.code === 200) {
    messages.value = result.data!.records
    await markRead()
    await nextTick()
    scrollToBottom()
  } else {
    ElMessage.error(result.message)
  }
}

const markRead = async () => {
  if (!schoolId.value || !peerId.value) return
  await put('/private-messages/read', undefined, { schoolId: schoolId.value, peerId: peerId.value })
  friendStore.fetchUnread()
  const c = conversations.value.find(x => x.peerId === peerId.value)
  if (c) c.unreadCount = 0
}

const openConversation = (id: number) => {
  peerId.value = id
  loadMessages()
}

const send = async () => {
  if (!schoolId.value || !peerId.value || !draft.value.trim()) return
  sending.value = true
  const result = await post<PrivateMessageInfo>('/private-messages', {
    schoolId: schoolId.value,
    receiverId: peerId.value,
    content: draft.value.trim()
  })
  sending.value = false
  if (result.code === 200) {
    draft.value = ''
    await loadMessages()
    loadConversations()
  } else {
    ElMessage.error(result.message)
  }
}

const addPeerAsFriend = async () => {
  if (!schoolId.value || !peerId.value) return
  addingFriend.value = true
  const result = await post('/friends/requests', { schoolId: schoolId.value, targetUserId: peerId.value })
  addingFriend.value = false
  if (result.code === 200) {
    ElMessage.success(result.message || '好友申请已发送')
    loadConversations()
  } else {
    ElMessage.error(result.message)
  }
}

const showPolicyTip = () => {
  const p = policy.value
  if (!p) return
  ElMessageBox.alert(
    `当前学校：每 ${p.resetHours} 小时可向同一陌生用户发送 ${p.strangerLimit} 条私信。`
      + `\n${p.schoolOverridden ? '（本校已自定义）' : '（继承全站默认：每 ' + p.globalResetHours + ' 小时 ' + p.globalStrangerLimit + ' 条）'}`,
    '陌生私信规则',
    { confirmButtonText: '知道了' }
  )
}

const scrollToBottom = () => {
  const el = scrollRef.value
  if (el) el.scrollTop = el.scrollHeight
}

const onSchoolChange = () => {
  peerId.value = null
  messages.value = []
  loadPolicy()
  loadConversations()
}

onMounted(async () => {
  if (!schoolStore.mySchools) {
    await schoolStore.fetchMySchools()
  }
  if (!schoolId.value) {
    schoolId.value = schoolStore.currentSchoolId ?? schoolOptions.value[0]?.id ?? null
  }
  // 地址栏 peerId 优先选中
  const q = route.query.peerId
  if (q) peerId.value = Number(q)
  await Promise.all([loadPolicy(), loadConversations()])
  if (peerId.value) loadMessages()
  friendStore.fetchUnread()
})

watch(() => route.query.peerId, q => {
  if (q && Number(q) !== peerId.value) {
    peerId.value = Number(q)
    ensurePeerConversation()
    loadMessages()
  }
})

// 顶部「当前学校」切换时同步
watch(() => schoolStore.currentSchoolId, id => {
  if (id && id !== schoolId.value) {
    schoolId.value = id
    onSchoolChange()
  }
})
</script>

<style scoped>
.pm-page {
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

.school-select {
  width: 200px;
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
  min-height: 0;
}

.pm-layout {
  flex: 1;
  display: flex;
  gap: 0;
  min-height: 480px;
  width: 100%;
}

/* 左侧会话列表 */
.conv-pane {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
  padding-right: 12px;
}

.conv-search {
  margin-bottom: 10px;
}

.conv-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.conv-item:hover {
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.04);
}

.conv-item.active {
  background: rgba(102, 126, 234, 0.12);
}

.conv-main {
  flex: 1;
  min-width: 0;
}

.conv-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.conv-name {
  font-size: 14px;
  font-weight: 600;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.friend-dot {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: rgba(102, 126, 234, 0.2);
  color: #667eea;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
}

.conv-time {
  margin-left: auto;
  flex-shrink: 0;
  font-size: 11px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
}

.conv-preview {
  margin-top: 2px;
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.55);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-unread {
  flex-shrink: 0;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #f56c6c;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}

.conv-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 40px 8px;
  text-align: center;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
}

.conv-empty p {
  margin: 0;
  font-size: 13px;
}

.empty-tip {
  font-size: 12px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.35);
}

/* 右侧聊天区 */
.chat-pane {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding-left: 16px;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-name {
  font-size: 15px;
  font-weight: 600;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.95);
}

.role-tag {
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 12px;
  background: rgba(102, 126, 234, 0.15);
  color: #667eea;
}

.chat-header-actions {
  margin-left: auto;
}

.chat-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 4px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.msg-row {
  display: flex;
}

.msg-row.mine {
  justify-content: flex-end;
}

.msg-bubble {
  max-width: 70%;
  padding: 8px 12px;
  border-radius: 12px;
  background: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.06);
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.9);
}

.msg-row.mine .msg-bubble {
  background: rgba(102, 126, 234, 0.18);
}

.msg-content {
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-time {
  margin-top: 4px;
  font-size: 11px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.45);
  text-align: right;
}

.chat-empty,
.chat-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.4);
}

.chat-placeholder p,
.chat-empty p {
  margin: 0;
  font-size: 14px;
}

.chat-input {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  padding-top: 12px;
  border-top: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
}

.chat-input .el-textarea {
  flex: 1;
}

.stranger-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  padding: 8px 12px;
  border-radius: 10px;
  font-size: 12px;
  line-height: 1.5;
  color: rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.7);
  background: rgba(230, 162, 60, 0.12);
  border: 1px solid rgba(230, 162, 60, 0.3);
}

@media (max-width: 768px) {
  .pm-layout {
    flex-direction: column;
  }

  .conv-pane {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid rgba(var(--r-fg), var(--g-fg), var(--b-fg), 0.1);
    padding-right: 0;
    padding-bottom: 12px;
  }

  .chat-pane {
    padding-left: 0;
    padding-top: 12px;
  }

  .msg-bubble {
    max-width: 85%;
  }
}
</style>

import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { get } from '@/utils/http'
import type { UnreadCount } from '@/types/message'

/**
 * 站内信未读数。
 * 站内信按学校隔离，故未读数也按学校分组；侧边栏角标与站内信页面共用。
 */
export const useMessageStore = defineStore('message', () => {
  // 学校 ID -> 未读数
  const unreadBySchool = ref<Record<number, number>>({})

  // 全部学校未读总数（导航角标用）
  const totalUnread = computed(() =>
    Object.values(unreadBySchool.value).reduce((sum, n) => sum + n, 0)
  )

  // 取某学校的未读数（未提供 schoolId 时返回总数）
  const unreadOf = (schoolId?: number | null): number =>
    schoolId == null ? totalUnread.value : unreadBySchool.value[schoolId] ?? 0

  /**
   * 拉取各学校未读数
   */
  const fetchUnread = async (): Promise<void> => {
    try {
      const result = await get<UnreadCount[]>('/messages/unread-count')
      if (result.code === 200) {
        const map: Record<number, number> = {}
        for (const item of result.data ?? []) {
          map[item.schoolId] = item.count
        }
        unreadBySchool.value = map
      }
    } catch {
      // 未读数属辅助信息，失败时保留上一次结果，不打扰用户
    }
  }

  // 本地把某学校（或全部）的未读清零，用于「全部已读」后立即更新角标
  const clearUnread = (schoolId?: number | null): void => {
    if (schoolId == null) {
      unreadBySchool.value = {}
      return
    }
    const next = { ...unreadBySchool.value }
    delete next[schoolId]
    unreadBySchool.value = next
  }

  // 本地把某条消息标记为已读后，未读数减一
  const decrementUnread = (schoolId: number): void => {
    const current = unreadBySchool.value[schoolId] ?? 0
    if (current <= 1) {
      clearUnread(schoolId)
      return
    }
    unreadBySchool.value = { ...unreadBySchool.value, [schoolId]: current - 1 }
  }

  const clear = (): void => {
    unreadBySchool.value = {}
  }

  window.addEventListener('auth-expired', clear)

  return { unreadBySchool, totalUnread, unreadOf, fetchUnread, clearUnread, decrementUnread, clear }
})

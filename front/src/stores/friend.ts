import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { get } from '@/utils/http'

/**
 * 私信未读数（用于侧边栏角标）。
 * 私信按学校隔离，未读数按学校分组。
 */
export const useFriendStore = defineStore('friend', () => {
  // 学校 ID -> 未读私信数
  const unreadBySchool = ref<Record<number, number>>({})
  // 学校 ID -> 待处理好友申请数
  const pendingBySchool = ref<Record<number, number>>({})

  const totalUnread = computed(() =>
    Object.values(unreadBySchool.value).reduce((sum, n) => sum + n, 0)
  )

  const unreadOf = (schoolId?: number | null): number =>
    schoolId == null ? totalUnread.value : unreadBySchool.value[schoolId] ?? 0

  const pendingOf = (schoolId?: number | null): number =>
    schoolId == null ? 0 : pendingBySchool.value[schoolId] ?? 0

  /** 拉取各学校未读私信数 */
  const fetchUnread = async (): Promise<void> => {
    try {
      const result = await get<{ schoolId: number; count: number }[]>('/private-messages/unread-count')
      if (result.code === 200) {
        const map: Record<number, number> = {}
        for (const item of result.data ?? []) {
          map[item.schoolId] = item.count
        }
        unreadBySchool.value = map
      }
    } catch {
      // 辅助信息，失败时保留旧值
    }
  }

  /** 记录某学校的待处理好友申请数（由好友页设置） */
  const setPendingCount = (schoolId: number | null, count: number): void => {
    if (schoolId == null) return
    pendingBySchool.value = { ...pendingBySchool.value, [schoolId]: count }
  }

  /** 本地把某学校（或全部）的未读清零（读完会话后调用） */
  const clearUnread = (schoolId?: number | null): void => {
    if (schoolId == null) {
      unreadBySchool.value = {}
      return
    }
    const next = { ...unreadBySchool.value }
    delete next[schoolId]
    unreadBySchool.value = next
  }

  const clear = (): void => {
    unreadBySchool.value = {}
    pendingBySchool.value = {}
  }

  window.addEventListener('auth-expired', clear)

  return { unreadBySchool, pendingBySchool, totalUnread, unreadOf, pendingOf, fetchUnread, setPendingCount, clearUnread, clear }
})

import { ref, watch, type Ref } from 'vue'

/** 存「通过前二次确认」偏好的 localStorage key（AdminPanel 与 SchoolDetail 共用） */
const STORAGE_KEY = 'schoolReviewConfirmBeforeApprove'

/**
 * 「通过申请前是否需要二次确认」这一前端偏好，读写本地存储。
 *
 * 隐私模式下 localStorage 可能不可写，读写失败时回退到默认值（开启），
 * 保证功能可用而非抛错。AdminPanel 与 SchoolDetail 共用，避免两处各写一份。
 */
export function useConfirmBeforeApprove(): Ref<boolean> {
  const read = (): boolean => {
    try {
      return localStorage.getItem(STORAGE_KEY) !== '0'
    } catch {
      return true
    }
  }

  const confirmBeforeApprove = ref(read())

  watch(confirmBeforeApprove, value => {
    try {
      localStorage.setItem(STORAGE_KEY, value ? '1' : '0')
    } catch {
      // 存储不可用时仅本次会话生效
    }
  })

  return confirmBeforeApprove
}

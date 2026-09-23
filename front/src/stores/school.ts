import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { get } from '@/utils/http'
import type { SchoolDetail } from '@/types/school'
import { SCHOOL_ROLE_TEACHER } from '@/types/school'

const CURRENT_SCHOOL_KEY = 'currentSchoolId'

/**
 * 当前用户在学校中的身份。
 * 「我教的课」导航栏与 /teacher 路由守卫共用这里的判断结果，
 * 保证非学校老师既看不到入口也进不去页面；
 * currentSchoolId 是全局「当前学校」，班级列表与加入班级均以它为准。
 */
export const useSchoolStore = defineStore('school', () => {
  // 我加入的学校列表；null 表示尚未获取过
  const mySchools = ref<SchoolDetail[] | null>(null)

  // 当前所在学校 ID，null 表示未选择或未加入任何学校
  const currentSchoolId = ref<number | null>(readStoredSchoolId())

  // 是否在任一学校中拥有老师及以上身份（学校管理员同样具备建班能力）
  const isSchoolTeacher = computed(() =>
    (mySchools.value ?? []).some(school => (school.myRoleCode ?? 0) >= SCHOOL_ROLE_TEACHER)
  )

  // 当前学校；列表为空或所存 ID 已失效时为 null
  const currentSchool = computed(() =>
    (mySchools.value ?? []).find(school => school.id === currentSchoolId.value) ?? null
  )

  const setCurrentSchool = (schoolId: number | null) => {
    currentSchoolId.value = schoolId
  }

  /**
   * 获取我加入的学校列表
   * @param force 为 true 时忽略缓存重新请求
   * @returns 是否成功获取
   */
  const fetchMySchools = async (force = false): Promise<boolean> => {
    if (!force && mySchools.value) {
      return true
    }
    try {
      const result = await get<SchoolDetail[]>('/school/mine')
      if (result.code === 200) {
        mySchools.value = result.data ?? []
        return true
      }
    } catch {
      // 请求失败时保留上一次结果，由调用方按最保守方式处理
    }
    return false
  }

  // 当前学校失效（退出学校 / 学校被解散 / 换账号）时回退到列表首个学校
  watch(
    mySchools,
    schools => {
      // 尚未获取到列表时不改动当前学校，避免刷新瞬间丢掉已记住的选择
      if (schools === null) {
        return
      }
      if (!schools.some(school => school.id === currentSchoolId.value)) {
        currentSchoolId.value = schools.length > 0 ? schools[0].id : null
      }
    },
    { immediate: true }
  )

  // 记住所选学校，刷新后仍停留在同一所学校
  watch(currentSchoolId, schoolId => {
    if (schoolId === null) {
      localStorage.removeItem(CURRENT_SCHOOL_KEY)
    } else {
      localStorage.setItem(CURRENT_SCHOOL_KEY, String(schoolId))
    }
  })

  const clear = () => {
    mySchools.value = null
    currentSchoolId.value = null
    // 值本就为 null 时赋值不会触发 watch，这里直接清除持久化的学校选择
    localStorage.removeItem(CURRENT_SCHOOL_KEY)
  }

  // 会话失效时同步清空，避免下一个账号沿用上一个账号的学校身份
  window.addEventListener('auth-expired', clear)

  return { mySchools, currentSchoolId, currentSchool, isSchoolTeacher, setCurrentSchool, fetchMySchools, clear }
})

function readStoredSchoolId(): number | null {
  const raw = localStorage.getItem(CURRENT_SCHOOL_KEY)
  if (!raw) {
    return null
  }
  const parsed = Number(raw)
  return Number.isFinite(parsed) ? parsed : null
}

import { computed, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { post } from '@/utils/http'
import { useUserStore } from '@/stores/user'

/** 验证方式 */
export type VerifyMethod = 'password' | 'email_code'

/** 校验通过后返回给业务请求的请求头 */
export type VerifyHeaders = Record<string, string>

// ===== 模块级单例状态：全局仅一个验证弹窗 =====
const visible = ref(false)
const operationName = ref('')
const method = ref<VerifyMethod>('password')
const password = ref('')
const emailCode = ref('')
const sending = ref(false)
const countdown = ref(0)
/** 当前用户可用的验证方式（由安全验证设置决定） */
const availableMethods = ref<VerifyMethod[]>(['password', 'email_code'])

/** 是否两种方式都可用（决定弹窗是否展示方式切换） */
const canChooseMethod = computed(() => availableMethods.value.length > 1)

/**
 * 读取当前用户的安全验证设置（两个开关），得出可用的验证方式。
 * 未取到用户信息时按后端默认值（密码开、邮箱关）处理。
 */
const resolveAvailableMethods = (): VerifyMethod[] => {
  const userStore = useUserStore()
  const info = userStore.userInfo
  const byPassword = info?.verifyByPassword !== false
  const byEmail = info?.verifyByEmailCode === true
  const methods: VerifyMethod[] = []
  if (byPassword) methods.push('password')
  if (byEmail) methods.push('email_code')
  return methods
}

let resolver: ((headers: VerifyHeaders | null) => void) | null = null
let timer: number | null = null

const clearTimer = () => {
  if (timer !== null) {
    clearInterval(timer)
    timer = null
  }
}

const startCountdown = (seconds: number) => {
  countdown.value = seconds
  clearTimer()
  timer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      countdown.value = 0
      clearTimer()
    }
  }, 1000)
}

/**
 * 打开身份验证弹窗（或在无需验证时直接放行），等待用户输入凭据。
 * 校验通过的请求头通过 resolve 返回；用户取消时 resolve(null)。
 *
 * @param opName 操作名称，用于弹窗提示（如「解散班级」）
 * @returns 通过验证时的请求头（无需验证时为空对象）；取消时为 null
 */
export const requireSensitiveVerification = (opName = ''): Promise<VerifyHeaders | null> => {
  const methods = resolveAvailableMethods()

  // 两种方式都关闭：危险操作验证已关闭，无需验证，直接放行
  if (methods.length === 0) {
    return Promise.resolve({})
  }

  // 已有弹窗在等待时，先取消上一个，避免悬挂的 Promise
  if (resolver) {
    resolver(null)
    resolver = null
  }
  availableMethods.value = methods
  method.value = methods[0]
  operationName.value = opName
  password.value = ''
  emailCode.value = ''
  countdown.value = 0
  clearTimer()
  visible.value = true
  return new Promise<VerifyHeaders | null>((resolve) => {
    resolver = resolve
  })
}

const close = (headers: VerifyHeaders | null) => {
  visible.value = false
  password.value = ''
  emailCode.value = ''
  clearTimer()
  countdown.value = 0
  const r = resolver
  resolver = null
  r?.(headers)
}

/** 用户确认：按所选方式组装请求头并关闭弹窗 */
export const confirmVerification = (): boolean => {
  if (method.value === 'password') {
    const pwd = password.value
    if (!pwd) return false
    close({ 'X-Verify-Method': 'password', 'X-Verify-Password': pwd })
    return true
  }
  const code = emailCode.value.trim()
  if (!code) return false
  close({ 'X-Verify-Method': 'email_code', 'X-Verify-Code': code })
  return true
}

/** 用户取消 */
export const cancelVerification = () => close(null)

/** 发送邮箱验证码（发送到当前用户绑定邮箱） */
export const sendVerifyCode = async (): Promise<{ ok: boolean; message: string }> => {
  sending.value = true
  try {
    const res = await post<number>('/users/sensitive-verification/code')
    if (res.code === 200) {
      const cooldown = typeof res.data === 'number' && res.data > 0 ? res.data : 45
      startCountdown(cooldown)
      return { ok: true, message: res.message || '验证码已发送至绑定邮箱' }
    }
    return { ok: false, message: res.message || '验证码发送失败' }
  } catch {
    return { ok: false, message: '验证码发送失败，请稍后重试' }
  } finally {
    sending.value = false
  }
}

/** 供弹窗组件读取的响应式状态 */
export const useSensitiveVerificationState = () => ({
  visible,
  operationName,
  method,
  password,
  emailCode,
  sending,
  countdown,
  availableMethods,
  canChooseMethod,
})

/**
 * 高危操作的统一入口：先弹「红色警示框」让用户确认，确认后再进行身份二次验证。
 *
 * 把「红色警示框」与「身份验证」绑定在一起，避免各调用点只做其一：
 * 所有高危操作都应通过本函数触发。
 *
 * @param options.title           警示框标题（如「解散学校」）
 * @param options.message         警示内容（将风险讲清楚）
 * @param options.confirmText     确认按钮文案，默认「确认继续」
 * @param options.cancelText      取消按钮文案，默认「取消」
 * @param options.operationName   身份验证弹窗中展示的操作名，默认取 title
 * @returns 通过身份验证时的请求头；用户取消任一步骤时为 null
 */
export const confirmDangerousOperation = async (options: {
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  operationName?: string
}): Promise<VerifyHeaders | null> => {
  try {
    await ElMessageBox.confirm(options.message, options.title, {
      confirmButtonText: options.confirmText || '确认继续',
      cancelButtonText: options.cancelText || '取消',
      type: 'warning',
      // 红色警示框（不透明深红）：所有高危操作统一使用
      customClass: 'danger-warning-message-box',
      // 高危操作不允许点击遮罩误关
      closeOnClickModal: false,
    })
  } catch {
    // 用户取消
    return null
  }
  return requireSensitiveVerification(options.operationName || options.title)
}

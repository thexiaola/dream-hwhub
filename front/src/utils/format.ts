/**
 * 展示用格式化工具
 * 统一各页面重复实现的时间与文件大小格式化，避免同一套规则散落多处
 */

/** 按两位补零 */
const pad = (n: number) => String(n).padStart(2, '0')

/** 解析后端返回的时间字符串，兼容 "yyyy-MM-dd HH:mm:ss" 与 ISO 格式 */
const parse = (value: string): Date => new Date(value.replace(' ', 'T'))

/**
 * 格式化为 yyyy-MM-dd HH:mm
 *
 * @param value    后端返回的时间字符串
 * @param fallback 空值或无法解析时的替代文本
 */
export function formatDateTime(value?: string | null, fallback = '-'): string {
  if (!value) return fallback
  const date = parse(value)
  if (Number.isNaN(date.getTime())) return fallback
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/**
 * 格式化为 yyyy-MM-dd
 *
 * @param value    后端返回的时间字符串
 * @param fallback 空值或无法解析时的替代文本
 */
export function formatDateOnly(value?: string | null, fallback = '-'): string {
  if (!value) return fallback
  const date = parse(value)
  if (Number.isNaN(date.getTime())) return fallback
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/**
 * 格式化文件大小
 */
export function formatFileSize(bytes?: number | null): string {
  if (!bytes) return ''
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

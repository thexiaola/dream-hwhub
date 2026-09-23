import { ElMessage } from 'element-plus'

/**
 * 以「点击查看」方式打开附件（图片/PDF 等浏览器内联预览，其余类型由浏览器处理）。
 *
 * 直接 window.open 无法携带 JWT（后端 AuthInterceptor 需要 Authorization 头），
 * 因此先 fetch 拉取 blob，再通过 objectURL 打开。
 */
export async function openAttachmentPreview(
  filePath: string,
  fileName: string,
  inline = true
): Promise<void> {
  if (!filePath) {
    ElMessage.warning('附件路径缺失')
    return
  }
  const token = localStorage.getItem('token')
  try {
    const url = `/api/files/download?path=${encodeURIComponent(filePath)}&fileName=${encodeURIComponent(fileName)}&inline=${inline}`
    const res = await fetch(url, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
    if (!res.ok) {
      ElMessage.error(res.status === 401 ? '登录已过期，请重新登录' : '附件加载失败')
      return
    }
    const blob = await res.blob()
    const objectUrl = URL.createObjectURL(blob)
    window.open(objectUrl, '_blank')
    // 新窗口打开后延迟释放，避免对象 URL 提前失效
    setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000)
  } catch {
    ElMessage.error('附件加载失败，请稍后重试')
  }
}

/** 格式化文件大小：B / KB / MB */
export function formatFileSize(size?: number | null): string {
  if (!size && size !== 0) return ''
  if (size < 1024) return `${size}B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)}KB`
  return `${(size / 1024 / 1024).toFixed(2)}MB`
}

/**
 * 头像内容缓存：同一路径只向后端拉取一次，缓存的是不可变的 Blob。
 *
 * 后端头像接口需要 Authorization 头，无法直接用 <img src> 加载。这里把 Blob 缓存
 * 起来供所有头像组件复用，各组件各自 createObjectURL、卸载时自行 revoke，
 * 避免共享 objectURL 的引用计数问题。
 */
const avatarBlobCache = new Map<string, Promise<Blob | null>>()

/** 拉取头像 Blob（带缓存）；失败或路径为空时返回 null */
export function loadAvatarBlob(filePath: string): Promise<Blob | null> {
  if (!filePath) return Promise.resolve(null)
  const cached = avatarBlobCache.get(filePath)
  if (cached) return cached

  const task = (async (): Promise<Blob | null> => {
    const token = localStorage.getItem('token')
    try {
      const url = `/api/files/download?path=${encodeURIComponent(filePath)}&inline=true`
      const res = await fetch(url, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
      if (!res.ok) return null
      return await res.blob()
    } catch {
      return null
    }
  })()

  avatarBlobCache.set(filePath, task)
  // 失败结果不缓存，允许后续重试
  task.then(blob => {
    if (!blob) avatarBlobCache.delete(filePath)
  })
  return task
}

/** 头像更新后清除缓存，避免继续显示旧图 */
export function invalidateAvatarCache(filePath?: string | null): void {
  if (filePath) {
    avatarBlobCache.delete(filePath)
  } else {
    avatarBlobCache.clear()
  }
}

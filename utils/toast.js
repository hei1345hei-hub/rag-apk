/**
 * 统一移动端轻提示 Toast
 */

export function toastSuccess(title, duration = 1500) {
  uni.showToast({
    title: String(title || '操作成功'),
    icon: 'success',
    duration
  })
}

export function toastWarning(title, duration = 2000) {
  uni.showToast({
    title: String(title || '请注意'),
    icon: 'none',
    duration
  })
}

export function toastError(title, duration = 2500) {
  uni.showToast({
    title: String(title || '请求失败'),
    icon: 'none',
    duration
  })
}

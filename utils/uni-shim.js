import { onMounted } from 'vue'

let currentRouter = null
export function setRouter(router) {
  currentRouter = router
}

// 轻提示浮层
function showMobileToast(title, icon = 'none', duration = 2000) {
  const existing = document.getElementById('mobile-toast-container')
  if (existing) existing.remove()

  const toast = document.createElement('div')
  toast.id = 'mobile-toast-container'
  toast.style.cssText = `
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    background: rgba(19, 27, 35, 0.92);
    color: #FBFAF6;
    padding: 12px 24px;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 500;
    z-index: 10000;
    box-shadow: 0 4px 16px rgba(0,0,0,0.25);
    text-align: center;
    max-width: 80vw;
    line-height: 1.5;
    animation: toastFadeIn 0.2s ease-out;
  `
  toast.innerText = title
  document.body.appendChild(toast)

  setTimeout(() => {
    if (toast.parentNode) toast.remove()
  }, duration)
}

// 模态弹窗
function showMobileModal({ title = '提示', content = '', confirmColor = '#C4432B', success }) {
  const existing = document.getElementById('mobile-modal-container')
  if (existing) existing.remove()

  const modal = document.createElement('div')
  modal.id = 'mobile-modal-container'
  modal.style.cssText = `
    position: fixed;
    inset: 0;
    background: rgba(19, 27, 35, 0.6);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 10000;
    padding: 24px;
  `

  const box = document.createElement('div')
  box.style.cssText = `
    background: #FBFAF6;
    border: 1px solid #D8D2C4;
    border-radius: 12px;
    width: 100%;
    max-width: 340px;
    overflow: hidden;
    box-shadow: 0 8px 32px rgba(19, 27, 35, 0.2);
    display: flex;
    flex-direction: column;
  `

  const head = document.createElement('div')
  head.style.cssText = 'padding: 16px 20px 8px; font-size: 16px; font-weight: 700; color: #131B23;'
  head.innerText = title

  const body = document.createElement('div')
  body.style.cssText = 'padding: 8px 20px 18px; font-size: 14px; color: #56646F; line-height: 1.6;'
  body.innerText = content

  const foot = document.createElement('div')
  foot.style.cssText = 'display: flex; border-top: 1px solid #EAE5D9;'

  const cancelBtn = document.createElement('button')
  cancelBtn.style.cssText = 'flex: 1; padding: 14px; background: transparent; border: none; font-size: 15px; color: #8A959E; cursor: pointer;'
  cancelBtn.innerText = '取消'
  cancelBtn.onclick = () => {
    modal.remove()
    success?.({ confirm: false, cancel: true })
  }

  const confirmBtn = document.createElement('button')
  confirmBtn.style.cssText = `flex: 1; padding: 14px; background: transparent; border: none; border-left: 1px solid #EAE5D9; font-size: 15px; font-weight: 600; color: ${confirmColor}; cursor: pointer;`
  confirmBtn.innerText = '确定'
  confirmBtn.onclick = () => {
    modal.remove()
    success?.({ confirm: true, cancel: false })
  }

  foot.appendChild(cancelBtn)
  foot.appendChild(confirmBtn)
  box.appendChild(head)
  box.appendChild(body)
  box.appendChild(foot)
  modal.appendChild(box)
  document.body.appendChild(modal)
}

// 模拟 uni 全局对象
export const uni = {
  getStorageSync(key) {
    try {
      return localStorage.getItem(key)
    } catch {
      return null
    }
  },
  setStorageSync(key, val) {
    try {
      localStorage.setItem(key, typeof val === 'string' ? val : JSON.stringify(val))
    } catch (e) {}
  },
  removeStorageSync(key) {
    try {
      localStorage.removeItem(key)
    } catch (e) {}
  },
  navigateTo({ url }) {
    currentRouter?.push(url)
  },
  switchTab({ url }) {
    currentRouter?.push(url)
  },
  redirectTo({ url }) {
    currentRouter?.replace(url)
  },
  reLaunch({ url }) {
    currentRouter?.replace(url)
  },
  navigateBack() {
    currentRouter?.back()
  },
  showToast({ title, icon, duration }) {
    showMobileToast(title, icon, duration)
  },
  showModal(opts) {
    showMobileModal(opts)
  },
  setClipboardData({ data, success }) {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(data).then(() => success?.())
    } else {
      const ta = document.createElement('textarea')
      ta.value = data
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      ta.remove()
      success?.()
    }
  },
  request(options) {
    const method = options.method || 'GET'
    let url = options.url || ''
    const headers = {
      'Content-Type': 'application/json',
      ...(options.header || {})
    }

    const fetchOpts = {
      method,
      headers
    }

    if (method !== 'GET' && options.data) {
      fetchOpts.body = typeof options.data === 'string' ? options.data : JSON.stringify(options.data)
    }

    fetch(url, fetchOpts)
      .then(async (res) => {
        let data
        const text = await res.text()
        try {
          data = JSON.parse(text)
        } catch {
          data = text
        }
        options.success?.({ statusCode: res.status, data })
      })
      .catch((err) => {
        options.fail?.({ errMsg: err.message })
      })

    return {
      abort() {},
      onChunkReceived() {}
    }
  },
  uploadFile(options) {
    // 兼容文件上传
    const formData = new FormData()
    if (options.file) {
      formData.append(options.name || 'file', options.file)
    }
    if (options.formData) {
      Object.entries(options.formData).forEach(([k, v]) => formData.append(k, v))
    }

    fetch(options.url, {
      method: 'POST',
      headers: options.header || {},
      body: formData
    })
      .then(async (res) => {
        const text = await res.text()
        options.success?.({ statusCode: res.status, data: text })
      })
      .catch((err) => {
        options.fail?.({ errMsg: err.message })
      })
  },
  chooseFile(options) {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = options.extension ? options.extension.join(',') : '*/*'
    input.onchange = (e) => {
      const file = e.target.files[0]
      if (file) {
        const url = URL.createObjectURL(file)
        options.success?.({
          tempFiles: [file],
          tempFilePaths: [url]
        })
      }
    }
    input.click()
  },
  stopPullDownRefresh() {}
}

if (typeof window !== 'undefined') {
  window.uni = uni
}

// 模拟 @dcloudio/uni-app 常用 hooks
export function onLoad(callback) {
  onMounted(() => {
    const query = {}
    if (currentRouter?.currentRoute?.value?.query) {
      Object.assign(query, currentRouter.currentRoute.value.query)
    }
    callback(query)
  })
}

export function onPullDownRefresh(callback) {
  onMounted(() => {
    // 移动端下拉刷新 hook
  })
}

export function onShow(callback) {
  onMounted(callback)
}

export function onHide(callback) {}
export function onUnload(callback) {}

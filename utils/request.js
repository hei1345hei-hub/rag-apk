import { getBaseUrl } from './config'
import { toastError } from './toast'

/**
 * 封装 uni.request 为类似 axios 的 Promise API
 */
function createRequest() {
  const send = (options) => {
    return new Promise((resolve, reject) => {
      const baseUrl = getBaseUrl()
      let url = options.url || ''
      if (!url.startsWith('http://') && !url.startsWith('https://')) {
        url = `${baseUrl}${url.startsWith('/') ? '' : '/'}${url}`
      }

      // 请求头带上 Token
      const token = uni.getStorageSync('rag_token')
      const header = {
        'Content-Type': 'application/json',
        ...(options.header || {})
      }
      if (token) {
        header.Authorization = `Bearer ${token}`
      }

      uni.request({
        url,
        method: options.method || 'GET',
        data: options.data,
        header,
        timeout: options.timeout || 60000,
        success: (res) => {
          const { statusCode, data } = res

          // HTTP 401 登录失效
          if (statusCode === 401) {
            uni.removeStorageSync('rag_token')
            uni.removeStorageSync('rag_user')
            toastError('登录状态已失效，请重新登录')
            setTimeout(() => {
              uni.reLaunch({ url: '/pages/login/login' })
            }, 800)
            return reject(new Error('未授权或登录过期'))
          }

          if (statusCode !== 200) {
            const msg = data?.msg || data?.message || `网络异常 (${statusCode})`
            toastError(msg)
            return reject(new Error(msg))
          }

          // 标准返回结构: { code: 200, msg: "...", data: ... }
          if (data && typeof data === 'object' && 'code' in data) {
            if (data.code === 200 || data.code === 0) {
              return resolve(data.data)
            } else {
              const errMsg = data.msg || '业务请求失败'
              toastError(errMsg)
              return reject(new Error(errMsg))
            }
          }

          // 非标准封装（例如直接返回数组或布尔）
          return resolve(data)
        },
        fail: (err) => {
          const errMsg = err.errMsg || '无法连接到后端服务器'
          toastError(`连接失败: 请检查后端IP或网络`)
          reject(new Error(errMsg))
        }
      })
    })
  }

  return {
    get(url, config = {}) {
      let fullUrl = url
      if (config.params) {
        const query = Object.entries(config.params)
          .filter(([_, v]) => v !== undefined && v !== null && v !== '')
          .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
          .join('&')
        if (query) {
          fullUrl += (url.includes('?') ? '&' : '?') + query
        }
      }
      return send({ url: fullUrl, method: 'GET', ...config })
    },
    post(url, data, config = {}) {
      return send({ url, method: 'POST', data, ...config })
    },
    put(url, data, config = {}) {
      return send({ url, method: 'PUT', data, ...config })
    },
    delete(url, config = {}) {
      return send({ url, method: 'DELETE', ...config })
    },
    upload(url, filePath, name = 'file', formData = {}) {
      return new Promise((resolve, reject) => {
        const baseUrl = getBaseUrl()
        let targetUrl = url
        if (!targetUrl.startsWith('http://') && !targetUrl.startsWith('https://')) {
          targetUrl = `${baseUrl}${targetUrl.startsWith('/') ? '' : '/'}${targetUrl}`
        }
        const token = uni.getStorageSync('rag_token')
        const header = {}
        if (token) {
          header.Authorization = `Bearer ${token}`
        }

        uni.uploadFile({
          url: targetUrl,
          filePath,
          name,
          formData,
          header,
          success: (res) => {
            try {
              const data = JSON.parse(res.data)
              if (data.code === 200 || data.code === 0) {
                resolve(data.data)
              } else {
                toastError(data.msg || '上传失败')
                reject(new Error(data.msg || '上传失败'))
              }
            } catch (e) {
              resolve(res.data)
            }
          },
          fail: (err) => {
            toastError('文件上传失败，请重试')
            reject(err)
          }
        })
      })
    }
  }
}

export default createRequest()

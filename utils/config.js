/**
 * 全局服务端连接与模型配置
 */

// 默认连接阿里云服务器上的 rag-nginx (端口 81)
// 该端口已在安全组中开放，内部自动反向代理到 rag-backend (8081) 与 AI 大模型服务
export const DEFAULT_API_HOST = 'http://47.98.190.255:81/api'

const STORAGE_API_KEY = 'rag_api_base_url'

/**
 * 获取当前配置的后端服务基础 URL (例如 http://47.98.190.255:81/api)
 */
export function getBaseUrl() {
  const custom = uni.getStorageSync(STORAGE_API_KEY)
  if (custom && custom.trim()) {
    return custom.trim().replace(/\/+$/, '')
  }
  return DEFAULT_API_HOST
}

/**
 * 保存用户自定义的后端服务地址
 */
export function setBaseUrl(url) {
  if (!url) {
    uni.removeStorageSync(STORAGE_API_KEY)
  } else {
    uni.setStorageSync(STORAGE_API_KEY, url.trim().replace(/\/+$/, ''))
  }
}

/**
 * AI 对话大模型配置
 * 优先走服务器内置反向代理，亦支持直接调用
 */
export const AI_CONFIG = {
  baseUrl: 'http://47.98.190.255:81/ai/v1',
  apiKey: 'sk-r7o5HXZw7N7OVEj92rLXegnq2efaCQuWwE5CJrsLFF6eV6KC',
  model: 'agnes-2.5-flash'
}

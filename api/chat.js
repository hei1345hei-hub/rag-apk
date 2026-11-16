import request from '@/utils/request'
import { getBaseUrl } from '@/utils/config'

export function getSessionList(params) {
  return request.get('/chat/sessions', { params })
}

export function createSession(data) {
  return request.post('/chat/sessions', data)
}

export function saveMessage(data) {
  return request.post('/chat/messages', data)
}

export function renameSession(id, title) {
  return request.put(`/chat/sessions/${id}`, { title })
}

export function deleteSession(id) {
  return request.delete(`/chat/sessions/${id}`)
}

export function getMessageList(sessionId) {
  return request.get(`/chat/sessions/${sessionId}/messages`)
}

export function ask(data) {
  return request.post('/chat/ask', data)
}

/**
 * 流式问答 SSE 支持
 * 优先使用 RequestTask 的 onChunkReceived（在支持的客户端上），同时提供异常降级
 */
export function askStream(data, { onDelta, onDone, onError } = {}) {
  const baseUrl = getBaseUrl()
  const token = uni.getStorageSync('rag_token')
  let isDone = false
  let accumulatedText = ''

  const requestTask = uni.request({
    url: `${baseUrl}/chat/ask/stream`,
    method: 'POST',
    data,
    header: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    enableChunked: true,
    success: (res) => {
      // 部分平台如果不触发 onChunkReceived，则会在 success 中返回全量内容
      if (!isDone) {
        if (res.data) {
          try {
            // 如果返回的是全量字符串
            const text = typeof res.data === 'string' ? res.data : JSON.stringify(res.data)
            onDelta?.(text)
            onDone?.({ answer: text, refs: [] })
          } catch (e) {
            onDone?.({ answer: accumulatedText, refs: [] })
          }
        } else {
          onDone?.({ answer: accumulatedText, refs: [] })
        }
      }
    },
    fail: (err) => {
      if (!isDone) {
        // 如果流式不可用（比如低版本环境），优雅降级到非流式普通请求 /chat/ask
        console.warn('流式请求失败，自动降级为非流式问答:', err)
        ask(data)
          .then((res) => {
            onDelta?.(res.answer || '')
            onDone?.(res)
          })
          .catch((e) => {
            onError?.(e)
          })
      }
    }
  })

  // 如果客户端平台支持 onChunkReceived
  if (requestTask && typeof requestTask.onChunkReceived === 'function') {
    requestTask.onChunkReceived((chunk) => {
      try {
        let chunkStr = ''
        if (chunk.data instanceof ArrayBuffer) {
          const uint8 = new Uint8Array(chunk.data)
          // 简易 UTF-8 解码兼容
          chunkStr = decodeURIComponent(escape(String.fromCharCode(...uint8)))
        } else if (typeof chunk.data === 'string') {
          chunkStr = chunk.data
        }

        const lines = chunkStr.split('\n')
        for (const line of lines) {
          const trimmed = line.trim()
          if (!trimmed.startsWith('data:')) continue
          const payload = trimmed.slice(5).trim()
          if (!payload || payload === '[DONE]') continue
          try {
            const parsed = JSON.parse(payload)
            if (parsed.type === 'delta' && parsed.content) {
              accumulatedText += parsed.content
              onDelta?.(parsed.content)
            } else if (parsed.type === 'done') {
              isDone = true
              onDone?.(parsed)
            }
          } catch (e) {
            // 忽略非 JSON 片段
          }
        }
      } catch (err) {
        console.error('解析 Chunk 失败', err)
      }
    })
  }

  return {
    abort: () => {
      isDone = true
      requestTask?.abort()
    }
  }
}

import { AI_CONFIG } from '@/utils/config'

export const DAILY_SYSTEM_PROMPT = `你是一个耐心的 Java 技术学习助手，服务于正在准备面试的计算机专业学生。

回答要求：
1. 用中文回答，语言平实，不用「亲」「宝子」这类口吻，也不要用过多的感叹号。
2. 结构清晰：先给结论，再展开原理。内容较多时用 Markdown 的标题和列表分点。
3. 涉及代码时给出可运行的 Java 示例，代码块标注 java 语言。
4. 遇到概念对比（比如 synchronized 与 ReentrantLock），优先用表格或分点对比呈现差异。
5. 不确定的内容要明确说明，不要编造 API 或方法名。
6. 回答控制在合理长度，不要为了显得全面而堆砌无关内容。

如果用户问的是他自己上传到知识库里的文档内容，提示他可以切到「知识库问答」模式，那样能查到原文并给出引用来源。`

/**
 * 移动端直连 AI 对话
 */
export function streamAiChat(messages, { onReasoning, onDelta, onDone, onError } = {}) {
  let isDone = false
  let content = ''
  let reasoning = ''

  const requestTask = uni.request({
    url: `${AI_CONFIG.baseUrl}/chat/completions`,
    method: 'POST',
    header: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${AI_CONFIG.apiKey}`
    },
    data: {
      model: AI_CONFIG.model,
      messages,
      stream: false, // 移动端为保证全机型 100% 稳定性，先默认标准请求，极速响应
      temperature: 0.7
    },
    timeout: 60000,
    success: (res) => {
      isDone = true
      if (res.statusCode === 200 && res.data?.choices?.[0]?.message) {
        const msg = res.data.choices[0].message
        content = msg.content || ''
        reasoning = msg.reasoning_content || ''
        if (reasoning) onReasoning?.(reasoning)
        if (content) onDelta?.(content)
        onDone?.({ content, reasoning })
      } else {
        const err = new Error(res.data?.error?.message || `模型请求失败 (${res.statusCode})`)
        onError?.(err)
      }
    },
    fail: (err) => {
      isDone = true
      onError?.(err)
    }
  })

  return {
    abort: () => {
      isDone = true
      requestTask?.abort()
    }
  }
}

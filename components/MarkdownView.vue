<template>
  <view class="md-container">
    <view v-for="(block, idx) in parsedBlocks" :key="idx" class="md-block">
      <!-- 代码块 -->
      <view v-if="block.type === 'code'" class="md-code-box">
        <view class="md-code-head">
          <text class="md-code-lang">{{ block.lang || 'code' }}</text>
          <text class="md-code-copy" @tap="copyCode(block.content)">复制</text>
        </view>
        <scroll-view scroll-x class="md-code-scroll">
          <text class="md-code-text font-mono">{{ block.content }}</text>
        </scroll-view>
      </view>

      <!-- 标题 -->
      <view v-else-if="block.type === 'h1'" class="md-h1">
        <text>{{ block.content }}</text>
      </view>
      <view v-else-if="block.type === 'h2'" class="md-h2">
        <text>{{ block.content }}</text>
      </view>
      <view v-else-if="block.type === 'h3'" class="md-h3">
        <text>{{ block.content }}</text>
      </view>

      <!-- 引用块 -->
      <view v-else-if="block.type === 'quote'" class="md-quote">
        <text>{{ block.content }}</text>
      </view>

      <!-- 无序/有序列表 -->
      <view v-else-if="block.type === 'list'" class="md-list-item">
        <text class="md-list-bullet">{{ block.bullet }}</text>
        <rich-text class="md-list-text" :nodes="formatInline(block.content)"></rich-text>
      </view>

      <!-- 普通段落 -->
      <view v-else class="md-p">
        <rich-text :nodes="formatInline(block.content)"></rich-text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { toastSuccess } from '@/utils/toast'

const props = defineProps({
  content: {
    type: String,
    default: ''
  }
})

function copyCode(text) {
  uni.setClipboardData({
    data: text,
    success: () => {
      toastSuccess('代码已复制')
    }
  })
}

// 转义 HTML 字符
function escapeHtml(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

// 格式化行内元素（粗体、高亮标签等）
function formatInline(str) {
  if (!str) return ''
  let html = escapeHtml(str)
  // **粗体**
  html = html.replace(/\*\*(.*?)\*\*/g, '<strong style="color: #131B23; font-weight: 600;">$1</strong>')
  // `行内代码`
  html = html.replace(
    /`([^`]+)`/g,
    '<span style="background-color: #EAE5D9; color: #C4432B; padding: 2px 6px; border-radius: 4px; font-family: monospace; font-size: 13px;">$1</span>'
  )
  return html
}

const parsedBlocks = computed(() => {
  if (!props.content) return []
  const text = props.content.replace(/\r\n/g, '\n')
  const lines = text.split('\n')
  const blocks = []

  let inCode = false
  let codeLang = ''
  let codeBuffer = []

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]

    // 代码块判定 ```
    if (line.trim().startsWith('```')) {
      if (inCode) {
        // 代码块结束
        blocks.push({
          type: 'code',
          lang: codeLang,
          content: codeBuffer.join('\n')
        })
        inCode = false
        codeLang = ''
        codeBuffer = []
      } else {
        // 代码块开始
        inCode = true
        codeLang = line.trim().slice(3).trim()
        codeBuffer = []
      }
      continue
    }

    if (inCode) {
      codeBuffer.push(line)
      continue
    }

    const trimmed = line.trim()
    if (!trimmed) continue

    // 标题
    if (trimmed.startsWith('### ')) {
      blocks.push({ type: 'h3', content: trimmed.slice(4).trim() })
    } else if (trimmed.startsWith('## ')) {
      blocks.push({ type: 'h2', content: trimmed.slice(3).trim() })
    } else if (trimmed.startsWith('# ')) {
      blocks.push({ type: 'h1', content: trimmed.slice(2).trim() })
    }
    // 引用
    else if (trimmed.startsWith('> ')) {
      blocks.push({ type: 'quote', content: trimmed.slice(2).trim() })
    }
    // 无序列表
    else if (/^[-*]\s+/.test(trimmed)) {
      blocks.push({
        type: 'list',
        bullet: '•',
        content: trimmed.replace(/^[-*]\s+/, '')
      })
    }
    // 有序列表
    else if (/^\d+\.\s+/.test(trimmed)) {
      const match = trimmed.match(/^(\d+\.)\s+(.*)$/)
      blocks.push({
        type: 'list',
        bullet: match ? match[1] : '•',
        content: match ? match[2] : trimmed
      })
    }
    // 普通段落
    else {
      blocks.push({ type: 'p', content: trimmed })
    }
  }

  if (inCode && codeBuffer.length) {
    blocks.push({
      type: 'code',
      lang: codeLang,
      content: codeBuffer.join('\n')
    })
  }

  return blocks
})
</script>

<style lang="scss" scoped>
.md-container {
  font-size: 28rpx;
  line-height: 1.7;
  color: #2A3846;
}

.md-block {
  margin-bottom: 18rpx;
}

.md-h1 {
  font-size: 34rpx;
  font-weight: 700;
  color: #131B23;
  margin-top: 24rpx;
  margin-bottom: 12rpx;
  border-left: 6rpx solid #C4432B;
  padding-left: 14rpx;
}

.md-h2 {
  font-size: 30rpx;
  font-weight: 600;
  color: #131B23;
  margin-top: 20rpx;
  margin-bottom: 10rpx;
  border-left: 4rpx solid #131B23;
  padding-left: 12rpx;
}

.md-h3 {
  font-size: 28rpx;
  font-weight: 600;
  color: #131B23;
  margin-top: 16rpx;
  margin-bottom: 8rpx;
}

.md-p {
  line-height: 1.75;
  color: #2A3846;
}

.md-quote {
  padding: 12rpx 20rpx;
  background-color: #EFEBE2;
  border-left: 6rpx solid #C4432B;
  border-radius: 4rpx;
  font-size: 26rpx;
  color: #56646F;
}

.md-list-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 8rpx;
  line-height: 1.6;
}

.md-list-bullet {
  color: #C4432B;
  font-weight: bold;
  margin-right: 12rpx;
  flex-shrink: 0;
}

.md-list-text {
  flex: 1;
}

/* 代码块 */
.md-code-box {
  background-color: #1A232C;
  border-radius: 8rpx;
  border: 1px solid #2C3B47;
  overflow: hidden;
  margin: 14rpx 0;
}

.md-code-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #131B23;
  padding: 8rpx 20rpx;
  border-bottom: 1px solid #2A3846;
}

.md-code-lang {
  color: #AFB7BD;
  font-size: 22rpx;
  text-transform: uppercase;
  font-family: monospace;
}

.md-code-copy {
  color: #F6E4DF;
  font-size: 22rpx;
  background-color: #C4432B;
  padding: 2rpx 14rpx;
  border-radius: 4rpx;
}

.md-code-scroll {
  width: 100%;
  padding: 16rpx 20rpx;
}

.md-code-text {
  color: #E8E4DA;
  font-size: 24rpx;
  line-height: 1.5;
  white-space: pre;
}
</style>

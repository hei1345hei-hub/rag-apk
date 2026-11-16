<template>
  <view class="chat-page">
    <!-- 顶部状态栏：模式切换与会话切换 -->
    <view class="chat-top-bar">
      <!-- 模式切换：RAG / CHAT -->
      <view class="mode-pills">
        <view
          class="mode-pill"
          :class="{ active: mode === 'RAG' }"
          @tap="switchMode('RAG')"
        >
          <text>🧠 知识库问答</text>
        </view>
        <view
          class="mode-pill"
          :class="{ active: mode === 'CHAT' }"
          @tap="switchMode('CHAT')"
        >
          <text>🤖 日常对话</text>
        </view>
      </view>

      <!-- 会话抽屉入口 -->
      <view class="session-btn" @tap="sessionDrawerOpen = true">
        <text class="session-title-text">{{ currentSession?.title || '新会话' }}</text>
        <text class="session-arrow">▼</text>
      </view>
    </view>

    <!-- 消息列表滚动区域 -->
    <scroll-view
      scroll-y
      class="message-scroll"
      :scroll-top="scrollTop"
      :scroll-into-view="scrollIntoViewId"
      @tap="closeDrawer"
    >
      <!-- 欢迎卡片 -->
      <view v-if="!messages.length" class="welcome-box">
        <view class="welcome-badge">
          <text>{{ mode === 'RAG' ? 'RAG RETRIEVAL' : 'AI ASSISTANT' }}</text>
        </view>
        <text class="welcome-title">
          {{ mode === 'RAG' ? '基于企业知识库的问答' : 'Java 面试技术对话助手' }}
        </text>
        <text class="welcome-desc">
          {{ mode === 'RAG'
            ? '精准检索已上传的 Java 笔记与面试文档，每次回答均给出原文切片与引用来源。'
            : '支持底层原理剖析、源码解读、概念对比与手写可运行代码示例。' }}
        </text>

        <!-- 常用预设提问快捷气泡 -->
        <view class="prompt-chips">
          <text class="prompt-chip" @tap="quickAsk('HashMap 的底层数据结构与扩容机制？')">
            HashMap 底层原理与扩容机制
          </text>
          <text class="prompt-chip" @tap="quickAsk('synchronized 与 ReentrantLock 有什么区别？')">
            synchronized 与 ReentrantLock 区别
          </text>
          <text class="prompt-chip" @tap="quickAsk('JVM 堆内存分代与垃圾回收算法介绍？')">
            JVM 堆内存分代与垃圾回收
          </text>
          <text class="prompt-chip" @tap="quickAsk('Spring 声明式事务在什么情况下会失效？')">
            Spring 声明式事务失效场景
          </text>
        </view>
      </view>

      <!-- 消息气泡列表 -->
      <view
        v-for="(msg, idx) in messages"
        :id="'msg-' + idx"
        :key="msg.id || idx"
        class="message-wrapper"
        :class="msg.role === 'user' ? 'msg-user' : 'msg-ai'"
      >
        <!-- 头像 -->
        <view class="msg-avatar">
          <text>{{ msg.role === 'user' ? '我' : 'AI' }}</text>
        </view>

        <!-- 气泡内容 -->
        <view class="msg-bubble">
          <!-- 思维链折叠区 -->
          <view v-if="msg.reasoning" class="reasoning-box">
            <text class="reasoning-label">思考过程：</text>
            <text class="reasoning-text">{{ msg.reasoning }}</text>
          </view>

          <!-- Markdown 正文 -->
          <MarkdownView :content="msg.content" />

          <!-- 引用来源（仅 RAG 模式 assistant 消息） -->
          <view v-if="msg.refs && msg.refs.length" class="refs-section">
            <view class="refs-toggle" @tap="toggleRefs(idx)">
              <text class="refs-toggle-text">
                📑 引用了 {{ msg.refs.length }} 处知识库切片 {{ expandedRefs[idx] ? '▲' : '▼' }}
              </text>
            </view>
            <view v-if="expandedRefs[idx]" class="refs-list">
              <view
                v-for="(refItem, rIdx) in msg.refs"
                :key="rIdx"
                class="ref-card"
              >
                <view class="ref-card-head">
                  <text class="ref-doc-name">{{ refItem.docName || '文档片段' }}</text>
                  <text v-if="refItem.score != null" class="ref-score font-mono">
                    相似度: {{ (refItem.score * 100).toFixed(1) }}%
                  </text>
                </view>
                <text class="ref-content">{{ refItem.content }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 流式生成中思考反馈 -->
      <view v-if="streaming && thinking" class="thinking-indicator">
        <text class="thinking-dot">●</text>
        <text class="thinking-text">模型正在分析并检索思考中...</text>
      </view>

      <!-- 占位底部避免被输入栏遮挡 -->
      <view class="bottom-pad" id="bottom-pad"></view>
    </scroll-view>

    <!-- 底部输入栏 -->
    <view class="chat-input-bar">
      <textarea
        v-model="inputContent"
        class="chat-textarea"
        :auto-height="true"
        :maxlength="2000"
        placeholder="输入 Java 技术问题向助手提问..."
        placeholder-class="placeholder-style"
        :cursor-spacing="20"
      />
      <button
        class="btn-primary send-btn"
        :disabled="!canSend"
        :loading="sending"
        @tap="handleSend"
      >
        发送
      </button>
    </view>

    <!-- 会话列表抽屉 -->
    <view v-if="sessionDrawerOpen" class="drawer-mask" @tap="sessionDrawerOpen = false">
      <view class="drawer-content" @tap.stop>
        <view class="drawer-header">
          <text class="drawer-title">会话历史</text>
          <button class="btn-primary btn-sm new-session-btn" @tap="handleCreateNewSession">
            + 新建会话
          </button>
        </view>

        <scroll-view scroll-y class="drawer-session-list">
          <view
            v-for="s in sessions"
            :key="s.id"
            class="drawer-session-item"
            :class="{ active: currentId === s.id }"
            @tap="selectSession(s.id)"
          >
            <text class="session-item-title">{{ s.title }}</text>
            <view class="session-item-actions" @tap.stop>
              <text class="session-del-btn" @tap="handleDeleteSession(s.id)">✕</text>
            </view>
          </view>
          <view v-if="!sessions.length" class="drawer-empty">
            <text>暂无历史会话</text>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import {
  getSessionList,
  createSession,
  deleteSession,
  getMessageList,
  saveMessage,
  ask,
  askStream
} from '@/api/chat'
import { streamAiChat, DAILY_SYSTEM_PROMPT } from '@/api/ai'
import { toastSuccess, toastWarning, toastError } from '@/utils/toast'
import MarkdownView from '@/components/MarkdownView.vue'

const mode = ref('RAG') // RAG | CHAT
const sessions = ref([])
const messages = ref([])
const currentId = ref(null)
const sending = ref(false)
const streaming = ref(false)
const thinking = ref(false)
const inputContent = ref('')

const sessionDrawerOpen = ref(false)
const expandedRefs = ref({})

const scrollTop = ref(0)
const scrollIntoViewId = ref('')

const currentSession = computed(() =>
  sessions.value.find((s) => s.id === currentId.value) || null
)

const canSend = computed(() => inputContent.value.trim() && !sending.value)

onMounted(() => {
  loadSessions()
})

async function switchMode(next) {
  if (mode.value === next) return
  mode.value = next
  currentId.value = null
  messages.value = []
  expandedRefs.value = {}
  await loadSessions()
}

async function loadSessions() {
  try {
    const list = await getSessionList({ sessionType: mode.value })
    sessions.value = list || []
    if (!currentId.value && sessions.value.length) {
      await selectSession(sessions.value[0].id)
    }
  } catch (e) {
    sessions.value = []
  }
}

async function selectSession(id) {
  currentId.value = id
  sessionDrawerOpen.value = false
  expandedRefs.value = {}
  try {
    const list = await getMessageList(id)
    messages.value = list || []
    await scrollToBottom()
  } catch (e) {
    messages.value = []
  }
}

async function handleCreateNewSession() {
  try {
    const res = await createSession({
      title: '新对话',
      sessionType: mode.value
    })
    sessionDrawerOpen.value = false
    await loadSessions()
    if (res && res.id) {
      await selectSession(res.id)
    }
  } catch (e) {
    toastError('新建会话失败')
  }
}

async function handleDeleteSession(id) {
  uni.showModal({
    title: '确认删除',
    content: '确定要删除该问答会话吗？',
    confirmColor: '#C4432B',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteSession(id)
          toastSuccess('会话已删除')
          if (currentId.value === id) {
            currentId.value = null
            messages.value = []
          }
          await loadSessions()
        } catch (e) {
          toastError('删除失败')
        }
      }
    }
  })
}

function quickAsk(q) {
  inputContent.value = q
  handleSend()
}

function toggleRefs(idx) {
  expandedRefs.value[idx] = !expandedRefs.value[idx]
}

async function scrollToBottom() {
  await nextTick()
  scrollIntoViewId.value = 'bottom-pad'
  scrollTop.value = 99999 + Math.random()
}

async function handleSend() {
  const q = inputContent.value.trim()
  if (!q || sending.value) return

  inputContent.value = ''
  sending.value = true

  // 如果没有当前会话，先创建一个
  if (!currentId.value) {
    try {
      const s = await createSession({
        title: q.slice(0, 16),
        sessionType: mode.value
      })
      currentId.value = s.id
      await loadSessions()
    } catch (e) {
      sending.value = false
      return
    }
  }

  // 1. 本地立即追加用户消息
  messages.value.push({
    role: 'user',
    content: q
  })
  await scrollToBottom()

  // 2. 区分 RAG 与 CHAT 模式
  if (mode.value === 'RAG') {
    await sendRag(q)
  } else {
    await sendChat(q)
  }
  sending.value = false
}

// RAG 知识库问答
async function sendRag(q) {
  // 追加一条空的 assistant 消息
  const aiMsgIdx = messages.value.length
  messages.value.push({
    role: 'assistant',
    content: '',
    refs: []
  })
  streaming.value = true
  thinking.value = true

  try {
    // 优先尝试流式问答
    await new Promise((resolve, reject) => {
      askStream(
        {
          sessionId: currentId.value,
          question: q
        },
        {
          onDelta: (chunk) => {
            thinking.value = false
            messages.value[aiMsgIdx].content += chunk
            scrollToBottom()
          },
          onDone: (res) => {
            thinking.value = false
            streaming.value = false
            if (res.refs) {
              messages.value[aiMsgIdx].refs = res.refs
            }
            resolve(res)
          },
          onError: (err) => {
            thinking.value = false
            streaming.value = false
            reject(err)
          }
        }
      )
    })
  } catch (e) {
    // 降级为非流式
    try {
      const res = await ask({
        sessionId: currentId.value,
        question: q
      })
      messages.value[aiMsgIdx].content = res.answer || ''
      messages.value[aiMsgIdx].refs = res.refs || []
    } catch (err) {
      messages.value[aiMsgIdx].content = '问答请求失败，请检查后端网络连接'
    }
  } finally {
    streaming.value = false
    thinking.value = false
    await scrollToBottom()
  }
}

// AI 日常对话
async function sendChat(q) {
  const aiMsgIdx = messages.value.length
  messages.value.push({
    role: 'assistant',
    content: '',
    reasoning: ''
  })
  streaming.value = true
  thinking.value = true

  const chatHistory = [
    { role: 'system', content: DAILY_SYSTEM_PROMPT },
    ...messages.value.slice(0, -1).map((m) => ({
      role: m.role,
      content: m.content
    }))
  ]

  try {
    await new Promise((resolve, reject) => {
      streamAiChat(chatHistory, {
        onReasoning: (r) => {
          messages.value[aiMsgIdx].reasoning += r
        },
        onDelta: (c) => {
          thinking.value = false
          messages.value[aiMsgIdx].content += c
          scrollToBottom()
        },
        onDone: async ({ content }) => {
          thinking.value = false
          streaming.value = false
          // 提交落库
          try {
            await saveMessage({
              sessionId: currentId.value,
              role: 'user',
              content: q
            })
            await saveMessage({
              sessionId: currentId.value,
              role: 'assistant',
              content
            })
          } catch (e) {}
          resolve()
        },
        onError: (err) => {
          thinking.value = false
          streaming.value = false
          reject(err)
        }
      })
    })
  } catch (e) {
    messages.value[aiMsgIdx].content = '对话服务请求异常，请检查网络或配置'
  } finally {
    streaming.value = false
    thinking.value = false
    await scrollToBottom()
  }
}
</script>

<style lang="scss" scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  /* 减去底部悬浮 TabBar 占位，避免输入栏被盖住 */
  height: calc(100vh - 84px - var(--safe-bottom));
  background-color: #F4F1EA;
}

/* 顶部模式栏 */
.chat-top-bar {
  background-color: #FBFAF6;
  border-bottom: 1px solid #D8D2C4;
  padding: 16rpx 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.mode-pills {
  display: flex;
  background-color: #EFEBE2;
  border-radius: 30rpx;
  padding: 4rpx;
}

.mode-pill {
  padding: 8rpx 20rpx;
  border-radius: 26rpx;
  font-size: 24rpx;
  color: #56646F;
}

.mode-pill.active {
  background-color: #131B23;
  color: #FBFAF6;
  font-weight: 600;
}

.session-btn {
  display: flex;
  align-items: center;
  gap: 8rpx;
  background-color: #EFEBE2;
  border: 1px solid #D8D2C4;
  padding: 8rpx 16rpx;
  border-radius: 6rpx;
  max-width: 240rpx;
}

.session-title-text {
  font-size: 22rpx;
  color: #131B23;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-arrow {
  font-size: 18rpx;
  color: #8A959E;
}

/* 消息滚动区 */
.message-scroll {
  flex: 1;
  padding: 24rpx;
  box-sizing: border-box;
}

.welcome-box {
  background-color: #FBFAF6;
  border: 1px solid #D8D2C4;
  border-radius: 12rpx;
  padding: 32rpx;
  margin-bottom: 30rpx;
  text-align: center;
}

.welcome-badge {
  display: inline-block;
  background-color: #F6E4DF;
  color: #C4432B;
  font-size: 20rpx;
  font-weight: 700;
  padding: 4rpx 14rpx;
  border-radius: 4rpx;
  margin-bottom: 12rpx;
}

.welcome-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #131B23;
  display: block;
  margin-bottom: 8rpx;
}

.welcome-desc {
  font-size: 24rpx;
  color: #56646F;
  line-height: 1.6;
  display: block;
  margin-bottom: 24rpx;
}

.prompt-chips {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.prompt-chip {
  background-color: #FFFFFF;
  border: 1px solid #D8D2C4;
  border-radius: 6rpx;
  padding: 14rpx 20rpx;
  font-size: 24rpx;
  color: #24506B;
  text-align: left;
}

.prompt-chip:active {
  background-color: #EFEBE2;
}

/* 消息气泡 */
.message-wrapper {
  display: flex;
  gap: 16rpx;
  margin-bottom: 30rpx;
}

.msg-avatar {
  width: 56rpx;
  height: 56rpx;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  font-weight: 700;
  flex-shrink: 0;
}

.msg-user {
  flex-direction: row-reverse;
}

.msg-user .msg-avatar {
  background-color: #131B23;
  color: #FBFAF6;
}

.msg-user .msg-bubble {
  background-color: #131B23;
  color: #FBFAF6;
  border-radius: 12rpx 2rpx 12rpx 12rpx;
}

.msg-ai .msg-avatar {
  background-color: #C4432B;
  color: #FFFFFF;
}

.msg-ai .msg-bubble {
  background-color: #FBFAF6;
  border: 1px solid #D8D2C4;
  border-radius: 2rpx 12rpx 12rpx 12rpx;
}

.msg-bubble {
  max-width: 82%;
  padding: 20rpx 24rpx;
  box-shadow: 0 2rpx 6rpx rgba(19, 27, 35, 0.04);
}

.reasoning-box {
  background-color: #EFEBE2;
  border-left: 4rpx solid #A9761B;
  padding: 10rpx 14rpx;
  margin-bottom: 14rpx;
  border-radius: 4rpx;
  font-size: 22rpx;
  color: #56646F;
}

.reasoning-label {
  font-weight: 600;
  color: #A9761B;
  display: block;
  margin-bottom: 4rpx;
}

/* 引用切片 */
.refs-section {
  margin-top: 18rpx;
  padding-top: 14rpx;
  border-top: 1px dashed #D8D2C4;
}

.refs-toggle-text {
  font-size: 22rpx;
  color: #24506B;
  font-weight: 500;
}

.refs-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  margin-top: 12rpx;
}

.ref-card {
  background-color: #F4F1EA;
  border: 1px solid #EAE5D9;
  border-radius: 6rpx;
  padding: 12rpx 16rpx;
}

.ref-card-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6rpx;
}

.ref-doc-name {
  font-size: 20rpx;
  font-weight: 600;
  color: #131B23;
}

.ref-score {
  font-size: 20rpx;
  color: #2A6B63;
}

.ref-content {
  font-size: 22rpx;
  color: #56646F;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.thinking-indicator {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 14rpx 20rpx;
  background-color: #FBFAF6;
  border: 1px dashed #D8D2C4;
  border-radius: 8rpx;
  margin-bottom: 20rpx;
}

.thinking-dot {
  color: #C4432B;
  animation: pulse 1s infinite alternate;
}

@keyframes pulse {
  0% { opacity: 0.3; }
  100% { opacity: 1; }
}

.thinking-text {
  font-size: 24rpx;
  color: #8A959E;
}

.bottom-pad {
  height: 20rpx;
}

/* 底部输入栏 */
.chat-input-bar {
  background-color: #FBFAF6;
  border-top: 1px solid #D8D2C4;
  padding: 16rpx 20rpx;
  display: flex;
  align-items: flex-end;
  gap: 16rpx;
  flex-shrink: 0;
}

.chat-textarea {
  flex: 1;
  background-color: #FFFFFF;
  border: 1px solid #D8D2C4;
  border-radius: 8rpx;
  padding: 16rpx 20rpx;
  font-size: 28rpx;
  max-height: 180rpx;
  color: #131B23;
}

.send-btn {
  height: 72rpx;
  padding: 0 32rpx;
  font-size: 26rpx;
  border-radius: 8rpx;
  flex-shrink: 0;
}

/* 抽屉遮罩 */
.drawer-mask {
  position: fixed;
  inset: 0;
  background-color: rgba(19, 27, 35, 0.5);
  z-index: 1000;
  display: flex;
  justify-content: flex-end;
}

.drawer-content {
  width: 75%;
  height: 100%;
  background-color: #FBFAF6;
  border-left: 1px solid #D8D2C4;
  display: flex;
  flex-direction: column;
}

.drawer-header {
  padding: 28rpx 24rpx;
  border-bottom: 1px solid #EAE5D9;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.drawer-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #131B23;
}

.new-session-btn {
  margin: 0;
}

.drawer-session-list {
  flex: 1;
  padding: 16rpx;
}

.drawer-session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 16rpx;
  border-radius: 6rpx;
  margin-bottom: 8rpx;
  border: 1px solid transparent;
}

.drawer-session-item.active {
  background-color: #EFEBE2;
  border-color: #D8D2C4;
}

.session-item-title {
  font-size: 26rpx;
  color: #131B23;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-del-btn {
  font-size: 26rpx;
  color: #8A959E;
  padding: 6rpx 12rpx;
}

.drawer-empty {
  padding: 60rpx 0;
  text-align: center;
  font-size: 24rpx;
  color: #8A959E;
}
</style>

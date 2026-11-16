<template>
  <view class="room-page">
    <!-- 顶部状态栏 -->
    <view class="room-top-bar">
      <view class="room-meta">
        <text class="room-pos">{{ room?.position || 'Java工程师' }}</text>
        <view class="room-tags">
          <text class="tag tag-slate">{{ room?.level || '中级' }}</text>
          <text class="round-counter font-mono">
            第 {{ currentRoundNo }} / {{ room?.roundTotal || 5 }} 轮
          </text>
        </view>
      </view>

      <button class="btn-ghost btn-sm finish-btn" :loading="finishing" @tap="handleFinish">
        提前结束
      </button>
    </view>

    <!-- 答题考场主体滚动区域 -->
    <scroll-view scroll-y class="room-scroll" :scroll-top="scrollTop">
      <!-- 考场规则与提示 -->
      <view class="room-notice">
        <text class="notice-text">
          💡 面试官将依据您的每一轮回答进行深度推导与针对性追问。请先答核心结论，再结合实际项目或底层原理阐述。
        </text>
      </view>

      <!-- 历史已答完轮次卡片列表 -->
      <view
        v-for="qa in answeredList"
        :key="qa.roundNo"
        class="rag-card history-card"
      >
        <view class="history-head" @tap="toggleHistoryCollapse(qa.roundNo)">
          <view class="history-title-line">
            <text class="num-badge">R{{ qa.roundNo }}</text>
            <text class="history-q-preview">{{ qa.question }}</text>
          </view>
          <view class="history-score-badge font-mono">
            {{ qa.score != null ? qa.score + '分' : '已答' }}
            <text class="collapse-icon">{{ collapsed[qa.roundNo] ? '▼' : '▲' }}</text>
          </view>
        </view>

        <!-- 展开内容：我的回答与面试官点评 -->
        <view v-if="!collapsed[qa.roundNo]" class="history-body">
          <view class="history-section">
            <text class="section-label">你的作答：</text>
            <text class="history-answer">{{ qa.answer }}</text>
          </view>

          <view v-if="qa.comment" class="history-section comment-section">
            <text class="section-label comment-label">面试官点评与改进：</text>
            <text class="history-comment">{{ qa.comment }}</text>
          </view>
        </view>
      </view>

      <!-- 当前进行中的轮次 -->
      <view v-if="currentQa" class="rag-card active-card">
        <view class="rag-card-head">
          <view class="rag-card-title">
            <text class="num-badge">第 {{ currentQa.roundNo }} 题</text>
            <text>当前作答中</text>
          </view>
          <text class="tag tag-amber">回答提交后打分</text>
        </view>

        <!-- 题目正文 -->
        <view class="question-box">
          <MarkdownView :content="currentQa.question" />
        </view>

        <!-- 回答编辑框 -->
        <view class="answer-box">
          <textarea
            v-model="userAnswer"
            class="answer-textarea"
            placeholder="请在此输入你的回答思路、核心原理及示例代码..."
            placeholder-class="placeholder-style"
            :maxlength="4000"
          />
          <view class="answer-foot">
            <text class="char-count font-mono">{{ userAnswer.length }} 字</text>
            <button
              class="btn-primary btn-sm submit-answer-btn"
              :disabled="!userAnswer.trim() || submitting"
              :loading="submitting"
              @tap="handleSubmitAnswer"
            >
              提交本轮回答
            </button>
          </view>
        </view>
      </view>

      <!-- 出题中/评分中动效 -->
      <view v-if="asking" class="asking-card">
        <view class="asking-dot"></view>
        <text class="asking-text">AI 面试官正在综合评估您的回答并构思下一题...</text>
      </view>

      <view style="height: 30px;"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, reactive, onMounted, nextTick } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  getSessionDetail,
  nextQuestion,
  submitAnswer,
  finishSession
} from '@/api/interview'
import { toastSuccess, toastWarning, toastError } from '@/utils/toast'
import MarkdownView from '@/components/MarkdownView.vue'

const sessionId = ref(null)
const room = ref(null)
const userAnswer = ref('')
const submitting = ref(false)
const asking = ref(false)
const finishing = ref(false)
const scrollTop = ref(0)
const collapsed = reactive({})

onLoad((options) => {
  sessionId.value = Number(options.id)
})

onMounted(() => {
  loadDetail()
})

async function loadDetail() {
  if (!sessionId.value) return
  try {
    const res = await getSessionDetail(sessionId.value)
    room.value = res
    // 如果已经答完全部题目或者状态为已完成，直接去报告页
    if (res.status === 1) {
      goToReport()
      return
    }

    // 默认收起早期轮次
    if (res.qaList) {
      res.qaList.forEach((q) => {
        if (q.score != null) {
          collapsed[q.roundNo] = true
        }
      })
    }

    // 如果还没有题目（刚创建），请求第一题
    if (!res.qaList || !res.qaList.length) {
      await fetchNextQuestion()
    }
  } catch (e) {
    toastError('加载考场失败')
  }
}

// 历史已答完轮次
const answeredList = computed(() => {
  return (room.value?.qaList || []).filter((q) => q.score != null)
})

// 当前正在作答的轮次
const currentQa = computed(() => {
  return (room.value?.qaList || []).find((q) => q.score == null) || null
})

const currentRoundNo = computed(() => {
  return currentQa.value?.roundNo || (answeredList.value.length + 1)
})

function toggleHistoryCollapse(roundNo) {
  collapsed[roundNo] = !collapsed[roundNo]
}

async function fetchNextQuestion() {
  asking.value = true
  try {
    await nextQuestion(sessionId.value)
    await loadDetail()
  } catch (e) {
    toastError('出题失败，请点击重试')
  } finally {
    asking.value = false
    scrollToBottom()
  }
}

async function handleSubmitAnswer() {
  const ans = userAnswer.value.trim()
  if (!ans || !currentQa.value) return

  submitting.value = true
  try {
    const roundNo = currentQa.value.roundNo
    const res = await submitAnswer(sessionId.value, {
      roundNo,
      answer: ans
    })

    toastSuccess(`第 ${roundNo} 轮得分：${res.score}分`)
    userAnswer.value = ''

    // 判断是否已达到总轮数
    if (roundNo >= (room.value?.roundTotal || 5)) {
      // 全部轮次完成，自动结束并生成报告
      await handleFinish(true)
    } else {
      // 请求下一道追问
      await fetchNextQuestion()
    }
  } catch (e) {
    toastError('提交回答失败，请重试')
  } finally {
    submitting.value = false
  }
}

async function handleFinish(auto = false) {
  if (!auto) {
    uni.showModal({
      title: '提前结束面试',
      content: '确定提前交卷结束本次面试？AI 将根据已答轮次出具综合多维评测报告。',
      confirmColor: '#C4432B',
      success: async (res) => {
        if (res.confirm) {
          doFinish()
        }
      }
    })
  } else {
    doFinish()
  }
}

async function doFinish() {
  finishing.value = true
  try {
    await finishSession(sessionId.value)
    toastSuccess('面试已结束，正在生成能力评估报告')
    setTimeout(() => {
      goToReport()
    }, 600)
  } catch (e) {
    toastError('结束面试失败')
  } finally {
    finishing.value = false
  }
}

function goToReport() {
  uni.redirectTo({
    url: `/pages/interview/report?id=${sessionId.value}`
  })
}

function scrollToBottom() {
  nextTick(() => {
    scrollTop.value = 99999 + Math.random()
  })
}
</script>

<style lang="scss" scoped>
.room-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding-bottom: var(--safe-bottom);
  background-color: #F4F1EA;
}

.room-top-bar {
  background-color: #FBFAF6;
  border-bottom: 1px solid #D8D2C4;
  padding: 18rpx 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.room-meta {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.room-pos {
  font-size: 28rpx;
  font-weight: 700;
  color: #131B23;
}

.room-tags {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.round-counter {
  font-size: 22rpx;
  color: #24506B;
  font-weight: 600;
}

.finish-btn {
  margin: 0;
}

.room-scroll {
  flex: 1;
  padding: 24rpx;
  box-sizing: border-box;
}

.room-notice {
  background-color: #EFEBE2;
  border-left: 6rpx solid #C4432B;
  border-radius: 4rpx;
  padding: 14rpx 18rpx;
  margin-bottom: 24rpx;
}

.notice-text {
  font-size: 22rpx;
  color: #56646F;
  line-height: 1.5;
}

/* 历史卡片 */
.history-card {
  margin-bottom: 18rpx;
  padding: 20rpx;
}

.history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.history-title-line {
  display: flex;
  align-items: center;
  flex: 1;
  overflow: hidden;
  margin-right: 12rpx;
}

.history-q-preview {
  font-size: 24rpx;
  color: #56646F;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.history-score-badge {
  font-size: 24rpx;
  font-weight: 700;
  color: #2A6B63;
  display: flex;
  align-items: center;
  gap: 6rpx;
}

.collapse-icon {
  font-size: 18rpx;
  color: #8A959E;
}

.history-body {
  margin-top: 16rpx;
  padding-top: 14rpx;
  border-top: 1px dashed #EAE5D9;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.section-label {
  font-size: 22rpx;
  font-weight: 600;
  color: #131B23;
  display: block;
  margin-bottom: 4rpx;
}

.history-answer {
  font-size: 24rpx;
  color: #2A3846;
  line-height: 1.6;
  white-space: pre-wrap;
}

.comment-section {
  background-color: #F4F1EA;
  border-radius: 6rpx;
  padding: 12rpx 16rpx;
}

.comment-label {
  color: #A9761B;
}

.history-comment {
  font-size: 22rpx;
  color: #56646F;
  line-height: 1.5;
}

/* 当前答题卡片 */
.active-card {
  border: 1px solid #C4432B;
  box-shadow: 0 4rpx 14rpx rgba(196, 67, 43, 0.08);
}

.question-box {
  background-color: #FFFFFF;
  border: 1px solid #EAE5D9;
  border-radius: 8rpx;
  padding: 20rpx;
  margin-bottom: 20rpx;
}

.answer-box {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.answer-textarea {
  width: 100%;
  min-height: 240rpx;
  background-color: #FFFFFF;
  border: 1px solid #D8D2C4;
  border-radius: 8rpx;
  padding: 20rpx;
  font-size: 28rpx;
  color: #131B23;
  line-height: 1.6;
}

.answer-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.char-count {
  font-size: 22rpx;
  color: #8A959E;
}

.submit-answer-btn {
  margin: 0;
}

/* 出题中动画卡片 */
.asking-card {
  background-color: #FBFAF6;
  border: 1px dashed #D8D2C4;
  border-radius: 8rpx;
  padding: 24rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-top: 18rpx;
}

.asking-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 8rpx;
  background-color: #C4432B;
  animation: pulse 0.8s infinite alternate;
}

.asking-text {
  font-size: 24rpx;
  color: #56646F;
}
</style>

<template>
  <view class="report-page">
    <view v-if="loading" class="loading-state">
      <text class="loading-text">正在加载多维评测报告...</text>
    </view>

    <view v-else-if="error" class="rag-card">
      <EmptyState title="报告加载失败" :desc="error">
        <button class="btn-primary btn-sm" @tap="loadReport">重新加载</button>
      </EmptyState>
    </view>

    <scroll-view v-else scroll-y class="report-scroll">
      <!-- 顶部得分英雄区 -->
      <view class="rag-card score-hero-card">
        <view class="hero-top">
          <text class="hero-eyebrow">EVALUATION REPORT</text>
          <text class="hero-title">{{ session?.position || 'Java工程师' }} 模拟面试报告</text>
          <text class="hero-sub font-mono">{{ session?.createTime }} · 难度: {{ session?.level || '中级' }}</text>
        </view>

        <!-- 环形总分与等级 -->
        <view class="score-circle-wrapper">
          <view class="score-circle" :style="{ borderColor: getScoreColor(report?.totalScore) }">
            <text class="score-num font-mono" :style="{ color: getScoreColor(report?.totalScore) }">
              {{ report?.totalScore ?? 0 }}
            </text>
            <text class="score-unit">综合得分</text>
          </view>
          <view class="grade-badge" :style="{ backgroundColor: getScoreColor(report?.totalScore) }">
            <text class="grade-text">{{ report?.grade || 'B' }} 级评定</text>
          </view>
        </view>
      </view>

      <!-- 能力维度雷达图卡片 -->
      <view v-if="dims.length" class="rag-card">
        <view class="rag-card-head">
          <view class="rag-card-title">
            <text class="num-badge">01</text>
            <text>能力维度评估</text>
          </view>
        </view>

        <!-- 纯 SVG 动态多边形雷达图 -->
        <view class="radar-container">
          <svg class="radar-svg" viewBox="0 0 280 280">
            <!-- 背景同心多边形圈 -->
            <polygon
              v-for="(ring, rIdx) in radarRings"
              :key="'ring-' + rIdx"
              :points="ring"
              fill="none"
              stroke="#D8D2C4"
              stroke-width="1"
              stroke-dasharray="2,2"
            />
            <!-- 轴线 -->
            <line
              v-for="(axis, aIdx) in radarAxes"
              :key="'axis-' + aIdx"
              :x1="140"
              :y1="140"
              :x2="axis.tipX"
              :y2="axis.tipY"
              stroke="#EAE5D9"
              stroke-width="1"
            />
            <!-- 得分多边形区域 -->
            <polygon
              :points="radarAreaPoints"
              fill="rgba(196, 67, 43, 0.25)"
              stroke="#C4432B"
              stroke-width="2"
            />
            <!-- 轴点与文字 -->
            <g v-for="(axis, aIdx) in radarAxes" :key="'point-' + aIdx">
              <circle :cx="axis.px" :cy="axis.py" r="3.5" fill="#C4432B" />
              <text
                :x="axis.labelX"
                :y="axis.labelY"
                font-size="10"
                fill="#2A3846"
                :text-anchor="axis.anchor"
                dominant-baseline="central"
              >
                {{ axis.name }} ({{ axis.value }})
              </text>
            </g>
          </svg>
        </view>
      </view>

      <!-- AI 综合点评 -->
      <view class="rag-card">
        <view class="rag-card-head">
          <view class="rag-card-title">
            <text class="num-badge">02</text>
            <text>面试官综合评价</text>
          </view>
        </view>
        <view class="summary-text-box">
          <MarkdownView :content="report?.summary || '表现良好，基础概念掌握扎实。'" />
        </view>
      </view>

      <!-- 优势与薄弱环节 -->
      <view class="rag-card">
        <view class="rag-card-head">
          <view class="rag-card-title">
            <text class="num-badge">03</text>
            <text>优势与薄弱环节诊断</text>
          </view>
        </view>

        <view class="analysis-box">
          <view class="analysis-item strength-item">
            <view class="item-head">
              <text class="item-tag tag-teal">✓ 核心优势</text>
            </view>
            <text class="item-content">{{ report?.strengths || '回答条理清晰，具备较好的技术归纳与总结能力。' }}</text>
          </view>

          <view class="analysis-item weakness-item">
            <view class="item-head">
              <text class="item-tag tag-amber">⚠ 薄弱短板</text>
            </view>
            <text class="item-content">{{ report?.weaknesses || '在并发死锁排查与底层内存结构细节上仍有提升空间。' }}</text>
          </view>
        </view>
      </view>

      <!-- 后续复习与提升建议 -->
      <view v-if="report?.suggestion" class="rag-card">
        <view class="rag-card-head">
          <view class="rag-card-title">
            <text class="num-badge">04</text>
            <text>针对性复习建议</text>
          </view>
        </view>
        <view class="summary-text-box">
          <MarkdownView :content="report.suggestion" />
        </view>
      </view>

      <!-- 逐题问答回顾 -->
      <view v-if="session?.qaList?.length" class="rag-card">
        <view class="rag-card-head">
          <view class="rag-card-title">
            <text class="num-badge">05</text>
            <text>逐题答卷明细 ({{ session.qaList.length }} 题)</text>
          </view>
        </view>

        <view class="qa-breakdown-list">
          <view
            v-for="qa in session.qaList"
            :key="qa.roundNo"
            class="qa-card"
          >
            <view class="qa-head">
              <text class="qa-round font-mono">第 {{ qa.roundNo }} 轮</text>
              <text class="qa-score font-mono" :style="{ color: getScoreColor(qa.score) }">
                {{ qa.score != null ? qa.score + '分' : '—' }}
              </text>
            </view>

            <view class="qa-block">
              <text class="qa-block-label">面试问题：</text>
              <text class="qa-q-text">{{ qa.question }}</text>
            </view>

            <view class="qa-block">
              <text class="qa-block-label">我的回答：</text>
              <text class="qa-a-text">{{ qa.answer || '未作答' }}</text>
            </view>

            <view v-if="qa.comment" class="qa-block comment-block">
              <text class="qa-block-label text-amber">考官点评：</text>
              <text class="qa-c-text">{{ qa.comment }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 底部按钮 -->
      <view class="report-actions">
        <button class="btn-primary" @tap="handleBack">
          返回模拟面试大厅
        </button>
      </view>
      <view style="height: 30px;"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getReport, getSessionDetail } from '@/api/interview'
import MarkdownView from '@/components/MarkdownView.vue'
import EmptyState from '@/components/EmptyState.vue'

const sessionId = ref(null)
const loading = ref(true)
const report = ref(null)
const session = ref(null)
const error = ref('')

onLoad((options) => {
  sessionId.value = Number(options.id)
})

onMounted(() => {
  loadReport()
})

async function loadReport() {
  if (!sessionId.value) return
  loading.value = true
  error.value = ''
  try {
    const [r, s] = await Promise.all([
      getReport(sessionId.value),
      getSessionDetail(sessionId.value).catch(() => null)
    ])
    report.value = r
    session.value = s
  } catch (e) {
    error.value = e?.message || '获取报告失败'
  } finally {
    loading.value = false
  }
}

function getScoreColor(score) {
  if (score == null) return '#8A959E'
  if (score >= 85) return '#2A6B63'
  if (score >= 70) return '#24506B'
  if (score >= 60) return '#A9761B'
  return '#C4432B'
}

/* 雷达图计算 */
const CENTER = 140
const MAX_RADIUS = 80

const dims = computed(() => {
  const obj = report.value?.dimScores || {}
  const entries = Object.entries(obj)
  if (!entries.length) {
    return [
      { name: '基础扎实度', value: 80 },
      { name: '代码规范', value: 75 },
      { name: '深度理解', value: 70 },
      { name: '逻辑思维', value: 85 },
      { name: '工程实践', value: 75 }
    ]
  }
  return entries.map(([name, val]) => ({ name, value: Number(val) }))
})

const radarAxes = computed(() => {
  const n = dims.value.length
  if (!n) return []
  return dims.value.map((d, i) => {
    const angle = (Math.PI * 2 * i) / n - Math.PI / 2
    const cos = Math.cos(angle)
    const sin = Math.sin(angle)
    return {
      ...d,
      tipX: CENTER + cos * MAX_RADIUS,
      tipY: CENTER + sin * MAX_RADIUS,
      labelX: CENTER + cos * (MAX_RADIUS + 24),
      labelY: CENTER + sin * (MAX_RADIUS + 24),
      anchor: cos > 0.35 ? 'start' : cos < -0.35 ? 'end' : 'middle',
      px: CENTER + cos * MAX_RADIUS * (d.value / 100),
      py: CENTER + sin * MAX_RADIUS * (d.value / 100)
    }
  })
})

const radarRings = computed(() => {
  const n = dims.value.length
  if (!n) return []
  return [0.25, 0.5, 0.75, 1.0].map((scale) =>
    Array.from({ length: n }, (_, i) => {
      const angle = (Math.PI * 2 * i) / n - Math.PI / 2
      return `${CENTER + Math.cos(angle) * MAX_RADIUS * scale},${CENTER + Math.sin(angle) * MAX_RADIUS * scale}`
    }).join(' ')
  )
})

const radarAreaPoints = computed(() => {
  return radarAxes.value.map((a) => `${a.px},${a.py}`).join(' ')
})

function handleBack() {
  uni.switchTab({
    url: '/pages/interview/interview'
  })
}
</script>

<style lang="scss" scoped>
.report-page {
  min-height: 100vh;
  padding: 24rpx;
  padding-bottom: calc(24rpx + var(--safe-bottom));
  background-color: #F4F1EA;
}

.report-scroll {
  height: calc(100vh - var(--safe-top) - var(--safe-bottom));
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 100rpx 0;
}

.loading-text {
  font-size: 26rpx;
  color: #8A959E;
}

/* 英雄得分区 */
.score-hero-card {
  text-align: center;
  padding: 36rpx 24rpx;
}

.hero-eyebrow {
  font-size: 20rpx;
  color: #C4432B;
  font-weight: 700;
  letter-spacing: 1px;
}

.hero-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #131B23;
  margin: 6rpx 0;
  display: block;
}

.hero-sub {
  font-size: 22rpx;
  color: #8A959E;
  display: block;
  margin-bottom: 24rpx;
}

.score-circle-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 10rpx;
}

.score-circle {
  width: 170rpx;
  height: 170rpx;
  border-radius: 85rpx;
  border: 8rpx solid #C4432B;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #FFFFFF;
  box-shadow: 0 4rpx 12rpx rgba(19, 27, 35, 0.06);
}

.score-num {
  font-size: 54rpx;
  font-weight: 800;
  line-height: 1;
}

.score-unit {
  font-size: 20rpx;
  color: #8A959E;
  margin-top: 6rpx;
}

.grade-badge {
  margin-top: 18rpx;
  padding: 6rpx 20rpx;
  border-radius: 30rpx;
}

.grade-text {
  color: #FFFFFF;
  font-size: 24rpx;
  font-weight: 700;
}

/* 雷达图容器 */
.radar-container {
  display: flex;
  justify-content: center;
  padding: 10rpx 0;
}

.radar-svg {
  width: 560rpx;
  height: 560rpx;
}

.summary-text-box {
  line-height: 1.7;
}

/* 优势与短板 */
.analysis-box {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.analysis-item {
  border-radius: 6rpx;
  padding: 16rpx 20rpx;
}

.strength-item {
  background-color: #DFEDEA;
  border: 1px solid #B5D7D1;
}

.weakness-item {
  background-color: #F5EAD4;
  border: 1px solid #E5D3AD;
}

.item-head {
  margin-bottom: 8rpx;
}

.item-content {
  font-size: 24rpx;
  color: #2A3846;
  line-height: 1.6;
}

/* 逐题问答回顾 */
.qa-breakdown-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.qa-card {
  background-color: #FFFFFF;
  border: 1px solid #EAE5D9;
  border-radius: 8rpx;
  padding: 18rpx;
}

.qa-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12rpx;
  padding-bottom: 8rpx;
  border-bottom: 1px dashed #EAE5D9;
}

.qa-round {
  font-size: 24rpx;
  font-weight: 700;
  color: #131B23;
}

.qa-score {
  font-size: 24rpx;
  font-weight: 700;
}

.qa-block {
  margin-bottom: 10rpx;
}

.qa-block-label {
  font-size: 22rpx;
  font-weight: 600;
  color: #56646F;
  display: block;
  margin-bottom: 2rpx;
}

.qa-q-text {
  font-size: 24rpx;
  font-weight: 600;
  color: #131B23;
}

.qa-a-text {
  font-size: 24rpx;
  color: #2A3846;
  line-height: 1.5;
}

.comment-block {
  background-color: #F4F1EA;
  padding: 10rpx 14rpx;
  border-radius: 4rpx;
}

.qa-c-text {
  font-size: 22rpx;
  color: #56646F;
  line-height: 1.5;
}

.report-actions {
  margin-top: 30rpx;
}
</style>

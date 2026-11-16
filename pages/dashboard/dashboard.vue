<template>
  <view class="page-container">
    <!-- 顶部概览标头 -->
    <view class="dashboard-header">
      <view class="header-left">
        <text class="header-eyebrow">OVERVIEW</text>
        <text class="header-title">系统数据总览</text>
      </view>
      <view class="header-right">
        <button class="btn-ghost btn-sm refresh-btn" :loading="loading" @tap="loadStats">
          刷新
        </button>
      </view>
    </view>

    <!-- 4 个核心指标卡片 -->
    <view class="stat-grid">
      <!-- 知识文档 -->
      <view class="stat-card">
        <view class="stat-top">
          <text class="stat-label">知识文档</text>
          <text class="stat-unit">篇</text>
        </view>
        <text class="stat-number">{{ stats?.docTotal ?? 0 }}</text>
        <text class="stat-sub">切片: {{ stats?.chunkTotal ?? 0 }} · 处理中: {{ stats?.parsing ?? 0 }}</text>
      </view>

      <!-- 题库题量 -->
      <view class="stat-card">
        <view class="stat-top">
          <text class="stat-label">题库题量</text>
          <text class="stat-unit">题</text>
        </view>
        <text class="stat-number">{{ stats?.questionTotal ?? 0 }}</text>
        <text class="stat-sub">覆盖基础到分布式</text>
      </view>

      <!-- 面试场次 -->
      <view class="stat-card">
        <view class="stat-top">
          <text class="stat-label">面试场次</text>
          <text class="stat-unit">场</text>
        </view>
        <text class="stat-number">{{ stats?.interviewTotal ?? 0 }}</text>
        <text class="stat-sub">动态追问与评测</text>
      </view>

      <!-- 面试均分 -->
      <view class="stat-card stat-accent">
        <view class="stat-top">
          <text class="stat-label">面试均分</text>
          <text class="stat-unit">分</text>
        </view>
        <text class="stat-number" :style="{ color: getScoreColor(stats?.avgScore) }">
          {{ stats?.avgScore ?? '—' }}
        </text>
        <text class="stat-sub">综合多维评测指标</text>
      </view>
    </view>

    <!-- 快捷功能入场 -->
    <view class="rag-card quick-actions">
      <view class="rag-card-head">
        <view class="rag-card-title">
          <text class="num-badge">01</text>
          <text>核心功能快捷直达</text>
        </view>
      </view>
      <view class="action-grid">
        <view class="action-item" @tap="goToTab('/pages/interview/interview')">
          <view class="action-icon action-interview">
            <text class="action-icon-text">🎯</text>
          </view>
          <text class="action-title">模拟面试</text>
          <text class="action-desc">AI 动态出题与追问</text>
        </view>
        <view class="action-item" @tap="goToTab('/pages/chat/chat')">
          <view class="action-icon action-chat">
            <text class="action-icon-text">💬</text>
          </view>
          <text class="action-title">智能问答</text>
          <text class="action-desc">知识库检索增强</text>
        </view>
        <view class="action-item" @tap="goToTab('/pages/question/question')">
          <view class="action-icon action-question">
            <text class="action-icon-text">📚</text>
          </view>
          <text class="action-title">题库训练</text>
          <text class="action-desc">随机刷题自测</text>
        </view>
        <view class="action-item" @tap="goToKnowledge">
          <view class="action-icon action-knowledge">
            <text class="action-icon-text">📁</text>
          </view>
          <text class="action-title">文档知识库</text>
          <text class="action-desc">切片解析与向量</text>
        </view>
      </view>
    </view>

    <!-- 知识分类分布柱状条 -->
    <view class="rag-card">
      <view class="rag-card-head">
        <view class="rag-card-title">
          <text class="num-badge">02</text>
          <text>知识分类分布</text>
        </view>
        <text class="card-link" @tap="goToKnowledge">查看知识库 →</text>
      </view>

      <view v-if="!stats?.docByCategory?.length" class="empty-hint">
        <text>暂无分类数据，上传文档后自动统计</text>
      </view>
      <view v-else class="bars-container">
        <view
          v-for="item in stats.docByCategory"
          :key="item.name"
          class="bar-row"
        >
          <text class="bar-name">{{ item.name }}</text>
          <view class="bar-track">
            <view
              class="bar-fill"
              :style="{ width: getBarWidth(item.value) }"
            ></view>
          </view>
          <text class="bar-value font-mono">{{ item.value }}</text>
        </view>
      </view>
    </view>

    <!-- 文档解析状态条 -->
    <view class="rag-card">
      <view class="rag-card-head">
        <view class="rag-card-title">
          <text class="num-badge">03</text>
          <text>文档解析状态</text>
        </view>
      </view>

      <view class="status-seg-bar">
        <view
          v-for="(item, idx) in stats?.docByStatus || []"
          :key="item.name"
          class="seg-block"
          :class="'seg-' + idx"
          :style="{ width: getStatusWidth(item.value) }"
        ></view>
      </view>

      <view class="status-legend">
        <view
          v-for="(item, idx) in stats?.docByStatus || []"
          :key="item.name"
          class="legend-item"
        >
          <view class="legend-dot" :class="'seg-' + idx"></view>
          <text class="legend-name">{{ item.name }}</text>
          <text class="legend-count font-mono">{{ item.value }}</text>
        </view>
      </view>
    </view>

    <!-- 最近模拟面试 -->
    <view class="rag-card">
      <view class="rag-card-head">
        <view class="rag-card-title">
          <text class="num-badge">04</text>
          <text>最近模拟面试</text>
        </view>
        <text class="card-link" @tap="goToTab('/pages/interview/interview')">全部记录 →</text>
      </view>

      <view v-if="!stats?.recentInterviews?.length" class="empty-hint">
        <text>暂无面试记录，发起一场模拟面试即可生成多维评测报告</text>
      </view>
      <view v-else class="recent-list">
        <view
          v-for="it in stats.recentInterviews"
          :key="it.id"
          class="recent-item"
          @tap="handleInterviewClick(it)"
        >
          <view class="recent-left">
            <view class="recent-title-line">
              <text class="recent-position">{{ it.position || 'Java工程师' }}</text>
              <text class="tag tag-slate">{{ it.level || '中级' }}</text>
            </view>
            <text class="recent-date font-mono">{{ it.createTime }}</text>
          </view>
          <view class="recent-right">
            <template v-if="it.status === 1">
              <text class="recent-score font-mono" :style="{ color: getScoreColor(it.totalScore) }">
                {{ it.totalScore ?? '—' }}分
              </text>
              <text class="tag tag-teal">已结束</text>
            </template>
            <template v-else>
              <text class="tag tag-amber">进行中</text>
            </template>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import { getDashboardStats } from '@/api/dashboard'
import { toastError } from '@/utils/toast'

const loading = ref(false)
const stats = ref(null)

onMounted(() => {
  loadStats()
})

onPullDownRefresh(async () => {
  await loadStats()
  uni.stopPullDownRefresh()
})

async function loadStats() {
  loading.value = true
  try {
    stats.value = await getDashboardStats()
  } catch (e) {
    console.error('加载总览数据失败', e)
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

const maxCategoryVal = computed(() => {
  if (!stats.value?.docByCategory?.length) return 1
  return Math.max(1, ...stats.value.docByCategory.map((i) => i.value))
})

function getBarWidth(value) {
  return `${Math.max(5, (value / maxCategoryVal.value) * 100)}%`
}

const totalStatusVal = computed(() => {
  if (!stats.value?.docByStatus?.length) return 1
  return stats.value.docByStatus.reduce((acc, cur) => acc + cur.value, 0) || 1
})

function getStatusWidth(value) {
  return `${(value / totalStatusVal.value) * 100}%`
}

function goToTab(url) {
  uni.switchTab({ url })
}

function goToKnowledge() {
  uni.navigateTo({
    url: '/pages/knowledge/knowledge'
  })
}

function handleInterviewClick(it) {
  if (it.status === 1) {
    uni.navigateTo({
      url: `/pages/interview/report?id=${it.id}`
    })
  } else {
    uni.navigateTo({
      url: `/pages/interview/room?id=${it.id}`
    })
  }
}
</script>

<style lang="scss" scoped>
.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.header-eyebrow {
  font-size: 20rpx;
  color: #C4432B;
  font-weight: 700;
  letter-spacing: 1px;
  display: block;
}

.header-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #131B23;
  margin-top: 4rpx;
  display: block;
}

.refresh-btn {
  margin: 0;
}

/* 4 格指标卡片 */
.stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
  margin-bottom: 24rpx;
}

.stat-card {
  background-color: #FBFAF6;
  border: 1px solid #D8D2C4;
  border-radius: 8rpx;
  padding: 20rpx 22rpx;
  box-shadow: 0 2rpx 6rpx rgba(19, 27, 35, 0.03);
}

.stat-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-label {
  font-size: 24rpx;
  color: #56646F;
}

.stat-unit {
  font-size: 20rpx;
  color: #8A959E;
}

.stat-number {
  font-size: 44rpx;
  font-weight: 700;
  color: #131B23;
  font-family: Consolas, monospace;
  margin: 10rpx 0 6rpx;
  display: block;
}

.stat-sub {
  font-size: 20rpx;
  color: #8A959E;
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 快捷功能 */
.action-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
}

.action-item {
  background-color: #FFFFFF;
  border: 1px solid #EAE5D9;
  border-radius: 8rpx;
  padding: 22rpx 18rpx;
  display: flex;
  flex-direction: column;
}

.action-item:active {
  background-color: #EFEBE2;
}

.action-icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12rpx;
}

.action-interview { background-color: #F6E4DF; }
.action-chat { background-color: #DEE8EF; }
.action-question { background-color: #F5EAD4; }
.action-knowledge { background-color: #DFEDEA; }

.action-icon-text {
  font-size: 28rpx;
}

.action-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #131B23;
}

.action-desc {
  font-size: 20rpx;
  color: #8A959E;
  margin-top: 4rpx;
}

/* 分类柱状条 */
.card-link {
  font-size: 24rpx;
  color: #C4432B;
}

.bars-container {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.bar-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.bar-name {
  width: 140rpx;
  font-size: 24rpx;
  color: #2A3846;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bar-track {
  flex: 1;
  height: 16rpx;
  background-color: #EFEBE2;
  border-radius: 8rpx;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background-color: #24506B;
  border-radius: 8rpx;
}

.bar-value {
  width: 50rpx;
  text-align: right;
  font-size: 22rpx;
  color: #56646F;
}

/* 状态条 */
.status-seg-bar {
  height: 20rpx;
  border-radius: 10rpx;
  display: flex;
  overflow: hidden;
  background-color: #EAE5D9;
  margin-bottom: 18rpx;
}

.seg-block {
  height: 100%;
}

.seg-0 { background-color: #5D6B77; }
.seg-1 { background-color: #A9761B; }
.seg-2 { background-color: #2A6B63; }
.seg-3 { background-color: #C4432B; }

.status-legend {
  display: flex;
  justify-content: space-around;
  flex-wrap: wrap;
  gap: 12rpx;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 22rpx;
  color: #56646F;
}

.legend-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 7rpx;
}

.legend-count {
  font-weight: 600;
  color: #131B23;
}

/* 最近面试 */
.recent-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.recent-item {
  background-color: #FFFFFF;
  border: 1px solid #EAE5D9;
  border-radius: 8rpx;
  padding: 18rpx 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.recent-item:active {
  background-color: #EFEBE2;
}

.recent-left {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.recent-title-line {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.recent-position {
  font-size: 26rpx;
  font-weight: 600;
  color: #131B23;
}

.recent-date {
  font-size: 20rpx;
  color: #8A959E;
}

.recent-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6rpx;
}

.recent-score {
  font-size: 30rpx;
  font-weight: 700;
}

.empty-hint {
  padding: 30rpx 0;
  text-align: center;
  font-size: 24rpx;
  color: #8A959E;
}
</style>

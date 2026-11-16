<template>
  <view class="page-container">
    <!-- 顶部标题与新建入口 -->
    <view class="interview-header">
      <view>
        <text class="header-eyebrow">MOCK INTERVIEW</text>
        <text class="header-title">模拟面试大厅</text>
      </view>
      <button class="btn-primary btn-sm new-btn" @tap="setupModalOpen = true">
        + 发起新面试
      </button>
    </view>

    <!-- 状态筛选 Tab -->
    <view class="filter-tabs">
      <view
        class="filter-tab"
        :class="{ active: filterStatus === '' }"
        @tap="switchFilter('')"
      >
        <text>全部 ({{ total }})</text>
      </view>
      <view
        class="filter-tab"
        :class="{ active: filterStatus === 0 }"
        @tap="switchFilter(0)"
      >
        <text>进行中</text>
      </view>
      <view
        class="filter-tab"
        :class="{ active: filterStatus === 1 }"
        @tap="switchFilter(1)"
      >
        <text>已结束</text>
      </view>
    </view>

    <!-- 面试列表 -->
    <view v-if="loading && !list.length" class="skeleton-list">
      <view v-for="i in 3" :key="i" class="rag-card skeleton-card">
        <view class="skeleton-line" style="width: 50%; height: 14px;"></view>
        <view class="skeleton-line" style="width: 70%; height: 10px; margin-top: 8px;"></view>
      </view>
    </view>

    <view v-else-if="!list.length" class="rag-card">
      <EmptyState
        title="暂无面试记录"
        desc="选择心仪岗位与考察方向，AI 面试官将依据您的回答动态追问并生成多维评估报告"
      >
        <button class="btn-primary btn-sm" @tap="setupModalOpen = true">
          立即开启模拟面试
        </button>
      </EmptyState>
    </view>

    <view v-else class="session-list">
      <view
        v-for="item in list"
        :key="item.id"
        class="rag-card session-card"
        @tap="handleClickSession(item)"
      >
        <view class="card-top-row">
          <view class="position-info">
            <text class="session-position">{{ item.position || 'Java 开发工程师' }}</text>
            <text class="tag tag-slate">{{ item.level || '中级' }}</text>
            <text v-if="item.category" class="tag tag-indigo">{{ item.category }}</text>
          </view>
          <text v-if="item.status === 1" class="tag tag-teal">已结束</text>
          <text v-else class="tag tag-amber">答题中</text>
        </view>

        <view class="card-mid-row">
          <text class="session-time font-mono">{{ item.createTime }}</text>
          <text class="session-rounds">计划 {{ item.roundTotal || 5 }} 轮考察</text>
        </view>

        <view class="card-bot-row">
          <view class="score-box">
            <template v-if="item.status === 1">
              <text class="score-label">总评分：</text>
              <text class="score-val font-mono" :style="{ color: getScoreColor(item.totalScore) }">
                {{ item.totalScore ?? '—' }} 分
              </text>
            </template>
            <template v-else>
              <text class="score-hint">面试进行中，点击进入考场作答</text>
            </template>
          </view>

          <view class="actions-box" @tap.stop>
            <button
              v-if="item.status === 1"
              class="btn-secondary btn-sm"
              @tap="goToReport(item.id)"
            >
              查看报告
            </button>
            <button
              v-else
              class="btn-primary btn-sm"
              @tap="goToRoom(item.id)"
            >
              继续答题
            </button>
            <text class="delete-btn" @tap="handleDelete(item.id)">删除</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 新建面试配置弹窗 -->
    <AppModal
      v-model:visible="setupModalOpen"
      title="发起模拟面试"
      confirmText="开始答题"
      :loading="creating"
      @confirm="handleCreateSession"
    >
      <view class="modal-form">
        <!-- 目标岗位 -->
        <view class="form-group">
          <text class="form-label">目标岗位</text>
          <picker
            :value="positionIdx"
            :range="POSITIONS"
            @change="(e) => positionIdx = e.detail.value"
          >
            <view class="picker-display">
              <text>{{ POSITIONS[positionIdx] }}</text>
              <text class="picker-arrow">▼</text>
            </view>
          </picker>
        </view>

        <!-- 难度等级 -->
        <view class="form-group">
          <text class="form-label">难度等级</text>
          <view class="chip-options">
            <view
              v-for="lv in LEVELS"
              :key="lv"
              class="chip-option"
              :class="{ active: form.level === lv }"
              @tap="form.level = lv"
            >
              <text>{{ lv }}</text>
            </view>
          </view>
        </view>

        <!-- 考察方向 -->
        <view class="form-group">
          <text class="form-label">考察方向（可选）</text>
          <view class="chip-options chip-wrap">
            <view
              class="chip-option"
              :class="{ active: form.category === '' }"
              @tap="form.category = ''"
            >
              <text>全科综合</text>
            </view>
            <view
              v-for="cat in CATEGORIES"
              :key="cat"
              class="chip-option"
              :class="{ active: form.category === cat }"
              @tap="form.category = cat"
            >
              <text>{{ cat }}</text>
            </view>
          </view>
        </view>

        <!-- 提问轮数 -->
        <view class="form-group">
          <text class="form-label">提问轮数</text>
          <view class="chip-options">
            <view
              v-for="r in [3, 5, 8]"
              :key="r"
              class="chip-option"
              :class="{ active: form.roundTotal === r }"
              @tap="form.roundTotal = r"
            >
              <text>{{ r }} 轮 {{ r === 3 ? '(速测)' : r === 5 ? '(标准)' : '(深度)' }}</text>
            </view>
          </view>
        </view>
      </view>
    </AppModal>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import { getSessionList, createSession, deleteSession } from '@/api/interview'
import { toastSuccess, toastError } from '@/utils/toast'
import EmptyState from '@/components/EmptyState.vue'
import AppModal from '@/components/AppModal.vue'

const POSITIONS = [
  'Java 后端开发工程师',
  'Java 高级开发工程师',
  '后端开发实习生',
  '大数据开发工程师',
  '全栈开发工程师'
]
const LEVELS = ['初级', '中级', '高级']
const CATEGORIES = ['Java基础', '并发编程', 'JVM', 'Spring', 'MySQL', 'Redis', '微服务/分布式']

const loading = ref(false)
const list = ref([])
const total = ref(0)
const filterStatus = ref('')

const setupModalOpen = ref(false)
const creating = ref(false)
const positionIdx = ref(0)

const form = reactive({
  level: '中级',
  category: '',
  roundTotal: 5
})

onMounted(() => {
  loadList()
})

onPullDownRefresh(async () => {
  await loadList()
  uni.stopPullDownRefresh()
})

function switchFilter(status) {
  filterStatus.value = status
  loadList()
}

async function loadList() {
  loading.value = true
  try {
    const params = {
      pageNum: 1,
      pageSize: 20
    }
    if (filterStatus.value !== '') {
      params.status = filterStatus.value
    }
    const res = await getSessionList(params)
    list.value = res?.list || res || []
    total.value = res?.total || list.value.length
  } catch (e) {
    list.value = []
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

function handleClickSession(item) {
  if (item.status === 1) {
    goToReport(item.id)
  } else {
    goToRoom(item.id)
  }
}

function goToRoom(id) {
  uni.navigateTo({
    url: `/pages/interview/room?id=${id}`
  })
}

function goToReport(id) {
  uni.navigateTo({
    url: `/pages/interview/report?id=${id}`
  })
}

async function handleCreateSession() {
  creating.value = true
  try {
    const res = await createSession({
      position: POSITIONS[positionIdx.value],
      level: form.level,
      category: form.category || undefined,
      roundTotal: form.roundTotal
    })
    setupModalOpen.value = false
    toastSuccess('面试已创建，进入考场')
    if (res && res.id) {
      setTimeout(() => {
        goToRoom(res.id)
      }, 300)
    }
    loadList()
  } catch (e) {
    toastError('创建失败，请重试')
  } finally {
    creating.value = false
  }
}

function handleDelete(id) {
  uni.showModal({
    title: '确认删除',
    content: '删除后无法恢复该场面试记录与评分，确定删除？',
    confirmColor: '#C4432B',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteSession(id)
          toastSuccess('已删除')
          loadList()
        } catch (e) {
          toastError('删除失败')
        }
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.interview-header {
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

.new-btn {
  margin: 0;
}

/* 筛选 Tab */
.filter-tabs {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.filter-tab {
  background-color: #FBFAF6;
  border: 1px solid #D8D2C4;
  padding: 10rpx 24rpx;
  border-radius: 6rpx;
  font-size: 24rpx;
  color: #56646F;
}

.filter-tab.active {
  background-color: #131B23;
  border-color: #131B23;
  color: #FBFAF6;
  font-weight: 600;
}

/* 面试记录卡片 */
.session-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.session-card {
  margin-bottom: 0;
}

.card-top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.position-info {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-wrap: wrap;
}

.session-position {
  font-size: 28rpx;
  font-weight: 600;
  color: #131B23;
}

.card-mid-row {
  display: flex;
  justify-content: space-between;
  font-size: 22rpx;
  color: #8A959E;
  margin-bottom: 18rpx;
  padding-bottom: 14rpx;
  border-bottom: 1px dashed #EAE5D9;
}

.card-bot-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.score-box {
  display: flex;
  align-items: baseline;
}

.score-label {
  font-size: 24rpx;
  color: #56646F;
}

.score-val {
  font-size: 34rpx;
  font-weight: 700;
}

.score-hint {
  font-size: 22rpx;
  color: #A9761B;
}

.actions-box {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.delete-btn {
  font-size: 22rpx;
  color: #8A959E;
  padding: 8rpx;
}

/* 弹窗表单 */
.modal-form {
  display: flex;
  flex-direction: column;
  gap: 22rpx;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.form-label {
  font-size: 24rpx;
  font-weight: 600;
  color: #131B23;
}

.picker-display {
  background-color: #FFFFFF;
  border: 1px solid #D8D2C4;
  border-radius: 6rpx;
  padding: 16rpx 20rpx;
  font-size: 26rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #131B23;
}

.picker-arrow {
  font-size: 20rpx;
  color: #8A959E;
}

.chip-options {
  display: flex;
  gap: 12rpx;
}

.chip-wrap {
  flex-wrap: wrap;
}

.chip-option {
  background-color: #FFFFFF;
  border: 1px solid #D8D2C4;
  border-radius: 6rpx;
  padding: 10rpx 20rpx;
  font-size: 24rpx;
  color: #56646F;
}

.chip-option.active {
  background-color: #131B23;
  border-color: #131B23;
  color: #FBFAF6;
  font-weight: 600;
}
</style>

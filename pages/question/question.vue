<template>
  <view class="page-container">
    <!-- 顶部搜索与操作栏 -->
    <view class="search-top-bar">
      <view class="search-box">
        <text class="search-icon">🔍</text>
        <input
          v-model="query.keyword"
          class="search-input"
          placeholder="搜索题目关键字 (如 HashMap、死锁)..."
          placeholder-class="placeholder-style"
          confirm-type="search"
          @keyup.enter="handleSearch"
        />
        <text v-if="query.keyword" class="search-clear" @tap="clearSearch">✕</text>
      </view>

      <view class="top-action-btns">
        <button class="btn-primary btn-sm quiz-btn" @tap="openQuizMode">
          🎲 随机自测
        </button>
        <button class="btn-secondary btn-sm add-btn" @tap="openAddModal">
          + 录入
        </button>
      </view>
    </view>

    <!-- 分类横向滑动标签栏 -->
    <scroll-view scroll-x class="category-scroll" :show-scrollbar="false">
      <view class="category-pills">
        <view
          class="category-pill"
          :class="{ active: query.category === '' }"
          @tap="selectCategory('')"
        >
          <text>全部分类</text>
        </view>
        <view
          v-for="cat in categories"
          :key="cat"
          class="category-pill"
          :class="{ active: query.category === cat }"
          @tap="selectCategory(cat)"
        >
          <text>{{ cat }}</text>
        </view>
      </view>
    </scroll-view>

    <!-- 难度筛选 Tabs -->
    <view class="difficulty-tabs">
      <view
        class="diff-tab"
        :class="{ active: query.difficulty === '' }"
        @tap="selectDifficulty('')"
      >
        <text>全部难度</text>
      </view>
      <view
        class="diff-tab"
        :class="{ active: query.difficulty === 1 }"
        @tap="selectDifficulty(1)"
      >
        <text>🟢 简单</text>
      </view>
      <view
        class="diff-tab"
        :class="{ active: query.difficulty === 2 }"
        @tap="selectDifficulty(2)"
      >
        <text>🟡 中等</text>
      </view>
      <view
        class="diff-tab"
        :class="{ active: query.difficulty === 3 }"
        @tap="selectDifficulty(3)"
      >
        <text>🔴 困难</text>
      </view>
    </view>

    <!-- 题库列表 -->
    <view v-if="loading && !list.length" class="skeleton-list">
      <view v-for="i in 4" :key="i" class="rag-card skeleton-card">
        <view class="skeleton-line" style="width: 80%; height: 13px;"></view>
        <view class="skeleton-line" style="width: 40%; height: 9px; margin-top: 7px;"></view>
      </view>
    </view>

    <view v-else-if="!list.length" class="rag-card">
      <EmptyState title="未找到相关题目" desc="可调整分类或关键词搜索，也可从已解析知识库中一键提取题目">
        <button class="btn-primary btn-sm" @tap="clearAllFilters">重置筛选条件</button>
      </EmptyState>
    </view>

    <view v-else class="question-list">
      <view
        v-for="item in list"
        :key="item.id"
        class="rag-card q-card"
        @tap="openDetail(item)"
      >
        <view class="q-title-row">
          <text class="q-title">{{ item.title }}</text>
        </view>

        <view class="q-meta-row">
          <view class="q-tags">
            <text class="tag tag-slate">{{ item.category || '综合' }}</text>
            <text class="tag" :class="getDiffClass(item.difficulty)">
              {{ getDiffText(item.difficulty) }}
            </text>
          </view>
          <text class="q-freq font-mono">热度: {{ item.frequency || 1 }}</text>
        </view>
      </view>
    </view>

    <!-- 题目详情与答案解析弹窗 -->
    <AppModal
      v-model:visible="detailModalOpen"
      title="题目详情与参考答案"
      confirmText="关闭"
      :showCancel="false"
      @confirm="detailModalOpen = false"
    >
      <view v-if="currentDetail" class="detail-body">
        <view class="detail-meta-pills">
          <text class="tag tag-slate">{{ currentDetail.category }}</text>
          <text class="tag" :class="getDiffClass(currentDetail.difficulty)">
            {{ getDiffText(currentDetail.difficulty) }}
          </text>
        </view>

        <view class="detail-question">
          <text class="detail-label">题目：</text>
          <text class="detail-q-title">{{ currentDetail.title }}</text>
        </view>

        <view class="detail-answer-box">
          <text class="detail-label">参考答案与核心要点：</text>
          <MarkdownView :content="currentDetail.answer || '暂无详细参考答案'" />
        </view>
      </view>
    </AppModal>

    <!-- 随机刷题自测模式弹窗 -->
    <AppModal
      v-model:visible="quizModalOpen"
      title="随机刷题自测"
      confirmText="下一题"
      cancelText="结束自测"
      @confirm="nextQuizQuestion"
      @cancel="quizModalOpen = false"
    >
      <view v-if="quizLoading" class="quiz-loading">
        <text>正在从题库随机抽取题目...</text>
      </view>
      <view v-else-if="quizCurrent" class="quiz-content">
        <view class="quiz-head">
          <text class="quiz-progress font-mono">
            进度: 第 {{ quizIndex + 1 }} / {{ quizList.length }} 题
          </text>
          <text class="tag" :class="getDiffClass(quizCurrent.difficulty)">
            {{ getDiffText(quizCurrent.difficulty) }}
          </text>
        </view>

        <view class="quiz-q-box">
          <text class="quiz-q-title">{{ quizCurrent.title }}</text>
        </view>

        <!-- 查看答案折叠 -->
        <view class="quiz-reveal-section">
          <button
            v-if="!quizReveal"
            class="btn-secondary btn-sm reveal-btn"
            @tap="quizReveal = true"
          >
            💡 点击查看参考答案
          </button>
          <view v-else class="quiz-answer-display">
            <text class="quiz-ans-label">参考解析：</text>
            <MarkdownView :content="quizCurrent.answer" />
          </view>
        </view>
      </view>
      <view v-else class="quiz-empty">
        <text>未获取到自测题目</text>
      </view>
    </AppModal>

    <!-- 录入题目弹窗 -->
    <AppModal
      v-model:visible="addModalOpen"
      title="录入面试题"
      confirmText="提交入库"
      :loading="addLoading"
      @confirm="submitAddQuestion"
    >
      <view class="add-form">
        <view class="form-group">
          <text class="form-label">题目内容</text>
          <textarea
            v-model="addForm.title"
            class="add-input add-textarea"
            placeholder="请输入题目描述 (如 Spring Bean 生命周期)"
            placeholder-class="placeholder-style"
          />
        </view>

        <view class="form-group">
          <text class="form-label">技术分类</text>
          <picker
            :value="addCatIdx"
            :range="categories"
            @change="(e) => addCatIdx = e.detail.value"
          >
            <view class="picker-box">
              <text>{{ categories[addCatIdx] || '请选择分类' }}</text>
              <text class="picker-arrow">▼</text>
            </view>
          </picker>
        </view>

        <view class="form-group">
          <text class="form-label">难度级别</text>
          <view class="diff-options">
            <view
              v-for="d in [1, 2, 3]"
              :key="d"
              class="diff-opt"
              :class="{ active: addForm.difficulty === d }"
              @tap="addForm.difficulty = d"
            >
              <text>{{ getDiffText(d) }}</text>
            </view>
          </view>
        </view>

        <view class="form-group">
          <text class="form-label">参考答案 (支持 Markdown 与代码)</text>
          <textarea
            v-model="addForm.answer"
            class="add-input add-textarea"
            style="min-height: 100px;"
            placeholder="请输入详尽参考答案及源码分析..."
            placeholder-class="placeholder-style"
          />
        </view>
      </view>
    </AppModal>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import {
  getQuestionList,
  getQuestionDetail,
  addQuestion,
  getRandomQuestions
} from '@/api/question'
import { getCategories } from '@/api/knowledge'
import { toastSuccess, toastWarning, toastError } from '@/utils/toast'
import MarkdownView from '@/components/MarkdownView.vue'
import EmptyState from '@/components/EmptyState.vue'
import AppModal from '@/components/AppModal.vue'

const categories = ref([
  'Java基础',
  '并发编程',
  'JVM',
  'Spring',
  'MySQL',
  'Redis',
  '计算机网络',
  '微服务/分布式'
])

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 20,
  keyword: '',
  category: '',
  difficulty: ''
})

// 详情弹窗
const detailModalOpen = ref(false)
const currentDetail = ref(null)

// 随机自测
const quizModalOpen = ref(false)
const quizLoading = ref(false)
const quizList = ref([])
const quizIndex = ref(0)
const quizReveal = ref(false)
const quizCurrent = computed(() => quizList.value[quizIndex.value] || null)

// 新增题目
const addModalOpen = ref(false)
const addLoading = ref(false)
const addCatIdx = ref(0)
const addForm = reactive({
  title: '',
  category: '',
  difficulty: 2,
  answer: ''
})

onMounted(async () => {
  loadCategories()
  loadList()
})

onPullDownRefresh(async () => {
  await loadList()
  uni.stopPullDownRefresh()
})

async function loadCategories() {
  try {
    const res = await getCategories()
    if (res && res.length) {
      categories.value = res
    }
  } catch (e) {}
}

async function loadList() {
  loading.value = true
  try {
    const params = {
      pageNum: 1,
      pageSize: 30,
      keyword: query.keyword.trim() || undefined,
      category: query.category || undefined,
      difficulty: query.difficulty || undefined
    }
    const res = await getQuestionList(params)
    list.value = res?.list || res || []
    total.value = res?.total || list.value.length
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadList()
}

function clearSearch() {
  query.keyword = ''
  loadList()
}

function selectCategory(cat) {
  query.category = cat
  loadList()
}

function selectDifficulty(diff) {
  query.difficulty = diff
  loadList()
}

function clearAllFilters() {
  query.keyword = ''
  query.category = ''
  query.difficulty = ''
  loadList()
}

function getDiffText(d) {
  if (d === 1) return '简单'
  if (d === 2) return '中等'
  if (d === 3) return '困难'
  return '中等'
}

function getDiffClass(d) {
  if (d === 1) return 'tag-teal'
  if (d === 2) return 'tag-amber'
  if (d === 3) return 'tag-red'
  return 'tag-amber'
}

async function openDetail(item) {
  currentDetail.value = item
  detailModalOpen.value = true
  try {
    const full = await getQuestionDetail(item.id)
    if (full) currentDetail.value = full
  } catch (e) {}
}

// 随机刷题自测
async function openQuizMode() {
  quizModalOpen.value = true
  quizLoading.value = true
  quizReveal.value = false
  quizIndex.value = 0
  try {
    const res = await getRandomQuestions({
      category: query.category || undefined,
      count: 10
    })
    quizList.value = res || []
  } catch (e) {
    toastError('抽题失败')
  } finally {
    quizLoading.value = false
  }
}

function nextQuizQuestion() {
  if (quizIndex.value < quizList.value.length - 1) {
    quizIndex.value++
    quizReveal.value = false
  } else {
    toastSuccess('恭喜已完成本组自测！')
    quizModalOpen.value = false
  }
}

function openAddModal() {
  addForm.title = ''
  addForm.answer = ''
  addForm.difficulty = 2
  addCatIdx.value = 0
  addModalOpen.value = true
}

async function submitAddQuestion() {
  if (!addForm.title.trim()) {
    toastWarning('请输入题目描述')
    return
  }
  if (!addForm.answer.trim()) {
    toastWarning('请输入参考答案')
    return
  }

  addLoading.value = true
  try {
    await addQuestion({
      title: addForm.title.trim(),
      category: categories.value[addCatIdx.value] || 'Java基础',
      difficulty: addForm.difficulty,
      answer: addForm.answer.trim()
    })
    toastSuccess('题目已录入')
    addModalOpen.value = false
    loadList()
  } catch (e) {
    toastError('录入失败')
  } finally {
    addLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.search-top-bar {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.search-box {
  display: flex;
  align-items: center;
  background-color: #FFFFFF;
  border: 1px solid #D8D2C4;
  border-radius: 8rpx;
  padding: 12rpx 18rpx;
}

.search-icon {
  font-size: 26rpx;
  margin-right: 12rpx;
}

.search-input {
  flex: 1;
  font-size: 26rpx;
  color: #131B23;
}

.search-clear {
  color: #8A959E;
  font-size: 24rpx;
  padding: 6rpx;
}

.top-action-btns {
  display: flex;
  gap: 16rpx;
}

.quiz-btn, .add-btn {
  flex: 1;
  margin: 0;
  height: 68rpx;
}

/* 分类横向滑动 */
.category-scroll {
  white-space: nowrap;
  margin-bottom: 16rpx;
}

.category-pills {
  display: inline-flex;
  gap: 12rpx;
  padding-bottom: 8rpx;
}

.category-pill {
  background-color: #FBFAF6;
  border: 1px solid #D8D2C4;
  padding: 8rpx 20rpx;
  border-radius: 6rpx;
  font-size: 22rpx;
  color: #56646F;
}

.category-pill.active {
  background-color: #131B23;
  color: #FBFAF6;
  border-color: #131B23;
  font-weight: 600;
}

/* 难度筛选 Tabs */
.difficulty-tabs {
  display: flex;
  gap: 12rpx;
  margin-bottom: 20rpx;
}

.diff-tab {
  background-color: #FBFAF6;
  border: 1px solid #D8D2C4;
  padding: 6rpx 16rpx;
  border-radius: 4rpx;
  font-size: 22rpx;
  color: #56646F;
}

.diff-tab.active {
  background-color: #EFEBE2;
  border-color: #131B23;
  color: #131B23;
  font-weight: 600;
}

/* 题库卡片列表 */
.question-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.q-card {
  margin-bottom: 0;
  padding: 22rpx 24rpx;
}

.q-card:active {
  background-color: #EFEBE2;
}

.q-title-row {
  margin-bottom: 12rpx;
}

.q-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #131B23;
  line-height: 1.5;
}

.q-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.q-tags {
  display: flex;
  gap: 10rpx;
}

.q-freq {
  font-size: 22rpx;
  color: #8A959E;
}

/* 详情弹窗 */
.detail-body {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.detail-meta-pills {
  display: flex;
  gap: 10rpx;
}

.detail-label {
  font-size: 24rpx;
  font-weight: 600;
  color: #56646F;
  display: block;
  margin-bottom: 6rpx;
}

.detail-q-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #131B23;
  line-height: 1.5;
}

.detail-answer-box {
  background-color: #FFFFFF;
  border: 1px solid #EAE5D9;
  border-radius: 8rpx;
  padding: 20rpx;
  max-height: 50vh;
  overflow-y: auto;
}

/* 自测模式 */
.quiz-content {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.quiz-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.quiz-progress {
  font-size: 22rpx;
  color: #24506B;
  font-weight: 600;
}

.quiz-q-box {
  background-color: #FFFFFF;
  border: 1px solid #EAE5D9;
  border-radius: 8rpx;
  padding: 20rpx;
}

.quiz-q-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #131B23;
  line-height: 1.5;
}

.quiz-reveal-section {
  margin-top: 10rpx;
}

.reveal-btn {
  width: 100%;
}

.quiz-answer-display {
  background-color: #FFFFFF;
  border: 1px solid #D8D2C4;
  border-radius: 8rpx;
  padding: 20rpx;
  max-height: 45vh;
  overflow-y: auto;
}

.quiz-ans-label {
  font-size: 22rpx;
  color: #C4432B;
  font-weight: 600;
  display: block;
  margin-bottom: 8rpx;
}

/* 录入表单 */
.add-form {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.form-label {
  font-size: 24rpx;
  font-weight: 600;
  color: #131B23;
}

.add-input {
  background-color: #FFFFFF;
  border: 1px solid #D8D2C4;
  border-radius: 6rpx;
  padding: 16rpx 20rpx;
  font-size: 26rpx;
}

.add-textarea {
  min-height: 140rpx;
  width: 100%;
}

.picker-box {
  background-color: #FFFFFF;
  border: 1px solid #D8D2C4;
  border-radius: 6rpx;
  padding: 16rpx 20rpx;
  font-size: 26rpx;
  display: flex;
  justify-content: space-between;
}

.diff-options {
  display: flex;
  gap: 12rpx;
}

.diff-opt {
  flex: 1;
  text-align: center;
  background-color: #FFFFFF;
  border: 1px solid #D8D2C4;
  border-radius: 6rpx;
  padding: 12rpx 0;
  font-size: 24rpx;
  color: #56646F;
}

.diff-opt.active {
  background-color: #131B23;
  color: #FBFAF6;
  border-color: #131B23;
  font-weight: 600;
}
</style>

<template>
  <view class="page-container">
    <!-- 顶部标题与上传操作 -->
    <view class="knowledge-header">
      <view>
        <text class="header-eyebrow">KNOWLEDGE BASE</text>
        <text class="header-title">知识文档库</text>
      </view>
      <button class="btn-primary btn-sm upload-btn" @tap="uploadModalOpen = true">
        + 上传文档
      </button>
    </view>

    <!-- 知识库核心指标概览 -->
    <view class="rag-card stats-card">
      <view class="stats-row">
        <view class="stat-mini">
          <text class="stat-mini-val font-mono">{{ stats?.docTotal ?? 0 }}</text>
          <text class="stat-mini-label">文档总数</text>
        </view>
        <view class="stat-mini">
          <text class="stat-mini-val font-mono">{{ stats?.chunkTotal ?? 0 }}</text>
          <text class="stat-mini-label">知识切片</text>
        </view>
        <view class="stat-mini">
          <text class="stat-mini-val font-mono" :style="{ color: (stats?.parsing || 0) > 0 ? '#A9761B' : '#2A6B63' }">
            {{ stats?.parsing ?? 0 }}
          </text>
          <text class="stat-mini-label">解析中</text>
        </view>
      </view>
    </view>

    <!-- 筛选标签 -->
    <scroll-view scroll-x class="cat-scroll" :show-scrollbar="false">
      <view class="cat-pills">
        <view
          class="cat-pill"
          :class="{ active: query.category === '' }"
          @tap="selectCategory('')"
        >
          <text>全部</text>
        </view>
        <view
          v-for="cat in categories"
          :key="cat"
          class="cat-pill"
          :class="{ active: query.category === cat }"
          @tap="selectCategory(cat)"
        >
          <text>{{ cat }}</text>
        </view>
      </view>
    </scroll-view>

    <!-- 文档列表 -->
    <view v-if="loading && !list.length" class="skeleton-list">
      <view v-for="i in 3" :key="i" class="rag-card skeleton-card">
        <view class="skeleton-line" style="width: 60%; height: 13px;"></view>
        <view class="skeleton-line" style="width: 40%; height: 9px; margin-top: 7px;"></view>
      </view>
    </view>

    <view v-else-if="!list.length" class="rag-card">
      <EmptyState title="暂无知识文档" desc="上传 Java 面试笔记、技术文档或 PDF/MD 资料，系统将自动向量化并用于 RAG 问答与面试出题">
        <button class="btn-primary btn-sm" @tap="uploadModalOpen = true">上传第一篇文档</button>
      </EmptyState>
    </view>

    <view v-else class="doc-list">
      <view
        v-for="doc in list"
        :key="doc.id"
        class="rag-card doc-card"
      >
        <view class="doc-top">
          <view class="doc-name-box">
            <text class="doc-icon">📄</text>
            <text class="doc-title">{{ doc.docName }}</text>
          </view>
          <text class="tag" :class="getStatusClass(doc.status)">
            {{ getStatusText(doc.status) }}
          </text>
        </view>

        <view class="doc-mid">
          <text class="tag tag-slate">{{ doc.category || '默认分类' }}</text>
          <text class="doc-chunks font-mono">切片数: {{ doc.chunkCount ?? 0 }}</text>
          <text class="doc-time font-mono">{{ doc.createTime }}</text>
        </view>

        <view class="doc-bot">
          <view class="doc-actions">
            <button
              class="btn-secondary btn-sm doc-act-btn"
              :disabled="doc.status !== 2"
              @tap="openChunks(doc)"
            >
              切片明细
            </button>
            <button
              v-if="doc.status === 3"
              class="btn-ghost btn-sm doc-act-btn"
              @tap="handleReparse(doc.id)"
            >
              重新解析
            </button>
            <text class="doc-delete-btn" @tap="handleDelete(doc.id)">删除</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 切片详情弹窗 -->
    <AppModal
      v-model:visible="chunkModalOpen"
      :title="`文档切片明细 · ${currentDoc?.docName || ''}`"
      confirmText="关闭"
      :showCancel="false"
      @confirm="chunkModalOpen = false"
    >
      <view v-if="chunkLoading" class="chunk-loading">
        <text>正在加载切片列表...</text>
      </view>
      <view v-else-if="!chunks.length" class="empty-hint">
        <text>暂无切片数据</text>
      </view>
      <view v-else class="chunk-list">
        <view
          v-for="chunk in chunks"
          :key="chunk.id"
          class="chunk-item"
        >
          <view class="chunk-item-head">
            <text class="num-badge">#{{ chunk.chunkIndex }}</text>
            <text class="chunk-token-count font-mono">{{ chunk.content?.length || 0 }} 字符</text>
          </view>
          <text class="chunk-text">{{ chunk.content }}</text>
        </view>
      </view>
    </AppModal>

    <!-- 上传文档弹窗 -->
    <AppModal
      v-model:visible="uploadModalOpen"
      title="上传知识文档"
      confirmText="确定上传"
      :loading="uploading"
      @confirm="submitUpload"
    >
      <view class="upload-form">
        <view class="form-group">
          <text class="form-label">技术分类</text>
          <picker
            :value="uploadCatIdx"
            :range="categories"
            @change="(e) => uploadCatIdx = e.detail.value"
          >
            <view class="picker-box">
              <text>{{ categories[uploadCatIdx] || '请选择分类' }}</text>
              <text class="picker-arrow">▼</text>
            </view>
          </picker>
        </view>

        <!-- 选择文件模式 -->
        <view class="form-group">
          <text class="form-label">选择本地文件 (支持 .md / .pdf)</text>
          <view class="file-picker-box" @tap="chooseFile">
            <text v-if="selectedFile" class="file-selected-name">
              📎 {{ selectedFile.name || selectedFile.path }}
            </text>
            <text v-else class="file-picker-hint">
              点击选择手机中的 Markdown 或 PDF 文档
            </text>
          </view>
        </view>
      </view>
    </AppModal>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import {
  getDocumentList,
  uploadDocument,
  deleteDocument,
  getChunkList,
  reparseDocument,
  getKnowledgeStats,
  getCategories
} from '@/api/knowledge'
import { toastSuccess, toastWarning, toastError } from '@/utils/toast'
import EmptyState from '@/components/EmptyState.vue'
import AppModal from '@/components/AppModal.vue'

const categories = ref([
  'Java基础',
  '并发编程',
  'JVM',
  'Spring',
  'MySQL',
  'Redis',
  '分布式',
  '面试手册'
])

const loading = ref(false)
const list = ref([])
const total = ref(0)
const stats = ref(null)

const query = reactive({
  pageNum: 1,
  pageSize: 20,
  category: ''
})

// 切片弹窗
const chunkModalOpen = ref(false)
const chunkLoading = ref(false)
const currentDoc = ref(null)
const chunks = ref([])

// 上传弹窗
const uploadModalOpen = ref(false)
const uploading = ref(false)
const uploadCatIdx = ref(0)
const selectedFile = ref(null)

let pollTimer = null

onMounted(async () => {
  await loadCategories()
  await loadData()
})

onUnmounted(() => {
  stopPoll()
})

onPullDownRefresh(async () => {
  await loadData()
  uni.stopPullDownRefresh()
})

async function loadCategories() {
  try {
    const res = await getCategories()
    if (res && res.length) categories.value = res
  } catch (e) {}
}

async function loadData() {
  loading.value = true
  try {
    const [page, s] = await Promise.all([
      getDocumentList({
        pageNum: query.pageNum,
        pageSize: query.pageSize,
        category: query.category || undefined
      }),
      getKnowledgeStats()
    ])
    list.value = page?.list || page || []
    total.value = page?.total || list.value.length
    stats.value = s

    // 若有解析中的文档，轮询刷新
    const hasParsing = list.value.some((d) => d.status === 0 || d.status === 1)
    if (hasParsing) {
      startPoll()
    } else {
      stopPoll()
    }
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function startPoll() {
  if (pollTimer) return
  pollTimer = setInterval(() => {
    loadData()
  }, 4000)
}

function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function selectCategory(cat) {
  query.category = cat
  loadData()
}

function getStatusText(status) {
  if (status === 0) return '待处理'
  if (status === 1) return '解析中'
  if (status === 2) return '已完成'
  if (status === 3) return '解析失败'
  return '待处理'
}

function getStatusClass(status) {
  if (status === 0) return 'tag-slate'
  if (status === 1) return 'tag-amber'
  if (status === 2) return 'tag-teal'
  if (status === 3) return 'tag-red'
  return 'tag-slate'
}

async function openChunks(doc) {
  currentDoc.value = doc
  chunkModalOpen.value = true
  chunkLoading.value = true
  try {
    const res = await getChunkList(doc.id, { pageNum: 1, pageSize: 50 })
    chunks.value = res?.list || res || []
  } catch (e) {
    chunks.value = []
  } finally {
    chunkLoading.value = false
  }
}

async function handleReparse(id) {
  try {
    await reparseDocument(id)
    toastSuccess('已重新发起解析任务')
    loadData()
  } catch (e) {
    toastError('重新解析请求失败')
  }
}

function handleDelete(id) {
  uni.showModal({
    title: '确认删除文档',
    content: '删除后对应的所有向量切片也将同步移除，确定删除？',
    confirmColor: '#C4432B',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteDocument(id)
          toastSuccess('文档已删除')
          loadData()
        } catch (e) {
          toastError('删除失败')
        }
      }
    }
  })
}

function chooseFile() {
  // #ifdef H5
  uni.chooseFile({
    count: 1,
    extension: ['.md', '.pdf'],
    success: (res) => {
      if (res.tempFiles && res.tempFiles[0]) {
        selectedFile.value = {
          path: res.tempFilePaths[0],
          name: res.tempFiles[0].name
        }
      }
    }
  })
  // #endif

  // #ifdef APP-PLUS
  // Android 原生文件选择
  plus.io.resolveLocalFileSystemURL('_downloads/', (entry) => {
    // 兼容打开文件选择
  }, (e) => {})
  // 使用 uni.chooseFile
  if (uni.chooseFile) {
    uni.chooseFile({
      count: 1,
      extension: ['.md', '.pdf'],
      success: (res) => {
        if (res.tempFiles && res.tempFiles[0]) {
          selectedFile.value = {
            path: res.tempFilePaths[0],
            name: res.tempFiles[0].name
          }
        }
      }
    })
  }
  // #endif
}

async function submitUpload() {
  if (!selectedFile.value || !selectedFile.value.path) {
    toastWarning('请先选择待上传的文件')
    return
  }

  uploading.value = true
  try {
    await uploadDocument(selectedFile.value.path, {
      category: categories.value[uploadCatIdx.value] || 'Java基础'
    })
    toastSuccess('文件已上传，后台正在智能切片并向量化')
    uploadModalOpen.value = false
    selectedFile.value = null
    loadData()
  } catch (e) {
    toastError('上传失败，请检查网络或后端配置')
  } finally {
    uploading.value = false
  }
}
</script>

<style lang="scss" scoped>
.knowledge-header {
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

.upload-btn {
  margin: 0;
}

/* 统计小卡片 */
.stats-card {
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.stats-row {
  display: flex;
  justify-content: space-around;
  align-items: center;
}

.stat-mini {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-mini-val {
  font-size: 36rpx;
  font-weight: 700;
  color: #131B23;
}

.stat-mini-label {
  font-size: 22rpx;
  color: #8A959E;
  margin-top: 4rpx;
}

/* 分类标签滑动 */
.cat-scroll {
  white-space: nowrap;
  margin-bottom: 20rpx;
}

.cat-pills {
  display: inline-flex;
  gap: 12rpx;
}

.cat-pill {
  background-color: #FBFAF6;
  border: 1px solid #D8D2C4;
  padding: 8rpx 20rpx;
  border-radius: 6rpx;
  font-size: 22rpx;
  color: #56646F;
}

.cat-pill.active {
  background-color: #131B23;
  color: #FBFAF6;
  border-color: #131B23;
  font-weight: 600;
}

/* 文档列表 */
.doc-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.doc-card {
  margin-bottom: 0;
  padding: 22rpx 24rpx;
}

.doc-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.doc-name-box {
  display: flex;
  align-items: center;
  gap: 10rpx;
  flex: 1;
  overflow: hidden;
  margin-right: 12rpx;
}

.doc-icon {
  font-size: 26rpx;
}

.doc-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #131B23;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.doc-mid {
  display: flex;
  align-items: center;
  gap: 16rpx;
  font-size: 22rpx;
  color: #8A959E;
  margin-bottom: 16rpx;
  padding-bottom: 12rpx;
  border-bottom: 1px dashed #EAE5D9;
}

.doc-chunks {
  color: #24506B;
}

.doc-bot {
  display: flex;
  justify-content: flex-end;
}

.doc-actions {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.doc-act-btn {
  margin: 0;
}

.doc-delete-btn {
  font-size: 22rpx;
  color: #8A959E;
  padding: 6rpx 10rpx;
}

/* 切片弹窗明细 */
.chunk-list {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.chunk-item {
  background-color: #FFFFFF;
  border: 1px solid #EAE5D9;
  border-radius: 6rpx;
  padding: 16rpx;
}

.chunk-item-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8rpx;
}

.chunk-token-count {
  font-size: 20rpx;
  color: #8A959E;
}

.chunk-text {
  font-size: 24rpx;
  color: #2A3846;
  line-height: 1.6;
}

/* 上传表单 */
.upload-form {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
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

.picker-box {
  background-color: #FFFFFF;
  border: 1px solid #D8D2C4;
  border-radius: 6rpx;
  padding: 16rpx 20rpx;
  font-size: 26rpx;
  display: flex;
  justify-content: space-between;
}

.file-picker-box {
  background-color: #FFFFFF;
  border: 2px dashed #D8D2C4;
  border-radius: 8rpx;
  padding: 36rpx 20rpx;
  text-align: center;
}

.file-picker-box:active {
  background-color: #EFEBE2;
}

.file-picker-hint {
  font-size: 24rpx;
  color: #8A959E;
}

.file-selected-name {
  font-size: 24rpx;
  color: #C4432B;
  font-weight: 600;
}
</style>

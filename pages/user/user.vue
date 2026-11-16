<template>
  <view class="page-container">
    <!-- 用户信息卡片 -->
    <view class="rag-card user-hero-card">
      <view class="user-avatar-box">
        <text class="user-avatar-text">{{ avatarChar }}</text>
      </view>
      <view class="user-meta">
        <view class="user-name-line">
          <text class="user-nickname">{{ userStore.nickname }}</text>
          <text class="tag" :class="userStore.isAdmin ? 'tag-red' : 'tag-slate'">
            {{ userStore.isAdmin ? '超级管理员' : '普通用户' }}
          </text>
        </view>
        <text class="user-account font-mono">账号: {{ userStore.userInfo?.username || '未登录' }}</text>
        <text v-if="userStore.userInfo?.email" class="user-email">{{ userStore.userInfo.email }}</text>
      </view>
    </view>

    <!-- 服务端网络连接配置卡片 -->
    <view class="rag-card">
      <view class="rag-card-head">
        <view class="rag-card-title">
          <text class="num-badge">01</text>
          <text>后端服务连接设置</text>
        </view>
        <view class="ping-status" :class="pingState">
          <text class="ping-dot">●</text>
          <text class="ping-text">{{ pingLabel }}</text>
        </view>
      </view>

      <view class="server-info-box">
        <text class="server-label">当前服务 Base URL：</text>
        <text class="server-value font-mono">{{ currentServerUrl }}</text>
      </view>

      <view class="server-action-row">
        <button class="btn-secondary btn-sm" :loading="testing" @tap="testConnection">
          测试连通性
        </button>
        <button class="btn-ghost btn-sm" @tap="serverModalOpen = true">
          修改服务地址
        </button>
      </view>
    </view>

    <!-- 演示账号快捷切换 -->
    <view class="rag-card">
      <view class="rag-card-head">
        <view class="rag-card-title">
          <text class="num-badge">02</text>
          <text>演示账号快速登录</text>
        </view>
      </view>
      <view class="demo-switch-row">
        <button class="btn-secondary btn-sm demo-sw-btn" @tap="quickSwitchAccount('admin')">
          切换为管理员 (admin)
        </button>
        <button class="btn-secondary btn-sm demo-sw-btn" @tap="quickSwitchAccount('zhangsan')">
          切换为用户 (zhangsan)
        </button>
      </view>
    </view>

    <!-- 账户管理操作列表 -->
    <view class="rag-card settings-card">
      <view class="rag-card-head">
        <view class="rag-card-title">
          <text class="num-badge">03</text>
          <text>安全与设置</text>
        </view>
      </view>

      <view class="settings-list">
        <view class="setting-item" @tap="pwdModalOpen = true">
          <text class="setting-label">修改登录密码</text>
          <text class="setting-arrow">›</text>
        </view>
        <view class="setting-item" @tap="goToKnowledge">
          <text class="setting-label">管理知识文档库</text>
          <text class="setting-arrow">›</text>
        </view>
        <view class="setting-item" @tap="showAboutModal = true">
          <text class="setting-label">关于客户端与 APK 说明</text>
          <text class="setting-arrow">›</text>
        </view>
      </view>
    </view>

    <!-- 退出登录按钮 -->
    <view class="logout-section">
      <button class="btn-secondary logout-btn" @tap="handleLogout">
        退出当前账号
      </button>
    </view>

    <!-- 修改服务地址弹窗 -->
    <AppModal
      v-model:visible="serverModalOpen"
      title="配置后端服务地址"
      confirmText="保存并测试"
      @confirm="saveServerConfig"
    >
      <view class="config-modal-body">
        <text class="config-tip">
          手机运行 APK 时，请确保手机与电脑在同一无线局域网下，并填写电脑局域网 IP。
        </text>
        <view class="input-group" style="margin: 8px 0;">
          <input
            v-model="serverInput"
            class="input-field font-mono"
            placeholder="例如 http://172.29.155.80:8080"
          />
        </view>
        <view class="preset-group">
          <text class="preset-label">快捷预设选择：</text>
          <view class="preset-chips">
            <text class="preset-chip" @tap="serverInput = 'http://47.98.190.255:81/api'">云端服务器 (47.98.190.255:81/api)</text>
            <text class="preset-chip" @tap="serverInput = 'http://172.29.155.80:8080'">本机局域网 (172.29.155.80:8080)</text>
            <text class="preset-chip" @tap="serverInput = 'http://10.0.2.2:8080'">Android 模拟器 (10.0.2.2:8080)</text>
          </view>
        </view>
      </view>
    </AppModal>

    <!-- 修改密码弹窗 -->
    <AppModal
      v-model:visible="pwdModalOpen"
      title="修改登录密码"
      confirmText="确认修改"
      :loading="pwdLoading"
      @confirm="submitChangePassword"
    >
      <view class="pwd-form">
        <view class="input-group">
          <text class="input-label">原密码</text>
          <input
            v-model="pwdForm.oldPassword"
            password
            class="input-field"
            placeholder="请输入当前密码"
          />
        </view>
        <view class="input-group">
          <text class="input-label">新密码</text>
          <input
            v-model="pwdForm.newPassword"
            password
            class="input-field"
            placeholder="请输入新密码 (至少 6 位)"
          />
        </view>
      </view>
    </AppModal>

    <!-- 关于客户端说明弹窗 -->
    <AppModal
      v-model:visible="showAboutModal"
      title="关于移动端 APK"
      confirmText="我知道了"
      :showCancel="false"
      @confirm="showAboutModal = false"
    >
      <view class="about-content">
        <text class="about-title">Java 面试系统 · 移动端 (APK)</text>
        <text class="about-version font-mono">Version 1.0.0 (Release)</text>
        <text class="about-text">
          本客户端基于 UniApp (Vue 3 + Pinia) 构建，通过 RESTful 接口与原有 Java 后端微服务网关直接通信，具备完整的知识库检索增强问答、AI 模拟面试考场与实时评测报告生成能力。
        </text>
      </view>
    </AppModal>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { changePassword as changePasswordApi } from '@/api/user'
import { getDashboardStats } from '@/api/dashboard'
import { getBaseUrl, setBaseUrl, DEFAULT_API_HOST } from '@/utils/config'
import { toastSuccess, toastWarning, toastError } from '@/utils/toast'
import AppModal from '@/components/AppModal.vue'

const userStore = useUserStore()

const currentServerUrl = ref(getBaseUrl())
const serverModalOpen = ref(false)
const serverInput = ref(getBaseUrl())

const testing = ref(false)
const pingState = ref('ping-idle') // ping-idle | ping-ok | ping-err
const pingLabel = ref('未检测')

// 密码弹窗
const pwdModalOpen = ref(false)
const pwdLoading = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: ''
})

const showAboutModal = ref(false)

const avatarChar = computed(() => {
  const name = userStore.nickname || userStore.userInfo?.username || 'U'
  return name.charAt(0).toUpperCase()
})

onMounted(() => {
  currentServerUrl.value = getBaseUrl()
  serverInput.value = getBaseUrl()
  testConnection()
})

async function testConnection() {
  testing.value = true
  pingState.value = 'ping-idle'
  pingLabel.value = '测试中...'
  try {
    await getDashboardStats()
    pingState.value = 'ping-ok'
    pingLabel.value = '连接正常'
    toastSuccess('后端服务通信正常')
  } catch (e) {
    pingState.value = 'ping-err'
    pingLabel.value = '连接异常'
    toastError('后端服务无法连接，请核对IP')
  } finally {
    testing.value = false
  }
}

function saveServerConfig() {
  if (!serverInput.value.trim()) {
    setBaseUrl(DEFAULT_API_HOST)
  } else {
    setBaseUrl(serverInput.value.trim())
  }
  currentServerUrl.value = getBaseUrl()
  serverModalOpen.value = false
  testConnection()
}

async function quickSwitchAccount(username) {
  try {
    await userStore.login({
      username,
      password: '123456'
    })
    toastSuccess(`已切换为: ${userStore.nickname}`)
  } catch (e) {
    toastError('账号切换失败')
  }
}

async function submitChangePassword() {
  if (!pwdForm.oldPassword) {
    toastWarning('请输入原密码')
    return
  }
  if (!pwdForm.newPassword || pwdForm.newPassword.length < 6) {
    toastWarning('新密码至少 6 位')
    return
  }

  pwdLoading.value = true
  try {
    await changePasswordApi({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    toastSuccess('密码已修改，请重新登录')
    pwdModalOpen.value = false
    setTimeout(() => {
      userStore.logout()
    }, 800)
  } catch (e) {
    toastError('修改密码失败')
  } finally {
    pwdLoading.value = false
  }
}

function goToKnowledge() {
  uni.navigateTo({
    url: '/pages/knowledge/knowledge'
  })
}

function handleLogout() {
  uni.showModal({
    title: '确认退出',
    content: '退出登录后需重新验证账号密码，是否继续？',
    confirmColor: '#C4432B',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.user-hero-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 36rpx 28rpx;
}

.user-avatar-box {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50rpx;
  background-color: #131B23;
  color: #FBFAF6;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 4rpx solid #C4432B;
}

.user-avatar-text {
  font-size: 44rpx;
  font-weight: 700;
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
  flex: 1;
}

.user-name-line {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.user-nickname {
  font-size: 32rpx;
  font-weight: 700;
  color: #131B23;
}

.user-account {
  font-size: 22rpx;
  color: #8A959E;
}

.user-email {
  font-size: 22rpx;
  color: #56646F;
}

/* 服务端连接状态 */
.ping-status {
  display: flex;
  align-items: center;
  gap: 6rpx;
  font-size: 22rpx;
}

.ping-ok { color: #2A6B63; }
.ping-err { color: #C4432B; }
.ping-idle { color: #8A959E; }

.ping-dot {
  font-size: 16rpx;
}

.server-info-box {
  background-color: #FFFFFF;
  border: 1px solid #EAE5D9;
  border-radius: 6rpx;
  padding: 16rpx 20rpx;
  margin-bottom: 18rpx;
}

.server-label {
  font-size: 22rpx;
  color: #8A959E;
  display: block;
}

.server-value {
  font-size: 26rpx;
  color: #24506B;
  font-weight: 600;
}

.server-action-row {
  display: flex;
  gap: 16rpx;
}

/* 演示账号切换 */
.demo-switch-row {
  display: flex;
  gap: 16rpx;
}

.demo-sw-btn {
  flex: 1;
  margin: 0;
}

/* 设置列表 */
.settings-list {
  display: flex;
  flex-direction: column;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
  border-bottom: 1px dashed #EAE5D9;
}

.setting-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.setting-label {
  font-size: 26rpx;
  color: #131B23;
}

.setting-arrow {
  font-size: 32rpx;
  color: #8A959E;
}

/* 退出登录 */
.logout-section {
  margin: 36rpx 0;
}

.logout-btn {
  width: 100%;
  color: #C4432B;
  border-color: #F6E4DF;
  background-color: #FBFAF6;
}

.logout-btn:active {
  background-color: #F6E4DF;
}

/* 弹窗细节 */
.config-modal-body, .pwd-form, .about-content {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.config-tip {
  font-size: 24rpx;
  color: #56646F;
  line-height: 1.5;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.input-label {
  font-size: 22rpx;
  color: #56646F;
}

.input-field {
  background-color: #FFFFFF;
  border: 1px solid #D8D2C4;
  border-radius: 6rpx;
  padding: 16rpx;
  font-size: 26rpx;
}

.preset-group {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.preset-label {
  font-size: 22rpx;
  color: #8A959E;
}

.preset-chips {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.preset-chip {
  background-color: #EFEBE2;
  padding: 10rpx 14rpx;
  border-radius: 4rpx;
  font-size: 22rpx;
  color: #131B23;
  font-family: monospace;
}

.preset-chip:active {
  background-color: #C4432B;
  color: #FFFFFF;
}

.about-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #131B23;
}

.about-version {
  font-size: 22rpx;
  color: #C4432B;
}

.about-text {
  font-size: 24rpx;
  color: #56646F;
  line-height: 1.6;
}
</style>

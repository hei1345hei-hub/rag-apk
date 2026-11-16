<template>
  <view class="login-wrapper safe-top">
    <!-- 头部品牌标志 -->
    <view class="brand-section">
      <view class="logo-box">
        <img class="app-logo" :src="logoImg" alt="" />
      </view>
      <text class="brand-title">Java 面试系统</text>
      <text class="brand-subtitle">基于 RAG 的智能问答与模拟面试平台 · 移动端</text>
    </view>

    <!-- 登录卡片 -->
    <view class="auth-card">
      <!-- 切换登录 / 注册 -->
      <view class="tab-switch">
        <view
          class="tab-item"
          :class="{ active: mode === 'login' }"
          @tap="switchMode('login')"
        >
          <text>账号登录</text>
        </view>
        <view
          class="tab-item"
          :class="{ active: mode === 'register' }"
          @tap="switchMode('register')"
        >
          <text>注册新账号</text>
        </view>
      </view>

      <!-- 快速填入演示账号 -->
      <view v-if="mode === 'login'" class="demo-section">
        <text class="demo-tip">快捷演示账号：</text>
        <view class="demo-tags">
          <view class="demo-btn" @tap="fillDemo('admin')">
            <text class="demo-btn-label">管理员</text>
            <text class="demo-btn-user">admin</text>
          </view>
          <view class="demo-btn" @tap="fillDemo('zhangsan')">
            <text class="demo-btn-label">普通用户</text>
            <text class="demo-btn-user">zhangsan</text>
          </view>
        </view>
      </view>

      <!-- 表单输入 -->
      <view class="form-container">
        <!-- 账号 -->
        <view class="input-group">
          <text class="input-label">登录账号</text>
          <input
            v-model="form.username"
            class="input-field"
            placeholder="请输入账号 (如 admin)"
            placeholder-class="placeholder-style"
          />
        </view>

        <!-- 密码 -->
        <view class="input-group">
          <text class="input-label">密码</text>
          <input
            v-model="form.password"
            password
            class="input-field"
            placeholder="请输入密码 (至少 6 位)"
            placeholder-class="placeholder-style"
          />
        </view>

        <!-- 注册附加项 -->
        <template v-if="mode === 'register'">
          <view class="input-group">
            <text class="input-label">确认密码</text>
            <input
              v-model="form.confirm"
              password
              class="input-field"
              placeholder="请再次确认密码"
              placeholder-class="placeholder-style"
            />
          </view>
          <view class="input-group">
            <text class="input-label">用户昵称</text>
            <input
              v-model="form.nickname"
              class="input-field"
              placeholder="请输入您的昵称"
              placeholder-class="placeholder-style"
            />
          </view>
          <view class="input-group">
            <text class="input-label">电子邮箱</text>
            <input
              v-model="form.email"
              class="input-field"
              placeholder="用于找回与通知"
              placeholder-class="placeholder-style"
            />
          </view>
        </template>

        <!-- 提交按钮 -->
        <button
          class="btn-primary submit-btn"
          :loading="loading"
          @tap="submit"
        >
          {{ mode === 'login' ? '登 录 系 统' : '注 册 账 号' }}
        </button>
      </view>
    </view>

    <!-- 服务端网络配置入口 -->
    <view class="server-config-entry" @tap="serverModalOpen = true">
      <text class="server-entry-label">当前服务端地址：</text>
      <text class="server-entry-val font-mono">{{ currentServerUrl }}</text>
      <text class="server-entry-btn">[ 修改 ]</text>
    </view>

    <!-- 服务端配置弹窗 -->
    <AppModal
      v-model:visible="serverModalOpen"
      title="配置后端服务地址"
      confirmText="保存并测试"
      @confirm="saveServerConfig"
    >
      <view class="config-modal-body">
        <text class="config-desc">
          手机或模拟器需通过局域网连接后端服务。若在 Android 模拟器中运行，请填写 10.0.2.2:8080；若在真机运行，请保持手机与电脑同 WiFi 并填写电脑 IP。
        </text>
        <view class="input-group" style="margin-top: 8px">
          <input
            v-model="serverInput"
            class="input-field font-mono"
            placeholder="例如 http://172.29.155.80:8080"
          />
        </view>
        <view class="preset-links">
          <text class="preset-title">快捷预设：</text>
          <view class="preset-btns">
            <text class="preset-btn" @tap="serverInput = 'http://47.98.190.255:81/api'">云端服务 (47.98.190.255:81)</text>
            <text class="preset-btn" @tap="serverInput = 'http://172.29.155.80:8080'">本机局域网</text>
            <text class="preset-btn" @tap="serverInput = 'http://10.0.2.2:8080'">官方模拟器</text>
          </view>
        </view>
      </view>
    </AppModal>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { register as registerApi } from '@/api/user'
import { getBaseUrl, setBaseUrl, DEFAULT_API_HOST } from '@/utils/config'
import { toastSuccess, toastWarning, toastError } from '@/utils/toast'
import AppModal from '@/components/AppModal.vue'
import logoImg from '@/static/logo.png'

const userStore = useUserStore()

const mode = ref('login')
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirm: '',
  nickname: '',
  email: ''
})

// 服务端地址配置
const serverModalOpen = ref(false)
const currentServerUrl = ref(getBaseUrl())
const serverInput = ref(getBaseUrl())

onMounted(() => {
  currentServerUrl.value = getBaseUrl()
  serverInput.value = getBaseUrl()
  // 默认填入演示账号
  fillDemo('admin')
})

function switchMode(next) {
  mode.value = next
}

function fillDemo(username) {
  form.username = username
  form.password = '123456'
}

function validate() {
  if (!form.username.trim()) {
    toastWarning('请输入登录账号')
    return false
  }
  if (!form.password) {
    toastWarning('请输入密码')
    return false
  }
  if (form.password.length < 6) {
    toastWarning('密码至少 6 位')
    return false
  }
  if (mode.value === 'register') {
    if (!form.confirm) {
      toastWarning('请再次输入密码')
      return false
    }
    if (form.confirm !== form.password) {
      toastWarning('两次输入的密码不一致')
      return false
    }
  }
  return true
}

async function submit() {
  if (!validate()) return

  loading.value = true
  try {
    if (mode.value === 'login') {
      await userStore.login({
        username: form.username.trim(),
        password: form.password
      })
      toastSuccess(`欢迎，${userStore.nickname}`)
      setTimeout(() => {
        uni.switchTab({
          url: '/pages/dashboard/dashboard'
        })
      }, 500)
    } else {
      await registerApi({
        username: form.username.trim(),
        password: form.password,
        nickname: form.nickname.trim() || form.username.trim(),
        email: form.email.trim()
      })
      toastSuccess('注册成功，请使用新账号登录')
      mode.value = 'login'
      form.password = ''
      form.confirm = ''
    }
  } catch (e) {
    // 错误在 request 拦截中提示
  } finally {
    loading.value = false
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
  toastSuccess('后端地址已更新')
}
</script>

<style lang="scss" scoped>
.login-wrapper {
  min-height: 100vh;
  background-color: #F4F1EA;
  padding: 40rpx 30rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand-section {
  text-align: center;
  margin-bottom: 40rpx;
}

.logo-box {
  width: 130rpx;
  height: 130rpx;
  margin: 0 auto 20rpx;
  background-color: #131B23;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(19, 27, 35, 0.15);
  overflow: hidden;
}

.app-logo {
  width: 100%;
  height: 100%;
}

.brand-title {
  font-size: 40rpx;
  font-weight: 700;
  color: #131B23;
  letter-spacing: 2rpx;
  display: block;
}

.brand-subtitle {
  font-size: 24rpx;
  color: #56646F;
  margin-top: 10rpx;
  display: block;
}

.auth-card {
  background-color: #FBFAF6;
  border: 1px solid #D8D2C4;
  border-radius: 12rpx;
  padding: 36rpx 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(19, 27, 35, 0.05);
}

.tab-switch {
  display: flex;
  border-bottom: 2px solid #EAE5D9;
  margin-bottom: 28rpx;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  font-size: 30rpx;
  color: #8A959E;
  position: relative;
}

.tab-item.active {
  color: #C4432B;
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 20%;
  right: 20%;
  height: 2px;
  background-color: #C4432B;
}

.demo-section {
  background-color: #EFEBE2;
  border: 1px dashed #D8D2C4;
  border-radius: 6rpx;
  padding: 16rpx;
  margin-bottom: 28rpx;
}

.demo-tip {
  font-size: 22rpx;
  color: #56646F;
  display: block;
  margin-bottom: 12rpx;
}

.demo-tags {
  display: flex;
  gap: 16rpx;
}

.demo-btn {
  flex: 1;
  background-color: #FBFAF6;
  border: 1px solid #D8D2C4;
  border-radius: 6rpx;
  padding: 8rpx 12rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.demo-btn:active {
  background-color: #F6E4DF;
  border-color: #C4432B;
}

.demo-btn-label {
  font-size: 20rpx;
  color: #8A959E;
}

.demo-btn-user {
  font-size: 24rpx;
  font-weight: 600;
  color: #131B23;
  font-family: monospace;
}

.form-container {
  display: flex;
  flex-direction: column;
  gap: 22rpx;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.input-label {
  font-size: 24rpx;
  color: #56646F;
  font-weight: 500;
}

.input-field {
  background-color: #FFFFFF;
  border: 1px solid #D8D2C4;
  border-radius: 6rpx;
  padding: 16rpx 20rpx;
  font-size: 28rpx;
  color: #131B23;
}

.placeholder-style {
  color: #AFB7BD;
  font-size: 26rpx;
}

.submit-btn {
  margin-top: 18rpx;
  height: 84rpx;
  font-size: 30rpx;
}

.server-config-entry {
  margin-top: 36rpx;
  text-align: center;
  font-size: 22rpx;
  color: #8A959E;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 6rpx;
}

.server-entry-val {
  color: #24506B;
}

.server-entry-btn {
  color: #C4432B;
  font-weight: 600;
}

.config-modal-body {
  font-size: 26rpx;
  color: #56646F;
  line-height: 1.6;
}

.config-desc {
  font-size: 24rpx;
  color: #8A959E;
}

.preset-links {
  margin-top: 20rpx;
}

.preset-title {
  font-size: 24rpx;
  color: #56646F;
  margin-bottom: 10rpx;
  display: block;
}

.preset-btns {
  display: flex;
  gap: 12rpx;
  flex-wrap: wrap;
}

.preset-btn {
  background-color: #EAE5D9;
  color: #131B23;
  padding: 6rpx 16rpx;
  border-radius: 4rpx;
  font-size: 22rpx;
}

.preset-btn:active {
  background-color: #C4432B;
  color: #FFFFFF;
}
</style>

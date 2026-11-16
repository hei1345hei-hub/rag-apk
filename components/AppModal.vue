<template>
  <view v-if="visible" class="modal-mask" @tap.self="handleMaskClick">
    <view class="modal-box">
      <view class="modal-head">
        <text class="modal-title">{{ title }}</text>
        <text class="modal-close" @tap="handleClose">✕</text>
      </view>
      <scroll-view scroll-y class="modal-body">
        <slot></slot>
      </scroll-view>
      <view v-if="showFooter" class="modal-foot">
        <button v-if="showCancel" class="btn-secondary btn-sm foot-btn" @tap="handleCancel">
          {{ cancelText }}
        </button>
        <button class="btn-primary btn-sm foot-btn" :loading="loading" @tap="handleConfirm">
          {{ confirmText }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '提示' },
  confirmText: { type: String, default: '确定' },
  cancelText: { type: String, default: '取消' },
  showCancel: { type: Boolean, default: true },
  showFooter: { type: Boolean, default: true },
  loading: { type: Boolean, default: false },
  maskClosable: { type: Boolean, default: true }
})

const emit = defineEmits(['update:visible', 'confirm', 'cancel', 'close'])

function handleMaskClick() {
  if (props.maskClosable) {
    handleClose()
  }
}

function handleClose() {
  emit('update:visible', false)
  emit('close')
}

function handleCancel() {
  emit('update:visible', false)
  emit('cancel')
}

function handleConfirm() {
  emit('confirm')
}
</script>

<style lang="scss" scoped>
.modal-mask {
  position: fixed;
  inset: 0;
  background-color: rgba(19, 27, 35, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  padding: 30rpx;
}

.modal-box {
  width: 100%;
  max-width: 650rpx;
  background-color: #FBFAF6;
  border: 1px solid #D8D2C4;
  border-radius: 12rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 32rpx rgba(19, 27, 35, 0.2);
  display: flex;
  flex-direction: column;
  max-height: 85vh;
}

.modal-head {
  padding: 24rpx 28rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #EAE5D9;
}

.modal-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #131B23;
}

.modal-close {
  font-size: 32rpx;
  color: #8A959E;
  padding: 8rpx;
}

.modal-body {
  padding: 28rpx;
  max-height: 60vh;
}

.modal-foot {
  padding: 20rpx 28rpx;
  display: flex;
  justify-content: flex-end;
  gap: 20rpx;
  border-top: 1px solid #EAE5D9;
  background-color: #F4F1EA;
}

.foot-btn {
  margin: 0;
}
</style>

<template>
  <view class="empty-state">
    <view class="empty-icon-box">
      <text class="empty-symbol">∅</text>
    </view>
    <text class="empty-title">{{ title }}</text>
    <text v-if="desc" class="empty-desc">{{ desc }}</text>
    <view v-if="$slots.default" class="empty-slot">
      <slot></slot>
    </view>
  </view>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: '暂无数据'
  },
  desc: {
    type: String,
    default: ''
  }
})
</script>

<style lang="scss" scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60rpx 30rpx;
  text-align: center;
}

.empty-icon-box {
  width: 80rpx;
  height: 80rpx;
  border-radius: 40rpx;
  background-color: #EFEBE2;
  border: 1px solid #D8D2C4;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20rpx;
}

.empty-symbol {
  font-size: 40rpx;
  color: #8A959E;
  font-family: monospace;
}

.empty-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #2A3846;
  margin-bottom: 8rpx;
}

.empty-desc {
  font-size: 24rpx;
  color: #8A959E;
  max-width: 480rpx;
  line-height: 1.5;
  margin-bottom: 20rpx;
}

.empty-slot {
  margin-top: 10rpx;
}
</style>

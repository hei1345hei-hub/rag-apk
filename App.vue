<template>
  <div class="mobile-app-shell">
    <!-- 顶部状态栏液态毛玻璃遮罩（内容向上滚动时自然虚化） -->
    <div class="statusbar-glass" aria-hidden="true"></div>

    <!-- 主页面内容区域（原生流畅滚动，无卡顿无黑屏） -->
    <main
      class="mobile-main-content"
      :class="{ 'with-tabbar': isTabBarPage }"
    >
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- 原生导航栏底层的液态玻璃微光环境层 (Backdrop Catalog Liquid Aurora) -->
    <div v-if="isTabBarPage" class="dock-ambient-aurora" aria-hidden="true">
      <div class="aurora-blob aurora-1"></div>
      <div class="aurora-blob aurora-2"></div>
      <div class="aurora-blob aurora-3"></div>
      <div class="aurora-caustic"></div>
    </div>

    <!-- 原生 Kotlin 承载 Kyant0 AndroidLiquidGlass 悬浮液态玻璃导航栏 -->
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import homeIcon from '@/static/tabbar/home.png'
import homeActiveIcon from '@/static/tabbar/home-active.png'
import chatIcon from '@/static/tabbar/chat.png'
import chatActiveIcon from '@/static/tabbar/chat-active.png'
import interviewIcon from '@/static/tabbar/interview.png'
import interviewActiveIcon from '@/static/tabbar/interview-active.png'
import questionIcon from '@/static/tabbar/question.png'
import questionActiveIcon from '@/static/tabbar/question-active.png'
import userIcon from '@/static/tabbar/user.png'
import userActiveIcon from '@/static/tabbar/user-active.png'

const route = useRoute()
const router = useRouter()

const isTabBarPage = computed(() => !!route.meta?.isTab)
const currentTabKey = computed(() => route.meta?.tabKey || '')

const tabList = [
  {
    key: 'dashboard',
    name: '首页',
    path: '/pages/dashboard/dashboard',
    icon: homeIcon,
    iconActive: homeActiveIcon
  },
  {
    key: 'chat',
    name: 'AI问答',
    path: '/pages/chat/chat',
    icon: chatIcon,
    iconActive: chatActiveIcon
  },
  {
    key: 'interview',
    name: '模拟面试',
    path: '/pages/interview/interview',
    icon: interviewIcon,
    iconActive: interviewActiveIcon
  },
  {
    key: 'question',
    name: '题库精选',
    path: '/pages/question/question',
    icon: questionIcon,
    iconActive: questionActiveIcon
  },
  {
    key: 'user',
    name: '个人中心',
    path: '/pages/user/user',
    icon: userIcon,
    iconActive: userActiveIcon
  }
]

const dockRef = ref(null)

// 当前激活的 Tab 索引
const activeTabIndex = computed(() => {
  const idx = tabList.findIndex((t) => t.key === currentTabKey.value)
  return idx < 0 ? 0 : idx
})

// 弹性滑块拖拽与形变状态 (Kyant Damped Drag Animation)
const isDragging = ref(false)
const dragOffsetPercent = ref(0)
const scaleX = ref(1)
const scaleY = ref(1)

let startX = 0
let startPercent = 0
let dragMoved = false
let suppressClickUntil = 0

// 计算胶囊滑块的位移与果冻拉伸形变
const pillTransformStyle = computed(() => {
  const percent = isDragging.value
    ? dragOffsetPercent.value
    : activeTabIndex.value * 100

  const sX = scaleX.value
  const sY = scaleY.value

  const transitionStr = isDragging.value
    ? 'none'
    : 'transform 0.44s cubic-bezier(0.34, 1.45, 0.64, 1)'

  return {
    transform: `translateX(${percent}%) scale(${sX}, ${sY})`,
    transition: transitionStr
  }
})

function switchTab(path) {
  if (route.path !== path) {
    router.push(path)
  }
}

function handleTabClick(path) {
  if (Date.now() < suppressClickUntil) return
  switchTab(path)
}

function handleTouchStart(e) {
  const touch = e.touches && e.touches[0]
  if (!touch) return

  startX = touch.clientX
  startPercent = activeTabIndex.value * 100
  dragOffsetPercent.value = startPercent
  dragMoved = false
  isDragging.value = false
}

function handleTouchMove(e) {
  const touch = e.touches && e.touches[0]
  if (!touch) return

  const deltaX = touch.clientX - startX
  if (Math.abs(deltaX) > 8) {
    isDragging.value = true
    dragMoved = true
  }

  if (isDragging.value) {
    const dockW = dockRef.value ? dockRef.value.offsetWidth - 8 : 340
    const tabW = dockW / 5
    const deltaPercent = (deltaX / tabW) * 100

    // 限制拖拽边界（阻尼回弹）
    const target = startPercent + deltaPercent
    dragOffsetPercent.value = Math.max(-20, Math.min(420, target))

    // 模拟 Kyant 胶囊滑块随速度横向拉伸、纵向压缩的物理果冻质感
    const stretch = Math.min(0.24, Math.abs(deltaPercent) / 260)
    scaleX.value = 1 + stretch
    scaleY.value = 1 - stretch * 0.45
  }
}

function handleTouchEnd(e) {
  if (isDragging.value && dragMoved) {
    // 根据滑动位置吸附到最近的 Tab
    const targetIdx = Math.round(dragOffsetPercent.value / 100)
    const clampedIdx = Math.max(0, Math.min(tabList.length - 1, targetIdx))
    switchTab(tabList[clampedIdx].path)
    suppressClickUntil = Date.now() + 250
  }

  isDragging.value = false
  scaleX.value = 1
  scaleY.value = 1
}

watch(
  () => route.path,
  (newPath) => {
    if (typeof window !== 'undefined' && window.AndroidBridge && window.AndroidBridge.onRouteChanged) {
      window.AndroidBridge.onRouteChanged(newPath)
    }
  },
  { immediate: true }
)
</script>

<style lang="scss">
@import '@/styles/global.scss';

/* 移动端 HTML 元素增强 */
view, text, scroll-view {
  box-sizing: border-box;
}

view {
  display: flex;
  flex-direction: column;
  position: relative;
}

text {
  display: inline;
}

scroll-view {
  display: block;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.mobile-app-shell {
  width: 100%;
  min-height: 100vh;
  background-color: #F4F1EA;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow-x: hidden;
}

/* 主内容区 */
.mobile-main-content {
  flex: 1;
  width: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.mobile-main-content.with-tabbar {
  /* 为悬浮液态胶囊导航栏留足底部可视余量（64px导航条 + 12px底距 + 底部安全区） */
  padding-bottom: calc(92px + env(safe-area-inset-bottom, 16px));
}

/* 顶部状态栏液态毛玻璃虚化遮罩 */
.statusbar-glass {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: calc(env(safe-area-inset-top, 28px) + 10px);
  z-index: 998;
  pointer-events: none;
  background: linear-gradient(180deg, rgba(244, 241, 234, 0.95) 0%, rgba(244, 241, 234, 0.72) 60%, rgba(244, 241, 234, 0) 100%);
  backdrop-filter: blur(14px) saturate(160%);
  -webkit-backdrop-filter: blur(14px) saturate(160%);
  -webkit-mask-image: linear-gradient(180deg, #000 70%, transparent 100%);
  mask-image: linear-gradient(180deg, #000 70%, transparent 100%);
}

/* ==========================================================================
   Kyant0 AndroidLiquidGlass 液态玻璃导航栏悬浮胶囊
   ========================================================================== */

.lg-dock-wrapper {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
  pointer-events: none;
  display: flex;
  justify-content: center;
  padding: 0 16px;
  padding-bottom: calc(10px + env(safe-area-inset-bottom, 16px));
}

.lg-dock {
  pointer-events: auto;
  position: relative;
  width: 100%;
  max-width: 440px;
  height: 64px;
  padding: 4px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  border-radius: 9999px; /* Capsule */
  
  /* 液态玻璃基底材质：实时毛玻璃背景模糊 + 饱和度色彩增强 */
  background: rgba(253, 251, 247, 0.58);
  -webkit-backdrop-filter: blur(28px) saturate(190%) contrast(105%) brightness(1.02);
  backdrop-filter: blur(28px) saturate(190%) contrast(105%) brightness(1.02);
  
  /* 外层悬浮环境柔影 + 内层透镜斜角高光 */
  box-shadow:
    0 12px 32px -4px rgba(19, 27, 35, 0.12),
    0 4px 12px -2px rgba(19, 27, 35, 0.06),
    inset 0 1.5px 2px 0 rgba(255, 255, 255, 0.95),
    inset 0 -1.5px 2px 0 rgba(19, 27, 35, 0.04);
    
  border: 1px solid rgba(255, 255, 255, 0.65);
  user-select: none;
  -webkit-user-select: none;
  touch-action: pan-x;
  transform: translateZ(0);
}

/* 棱镜色散光环 (Kyant Chromatic Aberration) */
.lg-dock::before {
  content: '';
  position: absolute;
  inset: -1px;
  border-radius: 9999px;
  pointer-events: none;
  background: transparent;
  box-shadow:
    inset 1.5px 0 3px -1px rgba(255, 75, 55, 0.35),   /* 红移边缘 */
    inset -1.5px 0 3px -1px rgba(45, 125, 255, 0.35), /* 蓝移边缘 */
    inset 0 1px 1px 0 rgba(255, 255, 255, 0.8),
    inset 0 -1px 2px 0 rgba(255, 255, 255, 0.5);
  opacity: 0.85;
}

/* 顶部高光弧线 (Caustic Rim Highlight) */
.lg-dock-rim {
  position: absolute;
  top: 2px;
  left: 20%;
  right: 20%;
  height: 1px;
  border-radius: 9999px;
  background: linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.95) 50%, transparent 100%);
  pointer-events: none;
  opacity: 0.9;
}

/* ==========================================================================
   液态滑块胶囊 (Liquid Pill Indicator)
   ========================================================================== */

.lg-pill-track {
  position: absolute;
  top: 4px;
  bottom: 4px;
  left: 4px;
  right: 4px;
  pointer-events: none;
}

.lg-pill {
  position: absolute;
  top: 0;
  bottom: 0;
  width: calc(100% / 5);
  border-radius: 9999px;
  pointer-events: none;
  transform-origin: center center;
  will-change: transform;
  
  /* 液态水滴折射表面材质 */
  background: radial-gradient(120% 120% at 50% -10%,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(255, 255, 255, 0.72) 48%,
    rgba(244, 241, 234, 0.42) 100%);
    
  -webkit-backdrop-filter: blur(10px) saturate(200%) brightness(1.06);
  backdrop-filter: blur(10px) saturate(200%) brightness(1.06);
  
  /* 多重透镜内反射与微投影 */
  box-shadow:
    0 4px 16px -2px rgba(196, 67, 43, 0.16),
    0 2px 8px 0 rgba(19, 27, 35, 0.08),
    inset 0 1px 1px 0 rgba(255, 255, 255, 1),
    inset 0 -2px 6px -1px rgba(196, 67, 43, 0.12),
    inset 1px 0 2px -1px rgba(255, 80, 50, 0.35),
    inset -1px 0 2px -1px rgba(50, 120, 255, 0.35);
    
  border: 0.5px solid rgba(255, 255, 255, 0.9);
}

/* 滑块顶部圆弧高光 */
.lg-pill-gloss {
  position: absolute;
  top: 2px;
  left: 14%;
  right: 14%;
  height: 8px;
  border-radius: 9999px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(255, 255, 255, 0) 100%);
  filter: blur(0.5px);
  pointer-events: none;
}

/* 丹砂红柔光溢彩 */
.lg-pill-glow {
  position: absolute;
  bottom: 3px;
  left: 20%;
  right: 20%;
  height: 10px;
  border-radius: 9999px;
  background: radial-gradient(ellipse at center, rgba(196, 67, 43, 0.22) 0%, transparent 75%);
  pointer-events: none;
}

/* ==========================================================================
   Tab 导航项微动效
   ========================================================================== */

.lg-tab-item {
  position: relative;
  z-index: 3;
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: transform 0.16s cubic-bezier(0.2, 0, 0, 1);
}

.lg-tab-item:active {
  transform: scale(0.92);
}

.lg-tab-icon-wrap {
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.lg-tab-icon {
  width: 22px;
  height: 22px;
  object-fit: contain;
  transition: transform 0.32s cubic-bezier(0.34, 1.56, 0.64, 1), filter 0.2s ease;
}

.lg-tab-item.active .lg-tab-icon {
  transform: translateY(-2px) scale(1.14);
  filter: drop-shadow(0 2px 6px rgba(196, 67, 43, 0.3));
}

.lg-tab-label {
  font-size: 11px;
  line-height: 1;
  margin-top: 3px;
  font-weight: 500;
  color: #8A959E;
  transition: color 0.2s ease, font-weight 0.2s ease, transform 0.2s ease;
}

.lg-tab-item.active .lg-tab-label {
  color: #C4432B;
  font-weight: 700;
  transform: translateY(-0.5px);
}

/* 页面切换淡入淡出 */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.14s ease, transform 0.14s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(4px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* Backdrop Catalog 真实高动态液态微光环境层 */
.dock-ambient-aurora {
  position: fixed;
  bottom: 24px;
  left: 16px;
  right: 16px;
  height: 64px;
  border-radius: 9999px;
  pointer-events: none;
  z-index: 1;
  overflow: hidden;
  opacity: 0.88;
  background: linear-gradient(
    135deg,
    rgba(29, 209, 161, 0.40) 0%,
    rgba(0, 136, 255, 0.50) 40%,
    rgba(108, 92, 231, 0.45) 75%,
    rgba(255, 107, 107, 0.35) 100%
  );
  filter: blur(1px);
}

.dock-ambient-aurora .aurora-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(14px);
  will-change: transform;
}

.dock-ambient-aurora .aurora-1 {
  width: 140px;
  height: 80px;
  top: -15px;
  left: -20px;
  background: radial-gradient(circle, rgba(0, 210, 211, 0.85) 0%, rgba(29, 209, 161, 0) 70%);
}

.dock-ambient-aurora .aurora-2 {
  width: 180px;
  height: 90px;
  top: -20px;
  left: 30%;
  background: radial-gradient(circle, rgba(0, 136, 255, 0.80) 0%, rgba(9, 132, 227, 0) 70%);
}

.dock-ambient-aurora .aurora-3 {
  width: 150px;
  height: 80px;
  bottom: -15px;
  right: -20px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.75) 0%, rgba(255, 107, 107, 0) 70%);
}

.dock-ambient-aurora .aurora-caustic {
  position: absolute;
  top: 1px;
  left: 10%;
  right: 10%;
  height: 2px;
  border-radius: 9999px;
  background: linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.95) 50%, transparent 100%);
  opacity: 0.9;
}
</style>

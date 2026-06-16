<template>
  <!-- 基础网格背景（静态） -->
  <div class="grid-bg"></div>

  <!-- 顶部标题栏：仅静态指示灯（无闪烁） -->
  <header
      class="fixed top-0 left-0 w-full h-28 bg-gradient-to-r from-black/90 via-slate-900/90 to-black/90 border-b-2 border-cyan-400 z-50 flex items-center justify-center overflow-hidden"
  >
    <!-- 功能指示灯（静态圆点，无动画） -->
    <div class="absolute left-6 flex gap-3">
      <div class="w-4 h-4 rounded-full bg-cyan-400 shadow-[0_0_8px_#22d3ee]"></div>
      <div class="w-4 h-4 rounded-full bg-blue-500 shadow-[0_0_8px_#3b82f6]"></div>
    </div>
    <div class="absolute right-6 flex gap-3">
      <div class="w-4 h-4 rounded-full bg-blue-500 shadow-[0_0_8px_#3b82f6]"></div>
      <div class="w-4 h-4 rounded-full bg-cyan-400 shadow-[0_0_8px_#22d3ee]"></div>
    </div>
    <!-- 删除扫描线（title-scan-enhanced 已移除） -->
  </header>

  <!-- 主内容区：核心布局、滚动完全保留，无切换动画 -->
  <div class="fixed top-28 bottom-0 left-0 right-0 overflow-y-auto bg-transparent main-scroll-area">
    <RouterView />
  </div>
</template>

<style scoped>
/* ===================== 1. 基础网格背景（静态，无动画） ===================== */
.grid-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -1;
  background: #020617;
  background-attachment: fixed;
}
.grid-bg::before {
  content: "";
  position: absolute;
  inset: 0;
  background-image:
      linear-gradient(rgba(0, 255, 255, 0.08) 1px, transparent 1px),
      linear-gradient(90deg, rgba(0, 255, 255, 0.08) 1px, transparent 1px);
  background-size: 60px 60px;
  pointer-events: none;
}
/* 移除所有伪元素动画层 */

/* ===================== 2. 扫描线、指示灯动画：已全部删除 ===================== */

/* ===================== 3. 滚动区域 & 滚动条（静态样式） ===================== */
.main-scroll-area {
  scrollbar-width: thin;
  scrollbar-color: #22d3ee #0a1a1f;
  scroll-behavior: auto;
}
.main-scroll-area::-webkit-scrollbar {
  width: 6px;
}
.main-scroll-area::-webkit-scrollbar-track {
  background: #0a1a1f;
}
.main-scroll-area::-webkit-scrollbar-thumb {
  background: #22d3ee;
  border-radius: 3px;
}

/* ===================== 4. 路由切换动画：已移除（无过渡） ===================== */
/* 删除 :deep(.router-view-fade-*) 所有规则，页面切换直接显示 */
</style>

<style>
/* 全局样式：关闭所有冗余特效，适配核显 */
html,
body {
  margin: 0;
  background: #020617 !important;
  font-family: system-ui, monospace;
  cursor: default;
  overflow: hidden;
  scroll-behavior: auto;
  backface-visibility: hidden;
}
</style>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useSystemConfigStore } from './stores/systemConfig'
import { useDehumidifierStore } from '@/stores/useDehumidifier'

const systemConfigStore = useSystemConfigStore()
const dehumidifierStore = useDehumidifierStore()

// 彻底移除鼠标跟随光晕DOM与监听（该特效在UHD620上极易掉帧）
// 原有鼠标移动逻辑全部删除，释放主线程+GPU资源

onMounted(async () => {
  // 核心业务逻辑：WebSocket、配置加载 完全保留
  dehumidifierStore.connectDehumidifierWebSocket()
  try {
    await systemConfigStore.loadConfig()
  } catch (error) {
    console.error('初始化系统配置失败:', error)
  }
})

onBeforeUnmount(() => {
  // 资源释放
  dehumidifierStore.closeDehumidifierWebSocket()
})
</script>

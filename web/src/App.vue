<template>
  <!-- 科技感网格背景（全局装饰） -->
  <div class="grid-bg"></div>
  <!-- 动态极光光晕层 -->
  <div class="aurora-glow"></div>
  <!-- 数据流射线 -->
  <div class="data-stream data-stream-1"></div>
  <div class="data-stream data-stream-2"></div>
  <div class="data-stream data-stream-3"></div>
  <div class="data-stream data-stream-4"></div>
  <!-- 鼠标跟随光晕 -->
  <div class="mouse-glow" ref="mouseGlow"></div>

  <!-- 顶部标题栏：增强科技感 -->
  <header
      class="fixed top-0 left-0 w-full h-28 bg-gradient-to-r from-black/90 via-slate-900/90 to-black/90 backdrop-blur-xl border-b-2 border-cyan-400 shadow-[0_8px_32px_rgba(0,255,255,0.2)] z-50 flex items-center justify-center overflow-hidden"
  >
    <div class="absolute inset-0 pointer-events-none border-b-2 border-cyan-500/60 shadow-[inset_0_-1px_0_rgba(0,255,255,0.5)]"></div>
    <div class="absolute top-0 left-0 w-full h-full bg-[radial-gradient(ellipse_at_center,_rgba(0,255,255,0.15)_0%,_transparent_70%)]"></div>

    <div class="absolute left-6 flex gap-3">
      <div class="w-4 h-4 rounded-full bg-cyan-400 shadow-[0_0_16px_#22d3ee] animate-ping"></div>
      <div class="w-4 h-4 rounded-full bg-blue-500 shadow-[0_0_16px_#3b82f6] animate-pulse"></div>
    </div>
    <div class="absolute right-6 flex gap-3">
      <div class="w-4 h-4 rounded-full bg-blue-500 shadow-[0_0_16px_#3b82f6] animate-pulse"></div>
      <div class="w-4 h-4 rounded-full bg-cyan-400 shadow-[0_0_16px_#22d3ee] animate-ping"></div>
    </div>

    <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-80 h-16 border border-cyan-400/60 rounded-full blur-md animate-pulse"></div>
    <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-20 bg-gradient-to-r from-cyan-500/30 via-blue-500/30 to-cyan-500/30 rounded-full blur-2xl"></div>

    <div class="title-scan-enhanced"></div>
  </header>

  <!-- 主内容区：留出顶部高度 h-28，整体滚动，滚动条可见（科技感样式） -->
  <div class="fixed top-28 bottom-0 left-0 right-0 overflow-y-auto bg-transparent main-scroll-area">
    <RouterView />
  </div>
</template>

<style scoped>
/* ========== 全局网格背景 ========== */
.grid-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -1;
  background: #020617 radial-gradient(circle at 20% 30%, rgba(10, 20, 40, 1) 0%, #020617 100%);
  background-attachment: fixed;
}

.grid-bg::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image:
      linear-gradient(rgba(0, 255, 255, 0.1) 1px, transparent 1px),
      linear-gradient(90deg, rgba(0, 255, 255, 0.1) 1px, transparent 1px);
  background-size: 40px 40px;
  pointer-events: none;
}

.grid-bg::after {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: repeating-linear-gradient(
      0deg,
      rgba(0, 255, 255, 0.03) 0px,
      rgba(0, 255, 255, 0.03) 2px,
      transparent 2px,
      transparent 8px
  );
  pointer-events: none;
}

/* ========== 动态极光光晕 ========== */
.aurora-glow {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -1;
  background:
      radial-gradient(circle at 30% 40%, rgba(0, 200, 255, 0.15), transparent 60%),
      radial-gradient(circle at 70% 80%, rgba(100, 0, 255, 0.15), transparent 60%);
  animation: auroraMove 20s ease-in-out infinite alternate;
  pointer-events: none;
}

@keyframes auroraMove {
  0% { opacity: 0.4; transform: scale(1) translate(0%, 0%); }
  50% { opacity: 0.8; transform: scale(1.1) translate(2%, 1%); }
  100% { opacity: 0.4; transform: scale(1) translate(-1%, -2%); }
}

/* ========== 数据流射线 ========== */
.data-stream {
  position: fixed;
  width: 200px;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(0, 255, 255, 0.6), transparent);
  filter: blur(2px);
  opacity: 0.6;
  pointer-events: none;
  z-index: 0;
}

.data-stream-1 { top: 20%; left: -10%; transform: rotate(25deg); animation: streamMove1 8s linear infinite; }
.data-stream-2 { bottom: 30%; right: -10%; transform: rotate(-30deg); animation: streamMove2 10s linear infinite; }
.data-stream-3 { top: 60%; left: -15%; transform: rotate(-15deg); animation: streamMove3 12s linear infinite; }
.data-stream-4 { bottom: 15%; right: -5%; transform: rotate(40deg); animation: streamMove4 7s linear infinite; }

@keyframes streamMove1 {
  0% { left: -10%; opacity: 0; }
  20% { opacity: 0.8; }
  80% { opacity: 0.8; }
  100% { left: 110%; opacity: 0; }
}
@keyframes streamMove2 {
  0% { right: -10%; opacity: 0; }
  20% { opacity: 0.8; }
  80% { opacity: 0.8; }
  100% { right: 110%; opacity: 0; }
}
@keyframes streamMove3 {
  0% { left: -15%; opacity: 0; }
  20% { opacity: 0.6; }
  80% { opacity: 0.6; }
  100% { left: 115%; opacity: 0; }
}
@keyframes streamMove4 {
  0% { right: -5%; opacity: 0; }
  20% { opacity: 0.7; }
  80% { opacity: 0.7; }
  100% { right: 105%; opacity: 0; }
}

/* ========== 鼠标跟随光晕 ========== */
.mouse-glow {
  position: fixed;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(0, 200, 255, 0.15), transparent 70%);
  pointer-events: none;
  transform: translate(-50%, -50%);
  transition: transform 0.05s linear;
  z-index: 10;
  mix-blend-mode: screen;
}

/* ========== 标题栏扫描线 ========== */
.title-scan-enhanced {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg,
  transparent,
  rgba(0, 255, 255, 0.4),
  rgba(59, 130, 246, 0.6),
  rgba(0, 255, 255, 0.4),
  transparent);
  animation: titleScanEnhanced 2s linear infinite;
  pointer-events: none;
  filter: blur(2px);
}

@keyframes titleScanEnhanced {
  0% { transform: translateX(-100%) skewX(-15deg); opacity: 0; }
  20% { opacity: 1; }
  80% { opacity: 1; }
  100% { transform: translateX(100%) skewX(-15deg); opacity: 0; }
}

/* ========== 标题动画 ========== */
@keyframes titleGlowEnhanced {
  0% { text-shadow: 0 0 5px rgba(34, 211, 238, 0.5), 0 0 15px rgba(59, 130, 246, 0.3); }
  50% { text-shadow: 0 0 25px rgba(34, 211, 238, 1), 0 0 45px rgba(59, 130, 246, 0.8), 0 0 10px white; }
  100% { text-shadow: 0 0 5px rgba(34, 211, 238, 0.5), 0 0 15px rgba(59, 130, 246, 0.3); }
}
.animate-title-glow-enhanced { animation: titleGlowEnhanced 1.8s ease-in-out infinite; }

/* ========== 整体滚动区域（科技感滚动条，明显可见） ========== */
.main-scroll-area {
  scrollbar-width: thin;
  scrollbar-color: #22d3ee #0a1a1f;
}
.main-scroll-area::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}
.main-scroll-area::-webkit-scrollbar-track {
  background: #0a1a1f;
  border-radius: 10px;
}
.main-scroll-area::-webkit-scrollbar-thumb {
  background: #22d3ee;
  border-radius: 10px;
  box-shadow: inset 0 0 3px rgba(0,0,0,0.3);
}
.main-scroll-area::-webkit-scrollbar-thumb:hover {
  background: #0ff;
  box-shadow: 0 0 6px cyan;
}

/* 页面切换淡入效果（预留） */
:deep(.router-view-fade-enter-active),
:deep(.router-view-fade-leave-active) {
  transition: opacity 0.3s ease;
}
:deep(.router-view-fade-enter-from),
:deep(.router-view-fade-leave-to) {
  opacity: 0;
}
</style>

<style>
/* 全局样式 - 防止白屏 */
html,
body {
  margin: 0;
  background: #020617 !important;
  font-family: "Inter", "Segoe UI", system-ui, "Courier New", monospace;
  cursor: default;
  overflow: hidden;
}
html { scroll-behavior: smooth; }
</style>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useSystemConfigStore } from './stores/systemConfig'
const systemConfigStore = useSystemConfigStore()


const mouseGlow = ref<HTMLElement | null>(null)
const handleMouseMove = (e: MouseEvent) => {
  if (mouseGlow.value) {
    mouseGlow.value.style.transform = `translate(${e.clientX}px, ${e.clientY}px) translate(-50%, -50%)`
  }
}

onMounted(async() => {
  window.addEventListener('mousemove', handleMouseMove)
  try {
    await systemConfigStore.loadConfig()
  } catch (error) {
    console.error('初始化系统配置失败:', error)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', handleMouseMove)
})
</script>

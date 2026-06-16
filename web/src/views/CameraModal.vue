<template>
  <div v-if="visible" class="camera-overlay">
    <div class="camera-container" @click.stop>
      <div class="camera-header">
        <div class="header-info">
          <h3>{{ isBorrow ? '领用登记' : '归还登记' }}</h3>
          <div class="timer-badge" :class="{ 'time-warning': cameraSecondsLeft <= 5 }">
            <svg class="timer-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
            <span>{{ formattedTime }}</span>
          </div>
        </div>
        <button class="close-btn" @click="close">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <div class="camera-body">
        <!-- 视频预览区域 -->
        <div class="video-wrapper" :class="{ 'has-photo': capturedImage }">
          <video
              v-if="!capturedImage"
              ref="videoRef"
              class="video-preview"
              autoplay
              playsinline
              muted
              @loadedmetadata="onVideoLoaded"
              @canplay="onVideoCanPlay"
          ></video>
          <img
              v-else
              :src="formatImageUrl(capturedImage)"
              class="captured-preview"
              alt="拍摄的照片"
          />

          <!-- 扫描辅助线 -->
          <div v-if="!capturedImage" class="scan-frame">
            <div class="corner top-left"></div>
            <div class="corner top-right"></div>
            <div class="corner bottom-left"></div>
            <div class="corner bottom-right"></div>
          </div>

          <!-- 相机加载中提示 -->
          <div v-if="!isCameraReady && !capturedImage" class="camera-loading">
            <div class="loading-spinner"></div>
            <span>相机启动中...</span>
          </div>
        </div>

        <!-- 拍照按钮区域 -->
        <div class="camera-actions">
          <button
              v-if="!capturedImage"
              class="capture-btn"
              :class="{ 'btn-disabled': !isCameraReady }"
              :disabled="!isCameraReady"
              @click="capturePhoto"
          >
            <div class="capture-ring">
              <span class="capture-inner"></span>
            </div>
            <span>{{ isCameraReady ? '拍照' : '相机启动中...' }}</span>
          </button>
          <div v-else class="capture-actions">
            <button class="recapture-btn" @click="resetCapture">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
                <line x1="1" y1="1" x2="23" y2="23" />
              </svg>
              <span>重拍</span>
            </button>
            <button class="confirm-btn" @click="confirmPhoto">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <polyline points="20 6 9 17 4 12" />
              </svg>
              <span>确认</span>
            </button>
          </div>
        </div>

        <!-- 提示信息 -->
        <div class="camera-tip">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="16" />
            <line x1="8" y1="12" x2="16" y2="12" />
          </svg>
          <span>请将人脸对准相机，保持光线充足</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onBeforeUnmount, watch, computed, nextTick } from 'vue'
import { useCountdown } from '@/composables/useCountdown'
import {formatImageUrl} from '@/utils/fileUtils'

interface Props {
  visible: boolean
  isBorrow: boolean // true: 领用, false: 归还
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'confirm', imageData: string): void
  (e: 'close'): void
}>()

const videoRef = ref<HTMLVideoElement | null>(null)
const capturedImage = ref<string>('')
const isCameraReady = ref<boolean>(false)
let stream: MediaStream | null = null
let isCameraStarting = ref<boolean>(false)
let checkReadyInterval: number | null = null
// 标记是否正在进行重置相机的操作
let isResettingCamera = ref<boolean>(false)

// 使用通用倒计时（15秒，自动开始，超时自动关闭）
const {
  secondsLeft: cameraSecondsLeft,
  stop: stopTimer,
  reset: resetTimer,
  start: startTimer,
  cleanup: cleanupTimer,
} = useCountdown({
  autoStart: false, // 手动启动，等相机就绪后再启动
  onTimeout: () => {
    // 倒计时结束，自动关闭
    close()
  },
  onTick: (seconds) => {
    if (seconds === 5) {
      console.log('相机倒计时剩余5秒')
    }
  }
})

// 格式化时间显示 MM:SS
const formattedTime = computed(() => {
  const mins = Math.floor(cameraSecondsLeft.value / 60)
  const secs = cameraSecondsLeft.value % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
})

// 启动倒计时（仅在相机就绪且没有拍摄照片且不是重置状态时调用）
function startTimerIfCameraReady() {
  if (isCameraReady.value && !capturedImage.value && !isCameraStarting.value && !isResettingCamera.value) {
    // 确保倒计时是从完整时长开始的
    resetTimer()
    startTimer()
    console.log('倒计时已启动，时长:', cameraSecondsLeft.value)
  }
}

// 停止检查就绪状态
function stopCheckReadyInterval() {
  if (checkReadyInterval) {
    clearInterval(checkReadyInterval)
    checkReadyInterval = null
  }
}

// 重置倒计时状态（清理但不启动）
function resetCountdownState() {
  stopTimer()
  // 重置剩余时间到初始值
  // 由于 useCountdown 的 reset 会重新开始，我们需要手动设置
  // 这里使用 resetTimer 但先不启动
  resetTimer()
  stopTimer() // reset 后会启动，需要立即停止
  console.log('倒计时状态已重置')
}

// 开始检查视频就绪状态
function startCheckReadyStatus() {
  stopCheckReadyInterval()

  checkReadyInterval = window.setInterval(() => {
    if (videoRef.value && !capturedImage.value && !isResettingCamera.value) {
      const video = videoRef.value
      // 检查视频是否真正就绪
      if (video.readyState >= 2 && video.videoWidth > 0 && video.videoHeight > 0) {
        if (!isCameraReady.value) {
          console.log('视频就绪，readyState:', video.readyState)
          isCameraReady.value = true
          startTimerIfCameraReady()
          stopCheckReadyInterval()
        }
      } else if (video.readyState === 0 || video.readyState === 1) {
        // 视频还在加载中
        console.log('视频加载中，readyState:', video.readyState)
      }
    }
  }, 100)
}

// 视频加载完成（摄像头画面可用）
function onVideoLoaded() {
  console.log('onVideoLoaded 触发')
  if (videoRef.value && videoRef.value.videoWidth > 0 && videoRef.value.videoHeight > 0) {
    if (!isCameraReady.value) {
      isCameraReady.value = true
      startTimerIfCameraReady()
    }
    stopCheckReadyInterval()
  } else {
    startCheckReadyStatus()
  }
}

// 视频可以播放时再次确认
function onVideoCanPlay() {
  console.log('onVideoCanPlay 触发')
  if (videoRef.value && videoRef.value.readyState >= 2 && !isCameraReady.value) {
    isCameraReady.value = true
    startTimerIfCameraReady()
    stopCheckReadyInterval()
  }
}

// 检查并更新摄像头就绪状态（用于重拍等场景）
async function updateCameraReadyStatus() {
  return new Promise<void>((resolve) => {
    if (videoRef.value && !capturedImage.value) {
      const video = videoRef.value

      if (video.readyState >= 2 && video.videoWidth > 0 && video.videoHeight > 0) {
        isCameraReady.value = true
        startTimerIfCameraReady()
        stopCheckReadyInterval()
        resolve()
        return
      }

      console.log('视频未就绪，开始等待...')

      if (video.paused) {
        video.play().catch(e => console.log('视频播放失败:', e))
      }

      const checkReady = () => {
        if (video.readyState >= 2 && video.videoWidth > 0 && video.videoHeight > 0) {
          console.log('重拍后视频就绪')
          isCameraReady.value = true
          startTimerIfCameraReady()
          stopCheckReadyInterval()
          resolve()
          return true
        }
        return false
      }

      if (checkReady()) return

      stopCheckReadyInterval()
      checkReadyInterval = window.setInterval(() => {
        if (checkReady()) {
          stopCheckReadyInterval()
        }
      }, 100)

      setTimeout(() => {
        if (!isCameraReady.value) {
          console.warn('视频就绪超时')
          stopCheckReadyInterval()
          resolve()
        }
      }, 5000)
    } else {
      resolve()
    }
  })
}

// 初始化相机
async function initCamera() {
  try {
    // 重置状态
    isCameraReady.value = false
    isCameraStarting.value = false
    isResettingCamera.value = false

    // 停止计时器并重置倒计时（确保从完整时长开始）
    stopTimer()
    resetTimer()
    stopTimer() // 再次停止，因为 reset 可能会自动启动

    stopCheckReadyInterval()

    // 释放之前的流
    if (stream) {
      stream.getTracks().forEach(track => track.stop())
      stream = null
    }

    // 清空视频源
    if (videoRef.value) {
      videoRef.value.srcObject = null
    }

    stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'environment' }
    })

    if (videoRef.value) {
      videoRef.value.srcObject = stream
      startCheckReadyStatus()
    }
  } catch (error) {
    console.error('相机初始化失败:', error)
    alert('无法访问相机，请检查权限设置')
    close()
  }
}

// 拍照
function capturePhoto() {
  if (!isCameraReady.value) {
    console.warn('摄像头未就绪，无法拍照')
    return
  }

  if (!videoRef.value) return

  const video = videoRef.value
  const canvas = document.createElement('canvas')
  canvas.width = video.videoWidth
  canvas.height = video.videoHeight

  const ctx = canvas.getContext('2d')
  if (ctx) {
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
    capturedImage.value = canvas.toDataURL('image/jpeg', 0.8)
    // 拍照后停止倒计时
    stopTimer()
    stopCheckReadyInterval()
  }
}

// 重置拍摄
async function resetCapture() {
  console.log('重置拍摄')

  // 标记正在重置，防止倒计时启动
  isResettingCamera.value = true

  // 停止定时检查
  stopCheckReadyInterval()

  // 重置状态
  capturedImage.value = ''
  isCameraReady.value = false
  isCameraStarting.value = false


  resetTimer()
  startTimer();

  // 等待下一帧，让DOM更新
  await nextTick()

  // 检查视频流是否还存在
  if (!stream || !videoRef.value) {
    console.error('视频流不存在，重新初始化')
    isResettingCamera.value = false
    await initCamera()
    return
  }

  const video = videoRef.value

  if (video.srcObject !== stream) {
    video.srcObject = stream
  }

  if (video.paused) {
    try {
      await video.play()
    } catch (e) {
      console.error('视频播放失败:', e)
    }
  }

  video.load()

  await updateCameraReadyStatus()

  // 重置完成
  isResettingCamera.value = false

  // 如果相机已就绪，启动倒计时
  if (isCameraReady.value && !capturedImage.value) {
    startTimerIfCameraReady()
  }

  console.log('重拍完成，相机就绪状态:', isCameraReady.value)
}

// 确认照片并跳转
function confirmPhoto() {
  if (capturedImage.value) {
    stopTimer()
    stopCheckReadyInterval()
    emit('confirm', capturedImage.value)
    close()
  }
}

// 关闭弹窗
function close() {
  stopTimer()
  stopCheckReadyInterval()
  capturedImage.value = ''
  isCameraReady.value = false
  isCameraStarting.value = false
  isResettingCamera.value = false
  emit('update:visible', false)
  emit('close')
}

// 监听弹窗显示/隐藏
watch(() => props.visible, async (newVal, oldVal) => {
  if (newVal) {
    // 每次打开时重置所有状态
    capturedImage.value = ''
    isCameraReady.value = false
    isCameraStarting.value = false
    isResettingCamera.value = false

    // 停止所有计时器
    stopCheckReadyInterval()

    // 重置倒计时到完整时长（但不启动）
    stopTimer()
    resetTimer()
    stopTimer() // reset 可能会自动启动，需要停止

    // 初始化相机
    await initCamera()
  } else if (oldVal === true) {
    // 关闭时释放相机资源
    stopTimer()
    stopCheckReadyInterval()
    if (stream) {
      stream.getTracks().forEach(track => track.stop())
      stream = null
    }
    if (videoRef.value) {
      videoRef.value.srcObject = null
    }
    cleanupTimer()
    isCameraReady.value = false
    isCameraStarting.value = false
    isResettingCamera.value = false
  }
})

// 组件卸载时释放相机和定时器
onBeforeUnmount(() => {
  if (stream) {
    stream.getTracks().forEach(track => track.stop())
  }
  if (videoRef.value) {
    videoRef.value.srcObject = null
  }
  stopCheckReadyInterval()
  cleanupTimer()
})
</script>

<style lang="css" scoped>
/* 样式保持不变 */
.camera-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
}

.camera-container {
  width: 90%;
  max-width: 500px;
  background: rgba(18, 25, 45, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 40px;
  border: 1px solid rgba(56, 189, 248, 0.4);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(56, 189, 248, 0.1);
  overflow: hidden;
  animation: slideUp 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.camera-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  background: rgba(15, 23, 42, 0.8);
  border-bottom: 1px solid rgba(56, 189, 248, 0.2);
}

.header-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.camera-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  background: linear-gradient(135deg, #e2e8f0, #94a3b8);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: -0.3px;
}

.timer-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(0, 0, 0, 0.5);
  padding: 5px 12px;
  border-radius: 40px;
  font-size: 14px;
  font-weight: 500;
  font-family: 'SF Mono', 'Fira Code', monospace;
  color: #a5f3fc;
  backdrop-filter: blur(4px);
  border: 1px solid rgba(56, 189, 248, 0.3);
  transition: all 0.2s ease;
}

.timer-badge.time-warning {
  color: #f87171;
  border-color: #f87171;
  background: rgba(248, 113, 113, 0.15);
  animation: pulse 0.8s ease infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.timer-icon {
  width: 14px;
  height: 14px;
}

.close-btn {
  background: rgba(255, 255, 255, 0.08);
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 8px;
  border-radius: 32px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  color: white;
  transform: scale(1.05);
}

.camera-body {
  padding: 24px;
}

.video-wrapper {
  width: 100%;
  aspect-ratio: 4 / 3;
  background: #0a0f1a;
  border-radius: 28px;
  overflow: hidden;
  margin-bottom: 24px;
  position: relative;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(56, 189, 248, 0.2);
}

.video-preview,
.captured-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.camera-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba(10, 15, 26, 0.9);
  backdrop-filter: blur(8px);
  color: #94a3b8;
  font-size: 14px;
  z-index: 10;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(56, 189, 248, 0.2);
  border-top-color: #38bdf8;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.scan-frame {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.corner {
  position: absolute;
  width: 24px;
  height: 24px;
  border-color: rgba(56, 189, 248, 0.8);
  border-style: solid;
  border-width: 0;
}

.top-left {
  top: 20px;
  left: 20px;
  border-top-width: 3px;
  border-left-width: 3px;
}

.top-right {
  top: 20px;
  right: 20px;
  border-top-width: 3px;
  border-right-width: 3px;
}

.bottom-left {
  bottom: 20px;
  left: 20px;
  border-bottom-width: 3px;
  border-left-width: 3px;
}

.bottom-right {
  bottom: 20px;
  right: 20px;
  border-bottom-width: 3px;
  border-right-width: 3px;
}

.camera-actions {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.capture-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: white;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.capture-btn.btn-disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

.capture-ring {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.capture-inner {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #22d3ee, #3b82f6);
  transition: all 0.2s ease;
}

.capture-btn:active .capture-ring {
  transform: scale(0.92);
  background: rgba(255, 255, 255, 0.25);
}

.capture-btn:active .capture-inner {
  transform: scale(0.96);
}

.capture-actions {
  display: flex;
  gap: 20px;
  width: 100%;
  justify-content: center;
}

.recapture-btn,
.confirm-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 28px;
  border-radius: 60px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  backdrop-filter: blur(8px);
}

.recapture-btn {
  background: rgba(51, 65, 85, 0.9);
  color: #f1f5f9;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.confirm-btn {
  background: linear-gradient(135deg, #10b981, #059669);
  color: white;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.recapture-btn:active,
.confirm-btn:active {
  transform: scale(0.96);
}

.camera-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
  padding: 10px 16px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 40px;
  backdrop-filter: blur(4px);
}

@media (max-width: 480px) {
  .camera-container {
    width: 92%;
  }

  .camera-body {
    padding: 20px;
  }

  .capture-ring {
    width: 64px;
    height: 64px;
  }

  .capture-inner {
    width: 52px;
    height: 52px;
  }

  .recapture-btn,
  .confirm-btn {
    padding: 10px 22px;
    font-size: 14px;
  }
}
</style>

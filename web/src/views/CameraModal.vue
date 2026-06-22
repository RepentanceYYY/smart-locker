<template>
  <div v-if="visible" class="camera-overlay">
    <div class="camera-container" @click.stop>
      <div class="camera-header">
        <div class="header-info">
          <h3>{{ isBorrow ? '领用登记' : '归还登记' }}</h3>
          <!-- <label class="anti-spoofing-toggle"
            style="margin-left: 15px; font-size: 13px; display: flex; align-items: center; gap: 4px; color: #666;">
            <input type="checkbox" :checked="useAntiSpoofing" disabled />
            <span>开启活体检测(红外双目)</span>
          </label> -->

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
        <div class="video-wrapper" :class="{ 'has-photo': capturedImage }">
          <video v-if="!capturedImage" ref="videoRef" class="video-preview" autoplay playsinline muted
            @loadedmetadata="onVideoLoaded" @canplay="onVideoCanPlay"></video>

          <video v-if="!capturedImage && useAntiSpoofing" ref="irVideoRef" style="display: none;" autoplay playsinline
            muted></video>

          <img v-else :src="formatImageUrl(capturedImage)" class="captured-preview" alt="拍摄的照片" />

          <div v-if="!capturedImage" class="scan-frame">
            <div class="corner top-left"></div>
            <div class="corner top-right"></div>
            <div class="corner bottom-left"></div>
            <div class="corner bottom-right"></div>
          </div>

          <div v-if="!isCameraReady && !capturedImage" class="camera-loading">
            <div class="loading-spinner"></div>
            <span>相机启动中...</span>
          </div>
        </div>

        <div class="camera-actions">
          <div v-if="!capturedImage" class="auto-scan-tip">
            <div v-if="isProcessing" class="scan-loading-effect">正在识别人脸...</div>
            <div v-else-if="isCameraReady" class="scan-searching-effect">正在自动寻焦...</div>
          </div>

          <div v-else class="capture-actions">
            <button class="recapture-btn" @click="resetCapture">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
                <line x1="1" y1="1" x2="23" y2="23" />
              </svg>
              <span>重拍</span>
            </button>
            <button v-if="isShowButton" class="confirm-btn" @click="confirmPhoto">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <polyline points="20 6 9 17 4 12" />
              </svg>
              <span>确认</span>
            </button>
          </div>
        </div>

        <div class="camera-tip" :class="{ 'tip-error': wsErrorMessage }">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="16" />
            <line x1="8" y1="12" x2="16" y2="12" />
          </svg>
          <span>{{ wsErrorMessage || (useAntiSpoofing ? '双目活体检测已开启，请正对相机' : '请将人脸对准相机，保持光线充足') }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onBeforeUnmount, watch, computed, nextTick, onMounted } from 'vue'
import { useCountdown } from '@/composables/useCountdown'
import { formatImageUrl } from '@/utils/fileUtils'
import { useSystemConfigStore } from '@/stores/systemConfig'

interface Props {
  visible: boolean
  isBorrow: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'confirm', imageData: string): void
  (e: 'close'): void
  (e: 'notify', text: string, type: 'info' | 'success' | 'warning'): void
}>()

// --- DOM 引用 ---
const videoRef = ref<HTMLVideoElement | null>(null)
const irVideoRef = ref<HTMLVideoElement | null>(null)

// 系统配置
const systemConfigStore = useSystemConfigStore()

const useAntiSpoofing = computed(() => {
  return Number(systemConfigStore.silentLivenessEnabled) === 1
})
const isCameraReady = ref<boolean>(false)
const isStreaming = ref<boolean>(false)
const capturedImage = ref<string>('')
let isCameraStarting = ref<boolean>(false)
let checkReadyInterval: number | null = null
let isResettingCamera = ref<boolean>(false)
const isShowButton = ref(true)

// ==================== 【核心重构：流控锁与防内存泄漏定时器】 ====================
const isProcessing = ref<boolean>(false)      // 真正涉及核心接口往返的排他主锁
let autoCaptureTimeout: number | null = null   // 严格保有的全局唯一自动抓拍定时器句柄

// --- 摄像头多路流与设备管理 ---
let stream: MediaStream | null = null
let irStream: MediaStream | null = null
const videoDevices = ref<MediaDeviceInfo[]>([])
const selectedRgbId = ref<string>('')
const selectedIrId = ref<string>('')

// --- WebSocket 变量定义 ---
let ws: WebSocket | null = null
const wsUrl = 'ws://localhost:8080/ws/face'
const wsErrorMessage = ref<string>('')

// 倒计时
const {
  secondsLeft: cameraSecondsLeft,
  stop: stopTimer,
  reset: resetTimer,
  start: startTimer,
  cleanup: cleanupTimer,
} = useCountdown({
  autoStart: false,
  onTimeout: () => {
    close()
  },
  onTick: (seconds) => {
    if (seconds === 5) {
      console.log('相机倒计时剩余5秒')
    }
  }
})

const formattedTime = computed(() => {
  const mins = Math.floor(cameraSecondsLeft.value / 60)
  const secs = cameraSecondsLeft.value % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
})

/**
 * 设备检测与分类
 */
const loadDeviceList = async () => {
  try {
    const tempStream = await navigator.mediaDevices.getUserMedia({ video: true })
    tempStream.getTracks().forEach(track => track.stop())

    const devices = await navigator.mediaDevices.enumerateDevices()
    const allCameras = devices.filter(device => device.kind === 'videoinput')
    videoDevices.value = allCameras

    if (allCameras.length === 0) {
      console.warn('未检测到任何摄像头设备')
      return
    }

    selectedRgbId.value = ''
    selectedIrId.value = ''

    if (!useAntiSpoofing.value) {
      selectedRgbId.value = allCameras[0].deviceId
      return
    }

    const bothPool: any = []
    const irPool: any = []
    const rgbPool: any = []

    allCameras.forEach(device => {
      const label = device.label.toLowerCase()
      const isIrWord = label.includes('ir') || label.includes('infra') || label.includes('红外') || label.includes('850') || label.includes('940')
      const isRgbWord = label.includes('rgb') || label.includes('color') || label.includes('visible') || label.includes('彩色')

      if (isIrWord && isRgbWord) {
        bothPool.push(device)
      } else if (isIrWord) {
        irPool.push(device)
      } else if (isRgbWord) {
        rgbPool.push(device)
      }
    })

    if (bothPool.length > 0) {
      selectedRgbId.value = bothPool[0].deviceId
      selectedIrId.value = bothPool[1] ? bothPool[1].deviceId : bothPool[0].deviceId
    }

    if (!selectedRgbId.value && rgbPool.length > 0) selectedRgbId.value = rgbPool[0].deviceId
    if (!selectedIrId.value && irPool.length > 0) selectedIrId.value = irPool[0].deviceId

    if (!selectedRgbId.value && !selectedIrId.value) {
      if (allCameras[0]) selectedRgbId.value = allCameras[0].deviceId
      if (allCameras[1]) selectedIrId.value = allCameras[1].deviceId
    } else {
      if (!selectedRgbId.value) {
        const remaining = allCameras.find(d => d.deviceId !== selectedIrId.value)
        if (remaining) selectedRgbId.value = remaining.deviceId
      }
      if (!selectedIrId.value) {
        const remaining = allCameras.find(d => d.deviceId !== selectedRgbId.value)
        if (remaining) selectedRgbId.value = remaining.deviceId
      }
    }
    console.log(`设备分流结果 -> RGB ID: ${selectedRgbId.value}, IR ID: ${selectedIrId.value}`)
  } catch (err) {
    console.warn('获取多模摄像头设备列表失败:', err)
  }
}

/**
 * WebSocket 初始化与业务响应控制
 */
function initWebSocket() {
  if (ws) closeWebSocket()
  console.log('正在连接 WebSocket...', wsUrl)
  ws = new WebSocket(wsUrl)
  ws.onopen = () => console.log('WebSocket 连接成功')
  ws.onerror = (error) => console.error('WebSocket 发生错误:', error)
  ws.onclose = (event) => console.log('WebSocket 连接已关闭', event.reason)
  ws.onmessage = (event) => {
    try {
      const response = JSON.parse(event.data)
      console.log('收到 WebSocket 服务端响应:', response)

      if (response.code === 200) {
        console.log('人脸识别成功')
        wsErrorMessage.value = ''
        isShowButton.value = true

        // 1. 立刻切断、释放所有前端摄像头流
        stopLiveStreaming()

        // 2.  修复图片展示逻辑
        if (response.data) {
          const imgData = response.data.trim()

          if (imgData.startsWith('/uploads') || imgData.startsWith('http')) {
            capturedImage.value = imgData
          }
        }
        // 3. 释放锁
        isProcessing.value = false
      } else if (response.code === 404) {
        console.warn('未识别到人脸，准备重新自动捕获')
        wsErrorMessage.value = response.message || '未检测到清晰人脸，请微调位置并正对镜头...'

        // 失败：2秒控锁闭环，释放锁，并重新拉起下一次自动抓拍
        isProcessing.value = false
        triggerAutoCaptureDelay()
      } else {
        // 其他非 200/404 致命异常处理
        console.error('严重异常错误:', response.message)
        wsErrorMessage.value = response.message || '人脸识别失败'
        isShowButton.value = false
        isProcessing.value = false

        stopLiveStreaming()
        emit('notify', wsErrorMessage.value, 'warning')
        setTimeout(() => { close() }, 1500)
      }
    } catch (e) {
      console.error('解析消息失败:', e, event.data)
      wsErrorMessage.value = '人脸识别服务异常'
      isProcessing.value = false
      stopLiveStreaming()
    }
  }
}
function closeWebSocket() {
  if (ws) {
    ws.close()
    ws = null
    console.log('WebSocket 已主动断连')
  }
}

// 解决 1：封装统一安全的延迟捕获注册方法
function triggerAutoCaptureDelay() {
  clearAutoCaptureTimeout() // 每次注册新定时器前，无条件清除老旧定时器

  // 严格的安全过滤：处于正常状态流、未拍下照片、未被锁、且弹窗确实可见
  if (isCameraReady.value && !capturedImage.value && !isProcessing.value && props.visible) {
    autoCaptureTimeout = window.setTimeout(() => {
      autoCaptureFrame()
    }, 2000)
  }
}

// 解决 5：资源彻底清理方法
function clearAutoCaptureTimeout() {
  if (autoCaptureTimeout) {
    clearTimeout(autoCaptureTimeout)
    autoCaptureTimeout = null
  }
}

// 启动计时器
function startTimerIfCameraReady() {
  if (isCameraReady.value && !capturedImage.value && !isCameraStarting.value && !isResettingCamera.value) {
    resetTimer()
    startTimer()
    console.log('倒计时已启动')

    // 基础流就绪后，激活首次自动抓拍
    triggerAutoCaptureDelay()
  }
}

function stopCheckReadyInterval() {
  if (checkReadyInterval) {
    clearInterval(checkReadyInterval)
    checkReadyInterval = null
  }
}

// 检查多路媒体流就绪状态
function startCheckReadyStatus() {
  stopCheckReadyInterval()
  checkReadyInterval = window.setInterval(() => {
    if (videoRef.value && !capturedImage.value && !isResettingCamera.value) {
      const video = videoRef.value
      const irVideo = irVideoRef.value

      let isRgbReady = video.readyState >= 2 && video.videoWidth > 0 && video.videoHeight > 0
      let isIrReady = !useAntiSpoofing.value || (irVideo && irVideo.readyState >= 2 && irVideo.videoWidth > 0 && irVideo.videoHeight > 0)

      if (isRgbReady && isIrReady) {
        if (!isCameraReady.value) {
          console.log('所有请求的摄像头流均已就绪')
          isCameraReady.value = true
          isStreaming.value = true
          startTimerIfCameraReady()
          stopCheckReadyInterval()
        }
      }
    }
  }, 100)
}

function onVideoLoaded() { startCheckReadyStatus() }
function onVideoCanPlay() { startCheckReadyStatus() }

// --- 初始化单路/双路相机 ---
async function initCamera() {
  try {
    isCameraReady.value = false
    isCameraStarting.value = true
    isResettingCamera.value = false
    isStreaming.value = false
    isProcessing.value = false

    stopTimer()
    resetTimer()
    stopCheckReadyInterval()
    clearAutoCaptureTimeout()

    // 彻底释放旧的流媒体通道
    releaseTracks()

    await loadDeviceList()

    if (!selectedRgbId.value) {
      throw new Error('没有可用的可见光摄像头 ID')
    }

    const rgbConstraints: MediaStreamConstraints = {
      video: {
        deviceId: { exact: selectedRgbId.value },
        width: { ideal: 640 },
        height: { ideal: 480 }
      }
    }
    stream = await navigator.mediaDevices.getUserMedia(rgbConstraints)
    if (videoRef.value) videoRef.value.srcObject = stream

    if (useAntiSpoofing.value && selectedIrId.value && selectedIrId.value !== selectedRgbId.value) {
      try {
        const irConstraints: MediaStreamConstraints = {
          video: {
            deviceId: { exact: selectedIrId.value },
            width: { ideal: 640 },
            height: { ideal: 480 }
          }
        }
        irStream = await navigator.mediaDevices.getUserMedia(irConstraints)
        await nextTick()
        if (irVideoRef.value) irVideoRef.value.srcObject = irStream
        console.log('红外 IR 隐藏流成功绑定')
      } catch (irErr) {
        console.error('红外镜头打开失败:', irErr)
      }
    }

    isCameraStarting.value = false
    startCheckReadyStatus()
  } catch (error) {
    isCameraStarting.value = false
    console.error('相机工作流初始化失败:', error)
    emit('notify', '请检查设备连接和软件权限允许', 'warning')
    close()
  }
}

// 提取 Canvas 帧的 Base64
function getFrameBase64(videoElement: HTMLVideoElement | null): string | null {
  if (!videoElement || videoElement.readyState < 2 || videoElement.videoWidth === 0) return null

  const canvas = document.createElement('canvas')
  //  直接使用摄像头输出的真实原始宽高
  canvas.width = videoElement.videoWidth
  canvas.height = videoElement.videoHeight

  const ctx = canvas.getContext('2d')
  if (!ctx) return null

  //  原封不动全面绘制，不做任何裁剪
  ctx.drawImage(videoElement, 0, 0, canvas.width, canvas.height)

  return canvas.toDataURL('image/jpeg', 0.8)
}

/**
 *  解决 6：并发抓拍边缘问题优化
 */
const autoCaptureFrame = () => {
  // 严格执行防并发二次校验
  if (isProcessing.value || !isCameraReady.value || !videoRef.value || capturedImage.value) {
    return
  }

  // 挂载发送状态排他锁
  isProcessing.value = true

  // 1. 抓拍可见光
  const rgbBase64 = getFrameBase64(videoRef.value)
  if (!rgbBase64) {
    isProcessing.value = false // 捕获失败立刻释放排他锁
    triggerAutoCaptureDelay()  // 重新排队挂载下一次尝试
    return
  }

  // 2. 双目活体红外流抓拍
  let irBase64: string | null = null
  if (useAntiSpoofing.value && irVideoRef.value) {
    irBase64 = getFrameBase64(irVideoRef.value)
  }

  // 发送消息到后端
  if (ws && ws.readyState === WebSocket.OPEN) {
    const payload = {
      action: "detectFace",
      requestId: "10086",
      data: {
        rgbBase64,
        irBase64,
        silentLivenessEnabled: useAntiSpoofing.value
      }
    }
    ws.send(JSON.stringify(payload))
    console.log('自动捕获人脸完成，已向后端发送报文')
  } else {
    isProcessing.value = false
    console.warn('WebSocket 未处于开启状态，释放锁并取消本次请求')
  }
}

/**
 * 提取抽离：人脸识别成功或发生严重异常时，彻底截断释放所有的视频流和后台轮询
 */
function stopLiveStreaming() {
  stopTimer()
  stopCheckReadyInterval()
  clearAutoCaptureTimeout()
  releaseTracks()

  isCameraReady.value = false
  isStreaming.value = false
}

// 释放媒体流 Tracks
function releaseTracks() {
  if (stream) { stream.getTracks().forEach(track => track.stop()); stream = null; }
  if (irStream) { irStream.getTracks().forEach(track => track.stop()); irStream = null; }
  if (videoRef.value) videoRef.value.srcObject = null
  if (irVideoRef.value) irVideoRef.value.srcObject = null
}

// 重置拍摄
async function resetCapture() {
  isResettingCamera.value = true
  isProcessing.value = false
  stopCheckReadyInterval()
  clearAutoCaptureTimeout()

  capturedImage.value = ''
  isCameraReady.value = false
  isCameraStarting.value = false
  isStreaming.value = false
  isShowButton.value = true
  wsErrorMessage.value = ''

  resetTimer()
  startTimer()

  await nextTick()
  await initCamera()
  isResettingCamera.value = false
}

function confirmPhoto() {
  if (capturedImage.value) {
    stopTimer()
    stopCheckReadyInterval()
    clearAutoCaptureTimeout()
    emit('confirm', capturedImage.value)
    close()
  }
}

function close() {
  stopTimer()
  stopCheckReadyInterval()
  clearAutoCaptureTimeout()
  closeWebSocket()
  releaseTracks()

  capturedImage.value = ''
  isCameraReady.value = false
  isCameraStarting.value = false
  isResettingCamera.value = false
  isStreaming.value = false
  isProcessing.value = false
  wsErrorMessage.value = ''
  emit('update:visible', false)
  emit('close')
}

// 监听弹窗打开与关闭
watch(() => props.visible, async (newVal, oldVal) => {
  if (newVal) {
    capturedImage.value = ''
    isCameraReady.value = false
    isCameraStarting.value = false
    isResettingCamera.value = false
    isProcessing.value = false
    stopCheckReadyInterval()
    clearAutoCaptureTimeout()

    stopTimer()
    resetTimer()

    initWebSocket()
    await initCamera()
  } else if (oldVal === true) {
    stopTimer()
    stopCheckReadyInterval()
    clearAutoCaptureTimeout()
    closeWebSocket()
    releaseTracks()

    cleanupTimer()
    isCameraReady.value = false
    isStreaming.value = false
    isProcessing.value = false
  }
})

onMounted(() => {
  loadDeviceList()
})

onBeforeUnmount(() => {
  releaseTracks()
  stopCheckReadyInterval()
  clearAutoCaptureTimeout()
  cleanupTimer()
  closeWebSocket()
})
</script>

<style lang="css" scoped>
/* 原有样式保持不变 */
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

  0%,
  100% {
    opacity: 1;
  }

  50% {
    opacity: 0.7;
  }
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
  to {
    transform: rotate(360deg);
  }
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
  min-height: 48px;
  /* 维持操作栏基础高度 */
}

/* 新增：自动检测时的动效提示样式 */
.auto-scan-tip {
  font-size: 15px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
}

.scan-searching-effect {
  color: #38bdf8;
}

.scan-loading-effect {
  color: #10b981;
  animation: pulse 1s ease infinite;
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
  transition: all 0.3s ease;
}

.camera-tip.tip-error {
  color: #f87171;
  background: rgba(248, 113, 113, 0.15);
  border: 1px solid rgba(248, 113, 113, 0.3);
  animation: shake 0.5s ease;
}

@keyframes shake {

  0%,
  100% {
    transform: translateX(0);
  }

  25% {
    transform: translateX(-5px);
  }

  75% {
    transform: translateX(5px);
  }
}

@media (max-width: 480px) {
  .camera-container {
    width: 92%;
  }

  .camera-body {
    padding: 20px;
  }

  .recapture-btn,
  .confirm-btn {
    padding: 10px 22px;
    font-size: 14px;
  }
}
</style>
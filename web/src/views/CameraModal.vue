<template>
  <div v-if="visible" class="camera-overlay">
    <div class="camera-container" @click.stop>
      <div class="camera-header">
        <div class="header-info">
          <h3>{{ isBorrow ? '领用登记 ' : '归还登记 ' }}</h3>

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

          <video v-if="!capturedImage && useAntiSpoofing" ref="irVideoRef" style="display: none;" autoplay playsinline muted></video>

          <img v-else :src="capturedImage.startsWith('data:') ? capturedImage : formatImageUrl(capturedImage)"
            class="captured-preview" alt="人脸识别照片" />

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
            <div v-else class="scan-searching-effect">正在自动寻焦...</div>
          </div>
          <div v-else class="capture-actions">
            <button class="confirm-btn" disabled>
              <span>识别完成</span>
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
  (e: 'faceRecognized', imageUrl: string): void
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

const isProcessing = ref<boolean>(false)      // 接口往返的排他主锁
let autoCaptureTimeout: number | null = null   // 全局唯一自动抓拍定时器句柄

// --- 摄像头多路流与设备管理 ---
let stream: MediaStream | null = null
let irStream: MediaStream | null = null
const videoDevices = ref<MediaDeviceInfo[]>([])
const selectedRgbId = ref<string>('')
const selectedIrId = ref<string>('')

// --- WebSocket 变量定义 ---
let ws: WebSocket | null = null
const wsUrl = `${import.meta.env.VITE_WS_BASE_URL}/face`
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

    if (allCameras.length === 0) return

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

      if (isIrWord && isRgbWord) bothPool.push(device)
      else if (isIrWord) irPool.push(device)
      else if (isRgbWord) rgbPool.push(device)
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
  ws = new WebSocket(wsUrl)
  ws.onopen = () => console.log('WebSocket 连接成功')
  ws.onerror = (error) => console.error('WebSocket 发生错误:', error)
  ws.onclose = (event) => console.log('WebSocket 连接已关闭', event.reason)
  ws.onmessage = (event) => {
    try {
      const response = JSON.parse(event.data)
      console.log('收到 WebSocket 服务端响应:', response)

      if (response.code === 200) {
        wsErrorMessage.value = ''
        stopLiveStreaming()
      
        // 修复图片展示逻辑:只在返回标准路径或完整链接时渲染
        let faceImageUrl = ''
        if (response.data) {
          const imgData = response.data.trim()
          if (imgData.startsWith('/uploads') || imgData.startsWith('http')) {
            faceImageUrl = imgData
            capturedImage.value = faceImageUrl
          }
        }

        isProcessing.value = false
        emit('faceRecognized', faceImageUrl)
        emit('confirm', faceImageUrl)
        close()
      } else {
        //  优化 2：无论是 404（未匹配）还是其他代码，都不断开，继续重试
        console.warn(`业务未通过(code: ${response.code}): ${response.message}，保持连接继续自动抓拍`)
        wsErrorMessage.value = response.message || '未检测到清晰人脸，请微调位置并正对镜头...'
        isProcessing.value = false
        triggerAutoCaptureDelay()
      }
    } catch (e) {
      wsErrorMessage.value = '人脸识别服务异常'
      isProcessing.value = false
      triggerAutoCaptureDelay()
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
  if (autoCaptureTimeout) { clearTimeout(autoCaptureTimeout); autoCaptureTimeout = null; }
}

// 启动计时器
function startTimerIfCameraReady() {
  if (isCameraReady.value && !capturedImage.value && !isCameraStarting.value && !isResettingCamera.value) {
    resetTimer()
    startTimer()

    // 基础流就绪后，激活首次自动抓拍
    triggerAutoCaptureDelay()
  }
}

function stopCheckReadyInterval() {
  if (checkReadyInterval) { clearInterval(checkReadyInterval); checkReadyInterval = null; }
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
    releaseTracks()

    await loadDeviceList()

    //  优化1：精细化校验可见光摄像头连接
    if (!selectedRgbId.value) {
      wsErrorMessage.value = '请检查可见光设备，未检测到有效可见光摄像头'
      emit('notify', wsErrorMessage.value, 'warning')
      isCameraStarting.value = false
      return
    }

    const rgbConstraints: MediaStreamConstraints = {
      video: { deviceId: { exact: selectedRgbId.value }, width: { ideal: 640 }, height: { ideal: 480 } }
    }

    try {
      stream = await navigator.mediaDevices.getUserMedia(rgbConstraints)
      if (videoRef.value) videoRef.value.srcObject = stream
    } catch (rgbErr) {
      console.error('可见光流捕获失败:', rgbErr)
      wsErrorMessage.value = '请检查可见光设备，摄像头流开启被拒绝或占用'
      emit('notify', wsErrorMessage.value, 'warning')
      isCameraStarting.value = false
      return
    }

    // 优化 1：精细化校验红外双目摄像头连接
    if (useAntiSpoofing.value) {
      if (!selectedIrId.value || selectedIrId.value === selectedRgbId.value) {
        wsErrorMessage.value = '请检查红外设备，未检测到独立的红外双目摄像头'
        emit('notify', wsErrorMessage.value, 'warning')
        releaseTracks()
        isCameraStarting.value = false
        return
      }

      const irConstraints: MediaStreamConstraints = {
        video: { deviceId: { exact: selectedIrId.value }, width: { ideal: 640 }, height: { ideal: 480 } }
      }

      try {
        irStream = await navigator.mediaDevices.getUserMedia(irConstraints)
        await nextTick()
        if (irVideoRef.value) irVideoRef.value.srcObject = irStream
        console.log('红外 IR 隐藏流成功绑定')
      } catch (irErr) {
        console.error('红外镜头打开失败:', irErr)
        wsErrorMessage.value = '请检查红外设备，红外摄像头流开启失败'
        emit('notify', wsErrorMessage.value, 'warning')
        // 发生红外错误，及时释放已开启的可见光
        releaseTracks()
        isCameraStarting.value = false
        return
      }
    }

    isCameraStarting.value = false
    startCheckReadyStatus()
  } catch (error: any) {
    isCameraStarting.value = false
    console.error('相机工作流初始化失败:', error)
    wsErrorMessage.value = '请检查设备连接和软件权限允许'
    emit('notify', wsErrorMessage.value, 'warning')
  }
}

// 提取 Canvas 帧的 Base64
function getFrameBase64(videoElement: HTMLVideoElement | null): string | null {
  if (!videoElement || videoElement.readyState < 2 || videoElement.videoWidth === 0) return null
  const canvas = document.createElement('canvas')
  canvas.width = videoElement.videoWidth
  canvas.height = videoElement.videoHeight
  const ctx = canvas.getContext('2d')
  if (!ctx) return null
  ctx.drawImage(videoElement, 0, 0, canvas.width, canvas.height)
  return canvas.toDataURL('image/jpeg', 0.8)
}

/**
 *  解决 6：并发抓拍边缘问题优化
 */
const autoCaptureFrame = () => {
  if (isProcessing.value || !isCameraReady.value || !videoRef.value || capturedImage.value) return

  isProcessing.value = true
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
    close()
    cleanupTimer()
  }
})

onMounted(() => { loadDeviceList() })
onBeforeUnmount(() => { close(); cleanupTimer(); })
</script>

<style scoped>
/* 引用通用样式 */
.camera-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0, 0, 0, 0.85); backdrop-filter: blur(12px); display: flex; align-items: center; justify-content: center; z-index: 10000; }
.camera-container { width: 90%; max-width: 500px; background: rgba(18, 25, 45, 0.95); backdrop-filter: blur(20px); border-radius: 40px; border: 1px solid rgba(56, 189, 248, 0.4); box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5); overflow: hidden; animation: slideUp 0.35s cubic-bezier(0.16, 1, 0.3, 1); }
@keyframes slideUp { from { opacity: 0; transform: translateY(30px) scale(0.96); } to { opacity: 1; transform: translateY(0) scale(1); } }
.camera-header { display: flex; justify-content: space-between; align-items: center; padding: 18px 24px; background: rgba(15, 23, 42, 0.8); border-bottom: 1px solid rgba(56, 189, 248, 0.2); }
.header-info { display: flex; align-items: center; gap: 16px; }
.camera-header h3 { margin: 0; font-size: 20px; font-weight: 600; background: linear-gradient(135deg, #e2e8f0, #94a3b8); background-clip: text; -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.timer-badge { display: flex; align-items: center; gap: 6px; background: rgba(0, 0, 0, 0.5); padding: 5px 12px; border-radius: 40px; font-size: 14px; color: #a5f3fc; border: 1px solid rgba(56, 189, 248, 0.3); }
.timer-badge.time-warning { color: #f87171; border-color: #f87171; background: rgba(248, 113, 113, 0.15); animation: pulse 0.8s ease infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.7; } }
.timer-icon { width: 14px; height: 14px; }
.close-btn { background: rgba(255, 255, 255, 0.08); border: none; color: #94a3b8; cursor: pointer; padding: 8px; border-radius: 32px; width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; }
.camera-body { padding: 24px; }
.video-wrapper { width: 100%; aspect-ratio: 4 / 3; background: #0a0f1a; border-radius: 28px; overflow: hidden; margin-bottom: 24px; position: relative; border: 1px solid rgba(56, 189, 248, 0.2); }
.video-preview, .captured-preview { width: 100%; height: 100%; object-fit: cover; }
.camera-loading { position: absolute; top: 0; left: 0; right: 0; bottom: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; background: rgba(10, 15, 26, 0.9); backdrop-filter: blur(8px); color: #94a3b8; }
.loading-spinner { width: 32px; height: 32px; border: 3px solid rgba(56, 189, 248, 0.2); border-top-color: #38bdf8; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.scan-frame { position: absolute; top: 0; left: 0; right: 0; bottom: 0; pointer-events: none; }
.corner { position: absolute; width: 24px; height: 24px; border-color: rgba(56, 189, 248, 0.8); border-style: solid; border-width: 0; }
.top-left { top: 20px; left: 20px; border-top-width: 3px; border-left-width: 3px; }
.top-right { top: 20px; right: 20px; border-top-width: 3px; border-right-width: 3px; }
.bottom-left { bottom: 20px; left: 20px; border-bottom-width: 3px; border-left-width: 3px; }
.bottom-right { bottom: 20px; right: 20px; border-bottom-width: 3px; border-right-width: 3px; }
.camera-actions { display: flex; justify-content: center; margin-bottom: 20px; min-height: 48px; }
.auto-scan-tip { font-size: 15px; font-weight: 500; display: flex; align-items: center; justify-content: center; }
.scan-searching-effect { color: #38bdf8; }
.scan-loading-effect { color: #10b981; animation: pulse 1s ease infinite; }
.confirm-btn { display: flex; align-items: center; justify-content: center; padding: 12px 28px; border-radius: 60px; font-size: 15px; font-weight: 600; border: none; background: linear-gradient(135deg, #10b981, #059669); color: white; }
.camera-tip { display: flex; align-items: center; justify-content: center; gap: 8px; font-size: 12px; color: #94a3b8; padding: 10px 16px; background: rgba(0, 0, 0, 0.3); border-radius: 40px; }
.camera-tip.tip-error { color: #f87171; background: rgba(248, 113, 113, 0.15); border: 1px solid rgba(248, 113, 113, 0.3); animation: shake 0.5s ease; }
@keyframes shake { 0%, 100% { transform: translateX(0); } 25% { transform: translateX(-5px); } 75% { transform: translateX(5px); } }
</style>
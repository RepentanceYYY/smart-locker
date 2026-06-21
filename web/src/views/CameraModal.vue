<template>
  <div v-if="visible" class="camera-overlay">
    <div class="camera-container" @click.stop>
      <div class="camera-header">
        <div class="header-info">
          <h3>{{ isBorrow ? '领用登记' : '归还登记' }}</h3>
          
          <label class="anti-spoofing-toggle" style="margin-left: 15px; font-size: 13px; display: flex; align-items: center; gap: 4px; cursor: pointer; color: #666;">
            <input type="checkbox" v-model="useAntiSpoofing" :disabled="isStreaming" />
            <span>开启活体检测(红外双目)</span>
          </label>

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
          
          <video
            v-if="!capturedImage && useAntiSpoofing"
            ref="irVideoRef"
            style="display: none;"
            autoplay
            playsinline
            muted
          ></video>

          <img
            v-else
            :src="formatImageUrl(capturedImage)"
            class="captured-preview"
            alt="拍摄的照片"
          />

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

        <div class="camera-tip">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="16" />
            <line x1="8" y1="12" x2="16" y2="12" />
          </svg>
          <span>{{ useAntiSpoofing ? '双目活体检测已开启，请正对相机' : '请将人脸对准相机，保持光线充足' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onBeforeUnmount, watch, computed, nextTick, onMounted } from 'vue'
import { useCountdown } from '@/composables/useCountdown'
import { formatImageUrl } from '@/utils/fileUtils'

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

// --- DOM 引用 ---
const videoRef = ref<HTMLVideoElement | null>(null)
const irVideoRef = ref<HTMLVideoElement | null>(null) // 红外隐藏预览引用

// --- 控制变量与状态 ---
const useAntiSpoofing = ref<boolean>(true) // ⭐ 是否启动活体检测变量（默认开启，可供你随业务控制）
const isCameraReady = ref<boolean>(false)
const isStreaming = ref<boolean>(false)
const capturedImage = ref<string>('')
let isCameraStarting = ref<boolean>(false)
let checkReadyInterval: number | null = null
let isResettingCamera = ref<boolean>(false)

// --- 摄像头多路流与设备管理 ---
let stream: MediaStream | null = null   // RGB 视频流
let irStream: MediaStream | null = null // IR 视频流
const videoDevices = ref<MediaDeviceInfo[]>([])
const selectedRgbId = ref<string>('')
const selectedIrId = ref<string>('')

// --- WebSocket 变量定义 ---
let ws: WebSocket | null = null
const wsUrl = 'ws://localhost:8081/ws'

// 使用通用倒计时
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

// 格式化时间显示
const formattedTime = computed(() => {
  const mins = Math.floor(cameraSecondsLeft.value / 60)
  const secs = cameraSecondsLeft.value % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
})

/**
 * 设备检测与分类检测 (引入优先级策略)
 */
const loadDeviceList = async () => {
  try {
    // 唤起权限，用完立刻关闭
    const tempStream = await navigator.mediaDevices.getUserMedia({ video: true })
    tempStream.getTracks().forEach(track => track.stop())

    const devices = await navigator.mediaDevices.enumerateDevices()
    // 过滤出所有摄像头
    const allCameras = devices.filter(device => device.kind === 'videoinput')
    videoDevices.value = allCameras
    
    if (allCameras.length === 0) {
      console.warn('未检测到任何摄像头设备')
      return
    }

    // 重置选择
    selectedRgbId.value = ''
    selectedIrId.value = ''

    // 分类阶段：根据特征显性归类
    const bothPool:any = [] // 同时带 IR 和 RGB 特征的设备
    const irPool:any = []   // 纯 IR 特征的设备
    const rgbPool:any = []  // 纯 RGB 特征的设备

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

    // ==========================================
    // 选拔阶段：按优先级填充 selectedRgbId 和 selectedIrId
    // ==========================================
    
    // 【最高优先级】优先从“同时具备 IR 和 RGB”的池子里挑（比如某些高质量双目融合摄像头）
    // 如果存在多个这种设备，我们把第一个给 RGB，第二个给 IR（或者视业务而定）
    if (bothPool.length > 0) {
      selectedRgbId.value = bothPool[0].deviceId
      if (bothPool[1]) {
        selectedIrId.value = bothPool[1].deviceId
      } else {
        // 如果只有一台双目一体机，它可能在同一个 ID 下，也可能需要和常规池配合，先占个位
        selectedIrId.value = bothPool[0].deviceId 
      }
    }

    // 【常规优先级】如果刚才最高优先级没填满，用单模精准池补充
    if (!selectedRgbId.value && rgbPool.length > 0) {
      selectedRgbId.value = rgbPool[0].deviceId
    }
    if (!selectedIrId.value && irPool.length > 0) {
      selectedIrId.value = irPool[0].deviceId
    }

    // ==========================================
    // 3. 兜底阶段：物理索引盲切与交叉补偿
    // ==========================================
    // 如果两路都空（意味着所有摄像头的 Label 都没有任何特征词，比如都叫 "Camera"）
    if (!selectedRgbId.value && !selectedIrId.value) {
      if (allCameras[0]) selectedRgbId.value = allCameras[0].deviceId
      if (allCameras[1]) {
        selectedIrId.value = allCameras[1].deviceId
      } else if (allCameras[0]) {
        selectedIrId.value = allCameras[0].deviceId
      }
    } else {
      // 交叉补偿：如果只匹配到了其中一个，把剩下没被选中的物理设备分给另一个空的
      if (!selectedRgbId.value) {
        const remaining = allCameras.find(d => d.deviceId !== selectedIrId.value)
        if (remaining) selectedRgbId.value = remaining.deviceId
      }
      if (!selectedIrId.value) {
        const remaining = allCameras.find(d => d.deviceId !== selectedRgbId.value)
        if (remaining) selectedIrId.value = remaining.deviceId
      }
    }

    console.log(`设备分流结果 -> RGB ID: ${selectedRgbId.value}, IR ID: ${selectedIrId.value}`)
  } catch (err) {
    console.warn('获取多模摄像头设备列表失败:', err)
  }
}

// --- WebSocket 初始化与断开 ---
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
        console.log('人脸识别/注册成功，文件路径:', response.data)
      } else {
        console.error('业务处理失败:', response.message)
      }
    } catch (e) {
      console.error('解析 WebSocket 消息失败:', e, event.data)
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

// 启动计时器判断
function startTimerIfCameraReady() {
  if (isCameraReady.value && !capturedImage.value && !isCameraStarting.value && !isResettingCamera.value) {
    resetTimer()
    startTimer()
    console.log('倒计时已启动，时长:', cameraSecondsLeft.value)
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
      
      // 基础验证：RGB 必须就绪
      let isRgbReady = video.readyState >= 2 && video.videoWidth > 0 && video.videoHeight > 0
      // 进阶验证：如果开启活体，IR 也必须就绪
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

function onVideoLoaded() {
  startCheckReadyStatus()
}

function onVideoCanPlay() {
  startCheckReadyStatus()
}

async function updateCameraReadyStatus() {
  return new Promise<void>((resolve) => {
    startCheckReadyStatus()
    setTimeout(() => {
      resolve()
    }, 1500)
  })
}

// --- 初始化单路/双路相机（增加严格约束与降级） ---
async function initCamera() {
  try {
    isCameraReady.value = false
    isCameraStarting.value = true // 标记正在启动中
    isResettingCamera.value = false
    isStreaming.value = false

    stopTimer()
    resetTimer()
    stopTimer()
    stopCheckReadyInterval()

    // 彻底释放旧的流媒体通道（关键：两路都要干净利落地释放）
    if (stream) { stream.getTracks().forEach(track => track.stop()); stream = null; }
    if (irStream) { irStream.getTracks().forEach(track => track.stop()); irStream = null; }
    if (videoRef.value) videoRef.value.srcObject = null
    if (irVideoRef.value) irVideoRef.value.srcObject = null

    // 重新校准设备列表
    await loadDeviceList()

    if (!selectedRgbId.value) {
      throw new Error('没有可用的可见光摄像头 ID')
    }

    // 1. 启动 RGB 摄像头 (使用 exact 强制指定 ID，不让浏览器瞎选)
    const rgbConstraints: MediaStreamConstraints = {
      video: { 
        deviceId: { exact: selectedRgbId.value }, 
        width: { ideal: 640 }, 
        height: { ideal: 480 } 
      }
    }
    stream = await navigator.mediaDevices.getUserMedia(rgbConstraints)
    if (videoRef.value) videoRef.value.srcObject = stream

    // 2. 只有开启活体检测，且明确拿到了不同的 IR ID 时，才启动红外
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
        if (irVideoRef.value) {
          irVideoRef.value.srcObject = irStream
        }
        console.log('红外 IR 隐藏流成功绑定')
      } catch (irErr) {
        console.error('红外镜头独占打开失败(可能被其他应用占用):', irErr)
      }
    }

    isCameraStarting.value = false
    startCheckReadyStatus()
  } catch (error) {
    isCameraStarting.value = false
    console.error('相机工作流初始化失败:', error)
    alert('无法访问双目相机，请检查设备连接及浏览器权限允许')
    close()
  }
}

// --- 辅助工具：提取 Canvas 帧的 Base64 ---
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

// --- 拍照动作 ---
const capturePhoto=()=> {
  if (!isCameraReady.value || !videoRef.value) {
    console.warn('摄像头流未全部就绪')
    return
  }

  // 1. 抓拍可见光图
  const rgbBase64 = getFrameBase64(videoRef.value)
  if (!rgbBase64) return
  
  capturedImage.value = rgbBase64
  stopTimer()
  stopCheckReadyInterval()

  // 2. 如果启用了活体检测，同步抓取隐藏红外镜头的帧
  let irBase64: string | null = null
  if (useAntiSpoofing.value && irVideoRef.value) {
    irBase64 = getFrameBase64(irVideoRef.value)
  }

  // 拍照后立刻彻底关闭并释放摄像头
  if (stream) {
    stream.getTracks().forEach(track => track.stop())
    stream = null
  }
  if (irStream) {
    irStream.getTracks().forEach(track => track.stop())
    irStream = null
  }
  if (videoRef.value) videoRef.value.srcObject = null
  if (irVideoRef.value) irVideoRef.value.srcObject = null
  isCameraReady.value = false
  isStreaming.value = false

  // 构建统一消息结构体发送
  if (ws && ws.readyState === WebSocket.OPEN) {
    const payload = {
      action: "registerFaceIfNotExist",
      requestId: "1008613",
      data: { rgbBase64, irBase64 }
    }
    ws.send(JSON.stringify(payload))
  }
}

// 重置拍摄
async function resetCapture() {
  isResettingCamera.value = true
  stopCheckReadyInterval()

  capturedImage.value = ''
  isCameraReady.value = false
  isCameraStarting.value = false
  isStreaming.value = false

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
    emit('confirm', capturedImage.value)
    close()
  }
}

function close() {
  stopTimer()
  stopCheckReadyInterval()
  closeWebSocket()
  capturedImage.value = ''
  isCameraReady.value = false
  isCameraStarting.value = false
  isResettingCamera.value = false
  isStreaming.value = false
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
    stopCheckReadyInterval()

    stopTimer()
    resetTimer()
    stopTimer()

    initWebSocket()
    await initCamera()
  } else if (oldVal === true) {
    stopTimer()
    stopCheckReadyInterval()
    closeWebSocket()
    
    if (stream) { stream.getTracks().forEach(track => track.stop()); stream = null; }
    if (irStream) { irStream.getTracks().forEach(track => track.stop()); irStream = null; }
    if (videoRef.value) videoRef.value.srcObject = null
    if (irVideoRef.value) irVideoRef.value.srcObject = null
    
    cleanupTimer()
    isCameraReady.value = false
    isStreaming.value = false
  }
})

onMounted(() => {
  // 页面加载阶段可静默读取一次设备指纹
  loadDeviceList()
})

onBeforeUnmount(() => {
  if (stream) stream.getTracks().forEach(track => track.stop())
  if (irStream) irStream.getTracks().forEach(track => track.stop())
  stopCheckReadyInterval()
  cleanupTimer()
  closeWebSocket()
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

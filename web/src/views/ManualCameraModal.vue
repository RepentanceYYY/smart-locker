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

          <img v-else :src="capturedImage.startsWith('data:') ? capturedImage : formatImageUrl(capturedImage)" class="captured-preview" alt="拍摄的照片" />

          <div v-if="!isCameraReady && !capturedImage" class="camera-loading">
            <div class="loading-spinner"></div>
            <span>相机启动中...</span>
          </div>
        </div>

        <div class="camera-actions">
          <button v-if="!capturedImage" class="capture-btn" :class="{ 'btn-disabled': !isCameraReady }"
            :disabled="!isCameraReady" @click="takeManualPhoto">
            <div class="capture-ring">
              <div class="capture-inner"></div>
            </div>
            <span>{{ isCameraReady ? '拍照' : '相机启动中...' }}</span>
          </button>

          <div v-else class="capture-actions">
            <button class="recapture-btn" @click="resetCapture">重拍</button>
            <button class="confirm-btn" :disabled="isProcessing" @click="confirmPhoto">
              <span>{{ isProcessing ? '正在保存...' : '确认' }}</span>
            </button>
          </div>
        </div>

        <div class="camera-tip" :class="{ 'tip-error': wsErrorMessage }">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="16" />
            <line x1="8" y1="12" x2="16" y2="12" />
          </svg>
          <span>{{ wsErrorMessage || '请正对相机，点击拍照按钮' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onBeforeUnmount, watch, computed, nextTick } from 'vue'
import { useCountdown } from '@/composables/useCountdown'
import { formatImageUrl } from '@/utils/fileUtils'

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

const videoRef = ref<HTMLVideoElement | null>(null)
const isCameraReady = ref<boolean>(false)
const isProcessing = ref<boolean>(false)

let stream: MediaStream | null = null
let checkReadyInterval: number | null = null

let ws: WebSocket | null = null
const wsUrl = `${import.meta.env.VITE_WS_BASE_URL}/face`
const wsErrorMessage = ref<string>('')

const {
  secondsLeft: cameraSecondsLeft,
  stop: stopTimer,
  reset: resetTimer,
  start: startTimer,
  cleanup: cleanupTimer,
} = useCountdown({
  autoStart: false,
  onTimeout: () => { close() }
})

const formattedTime = computed(() => {
  const mins = Math.floor(cameraSecondsLeft.value / 60)
  const secs = cameraSecondsLeft.value % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
})

const capturedImage = ref<string>('')

// 初始化 WebSocket 连接并监听服务端消息
function initWebSocket() {
  if (ws) closeWebSocket()
  ws = new WebSocket(wsUrl)
  ws.onmessage = (event) => {
    try {
      const response = JSON.parse(event.data)
      
      if (response.code === 200) {
        wsErrorMessage.value = ''
        let serverImageUrl = ''
        if (response.data) {
          const imgData = response.data.trim()
          if (imgData.startsWith('/uploads')) {
            serverImageUrl = imgData
            // 用后端返回的远程相对路径覆盖掉前端本地预览的 base64
            capturedImage.value = serverImageUrl 
          }
        }
        isProcessing.value = false
        emit('confirm', serverImageUrl )
        close()
      } else {
        wsErrorMessage.value = response.message || '图片保存失败，请重新拍摄'
        emit('notify', wsErrorMessage.value, 'warning')
        capturedImage.value = ''
        isCameraReady.value = true
        isProcessing.value = false
        startTimerIfCameraReady()
      }
    } catch (e) {
      wsErrorMessage.value = '通信异常，请重试'
      isProcessing.value = false
    }
  }
}

// 关闭并释放 WebSocket 实例
function closeWebSocket() {
  if (ws) { ws.close(); ws = null; }
}

// 相机就绪且无照片时激活操作倒计时
function startTimerIfCameraReady() {
  if (isCameraReady.value && !capturedImage.value) {
    resetTimer()
    startTimer()
  }
}

// 清除相机就绪检测的轮询定时器
function stopCheckReadyInterval() {
  if (checkReadyInterval) { clearInterval(checkReadyInterval); checkReadyInterval = null; }
}

// 轮询检查 video 元素是否已加载具有真实宽高的画面
function startCheckReadyStatus() {
  stopCheckReadyInterval()
  checkReadyInterval = window.setInterval(() => {
    if (videoRef.value && !capturedImage.value) {
      const video = videoRef.value
      // 确保摄像头硬件已正常解码出画面像素（解决部分设备黑屏就绪问题）
      if (video.readyState >= 2 && video.videoWidth > 0 && video.videoHeight > 0) {
        if (!isCameraReady.value) {
          isCameraReady.value = true
          startTimerIfCameraReady()
          stopCheckReadyInterval()
        }
      }
    }
  }, 100)
}

// 视频元数据加载完成时的系统回调
function onVideoLoaded() { startCheckReadyStatus() }

// 视频达到可播放状态时的系统回调
function onVideoCanPlay() { startCheckReadyStatus() }

// 调用硬件设备初始化摄像头流
async function initCamera() {
  try {
    isCameraReady.value = false
    isProcessing.value = false
    stopTimer()
    resetTimer()
    stopCheckReadyInterval()
    releaseTracks()

    const constraints = {
      video: { width: { ideal: 640 }, height: { ideal: 480 } }
    }

    stream = await navigator.mediaDevices.getUserMedia(constraints)
    if (videoRef.value) {
      videoRef.value.srcObject = stream
    }
    startCheckReadyStatus()
  } catch (error) {
    wsErrorMessage.value = '请检查摄像头设备连接及权限允许'
    emit('notify', wsErrorMessage.value, 'warning')
  }
}

// 执行手动拍照，将当前帧画面绘制到画布并导出 Base64
function takeManualPhoto() {
  if (!isCameraReady.value || !videoRef.value || capturedImage.value) return

  const video = videoRef.value
  const canvas = document.createElement('canvas')
  canvas.width = video.videoWidth
  canvas.height = video.videoHeight
  const ctx = canvas.getContext('2d')

  if (ctx) {
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
    capturedImage.value = canvas.toDataURL('image/jpeg', 0.8)
    // 成功截取快照后立刻关停轮询并释放摄像头设备硬件
    stopTimer()
    stopCheckReadyInterval()
    releaseTracks()
    isCameraReady.value = false
  } else {
    emit('notify', '画面未就绪，拍照失败', 'warning')
  }
}

// 确认当前拍摄的照片，向 WebSocket 发送上传数据包
function confirmPhoto() {
  if (!capturedImage.value) return
  // 防止重复确认：若当前已是服务端传回的静态URL路径则直接拦截
  if (!capturedImage.value.startsWith('data:')) return 

  if (ws && ws.readyState === WebSocket.OPEN) {
    isProcessing.value = true
    const timestampStr = String(Date.now())
    const payload = {
      action: "saveFaceImage",
      requestId: timestampStr,
      timestamp: timestampStr,
      data: capturedImage.value 
    }
    ws.send(JSON.stringify(payload))
  } else {
    emit('notify', '网络连接异常，无法发送照片', 'warning')
  }
}

// 重置拍摄状态并重新激活摄像头
async function resetCapture() {
  capturedImage.value = ''
  isCameraReady.value = false
  isProcessing.value = false
  wsErrorMessage.value = ''
  // 等待 v-if 异步销毁图片并重新挂载原生的 video 标签节点
  await nextTick()
  await initCamera()
}

// 安全释放并关闭所有硬件媒体轨道流
function releaseTracks() {
  if (stream) { stream.getTracks().forEach(track => track.stop()); stream = null; }
  if (videoRef.value) videoRef.value.srcObject = null
}

// 完全关闭弹窗，清洗并释放所有定时器、连接与硬件设备
function close() {
  stopTimer()
  stopCheckReadyInterval()
  closeWebSocket()
  releaseTracks()
  capturedImage.value = ''
  isCameraReady.value = false
  isProcessing.value = false
  wsErrorMessage.value = ''
  emit('update:visible', false)
  emit('close')
}

watch(() => props.visible, async (newVal, oldVal) => {
  if (newVal) {
    initWebSocket()
    await initCamera()
  } else if (oldVal === true) {
    close()
    cleanupTimer()
  }
})

onBeforeUnmount(() => { close(); cleanupTimer(); })
</script>

<style scoped>
.camera-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0, 0, 0, 0.85); backdrop-filter: blur(12px); display: flex; align-items: center; justify-content: center; z-index: 10000; }
.camera-container { width: 90%; max-width: 500px; background: rgba(18, 25, 45, 0.95); backdrop-filter: blur(20px); border-radius: 40px; border: 1px solid rgba(56, 189, 248, 0.4); box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5); overflow: hidden; animation: slideUp 0.35s cubic-bezier(0.16, 1, 0.3, 1); }
@keyframes slideUp { from { opacity: 0; transform: translateY(30px) scale(0.96); } to { opacity: 1; transform: translateY(0) scale(1); } }
.camera-header { display: flex; justify-content: space-between; align-items: center; padding: 18px 24px; background: rgba(15, 23, 42, 0.8); border-bottom: 1px solid rgba(56, 189, 248, 0.2); }
.header-info { display: flex; align-items: center; gap: 16px; }
.camera-header h3 { margin: 0; font-size: 20px; font-weight: 600; background: linear-gradient(135deg, #e2e8f0, #94a3b8); background-clip: text; -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.timer-badge { display: flex; align-items: center; gap: 6px; background: rgba(0, 0, 0, 0.5); padding: 5px 12px; border-radius: 40px; font-size: 14px; color: #a5f3fc; border: 1px solid rgba(56, 189, 248, 0.3); }
.timer-badge.time-warning { color: #f87171; border-color: #f87171; background: rgba(248, 113, 113, 0.15); animation: pulse 0.8s ease infinite; }
.timer-icon { width: 14px; height: 14px; }
.close-btn { background: rgba(255, 255, 255, 0.08); border: none; color: #94a3b8; cursor: pointer; padding: 8px; border-radius: 32px; width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; }
.camera-body { padding: 24px; }
.video-wrapper { width: 100%; aspect-ratio: 4 / 3; background: #0a0f1a; border-radius: 28px; overflow: hidden; margin-bottom: 24px; position: relative; border: 1px solid rgba(56, 189, 248, 0.2); }
.video-preview, .captured-preview { width: 100%; height: 100%; object-fit: cover; }
.camera-loading { position: absolute; top: 0; left: 0; right: 0; bottom: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; background: rgba(10, 15, 26, 0.9); backdrop-filter: blur(8px); color: #94a3b8; }
.loading-spinner { width: 32px; height: 32px; border: 3px solid rgba(56, 189, 248, 0.2); border-top-color: #38bdf8; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.camera-actions { display: flex; justify-content: center; margin-bottom: 20px; min-height: 80px; align-items: center; }
.capture-actions { display: flex; gap: 20px; width: 100%; justify-content: center; }
.recapture-btn, .confirm-btn { display: flex; align-items: center; justify-content: center; padding: 12px 28px; border-radius: 60px; font-size: 15px; font-weight: 600; cursor: pointer; border: none; }
.recapture-btn { background: rgba(51, 65, 85, 0.9); color: #f1f5f9; border: 1px solid rgba(255, 255, 255, 0.1); }
.confirm-btn { background: linear-gradient(135deg, #10b981, #059669); color: white; box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3); }
.confirm-btn:disabled { background: #334155 !important; color: #64748b !important; box-shadow: none !important; cursor: not-allowed; }
.capture-btn { background: transparent; border: none; cursor: pointer; display: flex; flex-direction: column; align-items: center; gap: 8px; color: white; font-size: 14px; font-weight: 500; transition: all 0.2s ease; }
.capture-btn.btn-disabled { opacity: 0.5; cursor: not-allowed; pointer-events: none; }
.capture-ring { width: 72px; height: 72px; border-radius: 50%; background: rgba(255, 255, 255, 0.15); display: flex; align-items: center; justify-content: center; transition: all 0.2s ease; border: 1px solid rgba(255, 255, 255, 0.3); }
.capture-inner { width: 60px; height: 60px; border-radius: 50%; background: linear-gradient(135deg, #22d3ee, #3b82f6); transition: all 0.2s ease; }
.capture-btn:active .capture-ring { transform: scale(0.92); background: rgba(255, 255, 255, 0.25); }
.capture-btn:active .capture-inner { transform: scale(0.96); }
.camera-tip { display: flex; align-items: center; justify-content: center; gap: 8px; font-size: 12px; color: #94a3b8; padding: 10px 16px; background: rgba(0, 0, 0, 0.3); border-radius: 40px; }
.camera-tip.tip-error { color: #f87171; background: rgba(248, 113, 113, 0.15); border: 1px solid rgba(248, 113, 113, 0.3); }
</style>
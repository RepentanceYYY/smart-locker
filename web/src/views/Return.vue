<template>
  <div class="return-container">
    <!-- 通知列表 -->
    <div class="notification-container">
      <div v-for="notify in notifications" :key="notify.id" class="notification" :class="notify.type">
        {{ notify.text }}
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-mask">
      <div class="loading-spinner"></div>
      <span>加载配置中...</span>
    </div>
    <div v-else>
      <div v-if="totalCount === 0" class="empty-state">
        <img src="/警告 (1).svg" alt="警告" class="empty-icon" />
        ⚠️ 暂无柜子配置数据，请联系管理员
      </div>
      <div v-else class="full-layout">
        <!-- 页面标题 -->
        <div class="page-header">
          <img src="/归还.svg" alt="归还" class="title-icon" />

          <h1>物品归还</h1>
        </div>

        <!-- 扫描提示区（含手动输入后门） -->
        <div class="scanner-prompt" :class="{ active: isWaitingForScan }">
          <div class="scanner-icon">🔍</div>
          <div class="scanner-text">
            <span v-if="isWaitingForScan">请使用扫描枪扫描二维码或手动输入二维码号</span>
            <span v-else-if="scannedCellInfo">
              已扫描: {{ scannedCellInfo.cabinetTitle }} - {{ scannedCellInfo.cellNumber }}
              <span v-if="scannedCellInfo.isOpen" class="door-open-badge">门锁已开启</span>
            </span>
            <span v-else>等待扫描...</span>
          </div>

          <!-- 手动输入后门 -->
          <div class="manual-input-area">
            <input type="text" v-model="manualQRCode" placeholder="手动输入二维码号" @keyup.enter="submitManualQRCode"
              class="manual-input" />
            <button @click="submitManualQRCode" class="manual-submit-btn">提交</button>
          </div>

          <button v-if="!isWaitingForScan && scannedCellInfo" class="reset-scan-btn" @click="resetScanState">
            重新扫描
          </button>
        </div>

        <!-- 上部区域（3D 轮播 + 温湿度） -->
        <div class="upper-area">
          <div class="top-section">
            <!-- 温度卡片 -->
            <div class="temp-card">
              <img src="/温度.svg" alt="温度" class="card-icon" />
              <div class="card-value">{{ currentCabinetTemp }}°</div>
              <div class="card-label">温度</div>
            </div>

            <!-- 湿度卡片 -->
            <div class="humidity-card">
              <img src="/湿度-01.svg" alt="湿度" class="card-icon" />
              <div class="card-value">{{ currentCabinetHumidity }}%</div>
              <div class="card-label">湿度</div>
            </div>

            <!-- 导航按钮 -->
            <button class="nav-btn-left" :class="{ disabled: currentIndex === 0 }" @click="rotatePrev">
              <span class="arrow">◀</span><span class="btn-text">上一个</span>
            </button>
            <button class="nav-btn-right" :class="{ disabled: currentIndex === totalCount - 1 }" @click="rotateNext">
              <span class="btn-text">下一个</span><span class="arrow">▶</span>
            </button>

            <!-- 3D 圆柱轮播 -->
            <div class="carousel-cylinder">
              <div class="carousel-3d" :style="{ minHeight: carouselHeight + 'px' }">
                <div v-for="(cab, idx) in cabinets" :key="cab.id" class="cabinet-item"
                  :class="{ 'center-highlight': idx === currentIndex }" :style="{
                    ...getCabinetStyle(idx),
                    width: cab.width || '280px', height: cab.height || 'auto',
                  }">
                  <div class="cabinet-header">{{ cab.title }}</div>
                  <div class="cabinet-body">
                    <div class="cabinet-grid" :style="getGridStyle(cab)">
                      <template v-for="(cell, cellIdx) in cab.flatCells" :key="cellIdx">
                        <!-- 普通格口 -->
                        <div v-if="cell.type === 'cell'" class="cell-container"
                          :style="[getCellPosition(cell), cell.cellStyle]">
                          <div class="cell-inner"></div>
                          <div class="cabinet-cell" :class="{
                            'empty-door': cell.isEmpty,
                            'door-open': cell.isDoorOpen,
                          }">
                            <span class="cell-number">{{ cell.number }}</span>
                            <span class="tool-name">{{ truncateText(cell.toolName, 8) }}</span>
                          </div>
                        </div>
                        <!-- 图片格口 -->
                        <div v-else-if="cell.type === 'image'" class="custom-image-cell"
                          :style="[getCellPosition(cell), cell.cellStyle]">
                          <img :src="formatImageUrl(cell.imageUrl)" :alt="cell.label || '图标'" />
                          <span v-if="cell.label" class="image-label">{{
                            truncateText(cell.label, 10)
                          }}</span>
                        </div>
                      </template>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="indicator-upper">
            <span class="indicator-text">柜组 {{ currentIndex + 1 }} / {{ totalCount }}</span>
          </div>
          <div class="divider"></div>
        </div>

        <!-- 下部区域：归还表格 + 照片预览 + 归还完成按钮 -->
        <div class="bottom-section">
          <div class="bottom-container">
            <!-- 左侧：照片显示区域 -->
            <div class="photo-area">
              <div v-if="photoUrl" class="photo-card">
                <img :src="formatImageUrl(photoUrl)" alt="拍摄照片" class="preview-image" />
                <div class="photo-badge">归还照片</div>
              </div>
              <div v-else class="photo-placeholder">
                <svg class="placeholder-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="2" y="4" width="20" height="16" rx="2" ry="2" />
                  <circle cx="12" cy="12" r="3" />
                  <line x1="18" y1="8" x2="18" y2="8" stroke-width="2" />
                </svg>
                <span>暂无照片</span>
              </div>
            </div>

            <!-- 中间：归还记录表格 -->
            <div class="info-area">
              <div class="info-list">
                <div class="info-header">
                  <span class="header-item">柜子名称</span>
                  <span class="header-item">格口号</span>
                  <span class="header-item">工具名称</span>
                  <span class="header-item">归还时间</span>
                </div>
                <div class="info-scroll">
                  <div v-if="returnRecords.length === 0" class="info-row placeholder-row">
                    <span class="row-item" colspan="4">暂无归还记录，扫描后关门即自动记录</span>
                  </div>
                  <div v-for="record in returnRecords" :key="record.id" class="info-row return-record">
                    <span class="row-item">{{ record.cabinetTitle }}</span>
                    <span class="row-item">{{ record.cellNumber }}</span>
                    <span class="row-item">{{ record.toolName }}</span>
                    <span class="row-item">{{ record.returnTime }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 右侧：手动触发归还完成按钮 -->
            <div class="button-area">
              <button class="complete-btn return-btn" :disabled="isCompleteDisabled" @click="handleCompleteSession">
                <img src="/归还.svg" alt="归还" class="btn-icon" />
                <span class="btn-label">归还完成</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 归还汇总模态框 -->
    <ReturnSummaryModal v-model:visible="showReturnSummary" :return-items="returnRecords" :photo-data="photoUrl"
      @submit="onReturnSubmit" />

    <!-- 无归还记录提示模态框 -->
    <Teleport to="body">
      <div v-if="showEmptyReturnModal" class="modal-overlay" @click.self="closeEmptyReturnModal">
        <div class="modal-container info-modal">
          <div class="modal-header info-header">
            <img src="/笔记本.svg" alt="提示" class="modal-icon" />
            <button class="close-btn" @click="closeEmptyReturnModal">✕</button>
          </div>
          <div class="modal-body">
            <p class="info-tip">当前没有归还任何物品，无法提交归还完成。</p>
            <p class="countdown-text">
              倒计时 <strong class="countdown-number">{{ emptyReturnCountdown }}</strong> 秒后自动返回首页...
            </p>
          </div>
          <div class="modal-footer">
            <button class="cancel-btn" @click="closeEmptyReturnModal">取消</button>
            <button class="confirm-btn" @click="immediateReturnHome">立即返回</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 归还成功模态框 -->
    <Teleport to="body">
      <div v-if="showReturnSuccessModal" class="modal-overlay no-close">
        <div class="modal-container success-modal">
          <div class="modal-header success-header">
            <h3>
              <img src="/color-success.svg" alt="成功" class="modal-icon" />
              归还成功
            </h3>
          </div>
          <div class="modal-body">
            <p class="success-tip">
              成功归还 <strong>{{ returnSuccessItemCount }}</strong> 件物品！
            </p>
            <p class="countdown-text">
              倒计时 <strong class="countdown-number">{{ returnSuccessCountdown }}</strong> 秒后自动返回首页...
            </p>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { fetchCabinetList } from '@/api/cabinet'
import { submitReturnRecords } from '@/api/return'
import ReturnSummaryModal from './ReturnSummaryModal.vue'
import { useDehumidifierStore } from '@/stores/useDehumidifier'
import type { CSSProperties } from 'vue'
import { formatImageUrl } from '@/utils/fileUtils'

const dehumidifierStore = useDehumidifierStore()

// ================== 类型定义 ==================
interface BaseCell {
  type: 'cell' | 'image'
  columns: string
  height: string
  colSpan: number
  rowSpan: number
}

interface NormalCell extends BaseCell {
  type: 'cell'
  id: number
  number: string
  toolName: string
  isEmpty: boolean
  qrcodeContent?: string
  isDoorOpen?: boolean
}

interface ImageCell extends BaseCell {
  type: 'image'
  imageUrl: string
  label: string
}

type CellConfig = NormalCell | ImageCell

interface RowConfig {
  cells: CellConfig[]
}

interface CabinetConfig {
  id: number
  title: string
  width?: string
  height?: string
  isDefault?: boolean
  rows: RowConfig[]
  initialTemp?: number
  initialHumidity?: number
}

interface ProcessedCabinet extends CabinetConfig {
  flatCells: any[]
  colWidths: string[]
  rowHeights: string[]
  gridStyle: any
}

interface Notification {
  id: number
  text: string
  type: 'info' | 'success' | 'warning'
}

interface ScannedCellInfo {
  cabinetId: number
  cabinetTitle: string
  cellId: number
  cellNumber: string
  toolName: string
  isEmpty: boolean
  isOpen: boolean
}

interface ReturnRecord {
  id: string
  cabinetId: number
  cabinetTitle: string
  cellId: number
  cellNumber: string
  toolName: string
  returnTime: string
}

// ================== 辅助函数 ==================
function expandColumns(columns: string, count: number): string[] {
  const parts = columns.trim().split(/\s+/)
  if (parts.length === 1 && count > 1) return Array(count).fill(parts[0])
  return parts
}

function expandHeight(height: string, count: number): string[] {
  const parts = height.trim().split(/\s+/)
  if (parts.length === 1 && count > 1) return Array(count).fill(parts[0])
  return parts
}

function flattenCells(cab: CabinetConfig) {
  const flatCells: any[] = []
  const rowHeights: string[] = []
  const colWidths: string[] = []

  let totalRows = 0
  for (const row of cab.rows) {
    let rowMaxSpan = 1
    for (const cell of row.cells) rowMaxSpan = Math.max(rowMaxSpan, cell.rowSpan)
    totalRows += rowMaxSpan
  }

  let currentRowIdx = 0
  for (const row of cab.rows) {
    const rowStart = currentRowIdx + 1
    let groupRowSpan = 1
    for (const cell of row.cells) groupRowSpan = Math.max(groupRowSpan, cell.rowSpan)
    let colCursor = 0
    for (const cell of row.cells) {
      const startCol = colCursor
      const endCol = colCursor + cell.colSpan
      const colValues = expandColumns(cell.columns, cell.colSpan)
      const rowValues = expandHeight(cell.height, cell.rowSpan)

      for (let i = 0; i < colValues.length; i++) {
        const colIndex = startCol + i
        if (colWidths[colIndex] === undefined) colWidths[colIndex] = colValues[i]
      }
      for (let i = 0; i < rowValues.length; i++) {
        const rowIndex = rowStart + i - 1
        if (rowHeights[rowIndex] === undefined) rowHeights[rowIndex] = rowValues[i]
      }

      flatCells.push({
        ...cell,
        gridRowStart: rowStart,
        gridRowEnd: rowStart + cell.rowSpan,
        gridColumnStart: startCol + 1,
        gridColumnEnd: endCol + 1,
        originalColumns: colValues,
        originalHeight: rowValues,
        isDoorOpen: false,
      })
      colCursor = endCol
    }
    currentRowIdx += groupRowSpan
  }

  for (let i = 0; i < colWidths.length; i++) if (!colWidths[i]) colWidths[i] = '1fr'
  for (let i = 0; i < rowHeights.length; i++) if (!rowHeights[i]) rowHeights[i] = 'auto'

  const fixedRowHeights = rowHeights.map(h => (h === 'auto' ? '85px' : h))
  const fixedColWidths = colWidths.map(w => (w === 'auto' ? '1fr' : w))

  return { flatCells, colWidths: fixedColWidths, rowHeights: fixedRowHeights }
}

function getGridTemplate({ colWidths, rowHeights }: { colWidths: string[]; rowHeights: string[] }) {
  return {
    display: 'grid',
    gridTemplateRows: rowHeights.join(' '),
    gridTemplateColumns: colWidths.join(' '),
    gap: '10px',
    position: 'relative',
    zIndex: 2,
  }
}

function getCellPosition(cell: any) {
  return {
    gridRow: `${cell.gridRowStart} / ${cell.gridRowEnd}`,
    gridColumn: `${cell.gridColumnStart} / ${cell.gridColumnEnd}`,
  }
}

function processCabinetData(rawData: any[]): ProcessedCabinet[] {
  return rawData.map(cab => {
    const rows = cab.rows.map((row: any) => ({
      cells: row.cells.map((cell: any) => ({
        ...cell,
        columns: cell.columns || '1fr',
        height: cell.height || 'auto',
        colSpan: cell.colSpan || 1,
        rowSpan: cell.rowSpan || 1,
        isEmpty: cell.type === 'cell' ? cell.isEmpty || false : false,
        number: cell.type === 'cell' ? cell.number || '' : '',
        toolName: cell.type === 'cell' ? cell.toolName || '' : '',
        qrcodeContent: cell.type === 'cell' ? cell.qrcodeContent || '' : '',
        imageUrl: cell.type === 'image' ? cell.imageUrl || '' : '',
        label: cell.type === 'image' ? cell.label || '' : '',
      })),
    }))
    const { flatCells, colWidths, rowHeights } = flattenCells({ ...cab, rows })
    return {
      ...cab,
      width: cab.width || '280px',
      height: cab.height || 'auto',
      rows,
      flatCells,
      colWidths,
      rowHeights,
      gridStyle: getGridTemplate({ colWidths, rowHeights }),
    } as ProcessedCabinet
  })
}

function truncateText(text: string, maxLen: number): string {
  if (!text) return ''
  return text.length > maxLen ? text.slice(0, maxLen) + '...' : text
}

// ================== 通知 ==================
const notifications = ref<Notification[]>([])
let nextNotificationId = 1

function addNotification(
  text: string,
  type: 'info' | 'success' | 'warning' = 'info',
  duration = 5000
) {
  const id = nextNotificationId++
  const notification: Notification = { id, text, type }
  notifications.value.push(notification)
  setTimeout(() => {
    notifications.value = notifications.value.filter(n => n.id !== id)
  }, duration)
}

// ================== 路由 & 数据 ==================
const router = useRouter()

const cabinets = ref<ProcessedCabinet[]>([])
const loading = ref(true)
const currentIndex = ref(0)
const totalCount = computed(() => cabinets.value.length)

const radius = ref(320)
const carouselHeight = ref(600)
const maxScale = ref(1.4)

const photoUrl = ref('')
const scannerBuffer = ref('')
const scannerTimer = ref<ReturnType<typeof setTimeout> | null>(null)
// 手动二维码
const manualQRCode = ref('')

const isWaitingForScan = ref(true)
const scannedCellInfo = ref<ScannedCellInfo | null>(null)
const returnRecords = ref<ReturnRecord[]>([])
const showReturnSummary = ref(false)

// 成功归还相关状态
const showReturnSuccessModal = ref(false)
const returnSuccessCountdown = ref(10)
const returnSuccessItemCount = ref(0)
let returnSuccessTimer: ReturnType<typeof setInterval> | null = null

// 空归还记录模态框相关状态
const showEmptyReturnModal = ref(false)
const emptyReturnCountdown = ref(10)
let emptyReturnTimer: ReturnType<typeof setInterval> | null = null

// 禁用归还完成按钮的条件
const isCompleteDisabled = computed(() => {
  return loading.value || showReturnSummary.value || showEmptyReturnModal.value || showReturnSuccessModal.value
})

const targetCabinetName = ref('')
const targetCellNumber = ref('')

// 计算属性：当前柜子温度
const currentCabinetTemp = computed(() => {
  if (cabinets.value.length === 0) return '--'

  const currentCab = cabinets.value[currentIndex.value]
  if (!currentCab?.id) return '--'

  const envData = dehumidifierStore.cabinetEnvMap[currentCab.id]
  const temp = envData?.temperature

  return (temp === 0 || temp === undefined || temp === null) ? '--' : temp.toFixed(1)
})

// 计算属性：当前柜子湿度
const currentCabinetHumidity = computed(() => {
  if (cabinets.value.length === 0) return '--'

  const currentCab = cabinets.value[currentIndex.value]
  if (!currentCab?.id) return '--'
  const envData = dehumidifierStore.cabinetEnvMap[currentCab.id]
  const humidity = envData?.humidity

  return (humidity === 0 || humidity === undefined || humidity === null) ? '--' : humidity
})

async function loadCabinets() {
  loading.value = true
  try {
    const rawData = await fetchCabinetList()
    cabinets.value = processCabinetData(rawData)
    const defaultIdx = cabinets.value.findIndex(cab => cab.isDefault === true)
    currentIndex.value = defaultIdx !== -1 ? defaultIdx : 0

    if (targetCabinetName.value) {
      const targetIdx = cabinets.value.findIndex(cab => cab.title === targetCabinetName.value)
      if (targetIdx !== -1) currentIndex.value = targetIdx
    }
  } catch (error) {
    console.error('加载柜子配置失败:', error)
    cabinets.value = []
  } finally {
    loading.value = false
  }
}


function updateLayout() {
  if (typeof window === 'undefined') return
  const width = window.innerWidth
  const height = window.innerHeight
  radius.value = Math.min(width * 0.45, 380)
  maxScale.value = Math.min(2, Math.max(1.2, width / 220))
  const upperHeight = height * 0.85
  const extraHeight = 55
  let available = upperHeight - extraHeight
  carouselHeight.value = Math.max(450, available)
}

const getCabinetStyle = (idx: number): CSSProperties => {
  if (totalCount.value === 0) return { display: 'none' }
  const diff = Math.abs(idx - currentIndex.value)
  const isVisible = diff <= 1
  if (!isVisible) {
    return {
      transform: 'translateX(0) translateZ(-500px)',
      opacity: 0,
      visibility: 'hidden',
      pointerEvents: 'none',
      zIndex: -1,
      transition: 'opacity 0.3s, visibility 0.3s',
    }
  }
  const angleStep = (Math.PI * 2) / totalCount.value
  let angle = idx * angleStep
  const centerAngle = currentIndex.value * angleStep
  let relativeAngle = angle - centerAngle
  if (relativeAngle > Math.PI) relativeAngle -= Math.PI * 2
  if (relativeAngle < -Math.PI) relativeAngle += Math.PI * 2
  const x = Math.sin(relativeAngle) * radius.value
  const z = Math.cos(relativeAngle) * radius.value - 80
  let rotateY = -relativeAngle * (180 / Math.PI) * 0.35
  let scaleVal = 1
  let zIndexVal = Math.round(100 - Math.abs(relativeAngle) * 20)
  const absRel = Math.abs(relativeAngle)
  if (absRel < 0.2) {
    scaleVal = maxScale.value
    zIndexVal = 300
  } else {
    scaleVal = 0.75
    zIndexVal = 50
  }
  const transform = `translateX(${x}px) translateY(0px) translateZ(${z}px) rotateY(${rotateY}deg) scale(${scaleVal})`
  return { transform, zIndex: zIndexVal, opacity: 1, visibility: 'visible', pointerEvents: 'auto' }
}

function rotatePrev() {
  if (currentIndex.value > 0) currentIndex.value--
}

function rotateNext() {
  if (currentIndex.value < totalCount.value - 1) currentIndex.value++
}

function getGridStyle(cab: ProcessedCabinet) {
  return cab.gridStyle
}

// ================== WebSocket 连接 ==================
let closeAndCheckTimer: any = null // 轮询定时器
const isWaiting = ref(false)       // 状态锁：是否正在等待后端的响应
const isPollingActive = ref(true)  // 业务锁：轮询是否仍在继续进行

const closeAndCheck = (cabId: any, cellId: any, cellNumber: any, toolName: any) => {
  // 如果轮询已经结束，或者上一次的响应还没来，则不发送
  if (!isPollingActive.value || isWaiting.value) return
  // 检测柜门和物品状态
  sendMessage('closeAndCheck', {
    cabinetId: cabId,
    cellId: cellId,
    cellNumber: cellNumber,
    toolName: toolName
  })
}
// 递进式定时器
const startCloseAndCheckPolling = (cabId: any, cellId: any, cellNumber: any, toolName: any) => {
  isPollingActive.value = true
  // 这里的检查依然保留，用于拦截定时器内部递归时的状态
  if (!isPollingActive.value) return

  closeAndCheckTimer = setTimeout(() => {
    closeAndCheck(cabId, cellId, cellNumber, toolName)
    // 只有当定时器没有被外部中断，且轮询依然处于激活状态时，才进行下一次递归
    if (closeAndCheckTimer && isPollingActive.value) {
      startCloseAndCheckPolling(cabId, cellId, cellNumber, toolName)
    }
  }, 600)
}
/**
 * 停止轮询
 */
const stopCloseAndCheckPolling = () => {
  isPollingActive.value = false // 标记轮询已结束
  isWaiting.value = false       // 解锁等待状态
  if (closeAndCheckTimer) {
    clearTimeout(closeAndCheckTimer)         // 清除当前的定时器
    closeAndCheckTimer = null
  }
}

const wsUrl = `${import.meta.env.VITE_WS_BASE_URL}/return`
let socket: WebSocket | null = null
const wsConnected = ref(false)

function connectWebSocket() {
  socket = new WebSocket(wsUrl)
  socket.onopen = () => {
    console.log('WebSocket 连接成功')
    wsConnected.value = true
  }
  socket.onmessage = async (event) => {
    try {
      const message = JSON.parse(event.data)
      await handleWebSocketMessage(message)
    } catch (e) {
      console.error('解析消息失败', e)
    }
  }
  socket.onerror = (error) => {
    console.error('WebSocket 错误', error)
    addNotification('与服务器连接异常，请尝试重启', 'warning')
  }
  socket.onclose = () => {
    console.log('WebSocket 连接关闭')
    wsConnected.value = false
    // 尝试重连
    setTimeout(() => connectWebSocket(), 3000)
  }
}

const sendMessage = (action: string, data: any) => {
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    addNotification('网络连接异常，请稍后重试', 'warning')
    return false
  }
  socket.send(JSON.stringify({ action, data, requestId: Date.now().toString(), timestamp: Date.now().toString() }))
  return true
}

// 处理后端返回的消息
const handleWebSocketMessage = async (receive: any) => {
  const { action, code, data, message } = receive
  if (action === 'openLock') {
    await handleUnLockReply(receive)
  } else if (action === 'closeAndCheck') {
    await handleCloseAndCheck(receive)
    isWaiting.value = false
  } else if (action === 'checkAllLockStatus') {
    handleCheckAllLockStatus(receive)
  }
}

const handleUnLockReply = async (receive: any) => {

  const { action, code, data, message } = receive

  if (code === 200) {
    // 开锁成功
    const { cabinetId, cellId, cellNumber, toolName } = data
    // 找到对应的格口并设置开门状态
    for (const cab of cabinets.value) {
      if (cab.id === cabinetId) {
        const cell = cab.flatCells.find(c => c.type === 'cell' && c.id === cellId && c.number === cellNumber)
        if (cell) {
          cell.isDoorOpen = true
          scannedCellInfo.value = {
            cabinetId: cab.id,
            cabinetTitle: cab.title,
            cellId,
            cellNumber,
            toolName: toolName || getToolNameForCell(cell),
            isEmpty: cell.isEmpty,
            isOpen: true,
          }
          isWaitingForScan.value = false
          addNotification(`✅ 门锁已开启，请放入物品后关门`, 'success')
          // 切换到对应的柜子
          const idx = cabinets.value.findIndex(c => c.id === cabinetId)
          if (idx !== -1) currentIndex.value = idx
          // 启动轮询柜门状态和储物状态
          await new Promise((resolve) => setTimeout(resolve, 1000))
          startCloseAndCheckPolling(cab.id, cellId, cellNumber, toolName)
        } else {
          addNotification('开锁成功但未找到对应格口', 'warning')
        }
        break
      }
    }
  } else {
    addNotification(message || '开锁失败', 'warning')
    resetScanState()
  }
}

const handleCloseAndCheck = async (receive: any) => {
  const { action, code, data, message } = receive
  const { cabinetId, cellId, cellNumber, toolName, returnTime } = data
  switch (code) {
    case 200:
      // 关门成功且有物品归还
      stopCloseAndCheckPolling()
      // 更新前端状态
      for (const cab of cabinets.value) {
        if (cab.id === cabinetId) {
          const cell = cab.flatCells.find(c => c.type === 'cell' && c.id === cellId && c.number === cellNumber)
          if (cell) {
            cell.isDoorOpen = false
            cell.isEmpty = false
            cell.toolName = toolName
            // 添加到归还记录
            returnRecords.value.unshift({
              id: Date.now().toString(),
              cabinetId,
              cabinetTitle: cab.title,
              cellId,
              cellNumber,
              toolName,
              returnTime: returnTime || new Date().toLocaleString(),
            })
            addNotification(`✅ 工具 ${toolName} 已成功归还`, 'success')
            // 清空当前扫描信息，回到扫描等待状态
            scannedCellInfo.value = null
            isWaitingForScan.value = true
          }
          break
        }
      }
      break;
    case 204:
      console.log(`✅ ${cellNumber}号格口已关锁，但工具 ${toolName} 未归还`)
      // 已关锁但没有物品
      stopCloseAndCheckPolling()
      // 更新前端状态
      for (const cab of cabinets.value) {
        if (cab.id === cabinetId) {
          const cell = cab.flatCells.find(c => c.type === 'cell' && c.id === cellId && c.number === cellNumber)
          if (cell) {
            cell.isDoorOpen = false
            cell.isEmpty = true
            cell.toolName = toolName
            addNotification(`✅ ${cellNumber}号格口已关锁，但工具 ${toolName} 未归还`, 'warning')
            // 清空当前扫描信息，回到扫描等待状态
            scannedCellInfo.value = null
            isWaitingForScan.value = true
          }
          break
        }
      }
      break;
    case 400:
      // 请求数据错误
      stopCloseAndCheckPolling()
      addNotification('系统出现小差，请联系管理员', 'warning')
      break;
    case 409:
      // 检测到未关锁 后续不检测是否储物
      break;
    case 500:
      // 锁状态查询失败，后续不检测是否储物
      stopCloseAndCheckPolling()
      addNotification('门锁状态检测失败，请联系管理员', 'warning')
      break;

  }
  isWaiting.value = false
}

const handleCheckAllLockStatus = (receive: any) => {
  const { action, code, data, message } = receive
  switch (code) {
    case 200:
      if (returnRecords.value.length === 0) {
        startEmptyReturnCountdown()
        return
      }
      showReturnSummary.value = true
      break;
    case 501:
      addNotification(message, 'warning')
      break;
    default:
      addNotification('尚有柜门未关闭，请先关闭柜门完成归还', 'warning')
  }

}

// ================== 核心业务逻辑 ==================
function isCellDoorOpen(cell: any): boolean {
  return cell.isDoorOpen === true
}

function requestOpenLock(cabinetId: number, cellId: number, cellNumber: string): Promise<boolean> {
  if (!wsConnected.value) {
    addNotification('服务器未连接，请稍后重试', 'warning')
    return Promise.resolve(false)
  }
  sendMessage('openLock', { cabinetId, cellId, cellNumber })
  return Promise.resolve(true)
}
/**
 * 根据二维码内容查找单元格
 * @param content 二维码内容
 */
function findCellByQRCodeContent(
  content: string
): { cabinet: ProcessedCabinet; cabinetIndex: number; cell: NormalCell; cellIndex: number } | null {
  for (let i = 0; i < cabinets.value.length; i++) {
    const cabinet = cabinets.value[i]
    for (let j = 0; j < cabinet.flatCells.length; j++) {
      const cell = cabinet.flatCells[j]
      if (cell.type === 'cell' && cell.qrcodeContent === content) {
        return { cabinet, cabinetIndex: i, cell: cell as NormalCell, cellIndex: j }
      }
    }
  }
  return null
}
/**
 * 获取单元格的工具名称
 * @param cell
 */
function getToolNameForCell(cell: NormalCell): string {
  if (cell.toolName && cell.toolName.trim() !== '') {
    return cell.toolName
  }
  try {
    const storedData = sessionStorage.getItem('toolOperationData')
    if (storedData) {
      const data = JSON.parse(storedData)
      if (data.toolName) return data.toolName
    }
  } catch (e) {
    console.error(e)
  }
  return '未知工具'
}
/**
 * 处理扫描的二维码
 * @param content
 */
const processScannedQRCode = async (content: string) => {
  const trimmedContent = content.trim()
  if (!trimmedContent) {
    addNotification('扫描内容为空', 'warning')
    return
  }

  const anyOpen = cabinets.value.some(cab =>
    cab.flatCells.some(cell => cell.type === 'cell' && cell.isDoorOpen === true)
  )
  if (anyOpen) {
    addNotification('请先关闭当前开启的柜门', 'warning')
    return
  }

  const found = findCellByQRCodeContent(trimmedContent)
  if (!found) {
    addNotification(`未找到匹配的二维码: ${trimmedContent}，请确认二维码是否正确`, 'warning')
    return
  }

  const { cabinet, cell } = found

  if (!cell.isEmpty) {
    addNotification(`格口 ${cell.number} 不为空，无法归还`, 'warning')
    return
  }
  addNotification(`正在开启 ${cabinet.title} - ${cell.number} 门锁...`, 'info')
  await requestOpenLock(cabinet.id, cell.id, cell.number)
}

function resetScanState() {
  const anyOpen = cabinets.value.some(cab =>
    cab.flatCells.some(cell => cell.type === 'cell' && cell.isDoorOpen === true)
  )
  if (anyOpen) {
    addNotification('请先关闭当前开启的柜门', 'warning')
    return
  }
  scannedCellInfo.value = null
  isWaitingForScan.value = true
  scannerBuffer.value = ''
  addNotification('已重置扫描状态，请重新扫描二维码', 'info')
}
/**
 * 提交手动二维码
 */
function submitManualQRCode() {
  if (!manualQRCode.value.trim()) {
    addNotification('请输入二维码内容', 'warning')
    return
  }
  processScannedQRCode(manualQRCode.value.trim())
  manualQRCode.value = ''
}

function onGlobalKeydown(event: KeyboardEvent) {
  const target = event.target as HTMLElement
  if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable) {
    return
  }

  if (event.key === 'Enter') {
    if (scannerBuffer.value.length > 0) {
      event.preventDefault()
      const content = scannerBuffer.value
      scannerBuffer.value = ''
      if (scannerTimer.value) {
        clearTimeout(scannerTimer.value)
        scannerTimer.value = null
      }
      processScannedQRCode(content)
    }
    return
  }

  if (event.key.length === 1 || event.key === 'Backspace') {
    if (scannerTimer.value) {
      clearTimeout(scannerTimer.value)
    }
    if (event.key === 'Backspace') {
      scannerBuffer.value = scannerBuffer.value.slice(0, -1)
    } else {
      scannerBuffer.value += event.key
    }
    scannerTimer.value = setTimeout(() => {
      scannerBuffer.value = ''
    }, 100)
  }
}

// 处理归还完成按钮点击
function handleCompleteSession() {
  const anyOpen = cabinets.value.some(cab =>
    cab.flatCells.some(cell => cell.type === 'cell' && cell.isDoorOpen === true)
  )
  if (anyOpen) {
    addNotification('尚有柜门未关闭，请先关闭柜门完成归还', 'warning')
    return
  }
  sendMessage('checkAllLockStatus', {})
}

// 空归还记录倒计时相关方法
function startEmptyReturnCountdown() {
  if (emptyReturnTimer) clearInterval(emptyReturnTimer)
  showEmptyReturnModal.value = true
  emptyReturnCountdown.value = 10
  emptyReturnTimer = setInterval(() => {
    if (emptyReturnCountdown.value <= 1) {
      clearEmptyReturnTimer()
      showEmptyReturnModal.value = false
      router.push('/')
    } else {
      emptyReturnCountdown.value--
    }
  }, 1000)
}

function clearEmptyReturnTimer() {
  if (emptyReturnTimer) {
    clearInterval(emptyReturnTimer)
    emptyReturnTimer = null
  }
}

function closeEmptyReturnModal() {
  clearEmptyReturnTimer()
  showEmptyReturnModal.value = false
}

function immediateReturnHome() {
  clearEmptyReturnTimer()
  clearReturnSuccessTimer()
  showEmptyReturnModal.value = false
  showReturnSuccessModal.value = false
  router.push('/')
}

// 归还成功倒计时相关方法
function startReturnSuccessCountdown(count: number) {
  if (returnSuccessTimer) clearInterval(returnSuccessTimer)
  returnSuccessItemCount.value = count
  showReturnSuccessModal.value = true
  returnSuccessCountdown.value = 10
  returnSuccessTimer = setInterval(() => {
    if (returnSuccessCountdown.value <= 1) {
      clearReturnSuccessTimer()
      showReturnSuccessModal.value = false
      router.push('/')
    } else {
      returnSuccessCountdown.value--
    }
  }, 1000)
}

function clearReturnSuccessTimer() {
  if (returnSuccessTimer) {
    clearInterval(returnSuccessTimer)
    returnSuccessTimer = null
  }
}

function base64ToFile(base64: string, filename: string): File {
  const arr = base64.split(',')
  const mime = arr[0].match(/:(.*?);/)?.[1] || 'image/jpeg'
  const bstr = atob(arr[1])
  let n = bstr.length
  const u8arr = new Uint8Array(n)
  while (n--) u8arr[n] = bstr.charCodeAt(n)
  return new File([u8arr], filename, { type: mime })
}

const onReturnSubmit = async (data: {
  returnItems: ReturnRecord[]
  returnerName: string
  returnerNumber: string
  remark: string
}) => {
  try {

    // 映射为后端需要的格式
    const mappedReturnItems = data.returnItems.map(item => ({
      cabinetId: item.cabinetId,
      cabinetName: item.cabinetTitle,
      cellId: item.cellId,
      cellNumber: item.cellNumber,
      toolName: item.toolName,
      returnTime: item.returnTime
    }))

    await submitReturnRecords({
      returnItems: mappedReturnItems,
      returnerName: data.returnerName,
      returnerNumber: data.returnerNumber,
      remark: data.remark,
      borrowerPhoto:photoUrl.value
    })

    // 清空归还记录和照片
    returnRecords.value = []
    photoUrl.value = ''
    sessionStorage.removeItem('toolOperationData')

    startReturnSuccessCountdown(data.returnItems.length)
  } catch (error) {
    addNotification('归还提交失败，请重试', 'warning')
  }
}

function loadPhotoData() {
  // 从路由 query 参数获取人脸图片
  const faceImage = router.currentRoute.value.query.faceImage as string
  if (faceImage) {
    console.log('从路由参数加载人脸图片URL:', faceImage)
    photoUrl.value = faceImage
    return
  }
}

let resizeTimer: number | null = null
function handleResize() {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = window.setTimeout(() => {
    updateLayout()
  }, 100)
}

watch(currentIndex, (newIdx, oldIdx) => {
  if (newIdx !== oldIdx) {
    console.log(
      `切换到柜子 ${newIdx + 1}，温度：${currentCabinetTemp.value}℃，湿度：${currentCabinetHumidity.value}%`
    )
  }
})

onMounted(() => {
  targetCabinetName.value = (router.currentRoute.value.query.cabinetName as string) || ''
  targetCellNumber.value = (router.currentRoute.value.query.cellNumber as string) || ''

  loadPhotoData()
  loadCabinets()
  updateLayout()
  window.addEventListener('resize', handleResize)
  window.addEventListener('keydown', onGlobalKeydown)
  connectWebSocket()

  addNotification('请使用扫描枪扫描格口二维码或手动输入进行归还', 'info', 8000)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('keydown', onGlobalKeydown)
  if (resizeTimer) clearTimeout(resizeTimer)
  clearEmptyReturnTimer()
  clearReturnSuccessTimer()
  if (scannerTimer.value) clearTimeout(scannerTimer.value)
  stopCloseAndCheckPolling()
  if (socket) socket.close()
})
</script>

<style lang="css" scoped>
/* ================== 全局重置 ================== */
html,
body,
#app {
  height: 100%;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

.return-container {
  width: 100%;
  height: 100vh;
  background: radial-gradient(circle at 20% 30%, #0a1a1f, #051016);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
}

/* ---------- 扫描提示区 – 无模糊、无动画 ---------- */
.scanner-prompt {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px 20px;
  background: rgba(0, 0, 0, 0.7);
  border-bottom: 1px solid rgba(34, 211, 238, 0.3);
  border-radius: 0 0 20px 20px;
  margin: 0 20px;
  z-index: 100;
}

.scanner-prompt.active {
  background: rgba(0, 30, 40, 0.9);
  border-bottom-color: #22d3ee;
  box-shadow: 0 4px 12px rgba(34, 211, 238, 0.15);
}

.scanner-icon {
  font-size: 24px;
  filter: drop-shadow(0 0 4px #22d3ee);
  /* 删除动画 */
}

.scanner-text {
  font-size: 14px;
  font-weight: 600;
  color: #e0f2fe;
  flex: 1;
  text-align: center;
}

.door-open-badge {
  display: inline-block;
  margin-left: 10px;
  background: #10b981;
  padding: 2px 8px;
  border-radius: 20px;
  font-size: 11px;
  color: white;
}

.reset-scan-btn {
  background: rgba(245, 158, 11, 0.8);
  border: none;
  padding: 4px 12px;
  border-radius: 20px;
  color: white;
  font-size: 12px;
  cursor: pointer;
}

.reset-scan-btn:hover {
  background: #f59e0b;
  /* 无 scale */
}

/* 手动输入区域 */
.manual-input-area {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 16px;
}

.manual-input {
  padding: 6px 12px;
  border-radius: 24px;
  border: 1px solid rgba(34, 211, 238, 0.5);
  background: rgba(0, 0, 0, 0.6);
  color: #e0f2fe;
  font-size: 13px;
  outline: none;
  width: 180px;
}

.manual-input:focus {
  border-color: #22d3ee;
  box-shadow: 0 0 4px rgba(34, 211, 238, 0.4);
}

.manual-submit-btn {
  background: rgba(34, 211, 238, 0.3);
  border: 1px solid #22d3ee;
  border-radius: 24px;
  padding: 4px 12px;
  color: #22d3ee;
  cursor: pointer;
  font-size: 12px;
}

.manual-submit-btn:hover {
  background: #22d3ee;
  color: #0a1a1f;
}

/* ---------- 通知 – 无模糊、无动画 ---------- */
.notification-container {
  position: fixed;
  top: 90px;
  right: 24px;
  z-index: 10000;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 420px;
  min-width: 260px;
  pointer-events: none;
}

.notification {
  padding: 14px 24px;
  border-radius: 48px;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  pointer-events: auto;
  white-space: normal;
  word-break: break-word;
  overflow-wrap: break-word;
  max-width: 100%;
  /* 删除动画 */
}

.notification.info,
.notification.success {
  background: rgba(16, 185, 129, 0.95);
  color: white;
  border-left: 4px solid #a7f3d0;
}

.notification.warning {
  background: rgba(245, 158, 11, 0.95);
  color: white;
  border-left: 4px solid #fde68a;
}

.full-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  overflow: hidden;
}

/* ---------- 页面标题 ---------- */
.page-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  height: 60px;
  background: rgba(0, 0, 0, 0.4);
  border-bottom: 1px solid rgba(34, 211, 238, 0.3);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  flex-shrink: 0;
}

.title-icon {
  width: 32px;
  height: 32px;
  display: block;
  flex-shrink: 0;
  filter: drop-shadow(0 0 4px #22d3ee);
}

.page-header h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  background: linear-gradient(135deg, #e0f2fe, #22d3ee);
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
  text-shadow: 0 0 6px rgba(34, 211, 238, 0.3);
  letter-spacing: 2px;
}

/* ---------- 上部区域 ---------- */
.upper-area {
  height: calc(100vh - 350px - 60px - 50px);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.top-section {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  min-height: 0;
  padding: 10px 0;
}

/* ---------- 温湿度卡片 – 无模糊、无过渡 ---------- */
.temp-card,
.humidity-card {
  position: absolute;
  top: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 40px;
  padding: 5px 14px;
  z-index: 500;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
}

.temp-card {
  left: 20px;
  border-left: 3px solid #f87171;
}

.humidity-card {
  right: 20px;
  border-right: 3px solid #4ade80;
}

.card-icon {
  width: 22px;
  height: 22px;
  display: block;
  flex-shrink: 0;
}

.card-value {
  font-size: 18px;
  font-weight: 700;
  min-width: 45px;
  text-align: center;
}

.temp-card .card-value {
  color: #f87171;
  text-shadow: 0 0 4px rgba(248, 113, 113, 0.3);
}

.humidity-card .card-value {
  color: #4ade80;
  text-shadow: 0 0 4px rgba(74, 222, 128, 0.3);
}

.card-label {
  font-size: 11px;
  color: #94a3b8;
  background: rgba(0, 0, 0, 0.4);
  padding: 2px 6px;
  border-radius: 20px;
}

.temp-card:hover,
.humidity-card:hover {
  background: rgba(0, 0, 0, 0.8);
  /* 仅背景变深，无位移 */
}

/* ---------- 3D轮播 – 无过渡 ---------- */
.carousel-cylinder {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  overflow: hidden;
}

.carousel-3d {
  position: relative;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  transform-style: preserve-3d;
  overflow: visible;
}

.cabinet-item {
  position: absolute;
  background: #f1f4f9;
  border-radius: 20px;
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.25);
  border: 1px solid #d0d8e4;
  transform-style: preserve-3d;
  /* 恢复过渡，只针对合成层属性，0.25s 平滑且性能友好 */
  transition: transform 0.25s cubic-bezier(0.2, 0.9, 0.4, 1), opacity 0.25s ease;
  display: flex;
  flex-direction: column;
  min-width: 0;
  will-change: transform, opacity;
  backface-visibility: hidden;
  max-width: 88vw;
}

.cabinet-item.center-highlight {
  border: 2px solid #6fcf97;
  box-shadow: 0 0 12px rgba(100, 220, 160, 0.5);
}

.cabinet-header {
  background: linear-gradient(135deg, #eef2f7, #e3e9f0);
  border-radius: 20px 20px 0 0;
  padding: 8px 10px;
  text-align: center;
  font-weight: 700;
  font-size: 13px;
  color: #1e5a44;
  border-bottom: 1px solid #cbd5e0;
}

.cabinet-body {
  padding: 12px;
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.cabinet-grid {
  display: grid;
  gap: 8px;
  position: relative;
  z-index: 2;
  min-height: 260px;
}

.indicator-upper {
  display: flex;
  justify-content: center;
  margin: 4px 0 2px;
  flex-shrink: 0;
}

.indicator-text {
  background: rgba(0, 0, 0, 0.6);
  padding: 4px 16px;
  border-radius: 40px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #c2f0e0;
  border: 1px solid rgba(34, 211, 238, 0.5);
}

.divider {
  width: 85%;
  height: 2px;
  margin: 4px auto;
  background: linear-gradient(90deg, transparent, #22d3ee, #3b82f6, #22d3ee, transparent);
  border-radius: 4px;
  flex-shrink: 0;
}

/* ---------- 格口样式 – 无过渡动画 ---------- */
.cell-container {
  position: relative;
  background: #f9fbfe;
  border: 1px solid #cfdde6;
  border-radius: 12px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.7), 0 2px 8px rgba(0, 0, 0, 0.02);
  cursor: pointer;
  box-sizing: border-box;
  height: 100%;
}

.cell-container:active {
  /* 无 scale */
}

.cell-container:hover {
  border-color: #22d3ee;
  box-shadow: 0 0 0 2px rgba(34, 211, 238, 0.3), inset 0 0 0 1px rgba(255, 255, 255, 0.7);
}

.custom-image-cell {
  background: rgba(0, 0, 0, 0.4);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(34, 211, 238, 0.5);
  box-shadow: 0 0 8px rgba(0, 255, 255, 0.15);
  overflow: hidden;
  cursor: default;
}

.custom-image-cell img {
  max-width: 75%;
  max-height: 65%;
  object-fit: contain;
  border-radius: 8px;
}

.image-label {
  margin-top: 6px;
  font-size: 9px;
  color: #c2f0e0;
  background: rgba(0, 0, 0, 0.5);
  padding: 2px 8px;
  border-radius: 20px;
}

.cell-inner {
  position: absolute;
  inset: 0;
  background: #fffffffa;
  border-radius: 10px;
  transform: translateZ(-8px);
  pointer-events: none;
}

.cabinet-cell {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(235, 250, 240, 0.94);
  border: 1.5px solid rgba(70, 150, 110, 0.8);
  border-radius: 10px;
  transform: translateZ(6px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
  /* 删除 transition，开门瞬间旋转 */
}

.cabinet-cell.door-open {
  transform: translateZ(6px) rotateY(-75deg);
  box-shadow: -8px 0 16px rgba(0, 0, 0, 0.3), inset -1px 0 0 rgba(255, 255, 255, 0.5);
}

.cabinet-cell.empty-door {
  background: rgba(255, 255, 245, 0.6);
  border-color: rgba(140, 180, 160, 0.6);
}

.cell-number {
  position: absolute;
  top: 3px;
  left: 6px;
  font-size: 9px;
  font-weight: 800;
  color: #236b4c;
  background: rgba(250, 255, 240, 0.9);
  padding: 2px 6px;
  border-radius: 28px;
  z-index: 2;
  white-space: nowrap;
}

.tool-name {
  font-size: 8px;
  font-weight: 600;
  color: #1b5e42;
  background: rgba(255, 255, 245, 0.85);
  padding: 2px 5px;
  border-radius: 20px;
  text-align: center;
  max-width: 90%;
  margin-top: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.empty-door .tool-name {
  display: none;
}

/* ---------- 导航按钮 – 无过渡、无模糊 ---------- */
.nav-btn-left,
.nav-btn-right {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: rgba(10, 25, 30, 0.85);
  border: 1.5px solid rgba(34, 211, 238, 0.7);
  border-radius: 60px;
  color: #e0f2fe;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  z-index: 400;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
  white-space: nowrap;
}

.nav-btn-left {
  left: 12px;
}

.nav-btn-right {
  right: 12px;
}

.arrow {
  font-size: 1.3rem;
  line-height: 1;
}

.btn-text {
  font-size: 0.85rem;
  font-weight: 600;
}

.nav-btn-left:hover:not(.disabled),
.nav-btn-right:hover:not(.disabled) {
  background: rgba(14, 165, 233, 0.9);
  border-color: #7dd3fc;
  color: #0a1a1f;
  /* 无 scale/位移 */
}

.nav-btn-left.disabled,
.nav-btn-right.disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* ---------- 下部区域 ---------- */
.bottom-section {
  height: 350px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 16px;
  overflow: hidden;
  background: transparent;
}

.bottom-container {
  display: flex;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  gap: 16px;
  align-items: stretch;
  height: 100%;
}

.photo-area {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.photo-card {
  position: relative;
  width: 100%;
  max-width: 180px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(34, 211, 238, 0.5);
}

.preview-image {
  width: 100%;
  height: auto;
  display: block;
}

.photo-badge {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.7);
  padding: 4px 8px;
  border-radius: 20px;
  font-size: 10px;
  color: #4ade80;
}

.photo-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 16px;
  padding: 20px;
  border: 1px dashed rgba(34, 211, 238, 0.5);
}

.placeholder-icon {
  width: 48px;
  height: 48px;
  color: #94a3b8;
}

/* ---------- 归还信息表格 – 无模糊 ---------- */
.info-area {
  flex: 0 0 auto;
  width: 450px;
  min-width: 0;
  display: flex;
  align-items: center;
  background: rgba(0, 0, 0, 0.4);
  border-radius: 20px;
  padding: 8px 12px;
  border: 1px solid rgba(34, 211, 238, 0.2);
  overflow: hidden;
}

.info-list {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.info-header {
  display: grid;
  grid-template-columns: 1fr 0.8fr 1.2fr 1.5fr;
  gap: 12px;
  padding: 8px 8px 6px;
  border-bottom: 1px solid rgba(34, 211, 238, 0.3);
  margin-bottom: 4px;
  font-weight: 700;
  font-size: 12px;
  color: #a5f3fc;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

.info-scroll::-webkit-scrollbar {
  width: 4px;
}

.info-scroll::-webkit-scrollbar-track {
  background: #1e293b;
  border-radius: 4px;
}

.info-scroll::-webkit-scrollbar-thumb {
  background: #22d3ee;
  border-radius: 4px;
}

.info-row {
  display: grid;
  grid-template-columns: 1fr 0.8fr 1.2fr 1.5fr;
  gap: 12px;
  padding: 8px 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  font-size: 12px;
  flex-shrink: 0;
}

.info-row:hover {
  background: rgba(34, 211, 238, 0.1);
}

.info-row.return-record {
  background: rgba(0, 0, 0, 0.2);
}

.placeholder-row {
  text-align: center;
  color: #94a3b8;
  font-style: italic;
}

.row-item {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #e2e8f0;
  font-weight: 500;
}

.button-area {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.complete-btn {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 24px 16px;
  border: none;
  border-radius: 32px;
  font-weight: 700;
  cursor: pointer;
  background: linear-gradient(135deg, #f59e0b, #d97706);
  border: 1px solid rgba(245, 158, 11, 0.6);
  color: white;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.25);
  min-height: 100px;
  min-width: 140px;
}

.complete-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  /* 仅颜色变化，无位移/阴影变化 */
}

.complete-btn:active:not(:disabled) {
  /* 无位移 */
}

.complete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.complete-btn .btn-icon {
  width: 40px;
  height: 40px;
  display: block;
  flex-shrink: 0;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
}

.complete-btn .btn-label {
  font-size: 18px;
  letter-spacing: 2px;
  font-weight: 700;
}

/* ---------- 加载 & 空状态 – 保留旋转动画（仅初始化出现） ---------- */
.loading-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  color: #c2f0e0;
  gap: 16px;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid rgba(34, 211, 238, 0.3);
  border-top-color: #22d3ee;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.empty-state {
  text-align: center;
  color: #ffb347;
  background: rgba(0, 0, 0, 0.6);
  padding: 50px;
  border-radius: 30px;
  margin: auto;
  width: fit-content;
}

/* ---------- 模态框 – 无模糊、无动画 ---------- */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 20000;
}

.modal-overlay.no-close {
  cursor: default;
}

.modal-container {
  background: linear-gradient(145deg, #1e2a32, #0f1a1f);
  border-radius: 32px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(34, 211, 238, 0.3);
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid rgba(34, 211, 238, 0.3);
  background: rgba(0, 0, 0, 0.3);
}

.modal-header h3 {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 700;
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.6rem;
  cursor: pointer;
  color: #94a3b8;
  line-height: 1;
  padding: 0 8px;
}

.close-btn:hover {
  color: #f87171;
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  background: rgba(0, 0, 0, 0.2);
}

.modal-footer {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  border-top: 1px solid rgba(34, 211, 238, 0.2);
  background: rgba(0, 0, 0, 0.3);
}

.modal-icon {
  width: 28px;
  height: 28px;
  display: block;
  flex-shrink: 0;
  filter: drop-shadow(0 0 4px rgba(34, 211, 238, 0.3));
}

.confirm-btn {
  background: linear-gradient(135deg, #10b981, #059669);
  border: none;
  padding: 8px 28px;
  border-radius: 40px;
  font-weight: 600;
  font-size: 1rem;
  color: white;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.3);
}

.confirm-btn:hover {
  background: linear-gradient(135deg, #0fba7a, #048a5a);
}

.cancel-btn {
  background: rgba(100, 116, 139, 0.8);
  border: none;
  padding: 8px 28px;
  border-radius: 40px;
  font-weight: 600;
  font-size: 1rem;
  color: white;
  cursor: pointer;
}

.cancel-btn:hover {
  background: rgba(71, 85, 105, 0.9);
}

/* ---------- 信息模态框 ---------- */
.info-modal .modal-container {
  border-left: 4px solid #22d3ee;
}

.info-header h3 {
  color: #22d3ee;
}

.info-tip {
  color: #c2f0e0;
  margin-bottom: 16px;
  font-size: 1rem;
  text-align: center;
}

.countdown-text {
  text-align: center;
  font-size: 1.1rem;
  color: #facc15;
  margin-top: 16px;
}

.countdown-number {
  font-size: 1.6rem;
  font-weight: 800;
  color: #f97316;
  margin: 0 4px;
}

/* ---------- 成功模态框 ---------- */
.success-modal .modal-container {
  border-left: 4px solid #10b981;
}

.success-header h3 {
  color: #10b981;
}

.success-tip {
  color: #a7f3d0;
  margin-bottom: 16px;
  font-size: 1rem;
  text-align: center;
}

.success-tip strong {
  color: #4ade80;
  font-size: 1.3rem;
}

/* ---------- 响应式 – 无动画 ---------- */
@media (max-width: 680px) {
  .page-header {
    height: 50px;
    gap: 8px;
  }

  .title-icon {
    width: 26px;
    height: 26px;
  }

  .page-header h1 {
    font-size: 20px;
  }

  .upper-area {
    height: calc(100vh - 350px - 50px - 50px);
  }

  .info-area {
    width: 320px;
    padding: 8px;
  }

  .info-header,
  .info-row {
    grid-template-columns: 1fr 0.7fr 1fr 1.3fr;
    gap: 8px;
  }

  .header-item,
  .row-item {
    font-size: 10px;
  }

  .complete-btn .btn-icon {
    width: 32px;
    height: 32px;
  }

  .complete-btn .btn-label {
    font-size: 14px;
  }

  .complete-btn {
    padding: 16px 12px;
    min-height: 80px;
  }

  .temp-card,
  .humidity-card {
    padding: 3px 10px;
    top: 4px;
  }

  .card-value {
    font-size: 14px;
    min-width: 35px;
  }

  .scanner-prompt {
    padding: 6px 12px;
    margin: 0 12px;
  }

  .scanner-text {
    font-size: 11px;
  }

  .manual-input {
    width: 120px;
    font-size: 11px;
    padding: 4px 8px;
  }

  .manual-submit-btn {
    padding: 3px 8px;
    font-size: 10px;
  }

  .empty-icon {
    width: 26px;
    height: 26px;
  }

  .modal-icon {
    width: 24px;
    height: 24px;
  }

  .card-icon {
    width: 18px;
    height: 18px;
  }
}

@media (max-width: 480px) {
  .page-header h1 {
    font-size: 18px;
  }

  .title-icon {
    width: 22px;
    height: 22px;
  }

  .cabinet-item {
    width: 260px !important;
  }

  .bottom-container {
    gap: 8px;
  }

  .info-area {
    width: 260px;
    padding: 6px;
  }

  .info-header,
  .info-row {
    grid-template-columns: 1fr 0.6fr 0.9fr 1.2fr;
    gap: 6px;
  }

  .header-item,
  .row-item {
    font-size: 9px;
  }

  .info-row {
    padding: 6px 4px;
  }

  .complete-btn .btn-icon {
    width: 28px;
    height: 28px;
  }

  .complete-btn .btn-label {
    font-size: 12px;
  }

  .complete-btn {
    padding: 12px 8px;
    min-height: 70px;
    gap: 6px;
  }

  .placeholder-icon {
    width: 24px;
    height: 24px;
  }

  .photo-placeholder span {
    font-size: 9px;
  }

  .temp-card,
  .humidity-card {
    padding: 2px 8px;
    gap: 4px;
  }

  .card-icon {
    width: 16px;
    height: 16px;
  }

  .card-value {
    font-size: 12px;
    min-width: 30px;
  }

  .card-label {
    font-size: 8px;
  }

  .scanner-prompt {
    padding: 4px 8px;
  }

  .scanner-icon {
    font-size: 18px;
  }

  .scanner-text {
    font-size: 10px;
  }

  .reset-scan-btn {
    font-size: 10px;
    padding: 2px 8px;
  }

  .manual-input {
    width: 100px;
    font-size: 10px;
    padding: 3px 6px;
  }

  .manual-submit-btn {
    padding: 2px 6px;
    font-size: 9px;
  }

  .modal-icon {
    width: 20px;
    height: 20px;
  }

  .empty-icon {
    width: 22px;
    height: 22px;
  }
}

.empty-icon {
  width: 32px;
  height: 32px;
  display: block;
  flex-shrink: 0;
  filter: drop-shadow(0 0 4px #f97316);
}
</style>

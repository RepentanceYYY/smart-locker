<template>
  <div class="home-container">
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
      <div class="full-layout">
        <!-- 美化后的标题栏 -->
        <header ref="headerRef" class="app-header">
          <div class="header-left">
            <div class="logo-wrapper">
              <img src="/jg.svg" alt="系统Logo" class="logo-icon" />
              <span class="status-dot"></span>
            </div>
            <div class="title-wrapper">
              <span class="title-text">{{ systemConfigStore.systemName }}</span>
              <span class="title-sub">{{ systemConfigStore.engName }}</span>
            </div>
            <div class="title-wrapper">
              <span class="title-text">({{ systemConfigStore.location }})</span>
            </div>
          </div>
          <div class="header-right">
            <div class="time-wrapper">
              <span class="time-icon">⏱️</span>
              <span class="header-time">{{ currentTime }}</span>
            </div>
          </div>
        </header>

        <div class="upper-area">
          <div class="top-section">
            <!-- 左侧温度卡片 -->
            <div class="temp-card">
              <img src="/温度.svg" alt="温度Logo" class="card-icon" />
              <div class="card-value">{{ currentCabinetTemp }}°</div>
              <div class="card-label">温度</div>
            </div>

            <!-- 右侧湿度卡片 -->
            <div class="humidity-card">
              <img src="/湿度-01.svg" alt="湿度Logo" class="card-icon" />
              <div class="card-value">{{ currentCabinetHumidity }}%</div>
              <div class="card-label">湿度</div>
            </div>

            <button class="nav-btn-left" :class="{ disabled: currentIndex === 0 }" @click="rotatePrev">
              <span class="arrow">◀</span><span class="btn-text">上一个</span>
            </button>
            <button class="nav-btn-right" :class="{ disabled: currentIndex === totalCount - 1 }" @click="rotateNext">
              <span class="btn-text">下一个</span><span class="arrow">▶</span>
            </button>

            <div class="carousel-cylinder">
              <div class="carousel-3d" :style="{ minHeight: carouselHeight + 'px' }">
                <div
                    v-for="(cab, idx) in cabinets"
                    :key="cab.id"
                    class="cabinet-item"
                    :class="{ 'center-highlight': idx === currentIndex }"
                    :style="[cabinetStyles[idx], { width: cab.width || '280px', height: cab.height || 'auto' }]"
                >
                  <div class="cabinet-header">
                    {{ cab.title }}
                  </div>
                  <div class="cabinet-body">
                    <div class="cabinet-grid" v-memo="[cab.id]" :style="getGridStyle(cab)">
                      <template v-for="(cell, cellIdx) in cab.flatCells" :key="cellIdx">
                        <div v-if="cell.type === 'cell'" class="cell-container"
                             :style="[getCellPosition(cell), cell.cellStyle]" :class="{ 'empty-door': cell.isEmpty }"
                             @click="showCellDetail(cell)">
                          <div class="cell-inner"></div>
                          <div class="cabinet-cell" :class="{ 'empty-door': cell.isEmpty }">
                            <div class="hinge"></div>
                            <span class="cell-number">{{ cell.number }}</span>
                            <span class="tool-name">{{ truncateText(cell.toolName, 8) }}</span>
                            <div class="handle"></div>
                          </div>
                        </div>
                        <div v-else-if="cell.type === 'image'" class="custom-image-cell"
                             :style="[getCellPosition(cell), cell.cellStyle]">
                          <img :src="formatImageUrl(cell.imageUrl)" :alt="cell.label || '图标'" />
                          <span v-if="cell.label" class="image-label">{{ truncateText(cell.label, 10) }}</span>
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

        <!-- 下部区域：固定高度 -->
        <div class="bottom-section">
          <div class="big-buttons-container">
            <button :disabled="isAllowClickButton" class="big-action-btn borrow-btn" @click="handleBorrow">
              <img src="/盒子.svg" alt="领用" class="btn-icon" />
              <span class="btn-label">领用</span>
            </button>
            <button :disabled="isAllowClickButton" class="big-action-btn return-btn" @click="handleReturn">
              <img src="/进行中.svg" alt="归还" class="btn-icon" />
              <span class="btn-label">归还</span>
            </button>
            <button :disabled="isAllowClickButton" class="big-action-btn inventory-btn" @click="handleInventory">
              <img src="/盘点单.svg" alt="盘点" class="btn-icon" />
              <span class="btn-label">盘点</span>
            </button>
            <button :disabled="isAllowClickButton" class="big-action-btn settings-btn" @click="handleSettings">
              <img src="/设置.svg" alt="设置" class="btn-icon" />
              <span class="btn-label">设置</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 格口详情弹窗 -->
    <div v-if="showDialog" class="dialog-overlay" @click="closeDialog">
      <div class="dialog-content" @click.stop>
        <div class="dialog-header">
          <h3>{{ dialogTitle }}</h3>
          <button class="dialog-close" @click="closeDialog">✕</button>
        </div>
        <div class="dialog-body">
          <div class="detail-row">
            <span class="detail-label">柜子名称：</span>
            <span class="detail-value">{{ currentCabinetName }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">格口号：</span>
            <span class="detail-value">{{ dialogData.number || '无' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">工具名称：</span>
            <span class="detail-value">{{ dialogData.toolName || '空置' }}</span>
          </div>
          <div v-if="dialogData.isEmpty" class="detail-row empty-warning">
            <span class="detail-label">状态：</span>
            <span class="detail-value">空柜门</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">当前温度：</span>
            <span class="detail-value">{{ currentCabinetTemp }}°C</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">当前湿度：</span>
            <span class="detail-value">{{ currentCabinetHumidity }}%</span>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="dialog-action-btn" @click="handleDialogAction">确定</button>
        </div>
      </div>
    </div>

    <!-- 相机弹窗 -->
    <CameraModal v-model:visible="showCameraModal" :isBorrow="isBorrowMode" @confirm="handlePhotoConfirm" @notify="addNotification" @faceRecognized="handleFaceRecognized" />

    <InventoryDialog :visible="showInventoryDialog" :inventoryResult="inventoryDialogResult"
                     @close="closeInventoryDialog" @cancel="closeInventoryDialog" @confirm="closeInventoryDialog" />

    <!-- 密码弹窗（设置验证） -->
    <div v-if="showPasswordDialog" class="dialog-overlay" @click="closePasswordDialog">
      <div class="dialog-content password-dialog" @click.stop>
        <div class="dialog-header">
          <h3>
            <img src="/密码锁.svg" alt="锁" class="lock-icon" />
            管理员验证<span v-if="passwordDialogType === 'inventory'"> - 盘点</span>
          </h3>
          <button class="dialog-close" @click="closePasswordDialog">✕</button>
        </div>
        <div class="dialog-body">
          <div class="password-timer">
            <img src="/计时器.svg" alt="计时器" class="timer-icon" />
            <span class="timer-text" :class="{ 'timer-warning': passwordSecondsLeft <= 5 }">
              剩余时间 {{ passwordSecondsLeft }} 秒
            </span>
          </div>
          <div class="password-input-group">
            <input ref="passwordInputRef" type="password" v-model="passwordInput" placeholder="请输入管理员密码" class="password-input"
                   @keyup.enter="verifyPassword" @input="onPasswordInput" autofocus />
            <div v-if="passwordError" class="password-error">
              ❌ {{ passwordError }}
            </div>
          </div>
        </div>
        <div class="dialog-footer password-footer">
          <button class="dialog-action-btn cancel-btn" @click="closePasswordDialog">取消</button>
          <button class="dialog-action-btn confirm-btn" :class="{
            'settings-confirm-btn': passwordDialogType === 'settings',
            'inventory-confirm-btn': passwordDialogType === 'inventory'
          }" @click="verifyPassword">
            验证
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { fetchCabinetList } from '@/api/cabinet'
import CameraModal from './CameraModal.vue'
import InventoryDialog from './InventoryDialog.vue'
import { useSystemConfigStore } from '@/stores/systemConfig'
import { useDehumidifierStore } from '@/stores/useDehumidifier'
import { useCountdown } from '@/composables/useCountdown'
import type { CSSProperties } from 'vue'
import { formatImageUrl } from '@/utils/fileUtils'


const router = useRouter()
const systemConfigStore = useSystemConfigStore()
const dehumidifierStore = useDehumidifierStore();

const passwordDialogType = ref<'inventory' | 'settings'>('settings')
const isAllowClickButton = ref(false)
const showInventoryDialog = ref(false)
const inventoryDialogResult = ref<any>(null)
const passwordInputRef = ref<HTMLInputElement | null>(null)

const showCameraModal = ref(false)
const isBorrowMode = ref(true)

const showPasswordDialog = ref(false)
const passwordInput = ref('')
const passwordError = ref('')

const {
  secondsLeft: passwordSecondsLeft,
  reset: resetPasswordCountdown,
  cleanup: cleanupPasswordCountdown,
  handleOperation: handlePasswordOperation
} = useCountdown({
  onTimeout: () => {
    closePasswordDialog()
  }
})

const headerRef = ref<HTMLElement | null>(null)
const currentTime = ref('')

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
  number: string
  toolName: string
  isEmpty: boolean
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

interface CabinetEnvData {
  temperature: number
  humidity: number
  lastUpdate: string
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
  envData: CabinetEnvData
}

// ================== 辅助函数 ==================
function expandColumns(columns: string, count: number): string[] {
  const parts = columns.trim().split(/\s+/)
  if (parts.length === 1 && count > 1) {
    return Array(count).fill(parts[0])
  }
  if (parts.length !== count) {
    console.warn(`columns 值个数 (${parts.length}) 与 colSpan (${count}) 不匹配，将按原样使用。`)
  }
  return parts
}

function expandHeight(height: string, count: number): string[] {
  const parts = height.trim().split(/\s+/)
  if (parts.length === 1 && count > 1) {
    return Array(count).fill(parts[0])
  }
  if (parts.length !== count) {
    console.warn(`height 值个数 (${parts.length}) 与 rowSpan (${count}) 不匹配，将按原样使用。`)
  }
  return parts
}

function flattenCells(cab: CabinetConfig) {
  const flatCells: any[] = []
  const rowHeights: string[] = []
  const colWidths: string[] = []

  let totalRows = 0
  for (const row of cab.rows) {
    let rowMaxSpan = 1
    for (const cell of row.cells) {
      rowMaxSpan = Math.max(rowMaxSpan, cell.rowSpan)
    }
    totalRows += rowMaxSpan
  }

  let currentRowIdx = 0
  for (const row of cab.rows) {
    const rowStart = currentRowIdx + 1
    let groupRowSpan = 1
    for (const cell of row.cells) {
      groupRowSpan = Math.max(groupRowSpan, cell.rowSpan)
    }
    let colCursor = 0
    for (const cell of row.cells) {
      const startCol = colCursor
      const endCol = colCursor + cell.colSpan
      const colValues = expandColumns(cell.columns, cell.colSpan)
      const rowValues = expandHeight(cell.height, cell.rowSpan)

      for (let i = 0; i < colValues.length; i++) {
        const colIndex = startCol + i
        if (colWidths[colIndex] === undefined) {
          colWidths[colIndex] = colValues[i]
        }
      }

      for (let i = 0; i < rowValues.length; i++) {
        const rowIndex = rowStart + i - 1
        if (rowHeights[rowIndex] === undefined) {
          rowHeights[rowIndex] = rowValues[i]
        }
      }

      flatCells.push({
        ...cell,
        gridRowStart: rowStart,
        gridRowEnd: rowStart + cell.rowSpan,
        gridColumnStart: startCol + 1,
        gridColumnEnd: endCol + 1,
        originalColumns: colValues,
        originalHeight: rowValues,
      })
      colCursor = endCol
    }
    currentRowIdx += groupRowSpan
  }

  for (let i = 0; i < colWidths.length; i++) {
    if (!colWidths[i]) colWidths[i] = '1fr'
  }
  for (let i = 0; i < rowHeights.length; i++) {
    if (!rowHeights[i]) rowHeights[i] = 'auto'
  }

  return { flatCells, colWidths, rowHeights }
}

function getGridTemplate({ colWidths, rowHeights }: { colWidths: string[], rowHeights: string[] }) {
  return {
    display: 'grid',
    gridTemplateRows: rowHeights.join(' '),
    gridTemplateColumns: colWidths.join(' '),
    gap: '10px',
    position: 'relative',
    zIndex: 2
  }
}

function getCellPosition(cell: any) {
  return {
    gridRow: `${cell.gridRowStart} / ${cell.gridRowEnd}`,
    gridColumn: `${cell.gridColumnStart} / ${cell.gridColumnEnd}`,
  }
}

function generateRandomEnvData(baseTemp?: number, baseHumidity?: number): CabinetEnvData {
  const tempOffset = (Math.random() - 0.5) * 6
  const humidityOffset = (Math.random() - 0.5) * 20

  let temp: number
  let humidity: number

  if (baseTemp !== undefined) {
    temp = Math.min(35, Math.max(10, baseTemp + (Math.random() - 0.5) * 2))
  } else {
    temp = Number((20 + tempOffset + Math.random() * 6).toFixed(1))
  }

  if (baseHumidity !== undefined) {
    humidity = Math.min(85, Math.max(25, baseHumidity + (Math.random() - 0.5) * 8))
  } else {
    humidity = Math.floor(40 + humidityOffset + Math.random() * 30)
  }

  return {
    temperature: temp,
    humidity: Math.min(85, Math.max(25, humidity)),
    lastUpdate: new Date().toLocaleTimeString()
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
        isEmpty: cell.type === 'cell' ? (cell.isEmpty || false) : false,
        number: cell.type === 'cell' ? (cell.number || '') : '',
        toolName: cell.type === 'cell' ? (cell.toolName || '') : '',
        imageUrl: cell.type === 'image' ? (cell.imageUrl || '') : '',
        label: cell.type === 'image' ? (cell.label || '') : '',
      }))
    }))

    const { flatCells, colWidths, rowHeights } = flattenCells({ ...cab, rows })
    const initialEnvData: CabinetEnvData = {
      temperature: 0,
      humidity: 0,
      lastUpdate: '--'
    }

    return {
      ...cab,
      width: cab.width || '280px',
      height: cab.height || 'auto',
      rows,
      flatCells,
      colWidths,
      rowHeights,
      gridStyle: getGridTemplate({ colWidths, rowHeights }),
      envData: initialEnvData
    } as ProcessedCabinet
  })
}

function truncateText(text: string, maxLen: number): string {
  if (!text) return ''
  return text.length > maxLen ? text.slice(0, maxLen) + '...' : text
}

// ================== 响应式数据 ==================
const cabinets = ref<ProcessedCabinet[]>([])
const loading = ref(true)
const currentIndex = ref(0)
const totalCount = computed(() => cabinets.value.length)

const radius = ref(320)
const carouselHeight = ref(600)
const maxScale = ref(1.4)

const showDialog = ref(false)
const dialogTitle = ref('')
const dialogData = ref<any>({})
const selectedCell = ref<any>(null)

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

const currentCabinetName = computed(() => {
  if (cabinets.value.length === 0) return '--'
  return cabinets.value[currentIndex.value]?.title || '--'
})

// ================== 性能优化：只渲染当前及相邻柜子的样式 ==================
const getCabinetStyle = (idx: number): CSSProperties => {
  if (totalCount.value === 0) return { display: 'none' }
  const diff = Math.abs(idx - currentIndex.value)
  // 只显示当前及左右相邻的柜子（共3个）
  if (diff > 1) {
    return { display: 'none' }
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

// 使用计算属性缓存所有柜子的样式，避免重复计算
const cabinetStyles = computed(() => {
  return cabinets.value.map((_, idx) => getCabinetStyle(idx))
})

function rotatePrev() {
  if (currentIndex.value > 0) {
    currentIndex.value--
  }
}

function rotateNext() {
  if (currentIndex.value < totalCount.value - 1) {
    currentIndex.value++
  }
}

function getGridStyle(cab: ProcessedCabinet) {
  return cab.gridStyle
}

// ================== 格口点击 ==================
function showCellDetail(cell: any) {
  if (cell.type !== 'cell') return
  selectedCell.value = cell
  dialogTitle.value = `格口 ${cell.number || '未知'}`
  dialogData.value = {
    number: cell.number,
    toolName: cell.toolName || '（空置）',
    isEmpty: cell.isEmpty
  }
  showDialog.value = true
}

function closeDialog() {
  showDialog.value = false
  selectedCell.value = null
}

function handleDialogAction() {
  closeDialog()
}

// ================== 三大按钮 ==================
function handleBorrow() {
  if (isAllowClickButton.value) {
    addNotification('盘点中，稍后再试', 'warning')
    return
  }
  isBorrowMode.value = true
  showCameraModal.value = true
}

function handleReturn() {
  if (isAllowClickButton.value) {
    addNotification('盘点中，稍后再试', 'warning')
    return
  }
  isBorrowMode.value = false
  showCameraModal.value = true
}

function handleInventory() {
  if (isAllowClickButton.value) {
    addNotification('盘点中，稍后再试', 'warning')
    return
  }
  passwordDialogType.value = 'inventory'
  openPasswordDialog('inventory')
}

function openPasswordDialog(type: 'inventory' | 'settings') {
  passwordDialogType.value = type
  passwordInput.value = ''
  passwordError.value = ''
  showPasswordDialog.value = true

  nextTick(() => {
    resetPasswordCountdown()
    passwordInputRef.value?.focus()
  })
}

function closePasswordDialog() {
  showPasswordDialog.value = false
  cleanupPasswordCountdown()
}

function onPasswordInput() {
  if (showPasswordDialog.value) {
    handlePasswordOperation()
    if (passwordError.value) {
      passwordError.value = ''
    }
  }
}

// WebSocket - 盘点
const wsUrl = `${import.meta.env.VITE_WS_BASE_URL}/inventory`
let socket: WebSocket | null = null
const wsConnected = ref(false)
let allowReconnect = true

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
    addNotification('与服务器连接异常，请刷新页面重试', 'warning')
  }
  socket.onclose = (event) => {
    console.log('WebSocket 连接关闭', event.code, event.reason)
    wsConnected.value = false
    // // 如果是因为相机打开失败或连接问题导致的关闭（code 1006 表示异常关闭）
    // if (event.code === 1006 || event.code === 1011) {
    //   addNotification('相机打开失败或连接异常，请检查设备后重试', 'warning')
    // }
    if (allowReconnect) {
      setTimeout(() => connectWebSocket(), 3000)
    }
  }
}

function sendMessage(type: string, data: any) {
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    if (type === 'inventory') {
      addNotification('相机服务未连接，请检查相机设备是否正常', 'warning')
    } else {
      addNotification('网络连接异常，请稍后重试', 'warning')
    }
    return false
  }
  socket.send(JSON.stringify({ type, data }))
  return true
}

async function handleWebSocketMessage(msg: any) {
  const { type } = msg || {}
  if (type === 'inventory') {
    handleInventoryReply(msg)
  }
}

function handleInventoryReply(msg: any) {
  const { code, data, message: msgText } = msg || {}
  if (code === 200) {
    const payload = {
      ...data,
      inventoryTime: new Date().toISOString(),
    }
    inventoryDialogResult.value = payload
    isAllowClickButton.value = true
    showInventoryDialog.value = true
    return
  }
  isAllowClickButton.value = false
  addNotification(msgText || '盘点失败，请重试', 'warning')
}

function closeInventoryDialog() {
  showInventoryDialog.value = false
  inventoryDialogResult.value = null
  isAllowClickButton.value = false
}

// ================== 通知列表 ==================
interface Notification {
  id: number
  text: string
  type: 'info' | 'success' | 'warning'
}
const notifications = ref<Notification[]>([])
let nextNotificationId = 1

function addNotification(text: string, type: 'info' | 'success' | 'warning' = 'info', duration = 5000) {
  const id = nextNotificationId++
  const notification: Notification = { id, text, type }
  notifications.value.push(notification)
  setTimeout(() => {
    notifications.value = notifications.value.filter(n => n.id !== id)
  }, duration)
}

function verifyPassword() {
  if (passwordInput.value === systemConfigStore.adminPwd) {
    cleanupPasswordCountdown()
    showPasswordDialog.value = false
    if (passwordDialogType.value === 'inventory') {
      if (isAllowClickButton.value) {
        addNotification('盘点中，稍后再试', 'warning')
        return
      }
      isAllowClickButton.value = true
      addNotification('验证成功，正在盘点中...', 'info')
      const ok = sendMessage('inventory', {})
      if (!ok) {
        isAllowClickButton.value = false
      }
    } else if (passwordDialogType.value === 'settings') {
      router.push('/settings')
    }
  } else {
    passwordError.value = '密码错误，请重试'
    handlePasswordOperation()
  }
}

function handleSettings() {
  if (isAllowClickButton.value) {
    addNotification('盘点中，稍后再试', 'warning')
    return
  }
  passwordDialogType.value = 'settings'
  openPasswordDialog('settings')
}

function handlePhotoConfirm(imageData: string) {
  console.log('用户点击确认，图片数据:', imageData)
  
  // 跳转到对应页面，通过query参数传递图片数据
  if (isBorrowMode.value) {
    console.log('跳转到领用页面')
    router.push({
      path: '/borrow',
      query: {
        faceImage: imageData,
        timestamp: Date.now().toString()
      }
    })
  } else {
    console.log('跳转到归还页面')
    router.push({
      path: '/return',
      query: {
        faceImage: imageData,
        timestamp: Date.now().toString()
      }
    })
  }
}

// 人脸识别成功后处理
function handleFaceRecognized(faceImageUrl: string) {
  console.log('人脸识别成功，图片URL:', faceImageUrl)
}

// ================== 性能优化：温湿度更新只改当前柜子 ==================
function updateCurrentCabinetEnv() {
  const cab = cabinets.value[currentIndex.value]
  if (!cab) return
  const newData = generateRandomEnvData(cab.initialTemp, cab.initialHumidity)
  // 直接修改响应式对象的属性，触发最小更新
  cab.envData = newData
}

// ================== 时间更新 ==================
function updateCurrentTime() {
  const now = new Date()
  const hours = now.getHours().toString().padStart(2, '0')
  const minutes = now.getMinutes().toString().padStart(2, '0')
  currentTime.value = `${hours}:${minutes}`
}

// ================== 布局更新 ==================
function updateLayout() {
  if (typeof window === 'undefined') return
  const width = window.innerWidth
  const height = window.innerHeight

  const headerHeight = headerRef.value ? headerRef.value.offsetHeight : 60
  const bottomHeight = 350

  const upperAreaHeight = height - bottomHeight - headerHeight
  carouselHeight.value = Math.max(450, upperAreaHeight - 55)
  radius.value = Math.min(width * 0.45, 380)
  maxScale.value = Math.min(2, Math.max(1.2, width / 220))
}

let resizeTimer: number | null = null
function handleResize() {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = window.setTimeout(() => {
    updateLayout()
  }, 100)
}

// ================== 合并定时器到 requestAnimationFrame ==================
let rafId: number | null = null
let lastEnvUpdate = 0
const ENV_UPDATE_INTERVAL = 10000 // 10秒更新一次温湿度

function frameLoop(time: number) {
  // 更新时间（每秒更新）
  updateCurrentTime()

  // 温湿度更新（间隔10秒）
  if (time - lastEnvUpdate >= ENV_UPDATE_INTERVAL) {
    updateCurrentCabinetEnv()
    lastEnvUpdate = time
  }

  rafId = requestAnimationFrame(frameLoop)
}

// ================== 加载数据 ==================
async function loadCabinets() {
  loading.value = true
  try {
    const rawData = await fetchCabinetList()
    console.log(rawData)
    const processed = processCabinetData(rawData)
    cabinets.value = processed
    const defaultIdx = processed.findIndex(cab => cab.isDefault === true)
    currentIndex.value = defaultIdx !== -1 ? defaultIdx : 0
  } catch (error) {
    console.error('加载柜子配置失败:', error)
    cabinets.value = []
  } finally {
    loading.value = false
  }
}

watch(currentIndex, (newIdx, oldIdx) => {
  if (newIdx !== oldIdx) {
    console.log(`切换到柜子 ${newIdx + 1}，温度：${currentCabinetTemp.value}℃，湿度：${currentCabinetHumidity.value}%`)
    // 切换时立即更新一次该柜子的温湿度（可选）
    updateCurrentCabinetEnv()
  }
})

// ================== 生命周期 ==================
onMounted(async () => {
  await loadCabinets()
  if (!systemConfigStore.loaded && !systemConfigStore.loading) {
    console.log("3 load config")
    await systemConfigStore.loadConfig()
  }
  await nextTick()
  updateLayout()
  updateCurrentTime()
  connectWebSocket()

  window.addEventListener('resize', handleResize)

  // 使用 requestAnimationFrame 驱动更新
  lastEnvUpdate = performance.now()
  rafId = requestAnimationFrame(frameLoop)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (resizeTimer) clearTimeout(resizeTimer)
  cleanupPasswordCountdown()

  // 取消 requestAnimationFrame
  if (rafId) {
    cancelAnimationFrame(rafId)
    rafId = null
  }

  // 关闭盘点WebSocket
  allowReconnect = false
  if (socket) {
    socket.close()
    socket = null
  }
})
</script>

<style lang="css" scoped>

.confirm-btn.settings-confirm-btn {
  background: linear-gradient(135deg, #22d3ee, #3b82f6) !important;
}
.confirm-btn.inventory-confirm-btn {
  background: linear-gradient(135deg, #8b5cf6, #6d28d9) !important;
}

/* 全局无滚动条 */
html,
body,
#app {
  height: 100%;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

.home-container {
  width: 100%;
  height: 100vh;
  background: radial-gradient(circle at 20% 30%, #0a1a1f, #051016);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.full-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  overflow: hidden;
}

/* ---------- 标题栏（无扫描线、无动画） ---------- */
.app-header {
  height: 70px;
  flex-shrink: 0;
  padding: 0 28px;
  background: rgba(8, 20, 26, 0.85);
  border-bottom: 1px solid rgba(34, 211, 238, 0.4);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 400;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.app-header::before,
.app-header::after {
  display: none;
}
.app-header:hover {
  background: rgba(8, 20, 26, 0.95);
  border-bottom-color: rgba(34, 211, 238, 0.7);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-icon {
  width: 36px;
  height: 36px;
  display: block;
  flex-shrink: 0;
  filter: drop-shadow(0 0 6px rgba(34, 211, 238, 0.6));
}

/* 状态指示灯 – 静态 */
.status-dot {
  position: absolute;
  bottom: 0;
  right: -2px;
  width: 10px;
  height: 10px;
  background: #4ade80;
  border-radius: 50%;
  box-shadow: 0 0 4px #4ade80;
  border: 1px solid rgba(0, 0, 0, 0.3);
}

.title-wrapper {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}
.title-text {
  font-size: 22px;
  font-weight: 800;
  background: linear-gradient(135deg, #e0f2fe, #22d3ee, #60a5fa);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  letter-spacing: 1.5px;
  text-shadow: 0 0 8px rgba(34, 211, 238, 0.3);
}
.title-sub {
  font-size: 9px;
  font-weight: 500;
  letter-spacing: 2px;
  color: #7aa2b0;
  text-transform: uppercase;
  margin-top: 2px;
  background: rgba(34, 211, 238, 0.15);
  display: inline-block;
  width: fit-content;
  padding: 0 6px;
  border-radius: 20px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}
.time-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(0, 0, 0, 0.5);
  padding: 6px 18px 6px 14px;
  border-radius: 60px;
  border: 1px solid rgba(34, 211, 238, 0.5);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}
.time-wrapper:hover {
  background: rgba(34, 211, 238, 0.2);
  border-color: #22d3ee;
}
.time-icon {
  width: 20px;
  height: 20px;
  display: block;
  flex-shrink: 0;
  filter: drop-shadow(0 0 2px #22d3ee);
}
.header-time {
  font-size: 20px;
  font-weight: 700;
  font-family: 'Courier New', 'Fira Code', monospace;
  color: #c2f0e0;
  text-shadow: 0 0 4px rgba(34, 211, 238, 0.4);
  letter-spacing: 2px;
  min-width: 70px;
  text-align: center;
}

/* ---------- 布局容器 ---------- */
.upper-area {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
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

/* ---------- 温湿度卡片 ---------- */
.temp-card,
.humidity-card {
  position: absolute;
  top: 12px;
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
  width: 28px;
  height: 28px;
  display: block;
  flex-shrink: 0;
}
.card-value {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
  line-height: 1;
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
  font-weight: 500;
  letter-spacing: 1px;
  background: rgba(0, 0, 0, 0.4);
  padding: 2px 6px;
  border-radius: 20px;
}
.temp-card:hover,
.humidity-card:hover {
  background: rgba(0, 0, 0, 0.8);
}

/* ---------- 轮播容器 ---------- */
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

/* ---------- 柜子卡片：保留 transform 过渡，解决文字模糊 ---------- */
.cabinet-item {
  position: absolute;
  background: #f1f4f9;
  border-radius: 20px;
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.25);
  border: 1px solid #d0d8e4;
  transform-style: preserve-3d;
  /* 关键修复：保留 transform/opacity 过渡，0.25s 平滑且性能友好 */
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
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
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

/* ---------- 指示器 ---------- */
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

/* ---------- 底部按钮区 ---------- */
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
.big-buttons-container {
  display: flex;
  gap: 16px;
  width: 100%;
  max-width: 900px;
  margin: 0 auto;
  padding: 0 10px;
}
.big-action-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px 8px;
  border: none;
  border-radius: 24px;
  font-weight: 700;
  cursor: pointer;
  min-width: 0;
  min-height: 70px;
  color: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}
.btn-icon {
  width: 36px;
  height: 36px;
  display: block;
  flex-shrink: 0;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
}
.btn-label {
  font-size: 18px;
  letter-spacing: 2px;
}

.borrow-btn {
  background: linear-gradient(135deg, #10b981, #059669);
  border: 1px solid rgba(52, 211, 153, 0.6);
}
.borrow-btn:hover {
  background: linear-gradient(135deg, #34d399, #10b981);
}

.return-btn {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  border: 1px solid rgba(251, 191, 36, 0.6);
}
.return-btn:hover {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
}

.inventory-btn {
  background: linear-gradient(135deg, #8b5cf6, #6d28d9);
  border: 1px solid rgba(167, 139, 250, 0.6);
}
.inventory-btn:hover {
  background: linear-gradient(135deg, #a78bfa, #8b5cf6);
}

.settings-btn {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border: 1px solid rgba(96, 165, 250, 0.6);
}
.settings-btn:hover {
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
}

/* ---------- 导航按钮 ---------- */
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
  letter-spacing: 1px;
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
}
.nav-btn-left.disabled,
.nav-btn-right.disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* ---------- 格口单元格 ---------- */
.cell-container {
  position: relative;
  background: #f9fbfe;
  border: 1px solid #cfdde6;
  border-radius: 12px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.7), 0 2px 8px rgba(0, 0, 0, 0.02);
  cursor: pointer;
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

/* ---------- 弹窗 ---------- */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
}
.dialog-content {
  width: 85%;
  max-width: 400px;
  background: linear-gradient(135deg, #1e293b, #0f172a);
  border-radius: 32px;
  border: 1px solid rgba(34, 211, 238, 0.5);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.5);
  overflow: hidden;
}
.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: rgba(34, 211, 238, 0.15);
  border-bottom: 1px solid rgba(34, 211, 238, 0.3);
}
.dialog-header h3 {
  margin: 0;
  font-size: 20px;
  color: #c2f0e0;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 10px;
}
.lock-icon {
  width: 24px;
  height: 24px;
  display: block;
  flex-shrink: 0;
  filter: drop-shadow(0 0 4px rgba(34, 211, 238, 0.4));
}
.dialog-close {
  background: none;
  border: none;
  font-size: 24px;
  color: #94a3b8;
  cursor: pointer;
  padding: 8px;
  border-radius: 40px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.dialog-close:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}
.dialog-body {
  padding: 24px 20px;
}
.detail-row {
  margin-bottom: 16px;
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  font-size: 16px;
}
.detail-label {
  width: 80px;
  color: #94a3b8;
  font-weight: 500;
}
.detail-value {
  flex: 1;
  color: #e2e8f0;
  font-weight: 500;
  word-break: break-word;
}
.empty-warning .detail-value {
  color: #fbbf24;
}
.dialog-footer {
  padding: 16px 20px 24px;
  display: flex;
  justify-content: center;
}
.dialog-action-btn {
  background: linear-gradient(135deg, #22d3ee, #3b82f6);
  border: none;
  padding: 12px 40px;
  border-radius: 60px;
  font-size: 18px;
  font-weight: 600;
  color: white;
  cursor: pointer;
  width: 80%;
}

/* ---------- 密码弹窗 ---------- */
.password-dialog {
  max-width: 380px;
}
.password-timer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 20px;
  padding: 8px;
  background: rgba(34, 211, 238, 0.15);
  border-radius: 40px;
}
.timer-icon {
  width: 22px;
  height: 22px;
  display: block;
  flex-shrink: 0;
  filter: drop-shadow(0 0 2px #22d3ee);
}
.timer-text {
  font-size: 16px;
  font-weight: 600;
  color: #22d3ee;
}
.timer-warning {
  color: #f97316;
}
.password-input-group {
  margin-bottom: 16px;
}
.password-input {
  width: 100%;
  padding: 14px 16px;
  font-size: 16px;
  background: rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(34, 211, 238, 0.5);
  border-radius: 60px;
  color: white;
  outline: none;
  text-align: center;
  letter-spacing: 2px;
}
.password-input:focus {
  border-color: #22d3ee;
  box-shadow: 0 0 6px rgba(34, 211, 238, 0.3);
}
.password-error {
  margin-top: 8px;
  text-align: center;
  color: #f87171;
  font-size: 13px;
}
.password-footer {
  gap: 12px;
  padding: 16px 20px 24px;
}
.cancel-btn {
  background: rgba(100, 116, 139, 0.8) !important;
}
.confirm-btn {
  background: linear-gradient(135deg, #22d3ee, #3b82f6) !important;
}

/* ---------- 加载遮罩 ---------- */
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
  font-size: 1.2rem;
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
  to { transform: rotate(360deg); }
}

/* ========== 通知列表样式 ========== */
.notification-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 10001;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 350px;
  pointer-events: none;
}

.notification {
  padding: 12px 20px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  animation: slideInRight 0.3s ease;
  pointer-events: auto;
}

.notification.info,
.notification.success {
  background: rgba(34, 211, 238, 0.15);
  color: #22d3ee;
  border: 1px solid rgba(34, 211, 238, 0.4);
}

.notification.warning {
  background: rgba(248, 113, 113, 0.15);
  color: #f87171;
  border: 1px solid rgba(248, 113, 113, 0.4);
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(100px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* ---------- 响应式 ---------- */
@media (max-width: 680px) {
  .app-header { height: 56px; padding: 0 16px; }
  .logo-icon { width: 28px; height: 28px; }
  .title-text { font-size: 16px; }
  .title-sub { display: none; }
  .time-wrapper { padding: 4px 12px; gap: 6px; }
  .header-time { font-size: 16px; min-width: 55px; }
  .time-icon { width: 16px; height: 16px; }
  .temp-card, .humidity-card { padding: 3px 10px; top: 8px; }
  .card-icon { width: 20px; height: 20px; }
  .card-value { font-size: 14px; min-width: 35px; }
  .card-label { font-size: 9px; padding: 1px 4px; }
  .nav-btn-left, .nav-btn-right { padding: 8px 16px; }
  .btn-icon { width: 28px; height: 28px; }
  .btn-label { font-size: 14px; }
  .big-action-btn { padding: 12px 6px; min-height: 60px; }
  .dialog-content { width: 90%; }
  .lock-icon { width: 20px; height: 20px; }
  .timer-icon { width: 18px; height: 18px; }
}
@media (max-width: 480px) {
  .app-header { padding: 0 12px; }
  .header-time { font-size: 14px; min-width: 48px; }
  .title-text { font-size: 14px; letter-spacing: 1px; }
  .logo-icon { width: 22px; height: 22px; }
  .time-wrapper { padding: 2px 10px; }
  .time-icon { width: 14px; height: 14px; }
  .cabinet-item { width: 260px !important; }
  .nav-btn-left, .nav-btn-right { padding: 6px 12px; }
  .btn-icon { width: 24px; height: 24px; }
  .btn-label { font-size: 12px; }
  .big-action-btn { padding: 10px 4px; min-height: 55px; }
  .big-buttons-container { gap: 10px; }
  .temp-card, .humidity-card { padding: 2px 8px; gap: 4px; top: 6px; }
  .card-icon { width: 16px; height: 16px; }
  .card-value { font-size: 12px; min-width: 30px; }
  .card-label { font-size: 8px; }
  .lock-icon { width: 18px; height: 18px; }
  .timer-icon { width: 16px; height: 16px; }
}
</style>

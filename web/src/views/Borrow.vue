<template>
  <div class="borrow-container">
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
        ⚠️ 暂无柜子配置数据，请联系管理员
      </div>
      <div v-else class="full-layout">
        <!-- ================== 页面标题 ================== -->
        <div class="page-header">
          <div class="title-icon">📦</div>
          <h1>物品领用</h1>
        </div>
        <!-- ================== 上部区域 ================== -->
        <div class="upper-area">
          <div class="top-section">
            <!-- 温度卡片 -->
            <div class="temp-card">
              <div class="card-icon">🌡️</div>
              <div class="card-value">{{ currentCabinetTemp }}°</div>
              <div class="card-label">温度</div>
            </div>

            <!-- 湿度卡片 -->
            <div class="humidity-card">
              <div class="card-icon">💧</div>
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
                  :class="{ 'center-highlight': idx === currentIndex }"
                  :style="{ ...getCabinetStyle(idx), width: cab.width || '280px', height: cab.height || 'auto' }">
                  <div class="cabinet-header">{{ cab.title }}</div>
                  <div class="cabinet-body">
                    <div class="cabinet-grid" :style="getGridStyle(cab)">
                      <template v-for="(cell, cellIdx) in cab.flatCells" :key="cellIdx">
                        <!-- 普通格口 -->
                        <div v-if="cell.type === 'cell'" class="cell-container"
                          :style="[getCellPosition(cell), cell.cellStyle]" :class="{ 'empty-door': cell.isEmpty }"
                          @click="handleCellClick(cell, cab.id, cab.title)">
                          <div class="cell-inner"></div>
                          <div class="cabinet-cell"
                            :class="{ 'empty-door': cell.isEmpty, 'door-open': cell.isDoorOpen }">
                            <span class="cell-number">{{ cell.number }}</span>
                            <span class="tool-name">{{ truncateText(cell.toolName, 8) }}</span>
                          </div>
                        </div>
                        <!-- 图片格口 -->
                        <div v-else-if="cell.type === 'image'" class="custom-image-cell"
                          :style="[getCellPosition(cell), cell.cellStyle]">
                          <img :src="cell.imageUrl" :alt="cell.label || '图标'" />
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

        <!-- ================== 下部区域：领用完成按钮 ================== -->
        <div class="bottom-section">
          <div class="bottom-container">
            <!-- 左侧：照片显示区域 -->
            <div class="photo-area">
              <div v-if="photoPreviewUrl" class="photo-card">
                <img :src="photoPreviewUrl" alt="拍摄照片" class="preview-image" />
                <div class="photo-badge">已拍摄</div>
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

            <!-- 中间：领用记录表格 -->
            <div class="info-area">
              <div class="info-list">
                <div class="info-header">
                  <span class="header-item">柜子名称</span>
                  <span class="header-item">格口号</span>
                  <span class="header-item">工具名称</span>
                  <span class="header-item">领用时间</span>
                </div>
                <div class="info-scroll">
                  <div v-for="(item, idx) in borrowItems" :key="idx" class="info-row">
                    <span class="row-item">{{ item.cabinetName }}</span>
                    <span class="row-item">{{ item.cellNumber }}</span>
                    <span class="row-item">{{ item.toolName }}</span>
                    <span class="row-item">{{ item.borrowTime }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 右侧：领用完成按钮 -->
            <div class="button-area">
              <button class="complete-btn" :disabled="isCompleteDisabled" @click="handleComplete">
                <span class="btn-icon">✅</span>
                <span class="btn-label">领用完成</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 借用数据汇总模态框 -->
    <!-- Borrow.vue 模板中 -->
    <BorrowSummaryModal v-model:visible="showBorrowSummary" :borrow-items="borrowItems" :photo-data="photoData"
      :default-expected-return-time="defaultExpectedReturnTime" @submit="onBorrowSubmit" />

    <!-- 未关闭格口警告模态框 -->
    <Teleport to="body">
      <div v-if="showUnclosedModal" class="modal-overlay" @click.self="closeUnclosedModal">
        <div class="modal-container warning-modal">
          <div class="modal-header warning-header">
            <h3>⚠️ 存在未关闭的柜门</h3>
            <button class="close-btn" @click="closeUnclosedModal">✕</button>
          </div>
          <div class="modal-body">
            <p class="warning-tip">请先关闭以下格口的柜门，再进行领用完成操作：</p>
            <div class="unclosed-list">
              <div class="list-header">
                <span>柜子名称</span>
                <span>格口号</span>
              </div>
              <div class="list-scroll">
                <div v-for="(door, idx) in unclosedDoorsList" :key="idx" class="list-row">
                  <span>{{ door.cabinetName }}</span>
                  <span>{{ door.cellNumber }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="confirm-btn warning-btn" @click="closeUnclosedModal">知道了</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 无领用数据倒计时模态框 -->
    <Teleport to="body">
      <div v-if="showEmptyDataModal" class="modal-overlay" @click.self="closeEmptyDataModal">
        <div class="modal-container info-modal">
          <div class="modal-header info-header">
            <h3>📋 提示</h3>
            <button class="close-btn" @click="closeEmptyDataModal">✕</button>
          </div>
          <div class="modal-body">
            <p class="info-tip">当前没有领用任何工具，无法提交领用完成。</p>
            <p class="countdown-text">
              倒计时 <strong class="countdown-number">{{ emptyDataCountdown }}</strong> 秒后自动返回首页...
            </p>
          </div>
          <div class="modal-footer">
            <button class="cancel-btn" @click="closeEmptyDataModal">取消</button>
            <button class="confirm-btn" @click="immediateReturnHome">立即返回</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 领用成功倒计时模态框 -->
    <Teleport to="body">
      <div v-if="showSuccessModal" class="modal-overlay no-close">
        <div class="modal-container success-modal">
          <div class="modal-header success-header">
            <h3>🎉 领用成功</h3>
          </div>
          <div class="modal-body">
            <p class="success-tip">成功领用 <strong>{{ successBorrowCount }}</strong> 件工具！</p>
            <p class="countdown-text">
              倒计时 <strong class="countdown-number">{{ successCountdown }}</strong> 秒后自动返回首页...
            </p>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useSystemConfigStore } from '@/stores/systemConfig'
import { useDehumidifierStore } from '@/stores/useDehumidifier'
import { useRouter } from 'vue-router'
import { fetchCabinetList } from '@/api/cabinet'
import BorrowSummaryModal from './BorrowSummaryModal.vue'
import { submitBorrowRecords } from '@/api/borrow'
import type { CSSProperties } from 'vue'

const photoFile = ref<File | null>(null)      // 暂存照片文件
const photoPreviewUrl = ref<string>('')       // 用于预览的 blob URL
const systemStore = useSystemConfigStore()
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

interface BorrowItem {
  cabinetId: number
  cabinetName: string
  cellId: number
  cellNumber: string
  toolName: string
  borrowTime: string
}

interface Notification {
  id: number
  text: string
  type: 'info' | 'success' | 'warning'
}
function loadPhotoData() {
  const storedData = sessionStorage.getItem('toolOperationData')
  if (storedData) {
    try {
      const data = JSON.parse(storedData)
      // 如果拍照组件存的是 base64，可以将其转换为 File（可选）
      if (data.imageData) {
        // 将 base64 转为 File（兼容旧逻辑）
        const file = base64ToFile(data.imageData, 'photo.jpg')
        photoFile.value = file
        photoPreviewUrl.value = URL.createObjectURL(file)
      } else if (data.imageFile) {
        // 如果存储的是 File 相关信息（不可直接存，忽略）
      }
    } catch (e) { console.error(e) }
  }
}
// 辅助函数：base64 转 File
function base64ToFile(base64: string, filename: string): File {
  const arr = base64.split(',')
  const mime = arr[0].match(/:(.*?);/)?.[1] || 'image/jpeg'
  const bstr = atob(arr[1])
  let n = bstr.length
  const u8arr = new Uint8Array(n)
  while (n--) { u8arr[n] = bstr.charCodeAt(n) }
  return new File([u8arr], filename, { type: mime })
}


// ================== 辅助函数 ==================
function expandColumns(columns: string, count: number): string[] {
  const parts = columns.trim().split(/\s+/)
  if (parts.length === 1 && count > 1) return Array(count).fill(parts[0])
  if (parts.length !== count) console.warn(`columns 值个数 (${parts.length}) 与 colSpan (${count}) 不匹配`)
  return parts
}

function expandHeight(height: string, count: number): string[] {
  const parts = height.trim().split(/\s+/)
  if (parts.length === 1 && count > 1) return Array(count).fill(parts[0])
  if (parts.length !== count) console.warn(`height 值个数 (${parts.length}) 与 rowSpan (${count}) 不匹配`)
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
  return rawData.map((cab) => {
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

// ================== 通知列表 ==================
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

// ================== 领用记录（增强：包含ID） ==================
const borrowItems = ref<BorrowItem[]>([])

function addBorrowRecord(cabinetId: number, cabinetName: string, cellId: number, cellNumber: string, toolName: string) {
  const now = new Date()
  const borrowTime = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
  borrowItems.value.unshift({
    cabinetId,
    cabinetName,
    cellId,
    cellNumber,
    toolName,
    borrowTime
  })
}

/**
 * 处理格口点击事件
 * @param cell 
 * @param cabinetId 
 * @param cabinetTitle 
 */
const handleCellClick = async (cell: any, cabinetId: number, cabinetTitle: string) => {
  const anyOpen = cabinets.value.some(cab =>
    cab.flatCells.some(cell => cell.type === 'cell' && cell.isDoorOpen === true)
  )
  if (anyOpen) {
    addNotification('请先关闭当前开启的柜门', 'warning')
    return
  }
  if (cell.type !== 'cell') return
  if (cell.isEmpty) {
    addNotification(`🔒 该格口当前为空，无法打开 · 格口 ${cell.number}`, 'warning')
    return
  }
  if (unlocking.value) {
    addNotification(`操作频繁，请稍后再试`, 'warning')
    return
  }
  unlocking.value = true
  addNotification(`正在开启 ${cabinetTitle} - ${cell.number} 门锁...`, 'info')
  const sendSuccess = await requestOpenLock(cabinetId, cell.id, cell.number)
  if (!sendSuccess) {
    unlocking.value = false
  }
}

// ================== 响应式数据 ==================
const router = useRouter()
const cabinets = ref<ProcessedCabinet[]>([])
const loading = ref(true)
const currentIndex = ref(0)
const totalCount = computed(() => cabinets.value.length)

const radius = ref(320)
const carouselHeight = ref(600)
const maxScale = ref(1.4)

const photoData = ref('')
const showBorrowSummary = ref(false)
const showUnclosedModal = ref(false)
const unclosedDoorsList = ref<{ cabinetName: string; cellNumber: string }[]>([])

// 无领用数据倒计时相关
const showEmptyDataModal = ref(false)
const emptyDataCountdown = ref(10)
let emptyDataTimer: ReturnType<typeof setInterval> | null = null

// 领用成功倒计时相关
const showSuccessModal = ref(false)
const successCountdown = ref(10)
const successBorrowCount = ref(0)
let successTimer: ReturnType<typeof setInterval> | null = null

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

const isCompleteDisabled = computed(() => {
  return loading.value || showBorrowSummary.value || showUnclosedModal.value || showEmptyDataModal.value || showSuccessModal.value
})

function startEmptyDataCountdown() {
  if (emptyDataTimer) clearInterval(emptyDataTimer)
  showEmptyDataModal.value = true
  emptyDataCountdown.value = 10
  emptyDataTimer = setInterval(() => {
    if (emptyDataCountdown.value <= 1) {
      clearEmptyDataTimer()
      showEmptyDataModal.value = false
      router.push('/')
    } else {
      emptyDataCountdown.value--
    }
  }, 1000)
}

function clearEmptyDataTimer() {
  if (emptyDataTimer) {
    clearInterval(emptyDataTimer)
    emptyDataTimer = null
  }
}

function closeEmptyDataModal() {
  clearEmptyDataTimer()
  showEmptyDataModal.value = false
}

function startSuccessCountdown(borrowCount: number) {
  if (successTimer) clearInterval(successTimer)
  successBorrowCount.value = borrowCount
  showSuccessModal.value = true
  successCountdown.value = 10
  successTimer = setInterval(() => {
    if (successCountdown.value <= 1) {
      clearSuccessTimer()
      showSuccessModal.value = false
      router.push('/')
    } else {
      successCountdown.value--
    }
  }, 1000)
}

function clearSuccessTimer() {
  if (successTimer) {
    clearInterval(successTimer)
    successTimer = null
  }
}

function immediateReturnHome() {
  clearEmptyDataTimer()
  clearSuccessTimer()
  showEmptyDataModal.value = false
  showSuccessModal.value = false
  router.push('/')
}

async function loadCabinets() {
  loading.value = true
  try {
    const rawData = await fetchCabinetList()
    cabinets.value = processCabinetData(rawData)
    const defaultIdx = cabinets.value.findIndex((cab) => cab.isDefault === true)
    currentIndex.value = defaultIdx !== -1 ? defaultIdx : 0
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

// 格式化日期时间为 YYYY-MM-DDTHH:mm
function formatToDateTimeLocal(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}`
}

// 默认预计归还时间 = 当前时间 + borrowPeriod(天)
const defaultExpectedReturnTime = computed(() => {
  const period = systemStore.borrowPeriod
  if (!period) return ''
  const days = Number(period)
  if (isNaN(days) || days <= 0) return ''
  const now = new Date()
  now.setDate(now.getDate() + days)
  return formatToDateTimeLocal(now)
})
// ================== 领用完成逻辑 ==================
function handleComplete() {
  const openDoors: { cabinetName: string; cellNumber: string }[] = []
  for (const cab of cabinets.value) {
    for (const cell of cab.flatCells) {
      if (cell.type === 'cell' && cell.isDoorOpen) {
        openDoors.push({ cabinetName: cab.title, cellNumber: cell.number })
      }
    }
  }
  // 存在未关闭的柜门
  if (openDoors.length > 0) {
    addNotification('尚有柜门未关闭，请先关闭柜门完成领用', 'warning')
    return
  }
  sendMessage('checkAllLockStatus', {})
}

function closeUnclosedModal() {
  showUnclosedModal.value = false
}

// 处理领用提交（调用后端接口）
async function onBorrowSubmit(data: {
  borrowItems: BorrowItem[]
  borrowerName: string
  borrowerNumber: string
  expectedReturnTime: string
  remark: string
  // 移除 photoData，改为使用本地的 photoFile
}) {
  try {
    // 组装提交参数，包含照片文件
    await submitBorrowRecords({
      borrowItems: data.borrowItems,
      borrowerName: data.borrowerName,
      borrowerNumber: data.borrowerNumber,
      expectedReturnTime: data.expectedReturnTime,
      remark: data.remark,
      photoFile: photoFile.value || undefined   // 传递 File 对象或 undefined
    })
    // 成功后的处理...
    borrowItems.value = []
    photoFile.value = null
    if (photoPreviewUrl.value) URL.revokeObjectURL(photoPreviewUrl.value)
    photoPreviewUrl.value = ''
    sessionStorage.removeItem('toolOperationData')
    startSuccessCountdown(data.borrowItems.length)
  } catch (error) {
    addNotification('领用提交失败，请重试', 'warning')
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
    console.log(`切换到柜子 ${newIdx + 1}，温度：${currentCabinetTemp.value}℃，湿度：${currentCabinetHumidity.value}%`)
  }
})

// ================== WebSocket 连接 ==================
let closeAndCheckTimer: any = null // 轮询定时器
const isWaiting = ref(false)       // 状态锁：是否正在等待后端的响应
const isPollingActive = ref(true)  // 业务锁：轮询是否仍在继续进行

const closeAndCheck = async (cabId: any, cellId: any, cellNumber: any, toolName: any) => {
  // 如果轮询已经结束，或者上一次的响应还没来，则不发送
  if (!isPollingActive.value || isWaiting.value) return
  isWaiting.value = true
  // 检测柜门和物品状态
  console.log('发送检测柜门和物品状态')
  const sendSuccess = sendMessage('closeAndCheck', {
    cabinetId: cabId,
    cellId: cellId,
    cellNumber: cellNumber,
    toolName: toolName
  })
  if (!sendSuccess) {
    isWaiting.value = false
  }
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

const wsUrl  = `${import.meta.env.VITE_WS_BASE_URL}/borrow`
let socket: WebSocket | null = null
const wsConnected = ref(false)

const connectWebSocket = () => {
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
  socket.onclose = () => {
    console.log('WebSocket 连接关闭')
    wsConnected.value = false
    // 尝试重连
    setTimeout(() => connectWebSocket(), 3000)
  }
}
/**
 * 开锁按钮状态锁
 */
const unlocking = ref(false)

const requestOpenLock = (cabinetId: number, cellId: number, cellNumber: string): Promise<boolean> => {
  if (!wsConnected.value) {
    addNotification('服务器未连接，请稍后重试', 'warning')
    return Promise.resolve(false)
  }
  sendMessage('openLock', { cabinetId, cellId, cellNumber })
  return Promise.resolve(true)
}

/**
 * 处理后端websocket返回的消息
 * @param msg 
 */
const handleWebSocketMessage = async (msg: any) => {
  const { type, code, data, message: msgText } = msg
  switch (type) {
    case 'openLock':
      handleUnLockReply(msg)
      return;
    case 'checkAllLockStatus':
      handleCheckAllLockStatus(msg)
      return;
    case 'closeAndCheck':
      handleCloseAndCheck(msg)
      return;
  }
}
/**
 * 处理开锁回复
 * @param msg 
 */
const handleUnLockReply = async (msg: any) => {
  const { type, code, data, message: msgText } = msg
  switch (code) {
    case 200:
      // 开锁成功
      const { cabinetId, cellId, cellNumber, toolName } = data
      // 找到对应的格口并设置开门状态
      for (const cab of cabinets.value) {
        if (cab.id === cabinetId) {
          const cell = cab.flatCells.find(c => c.type === 'cell' && c.id === cellId && c.number === cellNumber)
          if (cell) {
            cell.isDoorOpen = true
            addNotification(`🚪 柜门已打开 · 格口 ${cell.number}`, 'success')
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
      break;
    default:
      addNotification(msgText, 'warning')
  }
  unlocking.value = false;
}
/**
 * 处理检测关锁和储物状态回复
 * @param msg 
 */
const handleCloseAndCheck = async (msg: any) => {
  const { type, code, data, message: msgText } = msg
  const { cabinetId, cellId, cellNumber, toolName, borrowTime } = data
  switch (code) {
    case 200:
      // 关门成功且物品被领走
      stopCloseAndCheckPolling()
      // 更新前端状态
      for (const cab of cabinets.value) {
        if (cab.id === cabinetId) {
          const cell = cab.flatCells.find(c => c.type === 'cell' && c.id === cellId && c.number === cellNumber)
          if (cell) {
            cell.isDoorOpen = false
            cell.isEmpty = true
            cell.toolName = toolName
            addNotification(`✅ 柜门已关闭 · 工具【${toolName}】已领用`, 'success')
            addBorrowRecord(cab.id, cab.title, cell.id, cell.number, toolName)
          }
          break
        }
      }
      break;
    case 204:
      // 关门成功但物品未被带走
      console.log(`✅ ${cellNumber}号格口已关锁，但工具 ${toolName} 未领用`)
      stopCloseAndCheckPolling()
      // 更新前端状态
      for (const cab of cabinets.value) {
        if (cab.id === cabinetId) {
          const cell = cab.flatCells.find(c => c.type === 'cell' && c.id === cellId && c.number === cellNumber)
          if (cell) {
            cell.isDoorOpen = false
            cell.isEmpty = false
            cell.toolName = toolName
            addNotification(`✅ ${cellNumber}号格口已关锁，但工具 ${toolName} 未领用`, 'warning')
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
      console.log(`进入500`)
      stopCloseAndCheckPolling()
      addNotification('门锁状态检测失败，请联系管理员', 'warning')
      break;

  }
  isWaiting.value = false
}
/**
 * 处理检测所有锁关闭状态回复
 * @param msg 
 */
const handleCheckAllLockStatus = async (msg: any) => {
  const { type, code, data, message: msgText } = msg
  switch (code) {
    case 200:
      if (borrowItems.value.length === 0) {
        startEmptyDataCountdown()
        return
      }
      showBorrowSummary.value = true
      break;
    case 501:
      addNotification(msgText, 'warning')
      startSuccessCountdown(0)
      break;
    default:
      addNotification('尚有柜门未关闭，请先关闭柜门完成领用', 'warning')
  }
}

const sendMessage = (type: string, data: any) => {
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    addNotification('网络连接异常，请稍后重试', 'warning')
    return false
  }
  socket.send(JSON.stringify({ type, data }))
  return true
}

onMounted(() => {
  loadPhotoData()
  loadCabinets()
  updateLayout()
  window.addEventListener('resize', handleResize)
  connectWebSocket()
})

onBeforeUnmount(() => {
  if (photoPreviewUrl.value) URL.revokeObjectURL(photoPreviewUrl.value)
  window.removeEventListener('resize', handleResize)
  if (resizeTimer) clearTimeout(resizeTimer)
  clearEmptyDataTimer()
  clearSuccessTimer()
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

.borrow-container {
  width: 100%;
  height: 100vh;
  background: radial-gradient(circle at 20% 30%, #0a1a1f, #051016);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
}

/* 通知列表 */
.notification-container {
  position: fixed;
  top: 80px;
  right: 20px;
  z-index: 10000;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 300px;
  pointer-events: none;
}

.notification {
  padding: 12px 20px;
  border-radius: 48px;
  font-size: 14px;
  font-weight: 600;
  backdrop-filter: blur(12px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3);
  animation: slideInRight 0.3s ease;
  pointer-events: auto;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(100%);
  }

  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.full-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  overflow: hidden;
}

/* ================== 页面标题 ================== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  height: 60px;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid rgba(34, 211, 238, 0.3);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
  flex-shrink: 0;
}

.title-icon {
  font-size: 28px;
  filter: drop-shadow(0 0 6px #22d3ee);
}

.page-header h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  background: linear-gradient(135deg, #e0f2fe, #22d3ee);
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
  text-shadow: 0 0 8px rgba(34, 211, 238, 0.3);
  letter-spacing: 2px;
}

/* 上部区域 */
.upper-area {
  height: calc(100vh - 350px - 60px);
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

/* 温湿度卡片 */
.temp-card,
.humidity-card {
  position: absolute;
  top: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(12px);
  border-radius: 40px;
  padding: 5px 14px;
  z-index: 500;
  border: 1px solid rgba(255, 255, 255, 0.15);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
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
  font-size: 18px;
  filter: drop-shadow(0 0 2px rgba(255, 255, 255, 0.5));
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
  text-shadow: 0 0 4px rgba(248, 113, 113, 0.4);
}

.humidity-card .card-value {
  color: #4ade80;
  text-shadow: 0 0 4px rgba(74, 222, 128, 0.4);
}

.card-label {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 500;
  background: rgba(0, 0, 0, 0.4);
  padding: 2px 6px;
  border-radius: 20px;
}

/* 3D轮播 */
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
  box-shadow: 0 20px 30px rgba(0, 0, 0, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.9);
  border: 1px solid #d0d8e4;
  transform-style: preserve-3d;
  transition: transform 0.5s cubic-bezier(0.2, 0.85, 0.35, 1), opacity 0.4s ease;
  display: flex;
  flex-direction: column;
  min-width: 0;
  will-change: transform;
  max-width: 88vw;
}

.cabinet-item.center-highlight {
  filter: drop-shadow(0 0 12px rgba(100, 220, 160, 0.6));
  border: 1px solid #6fcf97;
}

.cabinet-header {
  background: linear-gradient(135deg, #eef2f7 0%, #e3e9f0 100%);
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

.cabinet-backboard {
  position: absolute;
  top: 60px;
  left: 12px;
  right: 12px;
  bottom: 12px;
  background: #e3e8f0;
  transform: translateZ(-24px);
  border-radius: 12px;
  pointer-events: none;
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
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(8px);
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
  box-shadow: 0 0 6px rgba(34, 211, 238, 0.6);
  border-radius: 4px;
  flex-shrink: 0;
}

/* 格口样式 */
.cell-container {
  position: relative;
  background: #f9fbfe;
  border: 1px solid #cfdde6;
  border-radius: 12px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.7), 0 4px 12px rgba(0, 0, 0, 0.02);
  transition: all 0.2s ease;
  overflow: visible;
  cursor: pointer;
  box-sizing: border-box;
  height: 100%;
}

.cell-container:active {
  transform: scale(0.98);
}

.cell-container:hover {
  box-shadow: 0 0 0 2px rgba(34, 211, 238, 0.5), inset 0 0 0 1px rgba(255, 255, 255, 0.7);
}

.custom-image-cell {
  background: rgba(0, 0, 0, 0.4);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(8px);
  border: 1px solid rgba(34, 211, 238, 0.5);
  box-shadow: 0 0 12px rgba(0, 255, 255, 0.2);
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
  backdrop-filter: blur(1px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  transition: transform 0.3s cubic-bezier(0.2, 0.9, 0.4, 1.1);
  transform-origin: left center;
  will-change: transform;
  box-sizing: border-box;
}

.cabinet-cell.door-open {
  transform: translateZ(6px) rotateY(-75deg);
  box-shadow: -8px 0 16px rgba(0, 0, 0, 0.3), inset -1px 0 0 rgba(255, 255, 255, 0.5);
}

.cabinet-cell.empty-door {
  background: rgba(255, 255, 245, 0.55);
  backdrop-filter: blur(4px);
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

/* 导航按钮 */
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
  backdrop-filter: blur(12px);
  border: 1.5px solid rgba(34, 211, 238, 0.7);
  border-radius: 60px;
  color: #e0f2fe;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.2, 0.9, 0.4, 1.1);
  z-index: 400;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
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
  transform: translateY(-50%) scale(1.05);
}

.nav-btn-left.disabled,
.nav-btn-right.disabled {
  opacity: 0.35;
  cursor: not-allowed;
  filter: grayscale(0.2);
}

/* 下部区域 */
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
  align-self: center;
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
  backdrop-filter: blur(4px);
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

.info-area {
  flex: 0 0 auto;
  width: 400px;
  min-width: 0;
  display: flex;
  align-items: center;
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(8px);
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
  font-size: 11px;
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
  transition: background 0.2s;
  flex-shrink: 0;
}

.info-row:hover {
  background: rgba(34, 211, 238, 0.1);
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
  transition: all 0.25s ease;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.95), rgba(5, 150, 105, 0.95));
  border: 1px solid rgba(52, 211, 153, 0.6);
  color: white;
  box-shadow: 0 4px 15px rgba(16, 185, 129, 0.3);
  min-height: 100px;
  min-width: 140px;
}

.complete-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #10b981, #059669);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(16, 185, 129, 0.4);
}

.complete-btn:active:not(:disabled) {
  transform: translateY(1px);
}

.complete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.complete-btn .btn-icon {
  font-size: 38px;
}

.complete-btn .btn-label {
  font-size: 18px;
  letter-spacing: 2px;
  font-weight: 700;
}

/* 加载状态 */
.loading-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(4px);
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
  backdrop-filter: blur(10px);
  font-size: 1.2rem;
  margin: auto;
  width: fit-content;
  max-width: 90%;
}

/* 模态框通用样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 20000;
  animation: fadeIn 0.2s ease;
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
  box-shadow: 0 25px 40px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(34, 211, 238, 0.3);
  animation: slideUp 0.3s cubic-bezier(0.2, 0.9, 0.4, 1.1);
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
  transition: all 0.2s;
  line-height: 1;
  padding: 0 8px;
}

.close-btn:hover {
  color: #f87171;
  transform: scale(1.1);
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

.modal-footer.single-button {
  justify-content: center;
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
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.3);
}

.confirm-btn:hover {
  transform: translateY(-2px);
  background: linear-gradient(135deg, #0fba7a, #048a5a);
  box-shadow: 0 6px 14px rgba(16, 185, 129, 0.4);
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
  transition: all 0.2s;
}

.cancel-btn:hover {
  background: rgba(71, 85, 105, 0.9);
  transform: translateY(-2px);
}

/* 警告模态框特殊样式 */
.warning-modal .modal-container {
  border-left: 4px solid #f97316;
}

.warning-header h3 {
  color: #f97316;
}

.warning-tip {
  color: #facc15;
  margin-bottom: 16px;
  font-size: 0.9rem;
  background: rgba(0, 0, 0, 0.3);
  padding: 8px 12px;
  border-radius: 12px;
}

.unclosed-list .list-header {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 8px 12px;
  background: rgba(249, 115, 22, 0.2);
  border-radius: 12px;
  font-weight: 700;
  font-size: 0.85rem;
  color: #f97316;
  margin-bottom: 8px;
}

.unclosed-list .list-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 8px 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  color: #e2e8f0;
}

.unclosed-list .list-row:hover {
  background: rgba(249, 115, 22, 0.1);
}

.warning-btn {
  background: linear-gradient(135deg, #f97316, #ea580c);
}

.warning-btn:hover {
  background: linear-gradient(135deg, #fb923c, #ea580c);
}

/* 信息模态框（倒计时） */
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

/* 成功模态框（不可关闭） */
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

@keyframes fadeIn {
  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式 */
@media (max-width: 680px) {
  .page-header {
    height: 50px;
    gap: 8px;
  }

  .title-icon {
    font-size: 22px;
  }

  .page-header h1 {
    font-size: 20px;
  }

  .upper-area {
    height: calc(100vh - 350px - 50px);
  }

  .notification-container {
    top: 70px;
    right: 10px;
    max-width: 260px;
  }

  .notification {
    font-size: 12px;
    padding: 8px 16px;
  }

  .nav-btn-left,
  .nav-btn-right {
    padding: 8px 16px;
  }

  .nav-btn-left .btn-text,
  .nav-btn-right .btn-text {
    display: none;
  }

  .nav-btn-left .arrow,
  .nav-btn-right .arrow {
    font-size: 1.4rem;
  }

  .bottom-container {
    gap: 10px;
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
    font-size: 28px;
  }

  .complete-btn .btn-label {
    font-size: 14px;
  }

  .complete-btn {
    padding: 16px 12px;
    min-height: 80px;
    gap: 8px;
  }

  .temp-card,
  .humidity-card {
    padding: 3px 10px;
    top: 4px;
  }

  .card-icon {
    font-size: 14px;
  }

  .card-value {
    font-size: 14px;
    min-width: 35px;
  }

  .card-label {
    font-size: 9px;
    padding: 1px 4px;
  }
}

@media (max-width: 480px) {
  .page-header h1 {
    font-size: 18px;
  }

  .title-icon {
    font-size: 20px;
  }

  .cabinet-item {
    width: 260px !important;
  }

  .bottom-container {
    gap: 8px;
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
    font-size: 24px;
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
    font-size: 12px;
  }

  .card-value {
    font-size: 12px;
    min-width: 30px;
  }

  .card-label {
    font-size: 8px;
  }
}
</style>

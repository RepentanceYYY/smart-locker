<template>
  <div v-if="visible" class="dialog-overlay">
    <div class="dialog-content" @click.stop>
      <div class="inventory-container">
        <div class="inventory-header">
          <h1 class="page-title">
            <img src="/盘点单.svg" alt="盘点" class="icon title-icon" /> 盘点结果
          </h1>
          <div class="header-right">
            <div class="countdown-display" v-if="countdown && countdown.secondsLeft.value > 0">
              <img src="/计时器.svg" alt="倒计时" class="icon" />
              <span class="countdown-time">{{ countdown.secondsLeft.value }}</span>
              <span class="countdown-text">秒后自动关闭弹窗</span>
            </div>
            <button class="close-btn" @click="handleConfirm" title="关闭">
              ✕
            </button>
          </div>
        </div>

        <div v-if="loading" class="loading-wrapper">
          <div class="loading-spinner"></div>
          <span>加载中...</span>
        </div>

        <div v-else-if="error" class="error-wrapper">
          <img src="/警告 (1).svg" alt="警告" class="icon" />
          <span>{{ error }}</span>
          <button class="retry-btn" @click="fetchData">重试</button>
        </div>

        <div v-else>
          <div class="table-wrapper">
            <div class="table-container">
              <table class="inventory-table">
                <thead>
                <tr>
                  <th>柜子名称</th>
                  <th>格口号</th>
                  <th>工具名称</th>
                  <th>状态变更</th>
                  <th>检测时间</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="item in inventoryList" :key="item.id">
                  <td>{{ item.cabinetTitle || '-' }}</td>
                  <td>{{ item.cellNumber || '-' }}</td>
                  <td class="tool-name">{{ item.toolName || '-' }}</td>
                  <td>
                                            <span>
                                                <span :class="getFromClass(item.status)">{{ getFromText(item.status) }}</span>
                                                <span class="status-arrow">➜</span>
                                                <span :class="getToClass(item.status)">{{ getToText(item.status) }}</span>
                                            </span>
                  </td>
                  <td>{{ formatDateTime(item.lastOperationTime) }}</td>
                </tr>
                <tr v-if="inventoryList.length === 0">
                  <td colspan="5" class="empty-row" style="text-align: center;">暂无变化</td>
                </tr>
                </tbody>
              </table>
            </div>
            <div class="record-count" v-if="inventoryList.length > 0">共 {{ inventoryList.length }} 条记录</div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div v-if="showToast" class="toast-message">{{ toastText }}</div>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue'
import { useCountdown } from '@/composables/useCountdown'

interface InventoryItem {
  id: string
  cabinetTitle: string
  cellNumber: string
  toolName: string
  status: 'borrowed' | 'returned' | '柜内' | '柜外'
  lastOperationTime?: string
}

interface InventoryBorrowRecord {
  cabinetId: string | number
  cabinetName: string
  cellId: string | number
  cellNumber: string
  toolName: string
  borrowTime: string
}

interface InventoryReturnRecord {
  cabinetId: string | number
  cabinetName: string
  cellId: string | number
  cellNumber: string
  toolName: string
  returnTime: string
}

interface InventoryResultPayload {
  borrowItems?: InventoryBorrowRecord[]
  returnItems?: InventoryReturnRecord[]
  inventoryTime?: string
}

const props = withDefaults(defineProps<{
  visible?: boolean
  inventoryResult?: InventoryResultPayload | null
}>(), {
  visible: true,
  inventoryResult: null,
})

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'cancel'): void
  (e: 'confirm'): void
}>()

const countdown = useCountdown({ autoStart: false, onTimeout: () => handleConfirm() })

const loading = ref(false)
const error = ref('')
const inventoryList = ref<InventoryItem[]>([])


const showToast = ref(false)
const toastText = ref('')
// ==================== 方法 ====================
function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  try {
    return new Date(dateStr).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return dateStr
  }
}

function isToInside(status?: string) {
  return status === '柜内' || status === 'returned'
}

function getFromText(status?: string) {
  return isToInside(status) ? '柜外' : '柜内'
}

function getToText(status?: string) {
  return isToInside(status) ? '柜内' : '柜外'
}

function getFromClass(status?: string) {
  return isToInside(status) ? 'status-badge borrowed' : 'status-badge returned'
}

function getToClass(status?: string) {
  return isToInside(status) ? 'status-badge returned' : 'status-badge borrowed'
}

async function fetchData() {
  loading.value = true
  try {
    await new Promise(r => setTimeout(r, 200))
    error.value = ''
    const payload = props.inventoryResult

    const borrowItems = Array.isArray(payload?.borrowItems) ? payload!.borrowItems! : []
    const returnItems = Array.isArray(payload?.returnItems) ? payload!.returnItems! : []

    const rows: InventoryItem[] = [
      ...borrowItems.map((i) => ({
        id: `borrow-${i.cabinetId}-${i.cellId}-${i.borrowTime}`,
        cabinetTitle: i.cabinetName,
        cellNumber: i.cellNumber,
        toolName: i.toolName,
        status: '柜外' as const,
        lastOperationTime: i.borrowTime,
      })),
      ...returnItems.map((i) => ({
        id: `return-${i.cabinetId}-${i.cellId}-${i.returnTime}`,
        cabinetTitle: i.cabinetName,
        cellNumber: i.cellNumber,
        toolName: i.toolName,
        status: '柜内' as const,
        lastOperationTime: i.returnTime,
      })),
    ]

    inventoryList.value = rows
  } catch (err) {
    error.value = '加载失败'
  } finally {
    loading.value = false
  }
}

function showToastMessage(text: string) {
  toastText.value = text
  showToast.value = true
  setTimeout(() => { showToast.value = false }, 2000)
}


function handleConfirm() {
  emit('close')
}

watch(() => props.visible, (v) => {
  if (v) {
    fetchData()
    countdown.restart()
  } else {
    countdown.cleanup()
  }
})
</script>
<style lang="css" scoped>
/* 新增通用图标样式 */
.icon {
  width: 1.2em;
  height: 1.2em;
  vertical-align: middle;
  display: inline-block;
  flex-shrink: 0;
  fill: currentColor;
}

.title-icon {
  width: 1.6em;
  height: 1.6em;
}

/* 原有样式（完全保留，仅微调） */
.close-btn {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #94a3b8;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-left: 12px;
  flex-shrink: 0;
}

.close-btn:hover {
  background: #ef4444;
  color: white;
  border-color: #ef4444;
  transform: scale(1.1);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
}

.dialog-content {
  width: 1100px;
  max-width: 94vw;
  max-height: 90vh;
  overflow: hidden;
  border-radius: 24px;
  border: 1px solid rgba(34, 211, 238, 0.25);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.6);
}

.inventory-container {
  min-height: auto;
  background: radial-gradient(circle at 20% 30%, #0a1a1f, #051016);
  padding: 20px;
  overflow-y: auto;
  max-height: 90vh;
}

.inventory-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin: 0 0 30px;
  padding: 0;
}

.page-title {
  color: #c2f0e0;
  font-size: 24px;
  text-shadow: 0 0 10px rgba(34, 211, 238, 0.3);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.countdown-display {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(34, 211, 238, 0.15);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(34, 211, 238, 0.5);
  border-radius: 60px;
  padding: 8px 16px;
  color: #22d3ee;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  animation: pulse 1s ease-in-out infinite;
}

.countdown-time {
  font-size: 18px;
  font-weight: 700;
  font-family: monospace;
  letter-spacing: 1px;
}

.countdown-text {
  font-size: 12px;
  opacity: 0.8;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    border-color: rgba(34, 211, 238, 0.5);
  }
  50% {
    opacity: 0.8;
    border-color: rgba(34, 211, 238, 0.8);
    box-shadow: 0 0 8px rgba(34, 211, 238, 0.3);
  }
}

.table-wrapper {
  background: rgba(15, 25, 35, 0.7);
  backdrop-filter: blur(12px);
  border-radius: 20px;
  border: 1px solid rgba(34, 211, 238, 0.2);
  overflow: hidden;
}

.table-container {
  overflow-x: auto;
}

.inventory-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.inventory-table th,
.inventory-table td {
  padding: 14px 12px;
  text-align: center;
  border-bottom: 1px solid rgba(34, 211, 238, 0.1);
}

.inventory-table th {
  background: rgba(34, 211, 238, 0.1);
  color: #22d3ee;
  font-weight: 600;
  font-size: 13px;
}

.inventory-table td {
  color: #cbd5e1;
}

.inventory-table tr:hover td {
  background: rgba(34, 211, 238, 0.05);
}

.tool-name {
  font-weight: 500;
  color: #c2f0e0;
}

.status-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}

.status-arrow {
  color: #94a3b8;
  font-size: 12px;
  font-weight: 600;
  margin: 0 4px;
}

.status-badge.borrowed {
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
  border: 1px solid rgba(248, 113, 113, 0.3);
}

.status-badge.returned {
  background: rgba(34, 197, 94, 0.2);
  color: #4ade80;
  border: 1px solid rgba(74, 222, 128, 0.3);
}

.empty-row {
  text-align: center;
  padding: 40px !important;
  color: #5b6e8c;
}

.record-count {
  text-align: right;
  padding: 12px 20px;
  color: #94a3b8;
  font-size: 13px;
  border-top: 1px solid rgba(34, 211, 238, 0.1);
}

.loading-wrapper,
.error-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 16px;
  color: #94a3b8;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(34, 211, 238, 0.2);
  border-top-color: #22d3ee;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.retry-btn {
  background: rgba(34, 211, 238, 0.2);
  border: 1px solid #22d3ee;
  border-radius: 40px;
  padding: 6px 20px;
  color: #22d3ee;
  cursor: pointer;
  font-size: 13px;
}

.toast-message {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.9);
  backdrop-filter: blur(12px);
  color: #22d3ee;
  padding: 12px 24px;
  border-radius: 60px;
  font-size: 14px;
  font-weight: 500;
  border: 1px solid #22d3ee;
  z-index: 4000;
  animation: fadeInUp 0.3s ease;
  white-space: nowrap;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateX(-50%) translateY(20px); }
  to { opacity: 1; transform: translateX(-50%) translateY(0); }
}

/* 滚动条 */
.inventory-container::-webkit-scrollbar,
.table-container::-webkit-scrollbar {
  width: 5px;
  height: 5px;
}
.inventory-container::-webkit-scrollbar-track,
.table-container::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.3);
  border-radius: 4px;
}
.inventory-container::-webkit-scrollbar-thumb,
.table-container::-webkit-scrollbar-thumb {
  background: #22d3ee;
  border-radius: 4px;
}

/* 响应式 */
@media (max-width: 768px) {
  .page-title {
    font-size: 18px;
  }
  .countdown-display {
    padding: 6px 12px;
    font-size: 12px;
  }
  .countdown-time {
    font-size: 14px;
  }
  .inventory-table th,
  .inventory-table td {
    padding: 10px 8px;
    font-size: 12px;
  }
}
</style>

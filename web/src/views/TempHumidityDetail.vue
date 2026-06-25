<template>
  <div class="temp-detail-container">
    <div class="outer-frame">
      <div class="temp-detail-header">
        <button class="back-btn" @click="goBack">
          <img src="/bg-back.svg" alt="返回" class="icon" />
          <span>返回设置</span>
        </button>
        <h1 class="page-title">
          <img src="/温湿度.svg" alt="温湿度" class="icon title-icon" /> 温湿度日志详情
        </h1>
        <div class="countdown-display" v-if="countdown && countdown.secondsLeft.value > 0">
          <img src="/计时器.svg" alt="倒计时" class="icon" />
          <span class="countdown-time">{{ formatCountdownTime(countdown.secondsLeft.value) }}</span>
          <span class="countdown-text">后自动返回</span>
        </div>
      </div>

      <div class="temp-content" @click="handleUserOperation">
        <div class="filter-bar">
          <div class="filter-row">
            <div class="filter-item">
              <label>柜子名称</label>
              <input type="text" v-model="filters.cabinetTitle" placeholder="请输入柜子名称" @keyup.enter="handleSearch" />
            </div>
            <div class="filter-item date-filter">
              <label>记录时间</label>
              <div class="date-range">
                <input type="date" v-model="filters.startTime" />
                <span>至</span>
                <input type="date" v-model="filters.endTime" />
              </div>
            </div>
            <div class="filter-actions">
              <button class="search-btn" @click.stop="handleSearch">
                <img src="/查询.svg" alt="查询" class="icon" /> 查询
              </button>
              <button class="reset-btn" @click.stop="handleReset">重置</button>
            </div>
          </div>
        </div>

        <div v-if="loading" class="loading-wrapper">
          <div class="loading-spinner"></div>
          <span>加载温湿度记录中...</span>
        </div>

        <div v-else-if="error" class="error-wrapper">
          <img src="/警告 (1).svg" alt="警告" class="icon" />
          <span>{{ error }}</span>
          <button class="retry-btn" @click="fetchData">重试</button>
        </div>

        <div v-else class="table-wrapper">
          <div class="table-container">
            <table class="temp-table">
              <thead>
                <tr>
                  <th>柜子名称</th>
                  <th>温度 (°C)</th>
                  <th>湿度 (%)</th>
                  <th>记录时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in logList" :key="item.recordTime + item.cabinetTitle">
                  <td>{{ item.cabinetTitle || '-' }}</td>
                  <td>{{ formatTemperature(item.temperature) }}</td>
                  <td>{{ formatHumidity(item.humidity) }}</td>
                  <td>{{ formatDateTime(item.recordTime) }}</td>
                  <td>
                    <button class="detail-row-btn" @click.stop="viewDetail(item)">查看详情</button>
                  </td>
                </tr>
                <tr v-if="filteredTotal === 0">
                  <td colspan="5" class="empty-row">
                    <div class="empty-content">
                      暂无温湿度记录
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="table-footer" v-if="filteredTotal > 0">
            <div class="footer-left">
              <div class="record-count">共 {{ filteredTotal }} 条记录</div>

              <div class="page-size-selector">
                <span class="size-label">每页显示:</span>
                <select v-model="pageSize" class="size-select" @change="handlePageSizeChange">
                  <option :value="10">10 条</option>
                  <option :value="20">20 条</option>
                  <option :value="50">50 条</option>
                </select>
              </div>
              <div class="page-jump-selector">
                <span class="jump-label">跳至</span>
                <input type="number" v-model.number="inputPageValue" class="jump-page-input" min="1" :max="totalPages"
                  @blur="jumpToPage" @keyup.enter="jumpToPage" />
                <span class="jump-unit">页</span>
              </div>
            </div>

            <div class="pagination-controls">
              <button class="page-btn" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">上一页</button>
              <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
              <button class="page-btn" :disabled="currentPage === totalPages"
                @click="changePage(currentPage + 1)">下一页</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <Transition name="modal-fade">
      <div v-if="detailVisible" class="detail-modal" @click="detailVisible = false">
        <div class="detail-card" @click.stop>
          <div class="card-glow"></div>
          <div class="detail-header">
            <div class="header-left">
              <img src="/温湿度.svg" alt="温湿度" class="icon header-icon" />
              <h3>温湿度详情</h3>
            </div>
            <button class="close-btn" @click="detailVisible = false">
              <img src="/关闭小.svg" alt="关闭" class="icon" />
            </button>
          </div>
          <div class="detail-body">
            <div class="info-card cabinet-info">
              <div class="card-title">
                <img src="/柜子.svg" alt="柜子" class="icon title-icon" />
                <span>柜子信息</span>
              </div>
              <div class="info-list">
                <div class="info-field">
                  <span class="field-label">柜子名称</span>
                  <span class="field-value">{{ currentDetail?.cabinetTitle || '-' }}</span>
                </div>
              </div>
            </div>

            <div class="sensor-card">
              <div class="sensor-item temperature">
                <div class="sensor-label">
                  <img src="/温度.svg" alt="温度" class="icon sensor-icon" />
                  <span>温度</span>
                </div>
                <div class="sensor-value">
                  {{ formatTemperature(currentDetail?.temperature) }}
                  <span class="sensor-unit">°C</span>
                </div>
              </div>
              <div class="sensor-divider"></div>
              <div class="sensor-item humidity">
                <div class="sensor-label">
                  <img src="/湿度-01.svg" alt="湿度" class="icon sensor-icon" />
                  <span>湿度</span>
                </div>
                <div class="sensor-value">
                  {{ formatHumidity(currentDetail?.humidity) }}
                  <span class="sensor-unit">%</span>
                </div>
              </div>
            </div>

            <div class="info-card time-info">
              <div class="card-title">
                <img src="/记录.svg" alt="记录" class="icon title-icon" />
                <span>记录信息</span>
              </div>
              <div class="info-list">
                <div class="info-field">
                  <span class="field-label">记录时间</span>
                  <span class="field-value">{{ formatDateTime(currentDetail?.recordTime) }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="detail-footer">
            <button class="footer-btn" @click="detailVisible = false">关 闭</button>
          </div>
        </div>
      </div>
    </Transition>

    <div v-if="showToast" class="toast-message">{{ toastText }}</div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCountdown } from '@/composables/useCountdown'
import { searchTempHumidityLogsByPage } from '@/api/tempHumidity'

interface TempHumidityLog {
  cabinetTitle: string
  temperature: number | undefined
  humidity: number | undefined
  recordTime: string
  cabinetId?: string
}

const router = useRouter()

// 倒计时管理
const countdown = useCountdown({
  onTimeout: () => {
    console.log('倒计时结束，返回设置页面')
    router.push('/settings')
  }
})

function handleUserOperation() {
  countdown.handleOperation()
}

function formatCountdownTime(seconds: number): string {
  if (seconds >= 60) {
    const minutes = Math.floor(seconds / 60)
    const remainingSeconds = seconds % 60
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`
  }
  return `${seconds}秒`
}

// 基础状态
const loading = ref(false)
const error = ref('')
const logList = ref<TempHumidityLog[]>([])

// ==================== 后端分页核心逻辑 ====================
const currentPage = ref(1)
const pageSize = ref(10)          // 默认每页展示10条
const inputPageValue = ref(1)     // 跳转目标页码
const filteredTotal = ref(0)      // 总条数
const totalPages = ref(1)         // 总页数

// 改变每页显示条数选择
function handlePageSizeChange() {
  handleUserOperation()
  currentPage.value = 1
  inputPageValue.value = 1
  fetchData() 
}

// 切换页码事件
function changePage(page: number) {
  handleUserOperation()
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    inputPageValue.value = page 

    // 换页时令外层容器滚动回顶部
    const outerFrame = document.querySelector('.outer-frame')
    if (outerFrame) outerFrame.scrollTop = 0

    fetchData()
  }
}

// 跳转页码逻辑
function jumpToPage() {
  handleUserOperation()
  let targetPage = Math.floor(inputPageValue.value)

  if (isNaN(targetPage) || targetPage < 1) {
    targetPage = 1
  } else if (targetPage > totalPages.value) {
    targetPage = totalPages.value
  }

  inputPageValue.value = targetPage
  changePage(targetPage)
}
// ============================================================

const getOffsetDateString = (offsetDays: number): string => {
  const date = new Date()
  date.setDate(date.getDate() + offsetDays)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const defaultStartTime = getOffsetDateString(-2)
const defaultEndTime = getOffsetDateString(0)

const filters = ref({
  cabinetTitle: '',
  startTime: defaultStartTime,
  endTime: defaultEndTime
})

const detailVisible = ref(false)
const currentDetail = ref<TempHumidityLog | null>(null)

const showToast = ref(false)
const toastText = ref('')
let toastTimer: ReturnType<typeof setTimeout> | null = null

function showMessage(text: string) {
  if (toastTimer) clearTimeout(toastTimer)
  toastText.value = text
  showToast.value = true
  toastTimer = setTimeout(() => {
    showToast.value = false
  }, 2000)
}

function formatDateTime(dateStr: string | null | undefined): string {
  if (!dateStr) return '-'
  try {
    return new Date(dateStr).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch {
    return dateStr
  }
}

function formatTemperature(temp: number | undefined): string {
  if (temp === undefined || temp === null) return '--'
  return temp.toFixed(1)
}

function formatHumidity(humidity: number | undefined): string {
  if (humidity === undefined || humidity === null) return '--'
  return Math.round(humidity).toString()
}

// 数据结构归一化
function normalizeData(rawList: any[]): TempHumidityLog[] {
  return rawList.map(item => {
    let temp: number | undefined = undefined
    if (item.temperature !== undefined && item.temperature !== null) {
      const num = parseFloat(item.temperature)
      if (!isNaN(num)) temp = num
    }
    let humidity: number | undefined = undefined
    if (item.humidity !== undefined && item.humidity !== null) {
      const num = parseFloat(item.humidity)
      if (!isNaN(num)) humidity = num
    }
    return {
      cabinetTitle: item.cabinetTitle || '',
      temperature: temp,
      humidity: humidity,
      recordTime: item.recordTime || '',
      cabinetId: item.cabinetId
    }
  })
}

async function fetchData() {
  loading.value = true
  error.value = ''

  try {
    // 组装搜索参数，只将存在的值传给后端
    const params: Record<string, any> = {}
    if (filters.value.cabinetTitle) params.cabinetTitle = filters.value.cabinetTitle
    if (filters.value.startTime) params.startTime = filters.value.startTime
    if (filters.value.endTime) params.endTime = filters.value.endTime


    const res = await searchTempHumidityLogsByPage(
      currentPage.value,
      pageSize.value,
      params
    )

    // 同步分页总条数与总页数
    filteredTotal.value = res.total || 0
    totalPages.value = res.pages || 1

    // 归一化并渲染
    logList.value = normalizeData(res.records || [])
  } catch (err: any) {
    console.error('获取温湿度日志失败:', err)
    error.value = err.message || '加载失败，请稍后重试'
    showMessage('加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  inputPageValue.value = 1
  handleUserOperation()
  fetchData()
}

function handleReset() {
  currentPage.value = 1
  inputPageValue.value = 1
  pageSize.value = 10
  handleUserOperation()
  filters.value = {
    cabinetTitle: '',
    startTime: defaultStartTime,
    endTime: defaultEndTime
  }
  fetchData()
}

function viewDetail(item: TempHumidityLog) {
  handleUserOperation()
  currentDetail.value = item
  detailVisible.value = true
}

function goBack() {
  router.push('/settings')
}

onMounted(() => {
  fetchData()
})

onUnmounted(() => {
  countdown.cleanup()
})
</script>

<style lang="css" scoped>
/* 保持你原本的所有 CSS 样式不变 */
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

.header-icon {
  width: 1.8em;
  height: 1.8em;
  filter: drop-shadow(0 0 4px rgba(34, 211, 238, 0.5));
}

.sensor-icon {
  width: 1.4em;
  height: 1.4em;
}

.temp-detail-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 20% 30%, #0a1a1f, #051016);
  padding: 20px;
  overflow: hidden;
}

.outer-frame {
  width: 100%;
  height: 100%;
  background: rgba(15, 25, 35, 0.7);
  border: 2px solid rgba(34, 211, 238, 0.4);
  border-radius: 32px;
  box-shadow: 0 0 20px rgba(34, 211, 238, 0.08), inset 0 0 12px rgba(34, 211, 238, 0.03);
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px 24px;
  box-sizing: border-box;
  scrollbar-width: thin;
}

.outer-frame::-webkit-scrollbar {
  width: 6px;
}

.outer-frame::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.3);
  border-radius: 4px;
}

.outer-frame::-webkit-scrollbar-thumb {
  background: #22d3ee;
  border-radius: 4px;
}

.temp-detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1400px;
  margin: 0 auto 30px;
  padding: 0 10px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(34, 211, 238, 0.5);
  border-radius: 60px;
  padding: 10px 20px;
  color: #c2f0e0;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.back-btn:hover {
  background: rgba(34, 211, 238, 0.2);
  border-color: #22d3ee;
}

.page-title {
  color: #c2f0e0;
  font-size: 24px;
  text-shadow: 0 0 8px rgba(34, 211, 238, 0.25);
  display: flex;
  align-items: center;
  gap: 10px;
}

.countdown-display {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(34, 211, 238, 0.15);
  border: 1px solid rgba(34, 211, 238, 0.5);
  border-radius: 60px;
  padding: 8px 16px;
  color: #22d3ee;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
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

.temp-content {
  max-width: 1400px;
  margin: 0 auto;
}

.filter-bar {
  background: rgba(15, 25, 35, 0.7);
  border-radius: 20px;
  border: 1px solid rgba(34, 211, 238, 0.2);
  padding: 20px;
  margin-bottom: 24px;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1 0 auto;
  min-width: 140px;
}

.filter-item label {
  font-size: 12px;
  color: #94a3b8;
}

.filter-item input {
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(34, 211, 238, 0.3);
  border-radius: 8px;
  padding: 8px 12px;
  color: #e2e8f0;
  font-size: 13px;
  outline: none;
  width: 100%;
}

.filter-item input:focus {
  border-color: #22d3ee;
  box-shadow: 0 0 4px rgba(34, 211, 238, 0.2);
}

.date-range {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.date-range input {
  flex: 1;
  min-width: 0;
}

.date-range span {
  color: #94a3b8;
  font-size: 12px;
  flex-shrink: 0;
}

.filter-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-btn,
.reset-btn {
  padding: 8px 20px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.search-btn {
  background: #22d3ee;
  color: #051016;
}

.search-btn:hover {
  background: #1cb5cc;
}

.reset-btn {
  background: rgba(255, 255, 255, 0.1);
  color: #cbd5e1;
  border: 1px solid rgba(34, 211, 238, 0.3);
}

.reset-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.table-wrapper {
  background: rgba(15, 25, 35, 0.7);
  border-radius: 20px;
  border: 1px solid rgba(34, 211, 238, 0.2);
  overflow: hidden;
}

.table-container {
  overflow-x: auto;
}

.temp-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.temp-table th,
.temp-table td {
  padding: 14px 12px;
  text-align: left;
  border-bottom: 1px solid rgba(34, 211, 238, 0.1);
}

.temp-table th {
  background: rgba(34, 211, 238, 0.1);
  color: #22d3ee;
  font-weight: 600;
  font-size: 13px;
}

.temp-table td {
  color: #cbd5e1;
}

.temp-table tr:hover td {
  background: rgba(34, 211, 238, 0.05);
}

.detail-row-btn {
  background: rgba(34, 211, 238, 0.15);
  border: 1px solid rgba(34, 211, 238, 0.4);
  border-radius: 6px;
  padding: 4px 12px;
  color: #22d3ee;
  font-size: 12px;
  cursor: pointer;
}

.detail-row-btn:hover {
  background: rgba(34, 211, 238, 0.3);
}

.empty-row {
  padding: 0 !important;
}

.empty-content {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #5b6e8c;
  font-size: 14px;
}

.table-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-top: 1px solid rgba(34, 211, 238, 0.1);
  flex-wrap: wrap;
  gap: 16px;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.record-count {
  color: #94a3b8;
  font-size: 13px;
}

.page-size-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.size-label {
  color: #94a3b8;
  font-size: 13px;
}

.size-select {
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(34, 211, 238, 0.3);
  border-radius: 6px;
  padding: 4px 8px;
  color: #e2e8f0;
  font-size: 12px;
  outline: none;
  cursor: pointer;
}

.size-select:focus {
  border-color: #22d3ee;
}

.page-jump-selector {
  display: flex;
  align-items: center;
  gap: 6px;
  border-left: 1px solid rgba(34, 211, 238, 0.2);
  padding-left: 16px;
}

.jump-label,
.jump-unit {
  color: #94a3b8;
  font-size: 13px;
}

.jump-page-input {
  background: rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(34, 211, 238, 0.4);
  border-radius: 6px;
  padding: 4px 6px;
  color: #22d3ee;
  font-size: 12px;
  width: 48px;
  text-align: center;
  outline: none;
}

.jump-page-input:focus {
  border-color: #22d3ee;
}

.jump-page-input::-webkit-outer-spin-button,
.jump-page-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.jump-page-input[type=number] {
  -moz-appearance: textfield;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-info {
  color: #cbd5e1;
  font-size: 13px;
  font-family: monospace;
}

.page-btn {
  background: rgba(34, 211, 238, 0.1);
  border: 1px solid rgba(34, 211, 238, 0.3);
  color: #22d3ee;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-btn:hover:not(:disabled) {
  background: rgba(34, 211, 238, 0.25);
  border-color: #22d3ee;
}

.page-btn:disabled {
  background: rgba(255, 255, 255, 0.02);
  border-color: rgba(255, 255, 255, 0.1);
  color: #475569;
  cursor: not-allowed;
}
</style>
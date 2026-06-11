<template>
  <div class="temp-detail-container">
    <div class="outer-frame">
      <div class="temp-detail-header">
        <button class="back-btn" @click="goBack">
          <span class="back-icon">←</span>
          <span>返回设置</span>
        </button>
        <h1 class="page-title">🌡️ 温湿度日志详情</h1>
        <div class="countdown-display" v-if="countdown && countdown.secondsLeft.value > 0">
          <span class="countdown-icon">⏱️</span>
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
              <button class="search-btn" @click.stop="handleSearch">🔍 查询</button>
              <button class="reset-btn" @click.stop="handleReset">重置</button>
            </div>
          </div>
        </div>

        <div v-if="loading" class="loading-wrapper">
          <div class="loading-spinner"></div>
          <span>加载温湿度记录中...</span>
        </div>

        <div v-else-if="error" class="error-wrapper">
          <span>⚠️ {{ error }}</span>
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
              <tr v-if="logList.length === 0">
                <td colspan="5" class="empty-row">暂无温湿度记录</td>
              </tr>
              </tbody>
            </table>
          </div>
          <div class="record-count">共 {{ logList.length }} 条记录</div>
        </div>
      </div>
    </div>

    <Transition name="modal-fade">
      <div v-if="detailVisible" class="detail-modal" @click="detailVisible = false">
        <div class="detail-card" @click.stop>
          <div class="card-glow"></div>
          <div class="detail-header">
            <div class="header-left">
              <span class="header-icon">🌡️</span>
              <h3>温湿度详情</h3>
            </div>
            <button class="close-btn" @click="detailVisible = false">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
          <div class="detail-body">
            <div class="info-card cabinet-info">
              <div class="card-title">
                <span class="title-icon">📦</span>
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
                  <span class="sensor-icon">🌡️</span>
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
                  <span class="sensor-icon">💧</span>
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
                <span class="title-icon">⏱️</span>
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
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCountdown } from '@/composables/useCountdown'
import { searchTempHumidityLogs } from '@/api/tempHumidity'

interface TempHumidityLog {
  cabinetTitle: string
  temperature: number | undefined
  humidity: number | undefined
  recordTime: string
  cabinetId?: string // 保留但不展示
}

const router = useRouter()

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

const loading = ref(false)
const error = ref('')
const logList = ref<TempHumidityLog[]>([])

const filters = ref({
  cabinetTitle: '',
  startTime: '',
  endTime: '',
  minTemperature: undefined as number | undefined,
  maxTemperature: undefined as number | undefined,
  minHumidity: undefined as number | undefined,
  maxHumidity: undefined as number | undefined
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

function normalizeAndFilter(rawList: any[]): TempHumidityLog[] {
  return rawList
      .map(item => {
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
        } as TempHumidityLog
      })
      .filter(item => {
        const { minTemperature, maxTemperature, minHumidity, maxHumidity } = filters.value
        if (item.temperature === undefined) return false
        if (minTemperature !== undefined && item.temperature < minTemperature) return false
        if (maxTemperature !== undefined && item.temperature > maxTemperature) return false
        if (item.humidity === undefined) return false
        if (minHumidity !== undefined && item.humidity < minHumidity) return false
        if (maxHumidity !== undefined && item.humidity > maxHumidity) return false
        return true
      })
}

async function fetchData() {
  loading.value = true
  error.value = ''

  try {
    const params: Record<string, any> = {}
    if (filters.value.cabinetTitle) params.cabinetTitle = filters.value.cabinetTitle
    if (filters.value.startTime) params.startTime = filters.value.startTime
    if (filters.value.endTime) params.endTime = filters.value.endTime

    const rawData = await searchTempHumidityLogs(params)
    const filtered = normalizeAndFilter(rawData)
    logList.value = filtered
  } catch (err: any) {
    console.error('获取温湿度日志失败:', err)
    error.value = err.message || '加载失败，请稍后重试'
    showMessage('加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  handleUserOperation()
  fetchData()
}

function handleReset() {
  handleUserOperation()
  filters.value = {
    cabinetTitle: '',
    startTime: '',
    endTime: '',
    minTemperature: undefined,
    maxTemperature: undefined,
    minHumidity: undefined,
    maxHumidity: undefined
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
/* 样式与之前保持一致，已移除所有温湿度阈值相关样式，仅保留基础布局 */
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
  background: rgba(15, 25, 35, 0.6);
  backdrop-filter: blur(8px);
  border: 2px solid rgba(34, 211, 238, 0.4);
  border-radius: 32px;
  box-shadow: 0 0 30px rgba(34, 211, 238, 0.1), inset 0 0 20px rgba(34, 211, 238, 0.05);
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
  backdrop-filter: blur(10px);
  border: 1px solid rgba(34, 211, 238, 0.5);
  border-radius: 60px;
  padding: 10px 20px;
  color: #c2f0e0;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn:hover {
  background: rgba(34, 211, 238, 0.2);
  border-color: #22d3ee;
}

.back-btn:active {
  transform: scale(0.96);
}

.back-icon {
  font-size: 18px;
}

.page-title {
  color: #c2f0e0;
  font-size: 24px;
  text-shadow: 0 0 10px rgba(34, 211, 238, 0.3);
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

.countdown-icon {
  font-size: 16px;
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

.temp-content {
  max-width: 1400px;
  margin: 0 auto;
}

.filter-bar {
  background: rgba(15, 25, 35, 0.7);
  backdrop-filter: blur(12px);
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

.filter-item input,
.filter-item select {
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(34, 211, 238, 0.3);
  border-radius: 8px;
  padding: 8px 12px;
  color: #e2e8f0;
  font-size: 13px;
  outline: none;
  transition: all 0.2s;
  width: 100%;
}

.filter-item input:focus,
.filter-item select:focus {
  border-color: #22d3ee;
}

.date-range,
.range-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.date-range input,
.range-inputs input {
  flex: 1;
  min-width: 0;
}

.date-range span,
.range-inputs span {
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
  transition: all 0.2s;
  border: none;
  white-space: nowrap;
}

.search-btn {
  background: #22d3ee;
  color: #051016;
}

.search-btn:hover {
  background: #1cb5cc;
  transform: translateY(-1px);
}

.reset-btn {
  background: rgba(255, 255, 255, 0.1);
  color: #cbd5e1;
  border: 1px solid rgba(34, 211, 238, 0.3);
}

.reset-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

@media (max-width: 900px) {
  .filter-row {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  .filter-item.date-filter {
    grid-column: 1 / 2;
  }
  .filter-actions {
    grid-column: 2 / 3;
    justify-content: flex-end;
  }
}

@media (max-width: 600px) {
  .filter-row {
    grid-template-columns: 1fr;
  }
  .filter-item.date-filter,
  .filter-actions {
    grid-column: 1 / 2;
  }
  .filter-actions {
    justify-content: stretch;
  }
  .search-btn,
  .reset-btn {
    flex: 1;
    text-align: center;
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
  transition: all 0.2s;
}

.detail-row-btn:hover {
  background: rgba(34, 211, 238, 0.3);
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
  to {
    transform: rotate(360deg);
  }
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

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: all 0.3s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from .detail-card,
.modal-fade-leave-to .detail-card {
  transform: scale(0.95) translateY(20px);
}

.detail-card {
  transition: transform 0.3s ease;
}

.detail-modal {
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
  z-index: 2000;
}

.detail-card {
  position: relative;
  background: linear-gradient(135deg, rgba(18, 28, 35, 0.98) 0%, rgba(10, 18, 24, 0.98) 100%);
  backdrop-filter: blur(20px);
  border-radius: 28px;
  border: 1px solid rgba(34, 211, 238, 0.3);
  width: 520px;
  max-width: 92vw;
  max-height: 88vh;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(34, 211, 238, 0.1);
}

.card-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #22d3ee, #06b6d4, #22d3ee, transparent);
  filter: blur(1px);
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(34, 211, 238, 0.15);
  background: rgba(0, 0, 0, 0.2);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  font-size: 24px;
  filter: drop-shadow(0 0 4px rgba(34, 211, 238, 0.5));
}

.detail-header h3 {
  color: #22d3ee;
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 1px;
}

.close-btn {
  background: rgba(255, 255, 255, 0.05);
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.2s;
}

.close-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
  transform: rotate(90deg);
}

.detail-body {
  padding: 24px;
  overflow-y: auto;
  max-height: calc(88vh - 140px);
}

.info-card {
  background: rgba(0, 0, 0, 0.3);
  border-radius: 20px;
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid rgba(34, 211, 238, 0.12);
  transition: all 0.2s;
}

.info-card:hover {
  border-color: rgba(34, 211, 238, 0.25);
  background: rgba(0, 0, 0, 0.35);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(34, 211, 238, 0.2);
}

.title-icon {
  font-size: 18px;
}

.card-title span:last-child {
  color: #c2f0e0;
  font-weight: 600;
  font-size: 15px;
  letter-spacing: 0.5px;
}

.info-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 11px;
  color: #7e8b9f;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.field-value {
  font-size: 14px;
  color: #e2e8f0;
  font-weight: 500;
  word-break: break-word;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-list .info-field {
  flex-direction: row;
  justify-content: space-between;
  align-items: baseline;
  padding: 8px 0;
  border-bottom: 1px dashed rgba(34, 211, 238, 0.08);
}

.info-list .field-label {
  font-size: 13px;
  text-transform: none;
  color: #94a3b8;
  min-width: 80px;
}

.info-list .field-value {
  text-align: right;
  font-size: 13px;
}

.sensor-card {
  background: rgba(0, 0, 0, 0.4);
  border-radius: 24px;
  padding: 20px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: space-around;
  border: 1px solid rgba(34, 211, 238, 0.2);
}

.sensor-item {
  text-align: center;
  flex: 1;
}

.sensor-label {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 14px;
  color: #94a3b8;
  margin-bottom: 12px;
}

.sensor-icon {
  font-size: 20px;
}

.sensor-value {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 8px;
  color: #e2e8f0;
}

.sensor-unit {
  font-size: 14px;
  font-weight: 400;
  color: #7e8b9f;
}

.sensor-divider {
  width: 1px;
  height: 60px;
  background: rgba(34, 211, 238, 0.2);
}

.detail-footer {
  padding: 16px 24px;
  border-top: 1px solid rgba(34, 211, 238, 0.12);
  display: flex;
  justify-content: flex-end;
  background: rgba(0, 0, 0, 0.2);
}

.footer-btn {
  background: linear-gradient(135deg, #22d3ee, #06b6d4);
  border: none;
  padding: 8px 28px;
  border-radius: 40px;
  color: #051016;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.footer-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(34, 211, 238, 0.3);
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
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

@media (max-width: 768px) {
  .outer-frame {
    padding: 12px 16px;
  }
  .page-title {
    font-size: 18px;
  }
  .back-btn span:last-child {
    display: none;
  }
  .back-btn {
    padding: 10px 12px;
  }
  .back-icon {
    font-size: 18px;
    margin: 0;
  }
  .toast-message {
    white-space: normal;
    text-align: center;
    max-width: 80vw;
  }
  .sensor-card {
    flex-direction: column;
    gap: 16px;
  }
  .sensor-divider {
    width: 80%;
    height: 1px;
  }
  .countdown-display {
    padding: 6px 12px;
    font-size: 12px;
  }
  .countdown-time {
    font-size: 14px;
  }
  .countdown-text {
    display: none;
  }
}
</style>

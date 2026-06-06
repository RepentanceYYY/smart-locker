<template>
  <div class="log-detail-container">
    <div class="log-detail-header">
      <button class="back-btn" @click="goBack">
        <span class="back-icon">←</span>
        <span>返回设置</span>
      </button>
      <h1 class="page-title">📋 操作日志详情</h1>
      <!-- 倒计时显示 -->
      <div class="countdown-display" v-if="countdown && countdown.secondsLeft.value > 0">
        <span class="countdown-icon">⏱️</span>
        <span class="countdown-time">{{ formatCountdownTime(countdown.secondsLeft.value) }}</span>
        <span class="countdown-text">后自动返回</span>
      </div>
    </div>

    <div class="log-content" @click="handleUserOperation">
      <!-- 筛选栏 - 优化后响应式布局 -->
      <div class="filter-bar">
        <div class="filter-row">
          <div class="filter-item">
            <label>借用人</label>
            <input type="text" v-model="filters.borrowerName" placeholder="请输入借用人姓名" @keyup.enter="handleSearch" />
          </div>
          <div class="filter-item">
            <label>工具名称</label>
            <input type="text" v-model="filters.toolName" placeholder="请输入工具名称" @keyup.enter="handleSearch" />
          </div>
          <div class="filter-item">
            <label>状态</label>
            <select v-model="filters.status">
              <option :value="undefined">全部</option>
              <option :value="0">未归还</option>
              <option :value="1">已归还</option>
              <option :value="2">逾期归还</option>
            </select>
          </div>
          <div class="filter-item date-filter">
            <label>借用时间</label>
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

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-wrapper">
        <div class="loading-spinner"></div>
        <span>加载中...</span>
      </div>

      <!-- 错误状态 -->
      <div v-else-if="error" class="error-wrapper">
        <span>⚠️ {{ error }}</span>
        <button class="retry-btn" @click="fetchData">重试</button>
      </div>

      <!-- 数据表格 -->
      <div v-else class="table-wrapper">
        <div class="table-container">
          <table class="log-table">
            <thead>
            <tr>
              <th>柜子名称</th>
              <th>格口号</th>
              <th>工具名称</th>
              <th>借用人</th>
              <th>工号/卡号</th>
              <th>借用时间</th>
              <th>预计归还</th>
              <th>归还时间</th>
              <th>状态</th>
              <th>借用照片</th>
              <th>操作</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="item in logList" :key="item.id">
              <td>{{ item.cabinetTitle || '-' }}</td>
              <td>{{ item.cellNumber || '-' }}</td>
              <td>{{ item.toolName || '-' }}</td>
              <td>{{ item.borrowerName || '-' }}</td>
              <td>{{ item.borrowerNumber || '-' }}</td>
              <td>{{ formatDateTime(item.borrowTime) }}</td>
              <td>{{ formatDateTime(item.expectedReturnTime) }}</td>
              <td>{{ formatDateTime(item.returnTime) }}</td>
              <td>
                  <span :class="getStatusClass(item)">
                    {{ getStatusText(item) }}
                  </span>
              </td>
              <td>
                <img
                    v-if="item.borrowerPhoto"
                    :src="item.borrowerPhoto"
                    class="table-photo"
                    @click.stop="previewImage(item.borrowerPhoto)"
                    @error="handleImageError"
                    alt="借用照片"
                />
                <span v-else class="no-photo">无照片</span>
              </td>
              <td>
                <button class="detail-row-btn" @click.stop="viewDetail(item)">查看详情</button>
              </td>
            </tr>
            <tr v-if="logList.length === 0">
              <td colspan="11" class="empty-row">暂无数据</td>
            </tr>
            </tbody>
          </table>
        </div>
        <div class="record-count">共 {{ logList.length }} 条记录</div>
      </div>
    </div>

    <!-- 详情模态框 - 美化版（保持不变） -->
    <Transition name="modal-fade">
      <div v-if="detailVisible" class="detail-modal" @click="detailVisible = false">
        <div class="detail-card" @click.stop>
          <div class="card-glow"></div>
          <div class="detail-header">
            <div class="header-left">
              <span class="header-icon">📋</span>
              <h3>借还详情</h3>
              <span :class="['status-badge', getStatusClass(currentDetail)]" class="header-status">
                {{ getStatusText(currentDetail) }}
              </span>
            </div>
            <button class="close-btn" @click="detailVisible = false">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
          <div class="detail-body">
            <!-- 工具信息卡片 -->
            <div class="info-card tool-info">
              <div class="card-title">
                <span class="title-icon">🔧</span>
                <span>工具信息</span>
              </div>
              <div class="info-grid grid-2col">
                <div class="info-field">
                  <span class="field-label">柜子名称</span>
                  <span class="field-value">{{ currentDetail?.cabinetTitle || '-' }}</span>
                </div>
                <div class="info-field">
                  <span class="field-label">格口号</span>
                  <span class="field-value">{{ currentDetail?.cellNumber || '-' }}</span>
                </div>
                <div class="info-field">
                  <span class="field-label">工具名称</span>
                  <span class="field-value highlight">{{ currentDetail?.toolName || '-' }}</span>
                </div>
                <div class="info-field">
                  <span class="field-label">预计归还</span>
                  <span class="field-value">{{ formatDateTime(currentDetail?.expectedReturnTime) }}</span>
                </div>
              </div>
            </div>

            <!-- 借还信息双栏布局 -->
            <div class="borrow-return-wrapper">
              <!-- 借出信息卡片 -->
              <div class="info-card borrow-card">
                <div class="card-title">
                  <span class="title-icon">📤</span>
                  <span>借出信息</span>
                </div>
                <div class="info-list">
                  <div class="info-field">
                    <span class="field-label">👤 借用人</span>
                    <span class="field-value">{{ currentDetail?.borrowerName || '-' }}</span>
                  </div>
                  <div class="info-field">
                    <span class="field-label">🆔 工号/卡号</span>
                    <span class="field-value">{{ currentDetail?.borrowerNumber || '-' }}</span>
                  </div>
                  <div class="info-field">
                    <span class="field-label">⏱️ 借用时间</span>
                    <span class="field-value">{{ formatDateTime(currentDetail?.borrowTime) }}</span>
                  </div>
                  <div class="info-field">
                    <span class="field-label">📝 借用说明</span>
                    <span class="field-value">{{ currentDetail?.borrowRemark || '无' }}</span>
                  </div>
                  <div class="info-field photo-field">
                    <span class="field-label">📷 借用照片</span>
                    <div class="photo-wrapper">
                      <img
                          v-if="currentDetail?.borrowerPhoto"
                          :src="currentDetail.borrowerPhoto"
                          class="detail-photo"
                          @click.stop="previewImage(currentDetail.borrowerPhoto)"
                          @error="handleImageError"
                      />
                      <span v-else class="no-photo">无照片</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 归还信息卡片 -->
              <div class="info-card return-card">
                <div class="card-title">
                  <span class="title-icon">📥</span>
                  <span>归还信息</span>
                </div>
                <div v-if="currentDetail?.returnTime" class="info-list">
                  <div class="info-field">
                    <span class="field-label">👤 归还人</span>
                    <span class="field-value">{{ currentDetail?.returnName || '-' }}</span>
                  </div>
                  <div class="info-field">
                    <span class="field-label">🆔 工号/卡号</span>
                    <span class="field-value">{{ currentDetail?.returnNumber || '-' }}</span>
                  </div>
                  <div class="info-field">
                    <span class="field-label">⏱️ 归还时间</span>
                    <span class="field-value">{{ formatDateTime(currentDetail?.returnTime) }}</span>
                  </div>
                  <div class="info-field">
                    <span class="field-label">📝 归还说明</span>
                    <span class="field-value">{{ currentDetail?.returnRemark || '无' }}</span>
                  </div>
                  <div class="info-field photo-field">
                    <span class="field-label">📷 归还照片</span>
                    <div class="photo-wrapper">
                      <img
                          v-if="currentDetail?.returnPhoto"
                          :src="currentDetail.returnPhoto"
                          class="detail-photo"
                          @click.stop="previewImage(currentDetail.returnPhoto)"
                          @error="handleImageError"
                      />
                      <span v-else class="no-photo">无照片</span>
                    </div>
                  </div>
                </div>
                <div v-else class="unreturned-state">
                  <div class="unreturned-icon">⏳</div>
                  <div class="unreturned-text">尚未归还</div>
                  <div class="unreturned-desc">该工具仍在借用中</div>
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

    <!-- 图片预览模态框 -->
    <Transition name="preview-fade">
      <div v-if="previewVisible" class="preview-modal" @click="previewVisible = false">
        <div class="preview-container" @click.stop>
          <img :src="previewUrl" class="preview-image" />
          <button class="preview-close" @click="previewVisible = false">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M18 6L6 18M6 6L18 18" stroke="white" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
        </div>
      </div>
    </Transition>

    <!-- Toast -->
    <div v-if="showToast" class="toast-message">{{ toastText }}</div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchAllLogList, type LogListDTO } from '@/api/log'
import { useCountdown } from '@/composables/useCountdown'

const router = useRouter()

// ==================== 倒计时功能 ====================
// 使用统一配置，不传入任何自定义参数
const countdown = useCountdown({
  onTimeout: () => {
    console.log('倒计时结束，返回设置页面')
    router.push('/')
  }
})

// 用户操作处理（重置倒计时）
function handleUserOperation() {
  countdown.handleOperation()
}

// 格式化倒计时显示时间 (mm:ss 或 ss秒)
function formatCountdownTime(seconds: number): string {
  if (seconds >= 60) {
    const minutes = Math.floor(seconds / 60)
    const remainingSeconds = seconds % 60
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`
  }
  return `${seconds}秒`
}

// ==================== 数据状态 ====================
const loading = ref(false)
const error = ref('')
const logList = ref<LogListDTO[]>([])

// 筛选条件
const filters = ref({
  borrowerName: '',
  toolName: '',
  status: undefined as number | undefined,
  startTime: '',
  endTime: ''
})

// 详情模态框
const detailVisible = ref(false)
const currentDetail = ref<LogListDTO | null>(null)

// 图片预览
const previewVisible = ref(false)
const previewUrl = ref('')

// Toast
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

// 格式化日期时间
function formatDateTime(dateStr: string | null | undefined): string {
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

// 图片错误处理
function handleImageError(e: Event) {
  const img = e.target as HTMLImageElement
  img.style.display = 'none'
  const parent = img.parentElement
  if (parent && !parent.querySelector('.no-photo')) {
    const span = document.createElement('span')
    span.className = 'no-photo'
    span.innerText = '图片加载失败'
    parent.appendChild(span)
  }
}

// 预览图片
function previewImage(url: string) {
  if (url) {
    // 用户操作，重置倒计时
    handleUserOperation()
    previewUrl.value = url
    previewVisible.value = true
  }
}

// 查看详情
function viewDetail(item: LogListDTO) {
  // 用户操作，重置倒计时
  handleUserOperation()
  currentDetail.value = item
  detailVisible.value = true
}

// 获取状态文本（支持逾期）
function getStatusText(item: LogListDTO | null): string {
  if (!item) return '-'
  if (!item.returnTime) return '未归还'
  if (item.expectedReturnTime && new Date(item.returnTime) > new Date(item.expectedReturnTime)) {
    return '逾期归还'
  }
  return '已归还'
}

// 获取状态样式类
function getStatusClass(item: LogListDTO | null): string {
  if (!item) return ''
  if (!item.returnTime) return 'status-badge unreturned'
  if (item.expectedReturnTime && new Date(item.returnTime) > new Date(item.expectedReturnTime)) {
    return 'status-badge overdue'
  }
  return 'status-badge returned'
}

// 获取全部数据
async function fetchData() {
  loading.value = true
  error.value = ''

  try {
    const params = {
      borrowerName: filters.value.borrowerName || undefined,
      toolName: filters.value.toolName || undefined,
      status: filters.value.status,
      startTime: filters.value.startTime || undefined,
      endTime: filters.value.endTime || undefined
    }
    const data = await fetchAllLogList(params)
    logList.value = data
  } catch (err: any) {
    console.error('获取日志列表失败:', err)
    error.value = err.message || '加载失败，请稍后重试'
    showMessage('加载失败')
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  // 用户操作，重置倒计时
  handleUserOperation()
  fetchData()
}

// 重置筛选
function handleReset() {
  // 用户操作，重置倒计时
  handleUserOperation()
  filters.value = {
    borrowerName: '',
    toolName: '',
    status: undefined,
    startTime: '',
    endTime: ''
  }
  fetchData()
}

// 返回
function goBack() {
  router.push('/settings')
}

onMounted(() => {
  fetchData()
})

// 组件卸载时清理倒计时
onUnmounted(() => {
  countdown.cleanup()
})
</script>

<style lang="css" scoped>
.log-detail-container {
  min-height: 100vh;
  background: radial-gradient(circle at 20% 30%, #0a1a1f, #051016);
  padding: 20px;
  overflow-y: auto;
}

.log-detail-header {
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

/* 倒计时显示样式 */
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

.log-content {
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

/* 竖屏/小屏幕优化布局 */
@media (max-width: 700px) {
  .filter-row {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
    align-items: start;
  }

  .filter-item:nth-child(1) {
    grid-column: 1 / 2;
    grid-row: 1;
  }

  .filter-item:nth-child(2) {
    grid-column: 2 / 3;
    grid-row: 1;
  }

  .filter-item:nth-child(3) {
    grid-column: 3 / 4;
    grid-row: 1;
  }

  .filter-item.date-filter {
    grid-column: 1 / 3;
    grid-row: 2;
    margin: 0;
  }

  .filter-actions {
    grid-column: 3 / 4;
    grid-row: 2;
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 8px;
    margin: 0;
  }

  .filter-item {
    min-width: auto;
  }

  .date-range {
    flex-wrap: nowrap;
  }

  .date-range input {
    min-width: 0;
    width: auto;
  }

  .search-btn,
  .reset-btn {
    padding: 8px 12px;
    font-size: 12px;
  }
}

/* 更窄屏幕 */
@media (max-width: 560px) {
  .filter-row {
    gap: 10px;
  }

  .date-range span {
    padding: 0 2px;
  }

  .filter-actions {
    gap: 6px;
  }

  .search-btn,
  .reset-btn {
    padding: 6px 10px;
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

.log-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.log-table th,
.log-table td {
  padding: 14px 12px;
  text-align: left;
  border-bottom: 1px solid rgba(34, 211, 238, 0.1);
}

.log-table th {
  background: rgba(34, 211, 238, 0.1);
  color: #22d3ee;
  font-weight: 600;
  font-size: 13px;
}

.log-table td {
  color: #cbd5e1;
}

.log-table tr:hover td {
  background: rgba(34, 211, 238, 0.05);
}

.status-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}

.status-badge.returned {
  background: rgba(34, 197, 94, 0.2);
  color: #4ade80;
  border: 1px solid rgba(74, 222, 128, 0.3);
}

.status-badge.unreturned {
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
  border: 1px solid rgba(248, 113, 113, 0.3);
}

.status-badge.overdue {
  background: rgba(249, 115, 22, 0.2);
  color: #fb923c;
  border: 1px solid rgba(249, 115, 22, 0.3);
}

.table-photo {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid rgba(34, 211, 238, 0.3);
  transition: transform 0.2s;
}

.table-photo:hover {
  transform: scale(1.1);
}

.no-photo {
  color: #5b6e8c;
  font-size: 12px;
  font-style: italic;
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

/* ================= 美化后的详情模态框样式 ================= */
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
  width: 860px;
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

.header-status {
  margin-left: 8px;
  font-size: 11px;
  padding: 4px 12px;
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

.info-grid {
  display: grid;
  gap: 16px;
}

.grid-2col {
  grid-template-columns: repeat(2, 1fr);
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

.field-value.highlight {
  color: #22d3ee;
  font-weight: 600;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
  min-width: 100px;
}

.info-list .field-value {
  text-align: right;
  font-size: 13px;
}

.borrow-return-wrapper {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.borrow-card,
.return-card {
  margin-bottom: 0;
}

.photo-field {
  flex-direction: column !important;
  align-items: flex-start !important;
  border-bottom: none !important;
  padding-top: 4px !important;
}

.photo-wrapper {
  margin-top: 8px;
  width: 100%;
}

.detail-photo {
  max-width: 100%;
  max-height: 160px;
  border-radius: 12px;
  cursor: pointer;
  border: 1px solid rgba(34, 211, 238, 0.3);
  transition: all 0.2s;
  object-fit: cover;
}

.detail-photo:hover {
  transform: scale(1.02);
  border-color: #22d3ee;
  box-shadow: 0 4px 12px rgba(34, 211, 238, 0.2);
}

.unreturned-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  text-align: center;
}

.unreturned-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.6;
}

.unreturned-text {
  color: #f87171;
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 6px;
}

.unreturned-desc {
  color: #7e8b9f;
  font-size: 12px;
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

.footer-btn:active {
  transform: translateY(0);
}

/* 图片预览美化 */
.preview-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.95);
  backdrop-filter: blur(16px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
  cursor: pointer;
}

.preview-container {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
}

.preview-image {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 16px;
  box-shadow: 0 0 40px rgba(34, 211, 238, 0.3);
}

.preview-close {
  position: absolute;
  top: -50px;
  right: 0;
  background: rgba(0, 0, 0, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.2);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.preview-close:hover {
  background: rgba(239, 68, 68, 0.8);
  transform: scale(1.1);
}

.preview-fade-enter-active,
.preview-fade-leave-active {
  transition: opacity 0.25s ease;
}

.preview-fade-enter-from,
.preview-fade-leave-to {
  opacity: 0;
}

/* Toast */
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

/* 滚动条 */
.log-detail-container::-webkit-scrollbar,
.table-container::-webkit-scrollbar,
.detail-card::-webkit-scrollbar,
.detail-body::-webkit-scrollbar {
  width: 5px;
  height: 5px;
}

.log-detail-container::-webkit-scrollbar-track,
.table-container::-webkit-scrollbar-track,
.detail-card::-webkit-scrollbar-track,
.detail-body::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.3);
  border-radius: 4px;
}

.log-detail-container::-webkit-scrollbar-thumb,
.table-container::-webkit-scrollbar-thumb,
.detail-card::-webkit-scrollbar-thumb,
.detail-body::-webkit-scrollbar-thumb {
  background: #22d3ee;
  border-radius: 4px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .log-table th,
  .log-table td {
    padding: 10px 8px;
    font-size: 12px;
  }
}

@media (max-width: 768px) {
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

  .borrow-return-wrapper {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .detail-card {
    max-height: 90vh;
  }

  .info-list .info-field {
    flex-direction: column;
    gap: 4px;
  }

  .info-list .field-label {
    min-width: auto;
  }

  .info-list .field-value {
    text-align: left;
  }

  .detail-header {
    padding: 16px 20px;
  }

  .detail-body {
    padding: 16px;
  }

  .header-status {
    display: none;
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

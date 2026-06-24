<template>
  <div class="log-detail-container">
    <!-- 新增外框，与页面一样大，内部滚动 -->
    <div class="outer-frame">
      <div class="log-detail-header">
        <button class="back-btn" @click="goBack">
          <img src="/bg-back.svg" alt="返回" class="icon" />
          <span>返回设置</span>
        </button>
        <h1 class="page-title">
          <img src="/文档.svg" alt="日志" class="icon title-icon" /> 操作日志详情
        </h1>
        <div class="countdown-display" v-if="countdown && countdown.secondsLeft.value > 0">
          <img src="/计时器.svg" alt="倒计时" class="icon" />
          <span class="countdown-time">{{ formatCountdownTime(countdown.secondsLeft.value) }}</span>
          <span class="countdown-text">后自动返回</span>
        </div>
      </div>

      <div class="log-content" @click="handleUserOperation">
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
              <button class="search-btn" @click.stop="handleSearch">
                <img src="/查询.svg" alt="查询" class="icon" /> 查询
              </button>
              <button class="reset-btn" @click.stop="handleReset">重置</button>
            </div>
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
                    <img v-if="item.borrowerPhoto" :src="formatImageUrl(item.borrowerPhoto)" class="table-photo"
                      @click.stop="previewImage(item.borrowerPhoto)" @error="handleImageError" alt="借用照片" />
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
    </div>

    <!-- 详情模态框 -->
    <div v-if="detailVisible" class="detail-modal" @click="detailVisible = false">
      <div class="detail-card" @click.stop>
        <div class="card-glow"></div>
        <div class="detail-header">
          <div class="header-left">
            <img src="/详情.svg" alt="详情" class="icon header-icon" />
            <h3>借还详情</h3>
            <span :class="['status-badge', getStatusClass(currentDetail)]" class="header-status">
              {{ getStatusText(currentDetail) }}
            </span>
          </div>
          <button class="close-btn" @click="detailVisible = false">
            ✕
          </button>
        </div>
        <div class="detail-body">
          <div class="info-card tool-info">
            <div class="card-title">
              <img src="/工具.svg" alt="工具" class="icon title-icon" />
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

          <div class="borrow-return-wrapper">
            <div class="info-card borrow-card">
              <div class="card-title">
                <img src="/进行中.svg" alt="借出" class="icon title-icon" />
                <span>借出信息</span>
              </div>
              <div class="info-list">
                <div class="info-field">
                  <span class="field-label">
                    <img src="/顾客.svg" alt="借用人" class="icon" /> 借用人
                  </span>
                  <span class="field-value">{{ currentDetail?.borrowerName || '-' }}</span>
                </div>
                <div class="info-field">
                  <span class="field-label">
                    <img src="/卡号.svg" alt="工号" class="icon" /> 工号/卡号
                  </span>
                  <span class="field-value">{{ currentDetail?.borrowerNumber || '-' }}</span>
                </div>
                <div class="info-field">
                  <span class="field-label">
                    <img src="/借用时间.svg" alt="借用时间" class="icon" /> 借用时间
                  </span>
                  <span class="field-value">{{ formatDateTime(currentDetail?.borrowTime) }}</span>
                </div>
                <div class="info-field">
                  <span class="field-label">
                    <img src="/说明.svg" alt="借用说明" class="icon" /> 借用说明
                  </span>
                  <span class="field-value">{{ currentDetail?.borrowRemark || '无' }}</span>
                </div>
                <div class="info-field photo-field">
                  <span class="field-label">
                    <img src="/图片.svg" alt="借用照片" class="icon" /> 借用照片
                  </span>
                  <div class="photo-wrapper">
                    <img v-if="currentDetail?.borrowerPhoto" :src="formatImageUrl(currentDetail.borrowerPhoto)"
                      class="detail-photo" @click.stop="previewImage(currentDetail.borrowerPhoto)"
                      @error="handleImageError" />
                    <span v-else class="no-photo">无照片</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="info-card return-card">
              <div class="card-title">
                <img src="/进行中.svg" alt="归还" class="icon title-icon" />
                <span>归还信息</span>
              </div>
              <div v-if="currentDetail?.returnTime" class="info-list">
                <div class="info-field">
                  <span class="field-label">
                    <img src="/顾客.svg" alt="归还人" class="icon" /> 归还人
                  </span>
                  <span class="field-value">{{ currentDetail?.returnName || '-' }}</span>
                </div>
                <div class="info-field">
                  <span class="field-label">
                    <img src="/卡号.svg" alt="工号" class="icon" /> 工号/卡号
                  </span>
                  <span class="field-value">{{ currentDetail?.returnNumber || '-' }}</span>
                </div>
                <div class="info-field">
                  <span class="field-label">
                    <img src="/借用时间.svg" alt="归还时间" class="icon" /> 归还时间
                  </span>
                  <span class="field-value">{{ formatDateTime(currentDetail?.returnTime) }}</span>
                </div>
                <div class="info-field">
                  <span class="field-label">
                    <img src="/说明.svg" alt="归还说明" class="icon" /> 归还说明
                  </span>
                  <span class="field-value">{{ currentDetail?.returnRemark || '无' }}</span>
                </div>
                <div class="info-field photo-field">
                  <span class="field-label">
                    <img src="/图片.svg" alt="归还照片" class="icon" /> 归还照片
                  </span>
                  <div class="photo-wrapper">
                    <img v-if="currentDetail?.returnPhoto" :src="formatImageUrl(currentDetail.returnPhoto)"
                      class="detail-photo" @click.stop="previewImage(currentDetail.returnPhoto)"
                      @error="handleImageError" />
                    <span v-else class="no-photo">无照片</span>
                  </div>
                </div>
              </div>
              <div v-else class="unreturned-state">
                <img src="/等待.svg" alt="等待" class="icon unreturned-icon" />
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

    <!-- 图片预览模态框 -->
    <div v-if="previewVisible" class="preview-modal" @click="previewVisible = false">
      <div class="preview-container" @click.stop>
        <img :src="formatImageUrl(previewUrl)" class="preview-image" />
        <button class="preview-close" @click="previewVisible = false">
          ✕
        </button>
      </div>
    </div>

    <!-- Toast -->
    <div v-if="showToast" class="toast-message">{{ toastText }}</div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchAllLogList, type LogListDTO } from '@/api/log'
import { useCountdown } from '@/composables/useCountdown'
import { formatImageUrl } from '@/utils/fileUtils'

const router = useRouter()

const countdown = useCountdown({
  onTimeout: () => {
    console.log('倒计时结束，返回设置页面')
    router.push('/')
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
const logList = ref<LogListDTO[]>([])

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
  borrowerName: '',
  toolName: '',
  status: undefined as number | undefined,
  startTime: defaultStartTime,
  endTime: defaultEndTime
})

const detailVisible = ref(false)
const currentDetail = ref<LogListDTO | null>(null)

const previewVisible = ref(false)
const previewUrl = ref('')

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
      minute: '2-digit'
    })
  } catch {
    return dateStr
  }
}

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

function previewImage(url: string) {
  if (url) {
    handleUserOperation()
    previewUrl.value = url
    previewVisible.value = true
  }
}

function viewDetail(item: LogListDTO) {
  handleUserOperation()
  currentDetail.value = item
  detailVisible.value = true
}

function getStatusText(item: LogListDTO | null): string {
  if (!item) return '-'
  if (!item.returnTime) return '未归还'
  if (item.expectedReturnTime && new Date(item.returnTime) > new Date(item.expectedReturnTime)) {
    return '逾期归还'
  }
  return '已归还'
}

function getStatusClass(item: LogListDTO | null): string {
  if (!item) return ''
  if (!item.returnTime) return 'status-badge unreturned'
  if (item.expectedReturnTime && new Date(item.returnTime) > new Date(item.expectedReturnTime)) {
    return 'status-badge overdue'
  }
  return 'status-badge returned'
}

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

function handleSearch() {
  handleUserOperation()
  fetchData()
}

function handleReset() {
  handleUserOperation()
  filters.value = {
    borrowerName: '',
    toolName: '',
    status: undefined,
    startTime: defaultStartTime,
    endTime: defaultEndTime
  }
  fetchData()
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

/* 原有样式完全保留，仅微调一些与图标有关的尺寸 */
.log-detail-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 20% 30%, #0a1a1f, #051016);
  padding: 20px;
  overflow: hidden;
}

/* ---------- 外框 – 无模糊 ---------- */
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

/* ---------- 头部 ---------- */
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

.back-btn:active {
  /* 无 scale */
}

.page-title {
  color: #c2f0e0;
  font-size: 24px;
  text-shadow: 0 0 8px rgba(34, 211, 238, 0.25);
  display: flex;
  align-items: center;
  gap: 10px;
}

/* ---------- 倒计时 – 无动画 ---------- */
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

/* ---------- 内容区 ---------- */
.log-content {
  max-width: 1400px;
  margin: 0 auto;
}

/* ---------- 过滤栏 – 无模糊 ---------- */
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

.filter-item input,
.filter-item select {
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(34, 211, 238, 0.3);
  border-radius: 8px;
  padding: 8px 12px;
  color: #e2e8f0;
  font-size: 13px;
  outline: none;
  width: 100%;
}

.filter-item input:focus,
.filter-item select:focus {
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

/* ---------- 响应式过滤栏 ---------- */
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

/* ---------- 表格 – 无模糊、无过渡 ---------- */
.table-wrapper {
  background: rgba(15, 25, 35, 0.7);
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

/* ---------- 状态徽章 ---------- */
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

/* ---------- 表格图片 ---------- */
.table-photo {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid rgba(34, 211, 238, 0.3);
}

.table-photo:hover {
  border-color: #22d3ee;
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

/* ---------- 加载 / 错误 – 保留 spin 动画（仅初始化） ---------- */
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

.retry-btn:hover {
  background: rgba(34, 211, 238, 0.3);
}

/* ---------- 详情模态框 – 无模糊、无动画 ---------- */
.detail-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.detail-card {
  position: relative;
  background: linear-gradient(135deg, rgba(18, 28, 35, 0.98) 0%, rgba(10, 18, 24, 0.98) 100%);
  border-radius: 28px;
  border: 1px solid rgba(34, 211, 238, 0.3);
  width: 860px;
  max-width: 92vw;
  max-height: 88vh;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(34, 211, 238, 0.1);
}

.card-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #22d3ee, #06b6d4, #22d3ee, transparent);
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
  /* 覆盖 .icon 默认大小 */
  width: 1.6em;
  height: 1.6em;
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
}

.close-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
}

.detail-body {
  padding: 24px;
  overflow-y: auto;
  max-height: calc(88vh - 140px);
}

/* ---------- 详情卡片 ---------- */
.info-card {
  background: rgba(0, 0, 0, 0.3);
  border-radius: 20px;
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid rgba(34, 211, 238, 0.12);
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

.card-title .title-icon {
  font-size: 18px;
  width: 1.4em;
  height: 1.4em;
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
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.field-label .icon {
  width: 1em;
  height: 1em;
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
  object-fit: cover;
}

.detail-photo:hover {
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
  width: 48px;
  height: 48px;
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
}

.footer-btn:hover {
  box-shadow: 0 4px 12px rgba(34, 211, 238, 0.3);
}

.footer-btn:active {
  /* 无 translateY */
}

/* ---------- 图片预览 – 无模糊、无动画 ---------- */
.preview-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.95);
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
  box-shadow: 0 0 30px rgba(34, 211, 238, 0.25);
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
}

.preview-close:hover {
  background: rgba(239, 68, 68, 0.8);
}

/* ---------- Toast – 无动画 ---------- */
.toast-message {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.9);
  color: #22d3ee;
  padding: 12px 24px;
  border-radius: 60px;
  font-size: 14px;
  font-weight: 500;
  border: 1px solid #22d3ee;
  z-index: 4000;
  white-space: nowrap;
}

/* ---------- 响应式 ---------- */
@media (max-width: 1200px) {

  .log-table th,
  .log-table td {
    padding: 10px 8px;
    font-size: 12px;
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

  .back-btn .icon {
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

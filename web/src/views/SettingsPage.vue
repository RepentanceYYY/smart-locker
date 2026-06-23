<template>
  <div class="settings-container">
    <!-- 新增外框，与页面一样大，内部滚动 -->
    <div class="outer-frame">
      <div class="settings-header">
        <button class="back-btn" @click="goBack">
          <img src="/bg-back.svg" alt="返回" class="icon" />
          <span>返回首页</span>
        </button>
        <h1 class="settings-title">
          <img src="/系统概览.svg" alt="系统概览" class="icon title-icon" /> 系统概览
        </h1>
        <div class="countdown-display" v-if="countdown && countdown.secondsLeft.value > 0 && systemConfigStore.loaded">
          <img src="/计时器.svg" alt="倒计时" class="icon" />
          <span class="countdown-time">{{ formatCountdownTime(countdown.secondsLeft.value) }}</span>
          <span class="countdown-text">后自动返回</span>
        </div>
        <div class="placeholder" v-else></div>
      </div>

      <div class="settings-content">
        <!-- 日志查询模块 -->
        <div class="info-card log-card" @click="handleUserOperation">
          <div class="card-header">
            <div class="header-left">
              <img src="/日志.svg" alt="日志" class="icon card-icon" />
              <h3>日志查询</h3>
            </div>
            <button class="detail-btn" @click.stop="viewDetail('log')">查看详情 →</button>
          </div>
          <div class="stats-row">
            <div class="stat-item">
              <div class="stat-value">{{ logStats.total }}</div>
              <div class="stat-label">日志总数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ logStats.unreturnedCount }}</div>
              <div class="stat-label">未归还数量</div>
            </div>
          </div>
          <div class="section-title">
            <img src="/超期未归还.svg" alt="未归还" class="icon" /> 未归还记录
          </div>
          <div class="unreturned-scroll-wrapper">
            <div class="unreturned-table">
              <div v-if="unreturnedList.length === 0" class="empty-tip">暂无未归还记录</div>
              <div v-else>
                <div class="table-header">
                  <span>柜子名称</span>
                  <span>格口号</span>
                  <span>工具</span>
                  <span>借用图片</span>
                  <span>借用时间</span>
                </div>
                <div v-for="(item, idx) in unreturnedList" :key="idx" class="table-row">
                  <span class="cell cabinet">{{ item.cabinetTitle || '-' }}</span>
                  <span class="cell cell-number">{{ item.cellNumber || '-' }}</span>
                  <span class="cell tool">{{ item.toolName || '-' }}</span>
                  <span class="cell photo">
                    <img v-if="item.borrowerPhoto" :src="formatImageUrl(item.borrowerPhoto)" class="borrow-photo"
                      @click.stop="previewImage(item.borrowerPhoto)" @error="handleImageError" alt="借用图片" />
                    <span v-else class="no-photo">无图片</span>
                  </span>
                  <span class="cell time">{{ item.borrowTime || '-' }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 硬件设置模块 -->
        <div class="info-card hardware-card" @click="handleUserOperation">
          <div class="card-header">
            <div class="header-left" :class="{ 'pressing-active': isPressing }" @mousedown="startPress"
              @mouseup="cancelPress" @mouseleave="cancelPress" @touchstart.prevent="startPress" @touchend="cancelPress"
              style="cursor: pointer; user-select: none;">
              <img src="/硬件设备.svg" alt="硬件" class="icon card-icon" />
              <h3>硬件设置</h3>
            </div>
            <button class="detail-btn" @click.stop="viewDetail('hardware')">查看详情 →</button>
          </div>
          <div v-if="hardwareLoading" class="loading-placeholder">
            <span class="loading-spinner"></span>
            <span>加载硬件配置中...</span>
          </div>
          <div v-else-if="hardwareError" class="error-placeholder">
            <img src="/警告 (1).svg" alt="警告" class="icon" />
            <span>加载失败：{{ hardwareError }}</span>
            <button class="retry-btn" @click.stop="fetchHardwareData">重试</button>
          </div>
          <template v-else>
            <div class="stats-row three-cols">
              <div class="stat-item">
                <div class="stat-value">{{ hardwareInfo.cabinetCount }}</div>
                <div class="stat-label">柜子数量</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ hardwareInfo.totalSlots }}</div>
                <div class="stat-label">总格口数</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ hardwareInfo.emptySlots }}</div>
                <div class="stat-label">空位数量</div>
              </div>
            </div>
            <div class="info-row">
              <span class="info-key">已用格口：</span>
              <span class="info-value">{{ hardwareInfo.usedSlots }}</span>
              <span class="info-key">占用率：</span>
              <span class="info-value">{{ hardwareInfo.usageRate }}%</span>
            </div>
          </template>
        </div>

        <!-- 温湿度日志查询模块 -->
        <div class="info-card temp-card" @click="handleUserOperation">
          <div class="card-header">
            <div class="header-left">
              <img src="/温湿度.svg" alt="温湿度" class="icon card-icon" />
              <h3>温湿度日志</h3>
            </div>
            <button class="detail-btn" @click.stop="viewDetail('tempHumidity')">查看详情 →</button>
          </div>
          <div v-if="tempHumidityLoading" class="loading-placeholder">
            <span class="loading-spinner"></span>
            <span>加载温湿度记录中...</span>
          </div>
          <div v-else-if="tempHumidityError" class="error-placeholder">
            <img src="/警告 (1).svg" alt="警告" class="icon" />
            <span>加载失败：{{ tempHumidityError }}</span>
            <button class="retry-btn" @click.stop="fetchTempHumidityLogsData">重试</button>
          </div>
          <div v-else>
            <div class="section-title">
              <img src="/最近记录.svg" alt="最近记录" class="icon" /> 最近记录
            </div>
            <div class="temp-table-wrapper">
              <div v-if="recentTempLogs.length === 0" class="empty-tip">
                暂无温湿度记录
              </div>
              <div v-else>
                <div class="table-header temp-header">
                  <span>柜子名称</span>
                  <span>温度 (°C)</span>
                  <span>湿度 (%)</span>
                  <span>记录时间</span>
                </div>
                <div v-for="(log, idx) in recentTempLogs" :key="idx" class="table-row temp-row">
                  <span class="cell cabinet-name">{{ log.cabinetTitle || '-' }}</span>
                  <span class="cell cell-number">
                    {{ log.temperature }}
                  </span>
                  <span class="cell cell-number">
                    {{ log.humidity }}
                  </span>
                  <span class="cell time">{{ log.recordTime }}</span>
                </div>
              </div>
            </div>
            <div class="config-footer-tip" v-if="systemConfigStore.config.tempHumidityLogInterval">
              <img src="/间隔.svg" alt="记录间隔" class="icon" />
              记录间隔：{{ systemConfigStore.config.tempHumidityLogInterval }} 分钟
            </div>
          </div>
        </div>

        <!-- 系统配置模块 -->
        <div class="info-card system-card" @click="handleUserOperation">
          <div class="card-header">
            <div class="header-left">
              <img src="/设置.svg" alt="系统配置" class="icon card-icon" />
              <h3>系统配置</h3>
            </div>
            <button class="reset-btn" @click.stop="openResetModal">恢复出厂设置</button>
          </div>
          <div v-if="systemConfigStore.loading" class="loading-placeholder">
            <span class="loading-spinner"></span>
            <span>加载系统配置中...</span>
          </div>
          <div v-else>
            <div class="config-grid">
              <div class="config-item" v-for="field in configFields" :key="field.key">
                <div class="config-label">{{ field.label }}</div>
                <div class="config-value-group">
                  <span class="config-value" :class="{ 'password-mask': field.key === 'adminPwd' }">
                    {{ formatDisplayValue(field.key) }}
                  </span>
                  <button class="edit-icon" @click.stop="openEditModal(field.key, field.label)" title="编辑">
                    <img src="/编辑.svg" alt="编辑" class="icon" />
                  </button>
                </div>
              </div>
            </div>
            <div class="config-footer-tip">
              <img src="/提示.svg" alt="提示" class="icon" />
              提示：点击 <img src="/编辑.svg" alt="编辑" class="icon" /> 图标修改配置，所有修改将自动保存
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑配置模态框 -->
    <div v-if="editModalVisible" class="modal-mask" @click.self="closeEditModal">
      <div class="modal-container">
        <div class="modal-header">
          <span>编辑 {{ editLabel }}</span>
          <button class="modal-close" @click="closeEditModal">
            ✕
          </button>
        </div>
        <div class="modal-body">
          <input
            v-if="editField !== 'adminPwd' && editField !== 'borrowPeriod' && editField !== 'autoReturnTimeoutMinutes' && editField !== 'tempHumidityLogInterval' && editField !== 'enableFaceCapture' && editField != 'silentLivenessEnabled'"
            v-model="editTempValue" type="text" class="modal-input" :placeholder="`请输入新的${editLabel}`"
            :maxlength="editField === 'systemName' || editField === 'systemCode' ? 10 : undefined"
            @keyup.enter="confirmEdit" />
          <div v-else-if="editField === 'adminPwd'" class="password-input-wrapper">
            <input v-model="editTempValue" :type="passwordVisible ? 'text' : 'password'"
              class="modal-input password-input" placeholder="请输入新密码（不可包含中文）" @keyup.enter="confirmEdit" />
            <button type="button" class="password-toggle" @click="togglePasswordVisibility" tabindex="-1">
              <img v-if="passwordVisible" src="/隐藏.svg" alt="隐藏" class="icon" />
              <img v-else src="/显示.svg" alt="显示" class="icon" />
            </button>
          </div>
          <select v-else-if="editField === 'borrowPeriod'" v-model="editTempValue" class="modal-select"
            @change="confirmEdit">
            <option v-for="option in borrowPeriodOptions" :key="option" :value="option">
              {{ option }}
            </option>
          </select>
          <select v-else-if="editField === 'autoReturnTimeoutMinutes'" v-model="editTempValue" class="modal-select"
            @change="confirmEdit">
            <option v-for="option in timeoutOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
          <select v-else-if="editField === 'tempHumidityLogInterval'" v-model="editTempValue" class="modal-select"
            @change="confirmEdit">
            <option v-for="option in humidityIntervalOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
          <select v-else-if="editField === 'enableFaceCapture'" v-model="editTempValue" class="modal-select"
            @change="confirmEdit">
            <option v-for="option in faceCaptureOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
          <select v-else-if="editField === 'silentLivenessEnabled'" v-model="editTempValue" class="modal-select"
            @change="confirmEdit">
            <option v-for="option in silentLivenessEnabledOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </div>
        <div class="modal-footer">
          <button class="modal-btn cancel" @click="closeEditModal">取消</button>
          <button class="modal-btn confirm" :disabled="isActivatingFace" @click="confirmEdit">
            {{ isActivatingFace ? '激活中...' : '确认' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 恢复出厂设置确认模态框 -->
    <div v-if="resetModalVisible" class="modal-mask" @click.self="closeResetModal">
      <div class="modal-container reset-modal">
        <div class="modal-header warning-header">
          <img src="/警告 (1).svg" alt="警告" class="icon" />
          <span>恢复出厂设置</span>
          <button class="modal-close" @click="closeResetModal">
            ✕
          </button>
        </div>
        <div class="modal-body">
          <div class="reset-warning">
            <p>此操作将<strong>重置所有系统配置</strong>为默认值，且<strong>不可撤销</strong>！</p>
            <p>请输入 <strong class="confirm-text">"确定"</strong> 以继续操作：</p>
          </div>
          <input v-model="resetInputValue" type="text" class="modal-input" placeholder="请输入“确定”"
            @keyup.enter="confirmReset" :disabled="isResetting" autofocus />
          <div v-if="resetErrorMsg" class="reset-error">{{ resetErrorMsg }}</div>
        </div>
        <div class="modal-footer">
          <button class="modal-btn cancel" @click="closeResetModal" :disabled="isResetting">取消</button>
          <button class="modal-btn confirm danger" @click="confirmReset" :disabled="isResetting">
            {{ isResetting ? '重置中...' : '确认重置' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Toast 提示 -->
    <div v-if="showToast" class="toast-message">{{ toastText }}</div>

    <!-- 图片预览模态框 -->
    <div v-if="previewVisible" class="preview-modal" @click="previewVisible = false">
      <img :src="formatImageUrl(previewUrl)" class="preview-image" @click.stop />
    </div>

    <!-- 格口柜格口地址设置 -->
    <div v-if="slotAddressModalVisible" class="modal-mask" @click.self="closeSlotAddressModal">
      <div class="modal-container slot-modal" style="width: 480px; max-width: 95%; position: relative;">

        <div v-if="isWsConnectingForBoxAddressConfig" class="hw-loading-overlay" style="
      position: absolute; top: 0; left: 0; right: 0; bottom: 0; 
      background: rgba(15, 23, 42, 0.85); backdrop-filter: blur(4px);
      z-index: 10; display: flex; flex-direction: column; align-items: center; 
      justify-content: center; padding: 24px; text-align: center; border-radius: inherit;
    ">
          <div class="hw-spinner"></div>

          <h3 style="color: #f1f5f9; font-size: 18px; font-weight: 600; margin-bottom: 8px;">硬件配置中...</h3>
          <p style="color: #22d3ee; font-size: 15px; line-height: 1.5; max-width: 80%;">
            {{ wsStatusMessage || '正在建立硬件连接通道...' }}
          </p>
          <span style="color: #94a3b8; font-size: 13px; margin-top: 16px;">请按照硬件提示进行操作，在此期间请勿关闭页面</span>
        </div>

        <div class="modal-header" style="font-size: 18px; padding: 18px 24px;">
          <span style="display: flex; align-items: center; gap: 10px;">
            <img src="/密码锁.svg" alt="锁" class="icon" style="color: #22d3ee; width: 1.4em; height: 1.4em;" />
            格口柜格口地址设置
          </span>
          <button v-if="!isWsConnectingForBoxAddressConfig" class="modal-close" @click="closeSlotAddressModal"
            style="font-size: 20px;">✕</button>
        </div>

        <div class="modal-body" style="padding: 28px 24px;">
          <div class="modal-form-group" style="margin-bottom: 24px;">
            <label class="form-label"
              style="display: flex; align-items: center; gap: 8px; margin-bottom: 10px; color: #cbd5e1; font-size: 16px; font-weight: 500;">
              <img src="/通讯方式.svg" alt="通讯方式" class="icon" style="width: 18px; height: 18px;" />
              通讯方式 <span style="color: #f87171;">*</span>
            </label>
            <div class="type-selector">
              <button type="button" class="type-btn" :class="{ active: slotConfigForm.communicationType === 'serial' }"
                :disabled="isWsConnectingForBoxAddressConfig" @click="slotConfigForm.communicationType = 'serial'"
                style="height: 46px; font-size: 16px; gap: 10px;">
                <img src="/rs485.svg" alt="RS485" class="icon" style="width: 18px; height: 18px;" /> 串口通信
              </button>
              <button type="button" class="type-btn" :class="{ active: slotConfigForm.communicationType === 'tcp' }"
                :disabled="isWsConnectingForBoxAddressConfig" @click="slotConfigForm.communicationType = 'tcp'"
                style="height: 46px; font-size: 16px; gap: 10px;">
                <img src="/TCP-.svg" alt="TCP" class="icon" style="width: 18px; height: 18px;" /> TCP 网络通信
              </button>
            </div>
          </div>

          <div class="modal-form-group" style="margin-bottom: 24px;">
            <label class="form-label"
              style="display: flex; align-items: center; gap: 8px; margin-bottom: 10px; color: #cbd5e1; font-size: 16px; font-weight: 500;">
              <img src="/端口.svg" alt="端口" class="icon" style="width: 18px; height: 18px;" />
              硬件通信地址 <span style="color: #f87171;">*</span>
            </label>
            <input v-model="slotConfigForm.communicationAddress" type="text" class="modal-input large-input"
              :disabled="isWsConnectingForBoxAddressConfig"
              :placeholder="slotConfigForm.communicationType === 'serial' ? '如: COM1@9600' : '如: 192.168.0.1:8252'" />
          </div>

          <div class="modal-form-group" style="margin-bottom: 24px;">
            <label class="form-label"
              style="display: flex; align-items: center; gap: 8px; margin-bottom: 10px; color: #cbd5e1; font-size: 16px; font-weight: 500;">
              <img src="/计时器.svg" alt="超时" class="icon" style="width: 18px; height: 18px;" />
              超时时间（秒）<span style="color: #f87171;">*</span>
            </label>

            <input v-model="slotConfigForm.timeout" type="text" class="modal-input large-input"
              :disabled="isWsConnectingForBoxAddressConfig" placeholder="如: 30" />

            <div style="margin-top: 6px; color: #94a3b8; font-size: 13px;">
              超过设定时间未完成配置将自动断开连接
            </div>
          </div>

          <div class="modal-form-group-row" style="gap: 20px;">
            <div class="modal-form-group" style="margin-bottom: 0;">
              <label class="form-label"
                style="display: flex; align-items: center; gap: 8px; margin-bottom: 10px; color: #cbd5e1; font-size: 16px; font-weight: 500;">
                <img src="/地址.svg" alt="地址" class="icon" style="width: 18px; height: 18px;" />
                起始地址 <span style="color: #f87171;">*</span>
              </label>
              <input v-model="slotConfigForm.startAddress" type="text" class="modal-input large-input"
                :disabled="isWsConnectingForBoxAddressConfig" placeholder="如: 1" />
            </div>
            <div class="modal-form-group" style="margin-bottom: 0;">
              <label class="form-label"
                style="display: flex; align-items: center; gap: 8px; margin-bottom: 10px; color: #cbd5e1; font-size: 16px; font-weight: 500;">
                <img src="/地址.svg" alt="地址" class="icon" style="width: 18px; height: 18px;" />
                结束地址 <span style="color: #f87171;">*</span>
              </label>
              <input v-model="slotConfigForm.endAddress" type="text" class="modal-input large-input" placeholder="如: 24"
                :disabled="isWsConnectingForBoxAddressConfig"
                @keyup.enter="!isWsConnectingForBoxAddressConfig && confirmSlotAddress()" />
            </div>
          </div>
        </div>

        <div class="modal-footer" style="padding: 16px 24px 28px; gap: 16px;">
          <button class="modal-btn cancel large-btn" :disabled="isWsConnectingForBoxAddressConfig"
            @click="closeSlotAddressModal">取消</button>
          <button class="modal-btn confirm large-btn" :disabled="isWsConnectingForBoxAddressConfig"
            @click="confirmSlotAddress" style="position: relative;">
            <span v-if="isWsConnectingForBoxAddressConfig">同步中...</span>
            <span v-else>确认保存</span>
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useSystemConfigStore } from '@/stores/systemConfig'
import { fetchLogOverview, type UnreturnedItem } from '@/api/log'
import { fetchCabinetList } from '@/api/cabinet'
import type { SystemConfig } from '@/api/system'
import { useCountdown } from '@/composables/useCountdown'
import { fetchTempHumidityLogs, type TempHumidityLog } from '@/api/tempHumidity'
import { formatImageUrl } from '@/utils/fileUtils'

const router = useRouter()
const systemConfigStore = useSystemConfigStore()

const countdown = useCountdown({
  onTimeout: () => {
    console.log('倒计时结束，返回首页')
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

// 日志数据
const logStats = ref({ total: 0, unreturnedCount: 0 })
const unreturnedList = ref<UnreturnedItem[]>([])

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

async function fetchLogData() {
  try {
    const data = await fetchLogOverview()
    logStats.value.total = data.totalLogs
    logStats.value.unreturnedCount = data.unreturnedCount
    const list = [...data.unreturnedList].sort((a, b) => {
      if (!a.borrowTime) return 1
      if (!b.borrowTime) return -1
      return new Date(a.borrowTime).getTime() - new Date(b.borrowTime).getTime()
    })
    unreturnedList.value = list
  } catch (error) {
    console.error('获取日志数据失败:', error)
    showMessage('获取日志数据失败，请稍后重试')
  }
}

// 硬件数据
interface CellItem { id: number; type: string; isEmpty?: boolean }
interface CabinetRow { cells: CellItem[] }
interface CabinetFromAPI { id: number; title: string; rows: CabinetRow[] }

const hardwareLoading = ref(true)
const hardwareError = ref('')
const hardwareInfo = ref({
  cabinetCount: 0,
  totalSlots: 0,
  emptySlots: 0,
  usedSlots: 0,
  usageRate: 0
})

async function fetchHardwareData() {
  hardwareLoading.value = true
  hardwareError.value = ''
  try {
    const cabinetList = (await fetchCabinetList()) as CabinetFromAPI[]
    let cabinetCount = 0
    let totalCells = 0
    let emptyCells = 0
    for (const cabinet of cabinetList) {
      cabinetCount++
      if (cabinet.rows) {
        for (const row of cabinet.rows) {
          if (row.cells) {
            for (const cell of row.cells) {
              if (cell.type === 'cell') {
                totalCells++
                if (cell.isEmpty === true) emptyCells++
              }
            }
          }
        }
      }
    }
    const usedCells = totalCells - emptyCells
    const usageRate = totalCells > 0 ? Number(((usedCells / totalCells) * 100).toFixed(1)) : 0
    hardwareInfo.value = {
      cabinetCount,
      totalSlots: totalCells,
      emptySlots: emptyCells,
      usedSlots: usedCells,
      usageRate
    }
  } catch (error) {
    console.error('获取硬件数据失败:', error)
    hardwareError.value = '获取硬件配置失败，请检查网络或稍后重试'
    showMessage('硬件数据加载失败')
  } finally {
    hardwareLoading.value = false
  }
}

// 温湿度日志数据
const tempHumidityLoading = ref(false)
const tempHumidityError = ref('')
const recentTempLogs = ref<TempHumidityLog[]>([])
const tempHumidityStats = ref({ total: 0, latestTemp: '--', latestHumidity: '--' })

async function fetchTempHumidityLogsData() {
  tempHumidityLoading.value = true
  tempHumidityError.value = ''
  try {
    const logs = await fetchTempHumidityLogs(5)
    recentTempLogs.value = logs
    tempHumidityStats.value.total = logs.length
  } catch (error) {
    console.error('获取温湿度日志失败:', error)
    tempHumidityError.value = '加载温湿度记录失败，请稍后重试'
    showMessage('温湿度数据加载失败')
    recentTempLogs.value = []
  } finally {
    tempHumidityLoading.value = false
  }
}

// 系统配置
const configFields: { key: keyof SystemConfig; label: string }[] = [
  { key: 'systemName', label: '系统名称' },
  { key: 'engName', label: '英文名称' },
  { key: 'systemCode', label: '系统编号' },
  { key: 'location', label: '所属位置' },
  { key: 'adminPwd', label: '管理密码' },
  { key: 'borrowPeriod', label: '使用周期' },
  { key: 'autoReturnTimeoutMinutes', label: '长时间不操作返回主页' },
  { key: 'tempHumidityLogInterval', label: '温湿度记录间隔（分钟）' },
  { key: 'enableFaceCapture', label: '开启抓拍人脸' },
  { key: 'baiduFaceLicenseKey', label: '人脸授权码' },
  { key: 'silentLivenessEnabled', label: '活体检测' }
]

const borrowPeriodOptions = ['1天', '3天', '5天', '7天', '15天', '30天']
const timeoutOptions = [
  { label: '1分钟', value: 1 },
  { label: '3分钟', value: 3 },
  { label: '5分钟', value: 5 },
  { label: '10分钟', value: 10 }
]
const humidityIntervalOptions = [
  { label: '1分钟', value: 1 },
  { label: '2分钟', value: 2 },
  { label: '5分钟', value: 5 },
  { label: '10分钟', value: 10 },
  { label: '30分钟', value: 30 },
  { label: '60分钟', value: 60 }
]
const faceCaptureOptions = [
  { label: '关闭', value: 0 },
  { label: '开启', value: 1 }
]
const silentLivenessEnabledOptions = [
  { label: '关闭', value: 0 },
  { label: '开启', value: 1 }
]

function formatDisplayValue(key: keyof SystemConfig): string {
  const val = systemConfigStore.config[key]
  if (key === 'adminPwd') {
    return val ? '●'.repeat(Math.min((val as string).length, 8)) : '未设置'
  }
  if (key === 'autoReturnTimeoutMinutes' || key === 'tempHumidityLogInterval') {
    return `${val} 分钟`
  }
  if (key === 'borrowPeriod') {
    return val && !(val as string).includes('天') ? `${val}天` : (val as string)
  }
  if (key === 'enableFaceCapture') {
    return val === 1 ? '开启' : '关闭'
  }
  if (key === 'silentLivenessEnabled') {
    return val === 1 ? '开启' : '关闭'
  }
  return val as string
}

// 配置格口柜格口地址相关
let longPressTimer: ReturnType<typeof setTimeout> | null = null
const isPressing = ref(false)
const slotAddressModalVisible = ref(false)
const isWsConnectingForBoxAddressConfig = ref(false)
const wsStatusMessage = ref('')

interface CabinetSlotConfig {
  communicationType: 'tcp' | 'serial'; // 硬件通信类型
  communicationAddress: string;       // 硬件通信地址  
  startAddress: string | number;      // 起始地址
  endAddress: string | number;        // 结束地址
  timeout: string | number // 超时时间
}

const slotConfigForm = ref<CabinetSlotConfig>({
  communicationAddress: '',
  communicationType: 'tcp',
  startAddress: '',
  endAddress: '',
  timeout: ''
})

// 开始长按计时
function startPress() {
  handleUserOperation() // 刷新页面倒计时
  isPressing.value = true

  // 设置 5000 毫秒（5秒）的定时器
  longPressTimer = setTimeout(() => {
    isPressing.value = false
    longPressTimer = null

    // 5秒到了！打开格口柜地址设置模态框
    openSlotAddressModal()
  }, 5000)
}

// 取消长按
function cancelPress() {
  if (longPressTimer) {
    clearTimeout(longPressTimer)
    longPressTimer = null
  }
  isPressing.value = false
}

// 控制格口地址设置模态框打开的方法
const openSlotAddressModal = (existingConfig?: Partial<CabinetSlotConfig>) => {
  handleUserOperation() // 刷新页面操作倒计时

  wsStatusMessage.value = ''

  if (existingConfig) {
    // 如果有历史配置，回显数据
    slotConfigForm.value = {
      communicationAddress: existingConfig.communicationAddress || '',
      communicationType: existingConfig.communicationType || 'tcp',
      startAddress: existingConfig.startAddress || '',
      endAddress: existingConfig.endAddress || '',
      timeout: existingConfig.timeout || '',
    }
  } else {
    // 否则清空表单，恢复默认
    slotConfigForm.value = {
      communicationAddress: '',
      communicationType: 'tcp',
      startAddress: '',
      endAddress: '',
      timeout: '',
    }
  }
  slotAddressModalVisible.value = true
}

// 控制格口地址设置模态框关闭的方法
const closeSlotAddressModal = () => {
  if (isWsConnectingForBoxAddressConfig.value) {
    showMessage('正在与硬件通信中，请稍候...')
    return // 防止在阻塞等待硬件响应时用户误触关闭
  }
  slotAddressModalVisible.value = false
}

const sendConfigViaWebSocket = (payloadData: any) => {
  if (isWsConnectingForBoxAddressConfig.value) return

  isWsConnectingForBoxAddressConfig.value = true
  wsStatusMessage.value = '正在建立硬件连接通道...'

  const wsUrl = `${import.meta.env.VITE_WS_BASE_URL}/boxAddressConfig`
  let socket: WebSocket | null = null

  try {
    socket = new WebSocket(wsUrl)

    // 连接成功后发送请求 JSON
    socket.onopen = () => {
      const timestamp = String(Date.now())

      const requestPayload = {
        action: 'boxAddressConfig',
        requestId: timestamp,
        timestamp: timestamp,
        data: payloadData
      }

      socket?.send(JSON.stringify(requestPayload))
    }

    // 接收后端推送
    socket.onmessage = (event) => {
      try {
        const response = JSON.parse(event.data)
        const { code, message } = response

        if (code === 201) {
          wsStatusMessage.value = message || '等待硬件响应中...'
          showMessage(wsStatusMessage.value)
          return
        }

        // 处理最终成功状态
        if (code === 200) {
          showMessage(message || '格口柜地址设置成功')
          isWsConnectingForBoxAddressConfig.value = false
          closeSlotAddressModal()
          socket?.close()
        } else {
          // 处理业务失败状态
          showMessage(message || '设置失败')
          isWsConnectingForBoxAddressConfig.value = false
          wsStatusMessage.value = ''
          socket?.close()
        }

      } catch (err) {
        console.error('解析后端 WebSocket 数据失败:', err)
        showMessage('解析服务器响应失败')
        isWsConnectingForBoxAddressConfig.value = false
        socket?.close()
      }
    }

    // 连接关闭
    socket.onclose = (event) => {
      console.log('WebSocket 通道已关闭:', event.reason)
      isWsConnectingForBoxAddressConfig.value = false
    }

    // 异常处理
    socket.onerror = (error) => {
      console.error('WebSocket 发生异常:', error)
      showMessage('连接服务器失败，请检查网络或后端服务')
      isWsConnectingForBoxAddressConfig.value = false
      wsStatusMessage.value = ''
    }

  } catch (error) {
    console.error('初始化 WebSocket 失败:', error)
    showMessage('无法创建网络连接')
    isWsConnectingForBoxAddressConfig.value = false
    wsStatusMessage.value = ''
  }
}

// 提交格口地址设置表单保存的方法
const confirmSlotAddress = () => {
  const { communicationAddress, communicationType, startAddress, endAddress, timeout } = slotConfigForm.value
  const addrTrimmed = communicationAddress.trim()

  // 表单非空基本校验
  if (!addrTrimmed) {
    showMessage('硬件通信地址不能为空')
    return
  }
  if (!timeout) {
    showMessage('超时时间不能为空')
    return
  }
  if (!String(startAddress).trim()) {
    showMessage('起始地址不能为空')
    return
  }
  if (!String(endAddress).trim()) {
    showMessage('结束地址不能为空')
    return
  }

  // 硬件通信格式精准校验
  if (communicationType === 'serial') {
    if (!addrTrimmed.includes('@')) {
      showMessage('串口格式错误，正确格式如：COM1@9600')
      return
    }

    const [port, baudRate] = addrTrimmed.split('@')

    if (!port?.trim()) {
      showMessage('串口通信地址错误：串口号（如COM1）不能为空')
      return
    }
    if (!baudRate?.trim() || !/^\d+$/.test(baudRate.trim())) {
      showMessage('串口通信地址错误：波特率必须为纯数字（如9600）')
      return
    }
  } else if (communicationType === 'tcp') {
    const hasEnglishColon = addrTrimmed.includes(':')
    const hasChineseColon = addrTrimmed.includes('：')

    if (!hasEnglishColon && !hasChineseColon) {
      showMessage('TCP格式错误，正确格式如：192.168.0.1:8252')
      return
    }

    const separator = hasEnglishColon ? ':' : '：'
    const [ip, portStr] = addrTrimmed.split(separator)

    if (!ip?.trim() || !/^((25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(25[0-5]|2[0-4]\d|[01]?\d\d?)$/.test(ip.trim())) {
      showMessage('TCP通信地址错误：IP 地址格式不合法')
      return
    }

    const portNum = parseInt(portStr?.trim() || '', 10)
    if (!portStr?.trim() || !/^\d+$/.test(portStr.trim()) || isNaN(portNum) || portNum < 0 || portNum > 65535) {
      showMessage('TCP通信地址错误：端口号必须是 0 ~ 65535 之间的数字')
      return
    }
  }

  const timoutNum = parseInt(String(timeout).trim(), 10)
  if (isNaN(timoutNum) || timoutNum <= 0) {
    showMessage('超时时间必须为正整数')
    return
  }


  const startNum = parseInt(String(startAddress).trim(), 10)
  const endNum = parseInt(String(endAddress).trim(), 10)
  if (isNaN(startNum) || startNum <= 0 || isNaN(endNum) || endNum <= 0) {
    showMessage('格口地址必须为正整数')
    return
  }
  if (startNum > endNum) {
    showMessage('起始地址不能大于结束地址')
    return
  }

  sendConfigViaWebSocket({
    communicationType,
    communicationAddress: addrTrimmed,
    startAddress: startNum,
    endAddress: endNum,
    timeout: timoutNum,
  })
}

// 编辑配置模态框
const editModalVisible = ref(false)
const editField = ref<keyof SystemConfig | ''>('')
const editLabel = ref('')
const editTempValue = ref<string | number>('')
const passwordVisible = ref(false)
// 是否正在激活人脸
const isActivatingFace = ref(false)

function togglePasswordVisibility() {
  passwordVisible.value = !passwordVisible.value
}

function openEditModal(field: keyof SystemConfig, label: string) {
  handleUserOperation()
  editField.value = field
  editLabel.value = label
  passwordVisible.value = false

  if (field === 'adminPwd') {
    editTempValue.value = ''
  } else if (field === 'borrowPeriod') {
    let period = systemConfigStore.config.borrowPeriod
    if (period && !period.includes('天')) {
      period = `${period}天`
    }
    editTempValue.value = period || '1天'
  } else if (field === 'autoReturnTimeoutMinutes') {
    editTempValue.value = systemConfigStore.config.autoReturnTimeoutMinutes
  } else if (field === 'tempHumidityLogInterval') {
    editTempValue.value = systemConfigStore.config.tempHumidityLogInterval
  } else if (field === 'enableFaceCapture') {
    editTempValue.value = systemConfigStore.config.enableFaceCapture
  } else {
    editTempValue.value = String(systemConfigStore.config[field])
  }
  editModalVisible.value = true
}

const confirmEdit = async () => {
  if (!editField.value) return
  const field = editField.value

  try {
    if (field === 'systemName') {
      let newVal = (editTempValue.value as string).trim()
      if (newVal === '') {
        showMessage('系统名称不能为空')
        return
      }
      if (newVal.length > 10) {
        showMessage('系统名称长度不能超过10个字符')
        return
      }
      await systemConfigStore.updateConfigField('systemName', newVal)
    } else if (field === 'systemCode') {
      let newVal = (editTempValue.value as string).trim()
      if (newVal === '') {
        showMessage('系统编号不能为空')
        return
      }
      if (newVal.length > 10) {
        showMessage('系统编号长度不能超过10个字符')
        return
      }
      await systemConfigStore.updateConfigField('systemCode', newVal)
    } else if (field === 'adminPwd') {
      const newPwd = (editTempValue.value as string).trim()
      if (newPwd === '') {
        showMessage('密码不能为空')
        return
      }
      if (/[\u4e00-\u9fa5]/.test(newPwd)) {
        showMessage('密码不能包含中文字符')
        return
      }
      await systemConfigStore.updateConfigField('adminPwd', newPwd)
    } else if (field === 'borrowPeriod') {
      const newPeriod = editTempValue.value as string
      if (!borrowPeriodOptions.includes(newPeriod)) {
        showMessage('请选择有效的使用周期')
        return
      }
      const periodValue = newPeriod.replace('天', '')
      await systemConfigStore.updateConfigField('borrowPeriod', periodValue)
    } else if (field === 'autoReturnTimeoutMinutes') {
      const newTimeout = editTempValue.value as number
      if (!timeoutOptions.some(opt => opt.value === newTimeout)) {
        showMessage('请选择有效的超时时间')
        return
      }
      await systemConfigStore.updateConfigField('autoReturnTimeoutMinutes', newTimeout)
      countdown.restart?.()
    } else if (field === 'tempHumidityLogInterval') {
      const newInterval = Number(editTempValue.value)
      if (!humidityIntervalOptions.some(opt => opt.value === newInterval)) {
        showMessage('请选择有效的温湿度记录间隔')
        return
      }
      await systemConfigStore.updateConfigField('tempHumidityLogInterval', newInterval)
      fetchTempHumidityLogsData()
    } else if (field === 'enableFaceCapture') {
      const newVal = editTempValue.value as number
      if (newVal !== 0 && newVal !== 1) {
        showMessage('请选择有效的选项')
        return
      }
      await systemConfigStore.updateConfigField('enableFaceCapture', newVal)
    } else if (field === 'engName') {
      const newVal = (editTempValue.value as string).trim()
      if (newVal === '') {
        showMessage('英文名称不能为空')
        return
      }
      await systemConfigStore.updateConfigField('engName', newVal)
    } else if (field === 'location') {
      const newVal = (editTempValue.value as string).trim()
      if (newVal === '') {
        showMessage('所属位置不能为空')
        return
      }
      await systemConfigStore.updateConfigField('location', newVal)
    } else if (field === 'baiduFaceLicenseKey') {
      const newVal = (editTempValue.value as string).trim()
      if (newVal === '') {
        showMessage('人脸授权码不能为空')
        return
      }
      await systemConfigStore.updateConfigField('baiduFaceLicenseKey', newVal)
      isActivatingFace.value = true;
      try {
        await new Promise<void>((resolve, reject) => {
          const ws = new WebSocket('ws://localhost:8080/ws/face')

          // 设置一个 60 秒超时，防止后端没响应导致界面死锁
          const timeoutTimer = setTimeout(() => {
            ws.close()
            reject(new Error('授权超时，请检查服务是否正常'))
          }, 60000)

          ws.onopen = () => {
            // 构造发送的消息体
            const msg = {
              action: 'activate',
              requestId: String(Date.now()),
              data: newVal
            }
            ws.send(JSON.stringify(msg))
          }

          ws.onmessage = (event) => {
            try {
              const res = JSON.parse(event.data)
              // 识别是否是当前激活动作的返回结果
              if (res.action === 'activate') {
                clearTimeout(timeoutTimer)
                ws.close() // 收到结果后关闭连接

                if (res.code === 200) {
                  resolve() // 授权成功
                } else {
                  // 授权失败，抛出后端返回的错误原因
                  reject(new Error(res.message || '人脸授权失败'))
                }
              }
            } catch (e) {
              console.error('解析 WebSocket 消息失败:', e)
            }
          }

          ws.onerror = (err) => {
            clearTimeout(timeoutTimer)
            reject(new Error('WebSocket 连接失败，无法连接到授权服务器'))
          }
        })

        showMessage('百度人脸授权成功')
        closeEditModal() // 只有成功才关闭模态框
        return
      } catch (error: any) {
        console.error('人脸授权激活失败:', error)
        showMessage(error?.message || '授权失败，请稍后重试')
        return // 失败了阻断流程，保持模态框开启，让用户检查或重新输入
      } finally {
        await systemConfigStore.updateConfigField('baiduFaceLicenseKey', newVal)
        isActivatingFace.value = false
      }
    }else if(field == 'silentLivenessEnabled'){
      const newVal = editTempValue.value as number
      if (newVal !== 0 && newVal !== 1) {
        showMessage('请选择有效的选项')
        return
      }
      
      await systemConfigStore.updateConfigField('silentLivenessEnabled', newVal)
    }

    showMessage(`${editLabel.value} 已更新`)
    closeEditModal()
  } catch (error: any) {
    console.error('更新配置失败:', error)
    showMessage(error?.message || '更新失败，请稍后重试')
  }
}

function closeEditModal() {
  editModalVisible.value = false
  editField.value = ''
  editLabel.value = ''
  editTempValue.value = ''
  passwordVisible.value = false
}

// 恢复出厂设置
const resetModalVisible = ref(false)
const resetInputValue = ref('')
const resetErrorMsg = ref('')
const isResetting = ref(false)

function openResetModal() {
  handleUserOperation()
  resetInputValue.value = ''
  resetErrorMsg.value = ''
  resetModalVisible.value = true
}

function closeResetModal() {
  resetModalVisible.value = false
  resetInputValue.value = ''
  resetErrorMsg.value = ''
}

async function confirmReset() {
  if (isResetting.value) return

  const inputText = resetInputValue.value.trim()
  if (inputText !== '确定') {
    resetErrorMsg.value = '输入错误，请输入"确定"以确认重置'
    resetInputValue.value = ''
    return
  }

  resetErrorMsg.value = ''
  isResetting.value = true

  try {
    await systemConfigStore.resetConfig()
    showMessage('已成功恢复出厂设置，页面即将刷新...')
    setTimeout(() => {
      window.location.reload()
    }, 1000)
  } catch (error: any) {
    console.error('恢复出厂设置失败:', error)
    resetErrorMsg.value = error?.message || '恢复出厂设置失败，请检查网络或联系管理员'
    isResetting.value = false
  }
}

// 图片预览
const previewVisible = ref(false)
const previewUrl = ref('')

function previewImage(url: string) {
  if (url) {
    previewUrl.value = url
    previewVisible.value = true
  }
}

// 页面跳转
function viewDetail(module: string) {
  handleUserOperation()
  if (module === 'log') {
    router.push('/log-detail')
  } else if (module === 'hardware') {
    router.push('/hardware-detail')
  } else if (module === 'tempHumidity') {
    router.push('/temp-humidity-detail')
  } else {
    showMessage('详情页开发中')
  }
}

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

function goBack() {
  router.push('/')
}

onMounted(() => {
  fetchLogData()
  fetchHardwareData()
  fetchTempHumidityLogsData()
  if (!systemConfigStore.loaded && !systemConfigStore.loading) {
    systemConfigStore.loadConfig()
  }
})
onUnmounted(() => {
  cancelPress()
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

.card-icon {
  width: 1.8em;
  height: 1.8em;
}

/* 原有样式完全保留，仅微调与图标相关的部分 */
.settings-container {
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
.settings-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1000px;
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

.settings-title {
  color: #c2f0e0;
  font-size: 24px;
  text-shadow: 0 0 8px rgba(34, 211, 238, 0.25);
  display: flex;
  align-items: center;
  gap: 10px;
}

.placeholder {
  width: 80px;
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
.settings-content {
  max-width: 1000px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ---------- 信息卡片 – 无模糊、无过渡 ---------- */
.info-card {
  background: rgba(15, 25, 35, 0.7);
  border-radius: 28px;
  border: 1px solid rgba(34, 211, 238, 0.2);
  padding: 24px;
  cursor: pointer;
}

.info-card:hover {
  border-color: rgba(34, 211, 238, 0.4);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(34, 211, 238, 0.2);
  cursor: default;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-header h3 {
  color: #e2e8f0;
  font-size: 20px;
  margin: 0;
}

.detail-btn {
  background: rgba(34, 211, 238, 0.1);
  border: 1px solid rgba(34, 211, 238, 0.4);
  border-radius: 40px;
  padding: 6px 16px;
  color: #22d3ee;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.detail-btn:hover {
  background: rgba(34, 211, 238, 0.2);
  border-color: #22d3ee;
}

.reset-btn {
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.5);
  border-radius: 40px;
  padding: 6px 16px;
  color: #f87171;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.reset-btn:hover {
  background: rgba(239, 68, 68, 0.3);
  border-color: #ef4444;
  color: #fecaca;
}

/* ---------- 统计行 ---------- */
.stats-row {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
  justify-content: space-around;
}

.stats-row.three-cols {
  justify-content: space-between;
}

.stat-item {
  text-align: center;
  flex: 1;
}

.stat-value {
  font-size: 36px;
  font-weight: 700;
  color: #22d3ee;
  line-height: 1.2;
  text-shadow: 0 0 6px rgba(34, 211, 238, 0.25);
}

.stat-label {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 6px;
}

.section-title {
  color: #cbd5e1;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
  padding-left: 4px;
  border-left: 3px solid #22d3ee;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ---------- 滚动容器 – 无模糊 ---------- */
.unreturned-scroll-wrapper,
.temp-table-wrapper {
  max-height: 360px;
  overflow-y: auto;
  border-radius: 16px;
  background: rgba(0, 0, 0, 0.2);
  padding: 4px;
  scrollbar-width: thin;
  scrollbar-color: #22d3ee rgba(0, 0, 0, 0.4);
}

.unreturned-scroll-wrapper::-webkit-scrollbar,
.temp-table-wrapper::-webkit-scrollbar {
  width: 6px;
}

.unreturned-scroll-wrapper::-webkit-scrollbar-track,
.temp-table-wrapper::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.4);
  border-radius: 4px;
}

.unreturned-scroll-wrapper::-webkit-scrollbar-thumb,
.temp-table-wrapper::-webkit-scrollbar-thumb {
  background: #22d3ee;
  border-radius: 4px;
}

/* ---------- 表格 – 无过渡、无位移 ---------- */
.unreturned-table {
  width: 100%;
  min-width: 700px;
}

.table-header,
.table-row {
  display: grid;
  grid-template-columns: 1fr 0.6fr 1.2fr 0.8fr 1.5fr;
  gap: 12px;
  padding: 12px 16px;
  align-items: center;
}

.table-header {
  background: rgba(34, 211, 238, 0.1);
  color: #22d3ee;
  font-size: 13px;
  font-weight: 600;
  border-radius: 12px;
  margin-bottom: 6px;
  position: sticky;
  top: 0;
}

.table-row {
  background: rgba(0, 0, 0, 0.3);
  margin-bottom: 6px;
  border-radius: 12px;
}

.table-row:hover {
  background: rgba(34, 211, 238, 0.1);
}

.cell {
  font-size: 13px;
  color: #cbd5e1;
  word-break: break-word;
}

.cabinet {
  font-weight: 500;
  color: #e2e8f0;
}

.cell-number {
  font-family: monospace;
  color: #22d3ee;
}

.tool {
  color: #94a3b8;
}

.borrow-photo {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid rgba(34, 211, 238, 0.3);
}

.borrow-photo:hover {
  border-color: #22d3ee;
}

.no-photo {
  color: #5b6e8c;
  font-size: 12px;
  font-style: italic;
}

.time {
  font-size: 12px;
  color: #7e8a98;
}

/* ---------- 温湿度表格专用 ---------- */
.temp-header,
.temp-row {
  display: grid;
  grid-template-columns: 1.5fr 0.8fr 0.8fr 1.5fr;
  gap: 12px;
  padding: 10px 16px;
  align-items: center;
}

.temp-header {
  background: rgba(34, 211, 238, 0.1);
  color: #22d3ee;
  font-size: 13px;
  font-weight: 600;
  border-radius: 12px;
  margin-bottom: 6px;
  position: sticky;
  top: 0;
}

.temp-row {
  background: rgba(0, 0, 0, 0.3);
  margin-bottom: 6px;
  border-radius: 12px;
}

.temp-row:hover {
  background: rgba(34, 211, 238, 0.1);
}

/* ---------- 信息行 ---------- */
.info-row {
  display: flex;
  gap: 20px;
  justify-content: center;
  margin-top: 8px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.info-key {
  color: #94a3b8;
  font-size: 13px;
}

.info-value {
  color: #cbd5e1;
  font-weight: 500;
}

/* ---------- 系统配置网格 ---------- */
.config-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
}

.config-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(0, 0, 0, 0.3);
  padding: 12px 18px;
  border-radius: 20px;
}

.config-item:hover {
  background: rgba(34, 211, 238, 0.08);
  border-left: 2px solid #22d3ee;
}

.config-label {
  color: #94a3b8;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.3px;
}

.config-value-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.config-value {
  color: #e2e8f0;
  font-weight: 500;
  font-size: 14px;
  background: rgba(0, 0, 0, 0.4);
  padding: 4px 10px;
  border-radius: 40px;
  min-width: 100px;
  text-align: center;
}

.password-mask {
  font-family: monospace;
  letter-spacing: 2px;
  font-size: 16px;
}

.edit-icon {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 20px;
  color: #6b8cae;
}

.edit-icon:hover {
  background: rgba(34, 211, 238, 0.2);
  color: #22d3ee;
}

.edit-icon .icon {
  width: 1em;
  height: 1em;
}

.config-footer-tip {
  margin-top: 18px;
  font-size: 12px;
  color: #5b7a9a;
  text-align: center;
  border-top: 1px dashed rgba(34, 211, 238, 0.2);
  padding-top: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
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
  z-index: 100000;
  white-space: nowrap;
}

/* ---------- 图片预览 ---------- */
.preview-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  cursor: pointer;
}

.preview-image {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 12px;
  box-shadow: 0 0 20px rgba(34, 211, 238, 0.4);
}

/* ---------- 模态框 – 无模糊 ---------- */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
}

.modal-container {
  background: rgba(20, 30, 40, 0.95);
  border-radius: 28px;
  border: 1px solid rgba(34, 211, 238, 0.4);
  width: 400px;
  max-width: 90%;
  box-shadow: 0 16px 30px rgba(0, 0, 0, 0.5);
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: rgba(0, 0, 0, 0.3);
  border-bottom: 1px solid rgba(34, 211, 238, 0.3);
  color: #22d3ee;
  font-weight: 600;
}

.modal-header .icon {
  margin-right: 8px;
}

.warning-header {
  color: #f87171;
  border-bottom-color: rgba(239, 68, 68, 0.5);
}

.modal-close {
  background: none;
  border: none;
  cursor: pointer;
  color: #94a3b8;
  padding: 0 4px;
}

.modal-close .icon {
  font-size: 20px;
  width: 1.4em;
  height: 1.4em;
}

.modal-close:hover .icon {
  color: #22d3ee;
}

.modal-body {
  padding: 24px 20px;
}

.modal-input {
  width: 100%;
  background: rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(34, 211, 238, 0.4);
  border-radius: 40px;
  padding: 10px 16px;
  color: #e2e8f0;
  font-size: 14px;
  outline: none;
}

.modal-input:focus {
  border-color: #22d3ee;
  box-shadow: 0 0 6px rgba(34, 211, 238, 0.25);
}

.modal-select {
  width: 100%;
  background: rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(34, 211, 238, 0.4);
  border-radius: 40px;
  padding: 10px 16px;
  color: #e2e8f0;
  font-size: 14px;
  outline: none;
  cursor: pointer;
}

.modal-select:focus {
  border-color: #22d3ee;
  box-shadow: 0 0 6px rgba(34, 211, 238, 0.25);
}

.password-input-wrapper {
  position: relative;
  width: 100%;
}

.password-input {
  padding-right: 48px;
}

.password-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 4px;
  color: #94a3b8;
}

.password-toggle:hover {
  color: #22d3ee;
}

.password-toggle .icon {
  width: 1.2em;
  height: 1.2em;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 12px 20px 20px;
}

.modal-btn {
  padding: 6px 20px;
  border-radius: 40px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: none;
}

.modal-btn.cancel {
  background: rgba(100, 116, 139, 0.3);
  color: #cbd5e1;
}

.modal-btn.cancel:hover {
  background: rgba(100, 116, 139, 0.5);
}

.modal-btn.confirm {
  background: rgba(34, 211, 238, 0.2);
  border: 1px solid #22d3ee;
  color: #22d3ee;
}

.modal-btn.confirm:hover {
  background: rgba(34, 211, 238, 0.4);
}

.modal-btn.confirm.danger {
  background: rgba(239, 68, 68, 0.2);
  border-color: #ef4444;
  color: #f87171;
}

.modal-btn.confirm.danger:hover {
  background: rgba(239, 68, 68, 0.4);
}

/* ---------- 重置确认 ---------- */
.reset-warning {
  margin-bottom: 20px;
  color: #cbd5e1;
  font-size: 14px;
  text-align: center;
}

.reset-warning p {
  margin: 8px 0;
}

.reset-warning .confirm-text {
  color: #f87171;
  font-size: 16px;
}

.reset-error {
  margin-top: 12px;
  color: #ef4444;
  font-size: 12px;
  text-align: center;
}

/* ---------- 加载 / 错误占位 – 保留 spin 动画（仅初始化） ---------- */
.loading-placeholder,
.error-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  gap: 16px;
  color: #94a3b8;
}

.error-placeholder {
  flex-direction: row;
  flex-wrap: wrap;
}

.loading-spinner {
  width: 32px;
  height: 32px;
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

.empty-tip {
  text-align: center;
  padding: 40px 20px;
  color: #5b6e8c;
  font-size: 14px;
}

/* ---------- 响应式 – 无动画 ---------- */
@media (max-width: 900px) {
  .outer-frame {
    padding: 15px 20px;
  }

  .table-header,
  .table-row {
    grid-template-columns: 1fr 0.5fr 1fr 0.7fr 1.2fr;
    gap: 8px;
    padding: 10px 12px;
  }

  .cell {
    font-size: 11px;
  }

  .borrow-photo {
    width: 32px;
    height: 32px;
  }

  .config-grid {
    grid-template-columns: 1fr;
  }

  .config-value {
    min-width: 80px;
    font-size: 12px;
  }

  .countdown-display {
    padding: 6px 12px;
    font-size: 12px;
  }

  .countdown-time {
    font-size: 14px;
  }

  .temp-header,
  .temp-row {
    grid-template-columns: 1.5fr 0.8fr 0.8fr;
    gap: 8px;
  }
}

@media (max-width: 700px) {
  .outer-frame {
    padding: 12px 16px;
  }

  .settings-title {
    font-size: 20px;
  }

  .placeholder {
    width: 60px;
  }

  .stats-row {
    gap: 12px;
  }

  .stat-value {
    font-size: 28px;
  }

  .toast-message {
    white-space: normal;
    text-align: center;
    max-width: 80vw;
  }

  .table-header,
  .table-row {
    grid-template-columns: 0.8fr 0.5fr 0.9fr 0.6fr 1fr;
    gap: 6px;
  }

  .cabinet,
  .tool {
    font-size: 10px;
  }

  .config-item {
    flex-wrap: wrap;
    gap: 8px;
  }

  .config-label {
    min-width: 100px;
  }

  .countdown-display {
    padding: 4px 10px;
  }

  .countdown-text {
    display: none;
  }

  .temp-header,
  .temp-row {
    grid-template-columns: 1.2fr 0.7fr 0.7fr;
    gap: 6px;
  }
}

@media (max-width: 480px) {
  .settings-header {
    flex-direction: column;
    gap: 12px;
  }

  .placeholder {
    display: none;
  }

  .back-btn {
    align-self: flex-start;
  }

  .countdown-display {
    align-self: flex-end;
  }
}

/* ---------- 追加：通讯配置专用高颜值按钮组 (深色科技风) ---------- */
.type-selector {
  display: flex;
  gap: 12px;
  width: 100%;
}

.type-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 40px;
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(34, 211, 238, 0.3);
  border-radius: 40px;
  /* 强行覆盖浏览器默认的黑字，使用跟你页面配套的浅色文字 */
  color: #cbd5e1;
  font-size: 14px;
  cursor: pointer;
  outline: none;
  transition: all 0.2s ease;
}

/* 悬浮时带有一点微弱的荧光蓝 */
.type-btn:hover {
  background: rgba(34, 211, 238, 0.1);
  border-color: rgba(34, 211, 238, 0.6);
  color: #22d3ee;
}

/* 激活选中状态：完美契合你主色调的亮蓝色 */
.type-btn.active {
  background: rgba(34, 211, 238, 0.25);
  border-color: #22d3ee;
  color: #22d3ee;
  font-weight: 600;
  box-shadow: 0 0 10px rgba(34, 211, 238, 0.2);
}

/* 保持内部图标颜色跟随文字 */
.type-btn .icon {
  width: 1.1em;
  height: 1.1em;
}

/* 规范你原有的 modal-form-group-row */
.modal-form-group-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 0;
}

.modal-form-group-row .modal-form-group {
  margin-bottom: 0;
}

/* 1. 按钮组容器 */
.type-selector {
  display: flex;
  gap: 16px;
  width: 100%;
}

/* 2. 通讯方式大按钮 */
.type-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(34, 211, 238, 0.3);
  border-radius: 12px;
  /* 稍微方正一点的圆角，大气一点 */
  color: #e2e8f0;
  cursor: pointer;
  outline: none;
  transition: all 0.2s ease;
}

.type-btn:hover {
  background: rgba(34, 211, 238, 0.12);
  border-color: rgba(34, 211, 238, 0.6);
  color: #22d3ee;
}

.type-btn.active {
  background: rgba(34, 211, 238, 0.25);
  border-color: #22d3ee;
  color: #22d3ee;
  font-weight: 600;
  box-shadow: 0 0 12px rgba(34, 211, 238, 0.3);
}

/* 3. 输入框大尺寸重写 */
.large-input.modal-input {
  height: 46px;
  font-size: 16px;
  border-radius: 12px;
  /* 统一改为 12px 大圆角，视觉更协调 */
  padding: 0 16px;
  box-sizing: border-box;
}

/* 4. 底部确定、取消大按钮 */
.large-btn.modal-btn {
  height: 44px;
  padding: 0 28px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* 5. 栅格并排对齐 */
.modal-form-group-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.hw-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid rgba(255, 255, 255, 0.1);
  border-top: 4px solid #22d3ee;
  border-radius: 50%;
  box-sizing: border-box;
  animation: hw-spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes hw-spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}
</style>

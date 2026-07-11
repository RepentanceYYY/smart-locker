<template>
  <div class="hardware-detail-container">
    <div class="detail-header">
      <button class="back-btn" @click="goBack">
        <img src="/bg-back.svg" alt="返回" class="icon" />
        <span>返回设置</span>
      </button>
      <h1 class="detail-title">
        <img src="/设置.svg" alt="硬件" class="icon title-icon" />
        硬件配置详情
      </h1>

      <!-- 倒计时显示 -->
      <div class="settings-wrapper" ref="settingsWrapper">
        <div class="countdown-display" v-if="countdown && countdown.secondsLeft.value > 0">
          <img src="/计时器.svg" alt="倒计时" class="icon" />
          <span class="countdown-time">{{ formatCountdownTime(countdown.secondsLeft.value) }}</span>
          <span class="countdown-text">后自动返回</span>
        </div>

        <button class="settings-btn" @click.stop="toggleSettingsDropdown">
          <img src="/设置.svg" alt="设置" class="icon settings-icon" />
          <img src="/下箭头.svg" alt="展开" class="icon settings-arrow" :class="{ open: settingsDropdownVisible }" />
        </button>
        <div v-if="settingsDropdownVisible" class="settings-dropdown" @click.stop>
          <button v-if="advancedSettingsEnabled" class="dropdown-item action-item" @click="handleAddCabinet">
            <img src="/加号.svg" alt="新增" class="icon item-icon" />
            <span class="item-text">新增柜子</span>
          </button>
          <button v-if="advancedSettingsEnabled" class="dropdown-item action-item" @click="handleAddCell">
            <img src="/加号.svg" alt="格口" class="icon item-icon" />
            <span class="item-text">新增格口</span>
          </button>
          <button class="dropdown-item action-item" @click.stop="handleExportWordClick" :disabled="exporting">
            <img src="/导出.svg" alt="Word" class="icon export-icon" />
            <span>{{ exporting ? '导出中...' : '导出 Word' }}</span>
          </button>
          <div v-if="advancedSettingsEnabled" class="dropdown-divider"></div>
          <label class="dropdown-item setting-item">
            <img src="/设置.svg" alt="高级" class="icon item-icon" />
            <span class="item-text">高级设置</span>
            <span class="toggle-switch" :class="{ active: advancedSettingsEnabled }" @click.stop="toggleAdvancedSetting">
              <span class="toggle-knob"></span>
            </span>
          </label>
        </div>
      </div>
    </div>

    <div class="detail-content">
      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>加载硬件配置中...</span>
      </div>

      <div v-else-if="error" class="error-state">
        <span>⚠️ {{ error }}</span>
        <button class="retry-btn" @click="fetchData">重试</button>
      </div>

      <template v-else>
        <!-- 总体统计卡片 -->
        <div class="summary-card">
          <div class="summary-stats">
            <div class="summary-item">
              <div class="summary-value">{{ totalStats.cabinetCount }}</div>
              <div class="summary-label">
                <img src="/盒子.svg" alt="柜子" class="icon" /> 柜子总数
              </div>
            </div>
            <div class="summary-item">
              <div class="summary-value">{{ totalStats.totalSlots }}</div>
              <div class="summary-label">
                <img src="/盒子.svg" alt="格口" class="icon" /> 总格口数
              </div>
            </div>
            <div class="summary-item">
              <div class="summary-value">{{ totalStats.usedSlots }}</div>
              <div class="summary-label">
                <img src="/盒子.svg" alt="已用" class="icon" /> 已用格口
              </div>
            </div>
            <div class="summary-item">
              <div class="summary-value">{{ totalStats.emptySlots }}</div>
              <div class="summary-label">
                <img src="/盒子.svg" alt="空闲" class="icon" /> 空闲格口
              </div>
            </div>
            <div class="summary-item">
              <div class="summary-value">{{ totalStats.usageRate }}%</div>
              <div class="summary-label">占用率</div>
            </div>
          </div>
        </div>

        <!-- 3D 柜子轮播区域 -->
        <div class="full-layout">
          <div class="upper-area">
            <div class="top-section">
              <div class="temp-card">
                <img src="/温度.svg" alt="温度" class="icon card-icon" />
                <div class="card-value">{{ currentCabinetTemp }}°</div>
                <div class="card-label">温度</div>
              </div>

              <div class="humidity-card">
                <img src="/湿度-01.svg" alt="湿度" class="icon card-icon" />
                <div class="card-value">{{ currentCabinetHumidity }}%</div>
                <div class="card-label">湿度</div>
              </div>

              <button class="nav-btn-left" :class="{ disabled: currentIndex === 0 }" @click="rotatePrev">
                <img src="/左箭头.svg" alt="上一个" class="icon arrow" />
                <span class="btn-text">上一个</span>
              </button>
              <button class="nav-btn-right" :class="{ disabled: currentIndex === totalCount - 1 }" @click="rotateNext">
                <span class="btn-text">下一个</span>
                <img src="/右箭头.svg" alt="下一个" class="icon arrow" />
              </button>

              <div class="carousel-cylinder">
                <div class="carousel-3d" :style="{ minHeight: carouselHeight + 'px' }">
                  <div
                      v-for="(cab, idx) in cabinets"
                      :key="cab.id"
                      class="cabinet-item"
                      :class="{ 'center-highlight': idx === currentIndex }"
                      :style="{ ...getCabinetStyle(idx), width: cab.width || '280px', height: cab.height || 'auto' }"
                  >
                    <div class="cabinet-header" @click="advancedSettingsEnabled && openCabinetEdit(cab)">
                      {{ cab.title }}
                    </div>
                    <div class="cabinet-body">
                      <div class="cabinet-grid" :style="getGridStyle(cab)">
                        <template v-for="(cell, cellIdx) in cab.flatCells" :key="cellIdx">
                          <div v-if="cell.type === 'cell'"
                               class="cell-container"
                               :style="[getCellPosition(cell), cell.cellStyle]"
                               :class="{ 'empty-door': cell.isEmpty }"
                               @click="showCellDetail(cell, cab.id, cell.number)">
                            <div class="cell-inner"></div>
                            <div class="cabinet-cell" :class="{ 'empty-door': cell.isEmpty }">
                              <span class="cell-number">{{ cell.number }}</span>
                              <span class="tool-name">{{ truncateText(cell.toolName, 8) }}</span>
                            </div>
                          </div>
                          <div v-else-if="cell.type === 'image'"
                               class="custom-image-cell"
                               :style="[getCellPosition(cell), cell.cellStyle]"
                               @click="advancedSettingsEnabled && showImageDetail(cell, cab.id)">
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
        </div>
      </template>
    </div>

    <!-- 统一弹窗 -->
    <div v-if="showDialog" class="dialog-overlay">
      <div class="dialog-content dialog-content--editable" @click.stop>
        <div class="dialog-glow"></div>

        <div class="dialog-header">
          <h3>
            <span v-if="dialogMode === 'edit'">
              <img src="/编辑.svg" alt="编辑" class="icon" /> 编辑格口信息
            </span>
            <span v-else-if="dialogMode === 'add'">
              <img src="/加号.svg" alt="新增" class="icon" /> 新增项目
            </span>
            <span v-else-if="dialogMode === 'editImage'">
              <img src="/图片.svg" alt="图片" class="icon" /> 编辑图片单元格
            </span>
            <span class="cabinet-name-badge">{{ currentCabinetName }}</span>
          </h3>
          <button class="dialog-close" @click="closeDialog">
            ✕
          </button>
        </div>

        <div class="dialog-body">
          <!-- 新增模式：所属柜子选择器 -->
          <div v-if="dialogMode === 'add'" class="form-row">
            <div class="form-field" :class="{ 'form-row--error': errors.cabinetId }">
              <label class="form-label">
                <img src="/柜子.svg" alt="柜子" class="icon label-icon" />
                所属柜子 <span class="required-star">*</span>
              </label>
              <select v-model.number="dialogForm.cabinetId" class="form-input" @change="onCabinetChange">
                <option v-for="cab in cabinets" :key="cab.id" :value="cab.id">{{ cab.title }}</option>
              </select>
              <div v-if="errors.cabinetId" class="form-error">{{ errors.cabinetId }}</div>
            </div>
          </div>

          <!-- 新增模式：类型选择 -->
          <div v-if="dialogMode === 'add'" class="form-row">
            <div class="form-field">
              <label class="form-label">
                <img src="/类型.svg" alt="类型" class="icon label-icon" />
                项目类型 <span class="required-star">*</span>
              </label>
              <div class="type-selector">
                <button type="button" class="type-btn" :class="{ active: addType === 'cell' }" @click="handleAddTypeChange('cell')">
                  <img src="/格口管理.svg" alt="格口" class="icon" /> 格口
                </button>
                <button type="button" class="type-btn" :class="{ active: addType === 'image' }" @click="handleAddTypeChange('image')">
                  <img src="/图片.svg" alt="图片" class="icon" /> 图片
                </button>
              </div>
            </div>
          </div>

          <!-- 格口（编辑/新增）的字段 -->
          <template v-if="(dialogMode === 'edit' || dialogMode === 'add') && (!(dialogMode === 'add') || addType === 'cell')">
            <div class="form-row form-row--two-columns">
              <div class="form-field" :class="{ 'form-row--error': errors.slotNumber }">
                <label class="form-label">
                  <img src="/标签.svg" alt="格口号" class="icon label-icon" />
                  格口号 <span class="required-star">*</span>
                </label>
                <input
                    type="number"
                    v-model.number="dialogForm.slotNumber"
                    class="form-input"
                    placeholder="正整数"
                    min="1"
                    step="1"
                    @input="clearError('slotNumber')"
                />
                <div v-if="errors.slotNumber" class="form-error">{{ errors.slotNumber }}</div>
              </div>
              <div class="form-field" :class="{ 'form-row--error': errors.toolName }">
                <label class="form-label">
                  <img src="/工具.svg" alt="工具" class="icon label-icon" />
                  工具名称 <span class="required-star">*</span>
                </label>
                <input
                    type="text"
                    v-model="dialogForm.toolName"
                    class="form-input"
                    placeholder="不超过10位"
                    maxlength="10"
                    @input="clearError('toolName')"
                />
                <div v-if="errors.toolName" class="form-error">{{ errors.toolName }}</div>
              </div>
            </div>

            <!-- 二维码区域 -->
            <div class="form-row" :class="{ 'form-row--error': errors.qrcodeContent }">
              <label class="form-label">
                <img src="/二维码.svg" alt="二维码" class="icon label-icon" />
                二维码 <span class="required-star">*</span>
              </label>
              <div class="qrcode-area">
                <div v-if="dialogForm.qrcodeContent" class="qrcode-preview">
                  <div class="qrcode-img-wrapper">
                    <vue-qr
                        v-if="dialogForm.qrcodeContent"
                        :text="dialogForm.qrcodeContent"
                        :size="120"
                        :margin="0"
                        colorDark="#000000"
                        colorLight="#ffffff"
                    />
                    <div class="qrcode-overlay-glow"></div>
                  </div>
                  <div class="qrcode-text">{{ dialogForm.qrcodeContent }}</div>
                  <button type="button" class="qrcode-copy-btn" @click="copyQrcode">
                    <img src="/复制.svg" alt="复制" class="icon" /> 复制
                  </button>
                </div>
                <div v-else class="qrcode-empty">
                  <img src="/空白页.svg" alt="空" class="icon empty-icon" />
                  <span class="empty-hint">请点击下方按钮生成二维码</span>
                  <button type="button" class="btn-generate-qr" @click="handleGenerateQrcode">
                    <img src="/保存-L.svg" alt="生成" class="icon" /> 生成二维码
                  </button>
                </div>
              </div>
              <div v-if="errors.qrcodeContent" class="form-error">{{ errors.qrcodeContent }}</div>
            </div>
          </template>

          <!-- 图片新增/编辑模式字段 -->
          <template v-if="(dialogMode === 'editImage') || (dialogMode === 'add' && addType === 'image')">
            <div class="form-row">
              <div class="image-upload-card" :class="{ 'form-row--error': errors.imageUrl }">
                <div class="upload-preview-area">
                  <div class="preview-image" v-if="dialogForm.imageUrl">
                    <img :src="formatImageUrl(dialogForm.imageUrl)" alt="预览图" />
                    <button type="button" class="remove-image-btn" @click="removeImage" title="移除图片">
                      <img src="/移除.svg" alt="移除" class="icon" />
                    </button>
                  </div>
                  <div v-else class="preview-placeholder">
                    <img src="/暂无图片.svg" alt="暂无图片" class="icon placeholder-icon" />
                    <span class="placeholder-text">暂无图片</span>
                  </div>
                </div>
                <div class="upload-actions">
                  <label class="upload-btn">
                    <img src="/选择.svg" alt="选择" class="icon" /> 选择图片
                    <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="onImageUpload" style="display: none;" />
                  </label>
                </div>
                <div v-if="errors.imageUrl" class="form-error">{{ errors.imageUrl }}</div>
              </div>
            </div>
            <div class="form-row">
              <div class="form-field" :class="{ 'form-row--error': errors.label }">
                <label class="form-label">
                  <img src="/标签.svg" alt="标签" class="icon label-icon" />
                  标签 <span class="required-star">*</span>
                </label>
                <input type="text" v-model="dialogForm.label" class="form-input" placeholder="不超过20位" maxlength="20" @input="clearError('label')" />
                <div v-if="errors.label" class="form-error">{{ errors.label }}</div>
              </div>
            </div>
          </template>

          <!-- 高级设置区域（布局相关） -->
          <div v-if="advancedSettingsEnabled" class="advanced-settings-section">
            <div class="section-divider">
              <span class="divider-text">
                <img src="/设置.svg" alt="高级" class="icon" /> 高级配置（布局）
              </span>
            </div>

            <div class="form-row form-row--three-columns">
              <div class="form-field" :class="{ 'form-row--error': errors.rowNum }">
                <label class="form-label">
                  <img src="/行号.svg" alt="行号" class="icon label-icon" />
                  行号 <span class="required-star">*</span>
                </label>
                <input
                    type="number"
                    v-model.number="dialogForm.rowNum"
                    class="form-input"
                    placeholder="正整数"
                    min="1"
                    step="1"
                    @input="clearError('rowNum')"
                />
                <div v-if="errors.rowNum" class="form-error">{{ errors.rowNum }}</div>
              </div>

              <div class="form-field" :class="{ 'form-row--error': errors.colWidthNumber }">
                <label class="form-label">
                  <img src="/列宽.svg" alt="列宽" class="icon label-icon" />
                  列宽 <span class="required-star">*</span>
                </label>
                <div class="input-with-unit">
                  <input
                      type="number"
                      v-model.number="dialogForm.colWidthNumber"
                      class="form-input"
                      placeholder="数字"
                      min="1"
                      step="1"
                      @input="clearError('colWidthNumber')"
                  />
                  <span class="unit-suffix">fr</span>
                </div>
                <div v-if="errors.colWidthNumber" class="form-error">{{ errors.colWidthNumber }}</div>
              </div>

              <div class="form-field" :class="{ 'form-row--error': errors.rowHeightNumber }">
                <label class="form-label">
                  <img src="/行高.svg" alt="行高" class="icon label-icon" />
                  行高 <span class="required-star">*</span>
                </label>
                <div class="input-with-unit">
                  <input
                      type="number"
                      v-model.number="dialogForm.rowHeightNumber"
                      class="form-input"
                      placeholder="数字"
                      min="1"
                      step="1"
                      @input="clearError('rowHeightNumber')"
                  />
                  <span class="unit-suffix">px</span>
                </div>
                <div v-if="errors.rowHeightNumber" class="form-error">{{ errors.rowHeightNumber }}</div>
              </div>
            </div>

            <div class="form-row form-row--three-columns">
              <div class="form-field" :class="{ 'form-row--error': errors.colSpan }">
                <label class="form-label">
                  <img src="/不跨列.svg" alt="跨列" class="icon label-icon" />
                  跨越列数 <span class="required-star">*</span>
                </label>
                <input
                    type="number"
                    v-model.number="dialogForm.colSpan"
                    class="form-input"
                    min="1"
                    step="1"
                    @input="clearError('colSpan')"
                />
                <div v-if="errors.colSpan" class="form-error">{{ errors.colSpan }}</div>
              </div>

              <div class="form-field" :class="{ 'form-row--error': errors.rowSpan }">
                <label class="form-label">
                  <img src="/不跨列.svg" alt="跨行" class="icon label-icon" />
                  跨越行数 <span class="required-star">*</span>
                </label>
                <input
                    type="number"
                    v-model.number="dialogForm.rowSpan"
                    class="form-input"
                    min="1"
                    step="1"
                    @input="clearError('rowSpan')"
                />
                <div v-if="errors.rowSpan" class="form-error">{{ errors.rowSpan }}</div>
              </div>

              <div v-if="dialogMode !== 'editImage' && !(dialogMode === 'add' && addType === 'image')" class="form-field" :class="{ 'form-row--error': errors.macAddressNumber }">
                <label class="form-label">
                  <img src="/硬件地址N_icon.svg" alt="硬件地址" class="icon label-icon" />
                  硬件地址 <span class="required-star">*</span>
                </label>
                <input
                    type="number"
                    v-model.number="dialogForm.macAddressNumber"
                    class="form-input"
                    placeholder="正整数，全局唯一"
                    min="1"
                    step="1"
                    @input="clearError('macAddressNumber')"
                />
                <div v-if="errors.macAddressNumber" class="form-error">{{ errors.macAddressNumber }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="dialog-footer dialog-footer--editable">
          <button
              v-if="advancedSettingsEnabled && (dialogMode === 'edit' || dialogMode === 'editImage')"
              class="dialog-delete-btn"
              @click="openDeleteConfirm"
          >
            <img src="/垃圾桶.svg" alt="删除" class="icon" /> 删除
          </button>
          <button class="dialog-cancel-btn" @click="closeDialog">取消</button>
          <button class="dialog-save-btn" @click="handleDialogSave">
            <img v-if="dialogMode === 'edit'" src="/保存-L.svg" alt="保存" class="icon" />
            <img v-else-if="dialogMode === 'add' && addType === 'cell'" src="/加号.svg" alt="新增" class="icon" />
            <img v-else-if="dialogMode === 'add' && addType === 'image'" src="/图片.svg" alt="图片" class="icon" />
            <img v-else-if="dialogMode === 'editImage'" src="/保存-L.svg" alt="保存" class="icon" />
            {{ dialogMode === 'edit' ? '保存修改' : dialogMode === 'add' ? (addType === 'cell' ? '新增格口' : '新增图片') : '保存图片修改' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 柜子编辑/新增弹窗 -->
    <div v-if="showCabinetDialog" class="dialog-overlay">
      <div class="dialog-content dialog-content--cabinet" @click.stop>
        <div class="dialog-glow"></div>
        <div class="dialog-header">
          <h3>
            <span>{{ editingCabinetId === null ? '➕ 新增柜子' : '🗄️ 编辑柜子信息' }}</span>
            <span v-if="editingCabinetId !== null" class="cabinet-name-badge">{{ cabinetEditForm.title || '新柜子' }}</span>
          </h3>
          <button class="dialog-close" @click="closeCabinetDialog">
            ✕
          </button>
        </div>
        <div class="dialog-body cabinet-edit-body">
          <div class="form-row form-row--two-columns">
            <div class="form-field" :class="{ 'form-row--error': cabinetErrors.title }">
              <label class="form-label">
                <img src="/名称.svg" alt="名称" class="icon label-icon" />
                柜子名称 <span class="required-star">*</span>
              </label>
              <input type="text" v-model="cabinetEditForm.title" class="form-input" placeholder="唯一名称，不能为空" maxlength="50" />
              <div v-if="cabinetErrors.title" class="form-error">{{ cabinetErrors.title }}</div>
            </div>
            <div class="form-field" :class="{ 'form-row--error': cabinetErrors.width }">
              <label class="form-label">
                <img src="/宽度.svg" alt="宽度" class="icon label-icon" />
                柜子宽度 <span class="required-star">*</span>
              </label>
              <input type="text" v-model="cabinetEditForm.width" class="form-input" placeholder="如: 280px, 320px, auto" />
              <div v-if="cabinetErrors.width" class="form-error">{{ cabinetErrors.width }}</div>
            </div>
          </div>

          <div class="form-row form-row--two-columns">
            <div class="form-field" :class="{ 'form-row--error': cabinetErrors.height }">
              <label class="form-label">
                <img src="/高度.svg" alt="高度" class="icon label-icon" />
                柜子高度 <span class="required-star">*</span>
              </label>
              <input type="text" v-model="cabinetEditForm.height" class="form-input" placeholder="如: auto, 500px" />
              <div v-if="cabinetErrors.height" class="form-error">{{ cabinetErrors.height }}</div>
            </div>
            <div class="form-field">
              <label class="form-label">
                <img src="/星星.svg" alt="默认" class="icon label-icon" />
                默认展示柜子
              </label>
              <div class="toggle-switch-cabinet" :class="{ active: cabinetEditForm.isDefault }" @click="cabinetEditForm.isDefault = !cabinetEditForm.isDefault">
                <span class="toggle-knob"></span>
              </div>
              <div class="field-hint">开启后，该柜子将作为首页默认展示</div>
            </div>
          </div>

          <div class="comm-config-row">
            <div class="comm-card">
              <div class="comm-card-header">
                <img src="/图标-除湿机.svg" alt="除湿机" class="icon comm-icon" />
                <span class="comm-title">除湿机通讯配置</span>
              </div>
              <div class="comm-card-body">
                <div class="form-field" :class="{ 'form-row--error': cabinetErrors.dehumidifierCommType }">
                  <label class="form-label">
                    <img src="/通讯方式.svg" alt="通讯方式" class="icon label-icon" />
                    通讯方式 <span class="required-star">*</span>
                  </label>
                  <div class="type-selector">
                    <button type="button" class="type-btn" :class="{ active: cabinetEditForm.dehumidifierCommType === '485' }" @click="cabinetEditForm.dehumidifierCommType = '485'">
                      <img src="/rs485.svg" alt="RS485" class="icon" /> RS485
                    </button>
                    <button type="button" class="type-btn" :class="{ active: cabinetEditForm.dehumidifierCommType === 'TCP' }" @click="cabinetEditForm.dehumidifierCommType = 'TCP'">
                      <img src="/TCP-.svg" alt="TCP" class="icon" /> TCP/IP
                    </button>
                  </div>
                  <div v-if="cabinetErrors.dehumidifierCommType" class="form-error">{{ cabinetErrors.dehumidifierCommType }}</div>
                </div>

                <div class="form-field" :class="{ 'form-row--error': cabinetErrors.dehumidifierCommPort }">
                  <label class="form-label">
                    <img src="/端口.svg" alt="端口" class="icon label-icon" />
                    通讯端口 <span class="required-star">*</span>
                  </label>
                  <input type="text" v-model="cabinetEditForm.dehumidifierCommPort" class="form-input" :placeholder="cabinetEditForm.dehumidifierCommType === '485' ? '如: COM1@9600' : '如: 192.168.0.1:8252'" />
                  <div v-if="cabinetErrors.dehumidifierCommPort" class="form-error">{{ cabinetErrors.dehumidifierCommPort }}</div>
                </div>

                <div class="form-field" :class="{ 'form-row--error': cabinetErrors.dehumidifierAddr }">
                  <label class="form-label">
                    <img src="/地址.svg" alt="地址" class="icon label-icon" />
                    除湿机地址 <span class="required-star">*</span>
                  </label>
                  <input type="text" v-model="cabinetEditForm.dehumidifierAddr" class="form-input" placeholder="除湿机设备地址" />
                  <div v-if="cabinetErrors.dehumidifierAddr" class="form-error">{{ cabinetErrors.dehumidifierAddr }}</div>
                </div>
              </div>
            </div>

            <div class="comm-card">
              <div class="comm-card-header">
                <img src="/密码锁.svg" alt="锁" class="icon comm-icon" />
                <span class="comm-title">锁控制器通讯配置</span>
              </div>
              <div class="comm-card-body">
                <div class="form-field" :class="{ 'form-row--error': cabinetErrors.lockCommType }">
                  <label class="form-label">
                    <img src="/通讯方式.svg" alt="通讯方式" class="icon label-icon" />
                    通讯方式 <span class="required-star">*</span>
                  </label>
                  <div class="type-selector">
                    <button type="button" class="type-btn" :class="{ active: cabinetEditForm.lockCommType === '485' }" @click="cabinetEditForm.lockCommType = '485'">
                      <img src="/rs485.svg" alt="RS485" class="icon" /> RS485
                    </button>
                    <button type="button" class="type-btn" :class="{ active: cabinetEditForm.lockCommType === 'TCP' }" @click="cabinetEditForm.lockCommType = 'TCP'">
                      <img src="/TCP-.svg" alt="TCP" class="icon" /> TCP/IP
                    </button>
                  </div>
                  <div v-if="cabinetErrors.lockCommType" class="form-error">{{ cabinetErrors.lockCommType }}</div>
                </div>

                <div class="form-field" :class="{ 'form-row--error': cabinetErrors.lockCommPort }">
                  <label class="form-label">
                    <img src="/端口.svg" alt="端口" class="icon label-icon" />
                    通讯端口 <span class="required-star">*</span>
                  </label>
                  <input type="text" v-model="cabinetEditForm.lockCommPort" class="form-input" :placeholder="cabinetEditForm.lockCommType === '485' ? '如: COM1@9600' : '如: 192.168.0.1:8252'" />
                  <div v-if="cabinetErrors.lockCommPort" class="form-error">{{ cabinetErrors.lockCommPort }}</div>
                </div>

                <div class="form-field" :class="{ 'form-row--error': cabinetErrors.lockBoardAddr }">
                  <label class="form-label">
                    <img src="/地址.svg" alt="锁板地址" class="icon label-icon" />
                    锁板地址
                  </label>
                  <input type="text" v-model="cabinetEditForm.lockBoardAddr" class="form-input" placeholder="锁控制器设备地址" />
                  <div v-if="cabinetErrors.lockBoardAddr" class="form-error">{{ cabinetErrors.lockBoardAddr }}</div>
                </div>
              </div>
            </div>
          </div>

          <div class="advanced-settings-section">
            <div class="section-divider">
              <span class="divider-text">
                <img src="/84环境阈值.svg" alt="环境" class="icon" /> 环境阈值配置
              </span>
            </div>
          </div>

          <div class="form-row form-row--two-columns">
            <div class="form-field" :class="{ 'form-row--error': cabinetErrors.humidityMin }">
              <label class="form-label">
                <img src="/湿度-01.svg" alt="湿度下限" class="icon label-icon" />
                湿度下限 (%RH) <span class="required-star">*</span>
              </label>
              <input type="number" v-model.number="cabinetEditForm.humidityMin" class="form-input" placeholder="0-100" min="0" max="100" step="1" />
              <div v-if="cabinetErrors.humidityMin" class="form-error">{{ cabinetErrors.humidityMin }}</div>
            </div>
            <div class="form-field" :class="{ 'form-row--error': cabinetErrors.humidityMax }">
              <label class="form-label">
                <img src="/湿度-01.svg" alt="湿度上限" class="icon label-icon" />
                湿度上限 (%RH) <span class="required-star">*</span>
              </label>
              <input type="number" v-model.number="cabinetEditForm.humidityMax" class="form-input" placeholder="0-100" min="0" max="100" step="1" />
              <div v-if="cabinetErrors.humidityMax" class="form-error">{{ cabinetErrors.humidityMax }}</div>
            </div>
          </div>
          <!-- 温度配置注释掉，这里保留 -->
        </div>
        <div class="dialog-footer dialog-footer--editable">
          <button
              v-if="editingCabinetId !== null"
              class="dialog-delete-btn"
              @click="openCabinetDeleteConfirm"
          >
            <img src="/移除.svg" alt="删除" class="icon" /> 删除柜子
          </button>
          <button class="dialog-cancel-btn" @click="closeCabinetDialog">取消</button>
          <button class="dialog-save-btn" @click="saveCabinetChanges">
            <img src="/save-3-fill.svg" alt="保存" class="icon" /> {{ editingCabinetId === null ? '创建柜子' : '保存柜子信息' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 确认删除格口弹窗 -->
    <div v-if="showDeleteConfirm" class="confirm-overlay">
      <div class="confirm-dialog">
        <div class="confirm-header">
          <img src="/警告 (1).svg" alt="警告" class="icon" /> 确认删除
        </div>
        <div class="confirm-body">确定要删除此项吗？删除后不可恢复。</div>
        <div class="confirm-footer">
          <button class="confirm-cancel" @click="showDeleteConfirm = false">取消</button>
          <button class="confirm-ok" @click="confirmDelete">
            <img src="/移除.svg" alt="删除" class="icon" /> 确认删除
          </button>
        </div>
      </div>
    </div>

    <!-- Toast 消息 -->
    <div v-if="showToast" class="toast-message">{{ toastText }}</div>

    <!-- 确认删除柜子 -->
    <div v-if="showCabinetDeleteConfirm" class="confirm-overlay">
      <div class="confirm-dialog">
        <div class="confirm-header">
          <img src="/警告 (1).svg" alt="警告" class="icon" /> 确认删除柜子
        </div>
        <div class="confirm-body">确定要删除该柜子吗？删除后该柜子下的所有格口和图片配置都将被删除，且不可恢复。</div>
        <div class="confirm-footer">
          <button class="confirm-cancel" @click="cancelCabinetDelete">取消</button>
          <button class="confirm-ok" @click="confirmCabinetDelete">
            <img src="/移除.svg" alt="删除" class="icon" /> 确认删除
          </button>
        </div>
      </div>
    </div>

    <!-- Word导出确认弹窗 -->
    <div v-if="showExportConfirm" class="confirm-overlay">
      <div class="confirm-dialog export-confirm-dialog">
        <div class="confirm-header">
          <img src="/Word.svg" alt="Word" class="icon" /> 导出Word文档
        </div>
        <div class="confirm-body">
          <p>确认要导出所有格口数据为Word文档吗？</p>
          <div class="export-summary">
            <div class="export-summary-item">
              <img src="/盒子.svg" alt="柜子" class="icon summary-icon" />
              <span>柜子总数: {{ totalStats.cabinetCount }}</span>
            </div>
            <div class="export-summary-item">
              <img src="/盒子.svg" alt="格口" class="icon summary-icon" />
              <span>格口总数: {{ totalStats.totalSlots }}</span>
            </div>
            <div class="export-summary-item">
              <img src="/盒子.svg" alt="已用" class="icon summary-icon" />
              <span>已用格口: {{ totalStats.usedSlots }}</span>
            </div>
          </div>
          <p class="export-note">导出内容包括：柜子名称、格口号、工具名称、二维码内容、硬件地址等信息</p>
        </div>
        <div class="confirm-footer">
          <button class="confirm-cancel" @click="showExportConfirm = false">取消</button>
          <button class="confirm-ok export-ok-btn" @click="confirmExportWord">
            <img src="/导出.svg" alt="导出" class="icon" /> 确认导出
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { fetchCabinetList, updateCabinet, createCabinet, deleteCabinet } from '@/api/cabinet'
import { addCellConfig, updateCellConfig, uploadImage, deleteCellConfig, type UpdateCellDTO } from '@/api/cell'
import { useCountdown } from '@/composables/useCountdown'
import {formatImageUrl} from '@/utils/fileUtils'
import VueQr from 'vue-qr'
// 导入导出工具函数
import { collectCellData, exportToWord, exportToExcel } from '@/utils/exportUtils'
import type { CSSProperties } from 'vue'
import {useDehumidifierStore} from '@/stores/useDehumidifier'
const router = useRouter()

const dehumidifierStore = useDehumidifierStore()

// ==================== 倒计时功能 ====================
const countdown = useCountdown({
  onTimeout: () => {
    console.log('倒计时结束，返回设置页面')
    router.push('/')
  }
})

function formatCountdownTime(seconds: number): string {
  if (seconds >= 60) {
    const minutes = Math.floor(seconds / 60)
    const remainingSeconds = seconds % 60
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`
  }
  return `${seconds}秒`
}

function handleGlobalClick(event: MouseEvent) {
  if (countdown && countdown.handleOperation) {
    countdown.handleOperation()
  }
}

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
  number: number
  toolName: string
  isEmpty: boolean
  qrcodeContent?: string
  rowNum?: number
  macAddress?: string
}

interface ImageCell extends BaseCell {
  type: 'image'
  id: number
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
  dehumidifierCommType?: '485' | 'TCP'
  dehumidifierCommPort?: string
  dehumidifierAddr?: string
  lockCommType?: '485' | 'TCP'
  lockCommPort?: string
  lockBoardAddr?: string
  humidityMin?: number
  humidityMax?: number
  temperatureMin?: number
  temperatureMax?: number
}

interface ProcessedCabinet extends CabinetConfig {
  flatCells: any[]
  colWidths: string[]
  rowHeights: string[]
  gridStyle: any
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
      })
      colCursor = endCol
    }
    currentRowIdx += groupRowSpan
  }

  for (let i = 0; i < colWidths.length; i++) if (!colWidths[i]) colWidths[i] = '1fr'
  for (let i = 0; i < rowHeights.length; i++) if (!rowHeights[i]) rowHeights[i] = 'auto'

  return { flatCells, colWidths, rowHeights }
}

const showCabinetDeleteConfirm = ref(false)
const deletingCabinetId = ref<number | null>(null)

function openCabinetDeleteConfirm() {
  if (editingCabinetId.value === null) return
  deletingCabinetId.value = editingCabinetId.value
  showCabinetDeleteConfirm.value = true
}

async function confirmCabinetDelete() {
  const id = deletingCabinetId.value
  if (!id) return

  try {
    showMessage('正在删除柜子...')
    await deleteCabinet(id)
    showMessage('柜子删除成功')
    showCabinetDeleteConfirm.value = false
    closeCabinetDialog()

    await fetchData()
    if (cabinets.value.length === 0) {
      currentIndex.value = 0
    } else if (currentIndex.value >= cabinets.value.length) {
      currentIndex.value = 0
    }
  } catch (err: any) {
    const msg = err.response?.data?.message || err.message || '删除失败，请重试'
    showMessage(msg)
    showCabinetDeleteConfirm.value = false
  }
}

function cancelCabinetDelete() {
  showCabinetDeleteConfirm.value = false
  deletingCabinetId.value = null
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

function processCabinetData(rawData: any[]): ProcessedCabinet[] {
  return rawData.map((cab: any) => {
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

function generateHex16(length: number = 15): string {
  const chars = '0123456789abcdef'
  let result = ''
  for (let i = 0; i < length; i++) result += chars[Math.floor(Math.random() * 16)]
  return result
}

function isQrcodeUnique(qrcode: string, excludeCell?: { cabinetId: number, cellNumber: string }): boolean {
  for (const cabinet of cabinets.value) {
    for (const row of cabinet.rows) {
      for (const cell of row.cells) {
        if (cell.type === 'cell' && cell.qrcodeContent === qrcode) {
          if (excludeCell && cabinet.id === excludeCell.cabinetId && cell.number.toString() === excludeCell.cellNumber) continue
          return false
        }
      }
    }
  }
  return true
}

function generateUniqueQrcodeContent(excludeCell?: { cabinetId: number, cellNumber: string }): string {
  let newQrcode = generateHex16(15)
  let attempts = 0
  while (!isQrcodeUnique(newQrcode, excludeCell) && attempts < 20) {
    newQrcode = generateHex16(15)
    attempts++
  }
  return newQrcode
}

function recalcTotalStats() {
  let cabinetCount = cabinets.value.length
  let totalSlots = 0
  let usedSlots = 0
  for (const cabinet of cabinets.value) {
    for (const row of cabinet.rows) {
      for (const cell of row.cells) {
        if (cell.type === 'cell') {
          totalSlots++
          if (!cell.isEmpty && cell.toolName && cell.toolName.trim() !== '') usedSlots++
        }
      }
    }
  }
  const emptySlots = totalSlots - usedSlots
  const usageRate = totalSlots > 0 ? Number(((usedSlots / totalSlots) * 100).toFixed(1)) : 0
  totalStats.value = { cabinetCount, totalSlots, usedSlots, emptySlots, usageRate }
}

function parseColumnNumber(columns: string): number {
  if (!columns) return 1
  const match = columns.match(/(\d+)/)
  return match ? parseInt(match[1], 10) : 1
}

function parseRowHeightNumber(height: string): number {
  if (!height) return 60
  const match = height.match(/(\d+)/)
  return match ? parseInt(match[1], 10) : 60
}

function getCabinetTotalColumns(cabinetId: number): number {
  const cabinet = cabinets.value.find(c => c.id === cabinetId)
  return cabinet?.colWidths?.length || 1
}

function getNextRowNum(cabinetId: number): number {
  const cabinet = cabinets.value.find(c => c.id === cabinetId)
  return cabinet ? cabinet.rows.length + 1 : 1
}

// ================== 响应式数据 ==================
const loading = ref(true)
const error = ref('')
const cabinets = ref<ProcessedCabinet[]>([])
const currentIndex = ref(0)
const totalCount = computed(() => cabinets.value.length)

const totalStats = ref({
  cabinetCount: 0,
  totalSlots: 0,
  usedSlots: 0,
  emptySlots: 0,
  usageRate: 0
})

const radius = ref(320)
const carouselHeight = ref(600)
const maxScale = ref(1.4)

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

const showDialog = ref(false)
const dialogMode = ref<'edit' | 'add' | 'editImage'>('edit')
const addType = ref<'cell' | 'image'>('cell')

const dialogForm = ref({
  id:0,
  cabinetId: 0,
  rowNum: 1,
  colWidthNumber: 1,
  rowHeightNumber: 60,
  colSpan: 1,
  rowSpan: 1,
  slotNumber: 1,
  toolName: '',
  qrcodeContent: '',
  macAddressNumber: 0,
  originalNumber: '',
  imageUrl: '',
  label: ''
})

const errors = ref({
  cabinetId: '',
  slotNumber: '',
  toolName: '',
  qrcodeContent: '',
  rowNum: '',
  colWidthNumber: '',
  rowHeightNumber: '',
  colSpan: '',
  rowSpan: '',
  macAddressNumber: '',
  imageUrl: '',
  label: ''
})

const showToast = ref(false)
const toastText = ref('')
let toastTimer: ReturnType<typeof setTimeout> | null = null

const settingsDropdownVisible = ref(false)
const advancedSettingsEnabled = ref(false)
const settingsWrapper = ref<HTMLElement | null>(null)

const pendingImageFile = ref<File | null>(null)
const pendingImagePreviewUrl = ref<string>('')

const showCabinetDialog = ref(false)
const editingCabinetId = ref<number | null>(null)
const cabinetEditForm = ref({
  title: '',
  width: '',
  height: '',
  isDefault: false,
  dehumidifierCommType: '485' as '485' | 'TCP',
  dehumidifierCommPort: '',
  dehumidifierAddr: '',
  lockCommType: '485' as '485' | 'TCP',
  lockCommPort: '',
  lockBoardAddr: '',
  humidityMin: 40,
  humidityMax: 60,
  temperatureMin: 15,
  temperatureMax: 25
})
const cabinetErrors = ref({
  title: '', width: '', height: '',
  dehumidifierCommType: '', dehumidifierCommPort: '', dehumidifierAddr: '',
  lockCommType: '', lockCommPort: '', lockBoardAddr: '',
  humidityMin: '', humidityMax: '', temperatureMin: '', temperatureMax: ''
})

// 导出相关状态
const exporting = ref(false)
const showExportConfirm = ref(false)  // 新增：导出确认弹窗显示状态

// ================== 导出功能 ==================
function handleExportWordClick() {
  if (exporting.value) return
  showExportConfirm.value = true
  settingsDropdownVisible.value = false  // 关闭下拉菜单
}
// 确认导出Word
async function confirmExportWord() {
  showExportConfirm.value = false

  if (exporting.value) return

  exporting.value = true
  try {
    const cellData = await collectCellData(cabinets.value)
    if (cellData.length === 0) {
      showMessage('没有可导出的格口数据')
      return
    }
    await exportToWord(cellData, (msg) => showMessage(msg))
    showMessage('Word导出成功！')
  } catch (error: any) {
    console.error('导出失败:', error)
    showMessage(error.message || '导出失败，请重试')
  } finally {
    exporting.value = false
  }
}

/*async function handleExportExcel() {
  if (exporting.value) return

  exporting.value = true
  try {
    const cellData = await collectCellData(cabinets.value)
    if (cellData.length === 0) {
      showMessage('没有可导出的格口数据')
      return
    }
    await exportToExcel(cellData, (msg) => showMessage(msg))
    showMessage('Excel导出成功！')
  } catch (error: any) {
    console.error('导出失败:', error)
    showMessage(error.message || '导出失败，请重试')
  } finally {
    exporting.value = false
  }
}*/

// ================== 方法 ==================
function showMessage(text: string) {
  if (toastTimer) clearTimeout(toastTimer)
  toastText.value = text
  showToast.value = true
  toastTimer = setTimeout(() => { showToast.value = false }, 2000)
}

const getCabinetStyle = (idx: number):CSSProperties => {
  if (totalCount.value === 0) return { display: 'none' }
  const diff = Math.abs(idx - currentIndex.value)
  if (diff > 1) return { transform: 'translateX(0) translateZ(-500px)', opacity: 0, visibility: 'hidden', pointerEvents: 'none', zIndex: -1, transition: 'opacity 0.3s, visibility 0.3s' }
  const angleStep = (Math.PI * 2) / totalCount.value
  let angle = idx * angleStep
  const centerAngle = currentIndex.value * angleStep
  let relativeAngle = angle - centerAngle
  if (relativeAngle > Math.PI) relativeAngle -= Math.PI * 2
  if (relativeAngle < -Math.PI) relativeAngle += Math.PI * 2
  const x = Math.sin(relativeAngle) * radius.value
  const z = Math.cos(relativeAngle) * radius.value - 80
  let rotateY = -relativeAngle * (180 / Math.PI) * 0.35
  let scaleVal = Math.abs(relativeAngle) < 0.2 ? maxScale.value : 0.75
  let zIndexVal = Math.abs(relativeAngle) < 0.2 ? 300 : 50
  const transform = `translateX(${x}px) translateY(0px) translateZ(${z}px) rotateY(${rotateY}deg) scale(${scaleVal})`
  return { transform, zIndex: zIndexVal, opacity: 1, visibility: 'visible', pointerEvents: 'auto' }
}

function rotatePrev() { if (currentIndex.value > 0) currentIndex.value-- }
function rotateNext() { if (currentIndex.value < totalCount.value - 1) currentIndex.value++ }
function getGridStyle(cab: ProcessedCabinet) { return cab.gridStyle }

function updateLayout() {
  if (typeof window === 'undefined') return
  const width = window.innerWidth
  const height = window.innerHeight
  radius.value = Math.min(width * 0.45, 380)
  maxScale.value = Math.min(2, Math.max(1.2, width / 220))
  const upperHeight = height * 0.85
  carouselHeight.value = Math.max(450, upperHeight - 55)
}

function clearError(field: keyof typeof errors.value) {
  errors.value[field] = ''
}

function clearAllErrors() {
  errors.value = {
    cabinetId: '', slotNumber: '', toolName: '', qrcodeContent: '',
    rowNum: '', colWidthNumber: '', rowHeightNumber: '',
    colSpan: '', rowSpan: '', macAddressNumber: '',
    imageUrl: '', label: ''
  }
}

function showCellDetail(cell: any, cabinetId: number, cellNumber: string) {
  if (cell.type !== 'cell') return
  clearAllErrors()
  dialogForm.value = {
    id:cell.id,
    cabinetId: cabinetId,
    slotNumber: cell.number,
    toolName: cell.toolName || '',
    qrcodeContent: cell.qrcodeContent || '',
    rowNum: cell.rowNum || 1,
    colWidthNumber: parseColumnNumber(cell.columns || '1fr'),
    rowHeightNumber: parseRowHeightNumber(cell.height || '60px'),
    colSpan: cell.colSpan || 1,
    rowSpan: cell.rowSpan || 1,
    macAddressNumber: cell.macAddress ? parseInt(cell.macAddress, 10) : 0,
    originalNumber: cell.number.toString(),
    imageUrl: '',
    label: ''
  }
  dialogMode.value = 'edit'
  showDialog.value = true
}

function showImageDetail(cell: ImageCell, cabinetId: number) {
  if (!advancedSettingsEnabled.value) return
  clearAllErrors()

  if (pendingImagePreviewUrl.value) {
    URL.revokeObjectURL(pendingImagePreviewUrl.value)
    pendingImagePreviewUrl.value = ''
  }
  pendingImageFile.value = null

  const cabinet = cabinets.value.find(c => c.id === cabinetId)
  let rowNum = 1
  if (cabinet) {
    for (let i = 0; i < cabinet.rows.length; i++) {
      const row = cabinet.rows[i]
      const found = row.cells.some(c => c.type === 'image' && c.id === cell.id)
      if (found) {
        rowNum = i + 1
        break
      }
    }
  }

  dialogForm.value = {
    id: cell.id,
    cabinetId: cabinetId,
    rowNum: rowNum,
    colWidthNumber: parseColumnNumber(cell.columns || '1fr'),
    rowHeightNumber: parseRowHeightNumber(cell.height || '60px'),
    colSpan: cell.colSpan || 1,
    rowSpan: cell.rowSpan || 1,
    imageUrl: cell.imageUrl || '',
    label: cell.label || '',
    slotNumber: 1,
    toolName: '',
    qrcodeContent: '',
    macAddressNumber: 0,
    originalNumber: ''
  }
  dialogMode.value = 'editImage'
  showDialog.value = true
  settingsDropdownVisible.value = false
}

function handleAddCell() {
  const defaultCabId = cabinets.value[currentIndex.value]?.id || cabinets.value[0]?.id || 0
  clearAllErrors()
  addType.value = 'cell'
  resetImageTempData()
  dialogForm.value = {
    id: 0,
    cabinetId: defaultCabId,
    slotNumber: 1,
    toolName: '',
    qrcodeContent: '',
    rowNum: getNextRowNum(defaultCabId),
    colWidthNumber: 1,
    rowHeightNumber: 60,
    colSpan: getCabinetTotalColumns(defaultCabId),
    rowSpan: 1,
    macAddressNumber: 0,
    originalNumber: '',
    imageUrl: '',
    label: ''
  }
  dialogMode.value = 'add'
  showDialog.value = true
  settingsDropdownVisible.value = false
}

function handleAddTypeChange(type: 'cell' | 'image') {
  if (addType.value === type) return
  addType.value = type
  clearAllErrors()
  resetImageTempData()
  if (type === 'cell') {
    dialogForm.value.imageUrl = ''
    dialogForm.value.label = ''
    if (!dialogForm.value.slotNumber || dialogForm.value.slotNumber <= 0) {
      dialogForm.value.slotNumber = 1
    }
    if (!dialogForm.value.qrcodeContent) {
      dialogForm.value.qrcodeContent = ''
    }
    if (!dialogForm.value.macAddressNumber || dialogForm.value.macAddressNumber <= 0) {
      dialogForm.value.macAddressNumber = 0
    }
  } else {
    dialogForm.value.slotNumber = 0
    dialogForm.value.toolName = ''
    dialogForm.value.qrcodeContent = ''
    dialogForm.value.macAddressNumber = 0
  }
}

function resetImageTempData() {
  if (pendingImagePreviewUrl.value) {
    URL.revokeObjectURL(pendingImagePreviewUrl.value)
    pendingImagePreviewUrl.value = ''
  }
  pendingImageFile.value = null
}

function getNextAvailableSlotNumber(): number {
  let max = 0
  for (const cab of cabinets.value) {
    for (const row of cab.rows) {
      for (const cell of row.cells) {
        if (cell.type === 'cell' && cell.number > max) max = cell.number
      }
    }
  }
  return max + 1
}

function closeDialog() {
  showDialog.value = false
  clearAllErrors()
  resetImageTempData()
  deleteTargetId.value = null
}

const MAX_IMAGE_SIZE = 2 * 1024 * 1024

function onImageUpload(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    showMessage('请选择图片文件')
    return
  }

  if (file.size > MAX_IMAGE_SIZE) {
    showMessage('图片大小不能超过 2MB')
    return
  }

  if (pendingImagePreviewUrl.value) {
    URL.revokeObjectURL(pendingImagePreviewUrl.value)
    pendingImagePreviewUrl.value = ''
  }

  pendingImageFile.value = file
  const previewUrl = URL.createObjectURL(file)
  pendingImagePreviewUrl.value = previewUrl
  dialogForm.value.imageUrl = previewUrl
  target.value = ''
}

function removeImage() {
  if (pendingImagePreviewUrl.value) {
    URL.revokeObjectURL(pendingImagePreviewUrl.value)
    pendingImagePreviewUrl.value = ''
  }
  pendingImageFile.value = null
  dialogForm.value.imageUrl = ''
  errors.value.imageUrl = ''
  showMessage('图片已清除')
}

function validateForm(): boolean {
  let isValid = true
  const form = dialogForm.value

  if (dialogMode.value === 'add') {
    if (!form.cabinetId || form.cabinetId <= 0) {
      errors.value.cabinetId = '请选择所属柜子'
      isValid = false
    } else {
      errors.value.cabinetId = ''
    }
  }

  if (dialogMode.value === 'edit' || (dialogMode.value === 'add' && addType.value === 'cell')) {
    const slotNumberStr = form.slotNumber.toString()
    if (!slotNumberStr) {
      errors.value.slotNumber = '格口号不能为空'
      isValid = false
    } else if (slotNumberStr.length > 10) {
      errors.value.slotNumber = '格口号不能超过10位'
      isValid = false
    } else {
      let duplicate = false
      for (const cab of cabinets.value) {
        for (const row of cab.rows) {
          for (const cell of row.cells) {
            if (cell.type === 'cell' && cell.number.toString() === slotNumberStr) {
              if (dialogMode.value === 'edit' && cab.id === form.cabinetId && cell.number.toString() === form.originalNumber) continue
              duplicate = true
              break
            }
          }
          if (duplicate) break
        }
        if (duplicate) break
      }
      if (duplicate) {
        errors.value.slotNumber = `格口号“${slotNumberStr}”已被使用，请使用唯一编号`
        isValid = false
      } else {
        errors.value.slotNumber = ''
      }
    }

    const toolName = form.toolName.trim()
    if (!toolName) {
      errors.value.toolName = '工具名称不能为空'
      isValid = false
    } else if (toolName.length > 10) {
      errors.value.toolName = '工具名称不能超过10位'
      isValid = false
    } else {
      errors.value.toolName = ''
    }

    if (!form.qrcodeContent) {
      errors.value.qrcodeContent = '请先生成二维码'
      isValid = false
    } else {
      errors.value.qrcodeContent = ''
    }
  }

  if (dialogMode.value === 'editImage' || (dialogMode.value === 'add' && addType.value === 'image')) {
    if (!form.imageUrl) {
      errors.value.imageUrl = '请上传图片'
      isValid = false
    } else {
      errors.value.imageUrl = ''
    }
    if (!form.label) {
      errors.value.label = '标签不能为空'
      isValid = false
    } else {
      errors.value.label = ''
    }
  }

  if (advancedSettingsEnabled.value) {
    if (!form.rowNum || form.rowNum < 1 || !Number.isInteger(form.rowNum)) {
      errors.value.rowNum = '行号必须是正整数'
      isValid = false
    } else {
      errors.value.rowNum = ''
    }

    if (!form.colWidthNumber || form.colWidthNumber < 1 || !Number.isInteger(form.colWidthNumber)) {
      errors.value.colWidthNumber = '列宽必须是正整数'
      isValid = false
    } else {
      errors.value.colWidthNumber = ''
    }

    if (!form.rowHeightNumber || form.rowHeightNumber < 1 || !Number.isInteger(form.rowHeightNumber)) {
      errors.value.rowHeightNumber = '行高必须是正整数'
      isValid = false
    } else {
      errors.value.rowHeightNumber = ''
    }

    if (!form.colSpan || form.colSpan < 1 || !Number.isInteger(form.colSpan)) {
      errors.value.colSpan = '跨越列数必须是正整数'
      isValid = false
    } else {
      errors.value.colSpan = ''
    }

    if (!form.rowSpan || form.rowSpan < 1 || !Number.isInteger(form.rowSpan)) {
      errors.value.rowSpan = '跨越行数必须是正整数'
      isValid = false
    } else {
      errors.value.rowSpan = ''
    }

    if (dialogMode.value !== 'editImage' && !(dialogMode.value === 'add' && addType.value === 'image')) {
      const macNum = form.macAddressNumber
      if (!macNum || macNum < 1 || !Number.isInteger(macNum)) {
        errors.value.macAddressNumber = '硬件地址必须是正整数'
        isValid = false
      } else {
        const macStr = macNum.toString()
        let macDuplicate = false
        for (const cab of cabinets.value) {
          for (const row of cab.rows) {
            for (const cell of row.cells) {
              if (cell.type === 'cell' && cell.macAddress === macStr) {
                if (dialogMode.value === 'edit' && cab.id === form.cabinetId && cell.number.toString() === form.originalNumber) continue
                macDuplicate = true
                break
              }
            }
            if (macDuplicate) break
          }
          if (macDuplicate) break
        }
        if (macDuplicate) {
          errors.value.macAddressNumber = `硬件地址“${macStr}”已被使用，请使用唯一地址`
          isValid = false
        } else {
          errors.value.macAddressNumber = ''
        }
      }
    }
  }

  return isValid
}

function handleGenerateQrcode() {
  const exclude = dialogMode.value === 'edit'
      ? { cabinetId: dialogForm.value.cabinetId, cellNumber: dialogForm.value.originalNumber }
      : undefined
  const newQrcode = generateUniqueQrcodeContent(exclude)
  dialogForm.value.qrcodeContent = newQrcode
  errors.value.qrcodeContent = ''
  showMessage('二维码生成成功')
}

function copyQrcode() {
  if (dialogForm.value.qrcodeContent) {
    navigator.clipboard.writeText(dialogForm.value.qrcodeContent)
    showMessage('二维码内容已复制')
  }
}

async function handleDialogSave() {
  if (!validateForm()) return
  if (dialogMode.value === 'edit') {
    await saveCellChanges()
  } else if (dialogMode.value === 'add') {
    if (addType.value === 'cell') {
      await saveNewCell()
    } else {
      await saveNewImage()
    }
  } else if (dialogMode.value === 'editImage') {
    await saveImageChanges()
  }
}

async function saveCellChanges() {
  const form = dialogForm.value
  const cabinet = cabinets.value.find(cab => cab.id === form.cabinetId)
  if (!cabinet) return

  let targetCell: NormalCell | null = null
  for (const row of cabinet.rows) {
    const cell = row.cells.find(c => c.type === 'cell' && c.number.toString() === form.originalNumber)
    if (cell) {
      targetCell = cell as NormalCell
      break
    }
  }
  if (!targetCell) {
    showMessage('未找到对应的格口数据')
    return
  }

  const updateData: UpdateCellDTO = {
    id: targetCell.id,
    cabinetId: cabinet.id,
    rowNum: advancedSettingsEnabled.value ? form.rowNum : (targetCell.rowNum || 1),
    type: 'cell',
    columns: targetCell.columns,
    height: targetCell.height,
    colSpan: advancedSettingsEnabled.value ? form.colSpan : targetCell.colSpan,
    rowSpan: advancedSettingsEnabled.value ? form.rowSpan : targetCell.rowSpan,
    number: form.slotNumber,
    toolName: form.toolName.trim(),
    isEmpty: targetCell.isEmpty ? 'true' : 'false',
    qrcodeContent: form.qrcodeContent,
    macAddress: advancedSettingsEnabled.value ? form.macAddressNumber.toString() : (targetCell.macAddress || null),
    imageUrl: null,
    label: null,
  }

  try {
    await updateCellConfig(updateData)
    showMessage('保存成功')
    closeDialog()
    const currentCabId = cabinets.value[currentIndex.value]?.id
    await fetchData()
    const newIndex = cabinets.value.findIndex(cab => cab.id === currentCabId)
    if (newIndex !== -1) currentIndex.value = newIndex
    else if (cabinets.value.length) currentIndex.value = 0
  } catch (err) {
    console.error(err)
    showMessage('保存失败，请重试')
  }
}

async function saveNewCell() {
  const form = dialogForm.value
  const targetCabinet = cabinets.value.find(cab => cab.id === form.cabinetId)
  if (!targetCabinet) {
    showMessage('找不到目标柜子')
    return
  }

  const addData: UpdateCellDTO = {
    cabinetId: form.cabinetId,
    rowNum: form.rowNum,
    type: 'cell',
    columns: `${form.colWidthNumber}fr`,
    height: `${form.rowHeightNumber}px`,
    colSpan: form.colSpan,
    rowSpan: form.rowSpan,
    number: form.slotNumber,
    toolName: form.toolName.trim(),
    isEmpty: 'true',
    qrcodeContent: form.qrcodeContent,
    macAddress: form.macAddressNumber ? form.macAddressNumber.toString() : null,
    imageUrl: null,
    label: null
  }

  try {
    await addCellConfig(addData)
    showMessage(`成功添加格口 ${form.slotNumber}`)
    closeDialog()
    const currentCabId = cabinets.value[currentIndex.value]?.id
    await fetchData()
    const newIndex = cabinets.value.findIndex(cab => cab.id === currentCabId)
    if (newIndex !== -1) currentIndex.value = newIndex
    else if (cabinets.value.length) currentIndex.value = 0
  } catch (error: any) {
    console.error('新增格口失败', error)
    const msg = error.response?.data?.message || error.message || '新增格口失败，请重试'
    showMessage(msg)
  }
}

async function saveNewImage() {
  const form = dialogForm.value
  const targetCabinet = cabinets.value.find(cab => cab.id === form.cabinetId)
  if (!targetCabinet) {
    showMessage('找不到目标柜子')
    return
  }

  let finalImageUrl = form.imageUrl
  if (pendingImageFile.value) {
    try {
      showMessage('正在上传图片...')
      const res: any = await uploadImage(pendingImageFile.value)
      if (res.code === 200) {
        finalImageUrl = res.data
      } else {
        showMessage(res.message || '图片上传失败')
        return
      }
    } catch (err: any) {
      console.error('上传失败', err)
      showMessage(err.responset?.message || '图片上传失败，请重试')
      return
    }
  }

  const addData: UpdateCellDTO = {
    cabinetId: form.cabinetId,
    rowNum: form.rowNum,
    type: 'image',
    columns: `${form.colWidthNumber}fr`,
    height: `${form.rowHeightNumber}px`,
    colSpan: form.colSpan,
    rowSpan: form.rowSpan,
    imageUrl: finalImageUrl,
    label: form.label,
    number: undefined,
    toolName: undefined,
    isEmpty: undefined,
    qrcodeContent: undefined,
    macAddress: undefined,
  }

  try {
    await addCellConfig(addData)
    showMessage('成功添加图片单元格')
    closeDialog()
    const currentCabId = cabinets.value[currentIndex.value]?.id
    await fetchData()
    const newIndex = cabinets.value.findIndex(cab => cab.id === currentCabId)
    if (newIndex !== -1) currentIndex.value = newIndex
    else if (cabinets.value.length) currentIndex.value = 0
  } catch (error: any) {
    console.error('新增图片失败', error)
    const msg = error.response?.data?.message || error.message || '新增图片失败，请重试'
    showMessage(msg)
  }
}

async function saveImageChanges() {
  const form = dialogForm.value
  const cabinet = cabinets.value.find(cab => cab.id === form.cabinetId)
  if (!cabinet) return

  let targetCell: ImageCell | null = null
  for (const row of cabinet.rows) {
    const cell = row.cells.find(c => c.type === 'image' && c.id === (dialogForm.value as any).id) as ImageCell | undefined
    if (cell) {
      targetCell = cell
      break
    }
  }
  if (!targetCell) {
    showMessage('未找到对应的图片单元格')
    return
  }

  let finalImageUrl = form.imageUrl
  if (pendingImageFile.value) {
    try {
      showMessage('正在上传图片...')
      const res = await uploadImage(pendingImageFile.value)
      if (res.data.code === 200) {
        finalImageUrl = res.data.data
      } else {
        showMessage(res.data.message || '图片上传失败')
        return
      }
    } catch (err: any) {
      console.error('上传失败', err)
      showMessage(err.response?.data?.message || '图片上传失败，请重试')
      return
    }
  }

  const updateData: UpdateCellDTO = {
    id: targetCell.id,
    cabinetId: cabinet.id,
    rowNum: form.rowNum,
    type: 'image',
    columns: `${form.colWidthNumber}fr`,
    height: `${form.rowHeightNumber}px`,
    colSpan: form.colSpan,
    rowSpan: form.rowSpan,
    imageUrl: finalImageUrl,
    label: form.label,
  }

  try {
    await updateCellConfig(updateData)
    showMessage('图片单元格已更新')
    closeDialog()
    const currentCabId = cabinets.value[currentIndex.value]?.id
    await fetchData()
    const newIndex = cabinets.value.findIndex(cab => cab.id === currentCabId)
    if (newIndex !== -1) currentIndex.value = newIndex
    else if (cabinets.value.length) currentIndex.value = 0
  } catch (err) {
    console.error(err)
    showMessage('保存失败，请重试')
  }
}

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const cabinetListData = await fetchCabinetList() as unknown as CabinetConfig[]
    const processed = processCabinetData(cabinetListData)
    cabinets.value = processed
    const defaultIdx = processed.findIndex(cab => cab.isDefault === true)
    currentIndex.value = defaultIdx !== -1 ? defaultIdx : 0
    recalcTotalStats()
  } catch (err) {
    console.error('获取硬件数据失败:', err)
    error.value = '加载硬件配置失败，请检查网络连接后重试'
    showMessage('硬件数据加载失败')
  } finally {
    loading.value = false
  }
}

function goBack() { router.back() }

function toggleSettingsDropdown() {
  settingsDropdownVisible.value = !settingsDropdownVisible.value
}

function handleAddCabinet() {
  cabinetEditForm.value = {
    title: '',
    width: '280px',
    height: 'auto',
    isDefault: false,
    dehumidifierCommType: '485',
    dehumidifierCommPort: '',
    dehumidifierAddr: '',
    lockCommType: '485',
    lockCommPort: '',
    lockBoardAddr: '',
    humidityMin: 40,
    humidityMax: 60,
    temperatureMin: 15,
    temperatureMax: 25
  }
  editingCabinetId.value = null
  clearCabinetErrors()
  showCabinetDialog.value = true
  settingsDropdownVisible.value = false
}

function toggleAdvancedSetting() {
  advancedSettingsEnabled.value = !advancedSettingsEnabled.value
}

function handleClickOutside(event: MouseEvent) {
  if (settingsWrapper.value && !settingsWrapper.value.contains(event.target as Node)) {
    settingsDropdownVisible.value = false
  }
}

function onCabinetChange() {
  if (dialogMode.value !== 'add') return
  const cabId = dialogForm.value.cabinetId
  dialogForm.value.colSpan = getCabinetTotalColumns(cabId)
  dialogForm.value.rowNum = getNextRowNum(cabId)
  if (addType.value === 'cell') {
    const maxSlot = getNextAvailableSlotNumber()
    if (dialogForm.value.slotNumber < maxSlot) {
      dialogForm.value.slotNumber = maxSlot
    }
  }
}

const showDeleteConfirm = ref(false)
const deleteTargetId = ref<number | null>(null)

function openDeleteConfirm() {
  const currentId = dialogForm.value.id
  if (!currentId || currentId <= 0) {
    showMessage('无效的格口ID，无法删除')
    return
  }
  deleteTargetId.value = currentId
  showDeleteConfirm.value = true
}

async function confirmDelete() {
  const id = deleteTargetId.value
  if (!id || id <= 0) {
    showMessage('无效的格口ID，无法删除')
    showDeleteConfirm.value = false
    return
  }

  try {
    showMessage('正在删除...')
    await deleteCellConfig(id)
    showMessage('删除成功')
    showDeleteConfirm.value = false
    closeDialog()
    const currentCabId = cabinets.value[currentIndex.value]?.id
    await fetchData()
    const newIndex = cabinets.value.findIndex(cab => cab.id === currentCabId)
    if (newIndex !== -1) {
      currentIndex.value = newIndex
    } else if (cabinets.value.length) {
      currentIndex.value = 0
    }
    deleteTargetId.value = null
  } catch (err: any) {
    console.error('删除失败', err)
    const msg = err.response?.data?.message || err.message || '删除失败，请重试'
    showMessage(msg)
    showDeleteConfirm.value = false
  }
}

function openCabinetEdit(cab: ProcessedCabinet) {
  if (!advancedSettingsEnabled.value) return

  clearCabinetErrors()
  editingCabinetId.value = cab.id
  cabinetEditForm.value = {
    title: cab.title || '',
    width: cab.width || '280px',
    height: cab.height || 'auto',
    isDefault: cab.isDefault || false,
    dehumidifierCommType: (cab as any).dehumidifierCommType || '485',
    dehumidifierCommPort: (cab as any).dehumidifierCommPort || '',
    dehumidifierAddr: (cab as any).dehumidifierAddr || '',
    lockCommType: (cab as any).lockCommType || '485',
    lockCommPort: (cab as any).lockCommPort || '',
    lockBoardAddr: (cab as any).lockBoardAddr || '',
    humidityMin: (cab as any).humidityMin ?? 30,
    humidityMax: (cab as any).humidityMax ?? 70,
    temperatureMin: (cab as any).temperatureMin ?? 15,
    temperatureMax: (cab as any).temperatureMax ?? 25
  }
  showCabinetDialog.value = true
  settingsDropdownVisible.value = false
}

function closeCabinetDialog() {
  showCabinetDialog.value = false
  editingCabinetId.value = null
  clearCabinetErrors()
}

function clearCabinetErrors() {
  cabinetErrors.value = {
    title: '', width: '', height: '',
    dehumidifierCommType: '', dehumidifierCommPort: '', dehumidifierAddr: '',
    lockCommType: '', lockCommPort: '', lockBoardAddr: '',
    humidityMin: '', humidityMax: '', temperatureMin: '', temperatureMax: ''
  }
}

/**
 * 验证通讯端口格式
 * @param commType 通讯方式 '485' 或 'TCP'
 * @param portStr 端口字符串
 * @returns 是否合法
 */
function validateCommPort(commType: string, portStr: string): boolean {
  if (!portStr || portStr.trim() === '') return false
  const trimmed = portStr.trim()
  if (commType === '485') {
    // 格式: COM1@9600, COM2@115200 等，不区分大小写
    return /^COM\d+@\d+$/i.test(trimmed)
  } else {
    // TCP/IP 格式: IP:端口，简单验证IP和端口存在
    // 允许IP不严格校验完整段，但常规要求点分十进制
    const ipPortRegex = /^(\d{1,3}\.){3}\d{1,3}:\d+$/
    return ipPortRegex.test(trimmed)
  }
}

function validateCabinetForm(): boolean {
  let isValid = true
  const form = cabinetEditForm.value
  const trimmedTitle = form.title.trim()

  if (!trimmedTitle) {
    cabinetErrors.value.title = '柜子名称不能为空'
    isValid = false
  } else {
    const isDuplicate = cabinets.value.some(cab =>
        (editingCabinetId.value === null || cab.id !== editingCabinetId.value) &&
        cab.title === trimmedTitle
    )
    if (isDuplicate) {
      cabinetErrors.value.title = '柜子名称已存在，请使用唯一名称'
      isValid = false
    } else {
      cabinetErrors.value.title = ''
    }
  }

  if (!form.width || form.width.trim() === '') {
    cabinetErrors.value.width = '柜子宽度不能为空'
    isValid = false
  } else {
    cabinetErrors.value.width = ''
  }

  if (!form.height || form.height.trim() === '') {
    cabinetErrors.value.height = '柜子高度不能为空'
    isValid = false
  } else {
    cabinetErrors.value.height = ''
  }

  if (!form.dehumidifierCommPort || form.dehumidifierCommPort.trim() === '') {
    cabinetErrors.value.dehumidifierCommPort = '通讯端口不能为空'
    isValid = false
  } else if (!validateCommPort(form.dehumidifierCommType, form.dehumidifierCommPort)) {
    cabinetErrors.value.dehumidifierCommPort = form.dehumidifierCommType === '485' ? '格式应为 COM1@9600' : '格式应为 IP:端口 (如 192.168.0.1:8252)'
    isValid = false
  } else {
    cabinetErrors.value.dehumidifierCommPort = ''
  }

  if (!form.dehumidifierAddr || form.dehumidifierAddr.trim() === '') {
    cabinetErrors.value.dehumidifierAddr = '除湿机地址不能为空'
    isValid = false
  } else {
    cabinetErrors.value.dehumidifierAddr = ''
  }

  if (!form.lockCommPort || form.lockCommPort.trim() === '') {
    cabinetErrors.value.lockCommPort = '通讯端口不能为空'
    isValid = false
  } else if (!validateCommPort(form.lockCommType, form.lockCommPort)) {
    cabinetErrors.value.lockCommPort = form.lockCommType === '485' ? '格式应为 COM1@9600' : '格式应为 IP:端口 (如 192.168.0.1:8252)'
    isValid = false
  } else {
    cabinetErrors.value.lockCommPort = ''
  }

  const humidityMin = form.humidityMin
  const humidityMax = form.humidityMax
  if (humidityMin === undefined || humidityMin === null || isNaN(humidityMin)) {
    cabinetErrors.value.humidityMin = '湿度下限必须为正整数'
    isValid = false
  } else if (humidityMin < 0 || humidityMin > 100) {
    cabinetErrors.value.humidityMin = '湿度下限应在 0-100 之间'
    isValid = false
  } else {
    cabinetErrors.value.humidityMin = ''
  }

  if (humidityMax === undefined || humidityMax === null || isNaN(humidityMax)) {
    cabinetErrors.value.humidityMax = '湿度上限必须为正整数'
    isValid = false
  } else if (humidityMax < 0 || humidityMax > 100) {
    cabinetErrors.value.humidityMax = '湿度上限应在 0-100 之间'
    isValid = false
  } else if (humidityMin >= humidityMax) {
    cabinetErrors.value.humidityMax = '湿度上限必须大于湿度下限'
    isValid = false
  } else {
    cabinetErrors.value.humidityMax = ''
  }

  const tempMin = form.temperatureMin
  const tempMax = form.temperatureMax
  if (tempMin === undefined || tempMin === null || isNaN(tempMin)) {
    cabinetErrors.value.temperatureMin = '温度下限必须为正整数'
    isValid = false
  } else {
    cabinetErrors.value.temperatureMin = ''
  }

  if (tempMax === undefined || tempMax === null || isNaN(tempMax)) {
    cabinetErrors.value.temperatureMax = '温度上限必须为正整数'
    isValid = false
  } else if (tempMin >= tempMax) {
    cabinetErrors.value.temperatureMax = '温度上限必须大于温度下限'
    isValid = false
  } else {
    cabinetErrors.value.temperatureMax = ''
  }

  return isValid
}

async function saveCabinetChanges() {
  if (!validateCabinetForm()) return

  const formData = {
    title: cabinetEditForm.value.title.trim(),
    width: cabinetEditForm.value.width.trim(),
    height: cabinetEditForm.value.height.trim(),
    isDefault: cabinetEditForm.value.isDefault,
    dehumidifierCommType: cabinetEditForm.value.dehumidifierCommType,
    dehumidifierCommPort: cabinetEditForm.value.dehumidifierCommPort.trim(),
    dehumidifierAddr: cabinetEditForm.value.dehumidifierAddr.trim(),
    lockCommType: cabinetEditForm.value.lockCommType,
    lockCommPort: cabinetEditForm.value.lockCommPort.trim(),
    lockBoardAddr: cabinetEditForm.value.lockBoardAddr.trim(),
    humidityMin: cabinetEditForm.value.humidityMin,
    humidityMax: cabinetEditForm.value.humidityMax,
    temperatureMin: cabinetEditForm.value.temperatureMin,
    temperatureMax: cabinetEditForm.value.temperatureMax
  }

  try {
    showMessage('正在保存...')

    if (editingCabinetId.value === null) {
      await createCabinet(formData)
      showMessage('柜子创建成功')
    } else {
      await updateCabinet(editingCabinetId.value, formData)
      showMessage('柜子信息保存成功')
    }

    closeCabinetDialog()
    const currentCabId = cabinets.value[currentIndex.value]?.id
    await fetchData()
    const newIndex = cabinets.value.findIndex(cab => cab.id === currentCabId)
    if (newIndex !== -1) {
      currentIndex.value = newIndex
    } else if (cabinets.value.length) {
      currentIndex.value = 0
    }
  } catch (err: any) {
    console.error('保存失败:', err)
    const msg = err.response?.data?.message || err.message || '保存失败，请重试'
    showMessage(msg)
  }
}

let resizeTimer: number | null = null
function handleResize() {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = window.setTimeout(() => updateLayout(), 100)
}

onMounted(() => {
  fetchData()
  updateLayout()
  window.addEventListener('resize', handleResize)
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('click', handleGlobalClick)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (resizeTimer) clearTimeout(resizeTimer)
  if (toastTimer) clearTimeout(toastTimer)
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('click', handleGlobalClick)
  resetImageTempData()
  if (countdown && countdown.cleanup) countdown.cleanup()
})
</script>

<style lang="css" scoped>
/* ==========================================================
   超精简静态样式 – 无动画、无过渡、无模糊、无闪烁
   保留颜色/边框/阴影区分，适合核显流畅运行
   ========================================================== */

/* ---------- 基础容器 ---------- */
.hardware-detail-container {
  min-height: 100vh;
  background: radial-gradient(circle at 20% 30%, #0a1a1f, #051016);
  padding: 20px;
  overflow-y: auto;
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
  margin-right: 12px;
}
.countdown-icon { font-size: 16px; }
.countdown-time {
  font-size: 18px;
  font-weight: 700;
  font-family: monospace;
  letter-spacing: 1px;
}
.countdown-text { font-size: 12px; opacity: 0.8; }

/* ---------- 头部 ---------- */
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1200px;
  margin: 0 auto 20px;
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
  background: rgba(34, 211, 238, 0.1);
  border-color: #22d3ee;
}
.back-btn:active { /* 无 scale */ }
.back-icon { font-size: 18px; }

.detail-title {
  color: #c2f0e0;
  font-size: 24px;
  text-shadow: 0 0 8px rgba(34, 211, 238, 0.25);
}

/* ---------- 导出按钮组（已整合到下拉菜单，此处保留部分样式） ---------- */
.export-group { display: flex; gap: 8px; margin-right: 12px; }
.export-btn-word, .export-btn-excel {
  display: flex; align-items: center; gap: 6px;
  background: rgba(34, 197, 94, 0.15);
  border: 1px solid rgba(34, 197, 94, 0.5);
  border-radius: 60px; padding: 8px 16px;
  color: #4ade80; font-size: 13px; font-weight: 600;
  cursor: pointer;
}
.export-btn-excel {
  background: rgba(59, 130, 246, 0.15);
  border-color: rgba(59, 130, 246, 0.5);
  color: #60a5fa;
}
.export-btn-word:hover:not(:disabled), .export-btn-excel:hover:not(:disabled) {
  background: rgba(34, 197, 94, 0.25);
  border-color: #4ade80;
}
.export-btn-excel:hover:not(:disabled) {
  background: rgba(59, 130, 246, 0.25);
  border-color: #60a5fa;
}
.export-btn-word:disabled, .export-btn-excel:disabled { opacity: 0.6; cursor: not-allowed; }
.export-icon { font-size: 14px; }

/* ---------- 设置下拉菜单 ---------- */
.settings-wrapper {
  position: relative;
  width: auto;
  display: flex;
  justify-content: flex-end;
}
.settings-btn {
  background: rgba(10, 25, 35, 0.8);
  border: 1px solid rgba(34, 211, 238, 0.6);
  border-radius: 60px;
  padding: 10px 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #e0f2fe;
  font-size: 18px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}
.settings-btn:hover {
  background: rgba(34, 211, 238, 0.15);
  border-color: #22d3ee;
}
.settings-arrow {
  font-size: 12px;
  opacity: 0.8;
  transition: transform 0.2s;
}
.settings-arrow.open {
  opacity: 1;
  transform: rotate(180deg);
}
.temp-card .card-icon {
  color: #f87171;
}

.humidity-card .card-icon {
  color: #4ade80;
}


.settings-dropdown {
  position: absolute;
  top: 54px;
  right: 0;
  min-width: 210px;
  background: rgba(12, 22, 30, 0.96);
  border-radius: 28px;
  border: 1px solid rgba(34, 211, 238, 0.35);
  box-shadow: 0 16px 30px rgba(0, 0, 0, 0.5), 0 0 0 0.5px rgba(34, 211, 238, 0.2);
  overflow: hidden;
  z-index: 1000;
  /* 删除动画 */
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  width: 100%;
  background: transparent;
  border: none;
  color: #cbd5e1;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  text-align: left;
  position: relative;
  letter-spacing: 0.3px;
}
.dropdown-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 0;
  background: linear-gradient(135deg, #22d3ee, #3b82f6);
  border-radius: 0 2px 2px 0;
}
.dropdown-item:hover::before { height: 60%; }
.dropdown-item:hover {
  background: rgba(34, 211, 238, 0.12);
  color: #22d3ee;
  padding-left: 24px;
}
.action-item .item-icon {
  text-shadow: 0 0 2px #22d3ee;
}
.action-item:hover .item-icon { /* 无 scale/rotate */ }

.dropdown-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(34, 211, 238, 0.3), transparent);
  margin: 4px 12px;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
}
.setting-item .item-text { flex: 1; margin-left: 4px; }

.toggle-switch {
  position: relative;
  width: 44px;
  height: 24px;
  background: rgba(100, 116, 139, 0.5);
  border-radius: 30px;
  border: 1px solid rgba(34, 211, 238, 0.3);
  cursor: pointer;
  flex-shrink: 0;
}
.toggle-switch.active {
  background: #22d3ee;
  box-shadow: 0 0 6px rgba(34, 211, 238, 0.4);
}
.toggle-knob {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  background: white;
  border-radius: 50%;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}
.toggle-switch.active .toggle-knob { transform: translateX(20px); }

/* 响应式下拉菜单 */
@media (max-width: 640px) {
  .export-group { gap: 6px; margin-right: 8px; }
  .export-btn-word, .export-btn-excel { padding: 6px 12px; font-size: 11px; }
  .export-icon { font-size: 12px; }
  .settings-dropdown { min-width: 180px; top: 48px; }
  .dropdown-item { padding: 12px 16px; font-size: 13px; }
  .toggle-switch { width: 38px; height: 20px; }
  .toggle-knob { width: 14px; height: 14px; }
  .toggle-switch.active .toggle-knob { transform: translateX(18px); }
}

/* ---------- 内容 ---------- */
.detail-content { max-width: 1200px; margin: 0 auto; }

.loading-state, .error-state {
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 80px 20px; gap: 20px;
  color: #94a3b8;
}

.loading-spinner {
  width: 48px; height: 48px;
  border: 4px solid rgba(34, 211, 238, 0.2);
  border-top-color: #22d3ee;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.retry-btn {
  background: rgba(34, 211, 238, 0.2);
  border: 1px solid #22d3ee;
  border-radius: 40px;
  padding: 8px 24px;
  color: #22d3ee;
  cursor: pointer;
  font-size: 14px;
}
.retry-btn:hover { background: rgba(34, 211, 238, 0.3); }

/* ---------- 统计卡片 ---------- */
.summary-card {
  background: rgba(15, 25, 35, 0.8);
  border-radius: 28px;
  border: 1px solid rgba(34, 211, 238, 0.3);
  padding: 20px 24px;
  margin-bottom: 24px;
}
.summary-stats {
  display: flex;
  justify-content: space-around;
  flex-wrap: wrap;
  gap: 20px;
}
.summary-item { text-align: center; min-width: 100px; }
.summary-value {
  font-size: 32px;
  font-weight: 700;
  color: #22d3ee;
  line-height: 1.2;
  text-shadow: 0 0 6px rgba(34, 211, 238, 0.25);
}
.summary-label { font-size: 13px; color: #94a3b8; margin-top: 6px; }

/* ---------- 3D轮播区 ---------- */
.full-layout {
  display: flex;
  flex-direction: column;
  width: 100%;
  overflow: hidden;
  background: transparent;
  border-radius: 24px;
}
.upper-area {
  min-height: 550px;
  flex-shrink: 0;
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
  min-height: 500px;
  padding: 10px 0;
}

.icon {
  width: 1.2em;
  height: 1.2em;
  vertical-align: middle;
  display: inline-block;
  flex-shrink: 0;
  fill: currentColor; /* 如果 SVG 使用 currentColor 可继承文字颜色 */
}

/* 针对标题中的图标稍大 */
.title-icon {
  width: 1.6em;
  height: 1.6em;
}

/* ---------- 温湿度卡片 ---------- */
.temp-card, .humidity-card {
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
.temp-card { left: 20px; border-left: 3px solid #f87171; }
.humidity-card { right: 20px; border-right: 3px solid #4ade80; }
.card-icon { font-size: 18px; }
.card-value {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
  line-height: 1;
  min-width: 45px;
  text-align: center;
}
.temp-card .card-value { color: #f87171; text-shadow: 0 0 4px rgba(248,113,113,0.3); }
.humidity-card .card-value { color: #4ade80; text-shadow: 0 0 4px rgba(74,222,128,0.3); }
.card-label {
  font-size: 11px; color: #94a3b8; font-weight: 500;
  letter-spacing: 1px; background: rgba(0,0,0,0.4);
  padding: 2px 6px; border-radius: 20px;
}
.temp-card:hover, .humidity-card:hover { background: rgba(0,0,0,0.8); }

/* ---------- 导航按钮 ---------- */
.nav-btn-left, .nav-btn-right {
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
.nav-btn-left { left: 12px; }
.nav-btn-right { right: 12px; }
.arrow { font-size: 1.3rem; line-height: 1; }
.btn-text { font-size: 0.85rem; font-weight: 600; }

.nav-btn-left:hover:not(.disabled), .nav-btn-right:hover:not(.disabled) {
  background: rgba(14, 165, 233, 0.9);
  border-color: #7dd3fc;
  color: #0a1a1f;
}
.nav-btn-left.disabled, .nav-btn-right.disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* ---------- 轮播 ---------- */
.carousel-cylinder {
  width: 100%; max-width: 1400px; margin: 0 auto;
  position: relative; display: flex;
  flex-direction: column; align-items: center; justify-content: center;
  height: 100%; overflow: hidden;
}
.carousel-3d {
  position: relative; width: 100%;
  display: flex; justify-content: center; align-items: center;
  transform-style: preserve-3d; overflow: visible;
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
  border: 1px solid #6fcf97;
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
  cursor: default;
}
.cabinet-header:active { /* 无 scale */ }
.cabinet-body { padding: 12px; position: relative; flex: 1; display: flex; flex-direction: column; }
.cabinet-grid {
  display: grid;
  gap: 8px;
  position: relative;
  z-index: 2;
  min-height: 260px;
}

/* ---------- 格口 ---------- */
.cell-container {
  position: relative;
  background: #f9fbfe;
  border: 1px solid #cfdde6;
  border-radius: 12px;
  box-shadow: inset 0 0 0 1px rgba(255,255,255,0.7), 0 2px 8px rgba(0,0,0,0.02);
  cursor: pointer;
}
.cell-container:active { /* 无 scale */ }
.cell-container:hover {
  border-color: #22d3ee;
  box-shadow: 0 0 0 2px rgba(34,211,238,0.3), inset 0 0 0 1px rgba(255,255,255,0.7);
}

.cabinet-cell {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
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
  top: 3px; left: 6px;
  font-size: 9px; font-weight: 800;
  color: #236b4c;
  background: rgba(250,255,240,0.9);
  padding: 2px 6px; border-radius: 28px;
  z-index: 2; white-space: nowrap;
}
.tool-name {
  font-size: 8px; font-weight: 600; color: #1b5e42;
  background: rgba(255,255,245,0.85);
  padding: 2px 5px; border-radius: 20px;
  text-align: center; max-width: 90%; margin-top: 16px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden;
}
.empty-door .tool-name { display: none; }

.custom-image-cell {
  background: rgba(0,0,0,0.4);
  border-radius: 16px;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  border: 1px solid rgba(34,211,238,0.5);
  box-shadow: 0 0 8px rgba(0,255,255,0.15);
  overflow: hidden; cursor: default;
}
.custom-image-cell img { max-width:75%; max-height:65%; object-fit:contain; border-radius:8px; }
.image-label {
  margin-top:6px; font-size:9px; color:#c2f0e0;
  background:rgba(0,0,0,0.5); padding:2px 8px; border-radius:20px;
}
.cell-inner {
  position:absolute; inset:0; background:#fffffffa;
  border-radius:10px; transform:translateZ(-8px); pointer-events:none;
}

.indicator-upper {
  display: flex; justify-content:center;
  margin:4px 0 2px; flex-shrink:0;
}
.indicator-text {
  background: rgba(0,0,0,0.6);
  padding:4px 16px; border-radius:40px;
  font-size:0.85rem; font-weight:600;
  color:#c2f0e0; border:1px solid rgba(34,211,238,0.5);
}
.divider {
  width:85%; height:2px; margin:4px auto;
  background: linear-gradient(90deg, transparent, #22d3ee, #3b82f6, #22d3ee, transparent);
  border-radius:4px; flex-shrink:0;
}

/* ---------- 弹窗 ---------- */
.dialog-overlay {
  position: fixed;
  top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0, 0, 0, 0.75);
  display: flex; align-items: center; justify-content: center;
  z-index: 10000;
  /* 删除动画 */
}

.dialog-content--editable,
.dialog-content--cabinet {
  position: relative;
  width: 90%; max-width: 800px;
  background: linear-gradient(145deg, rgba(20,35,45,0.95), rgba(10,20,28,0.98));
  border-radius: 48px;
  border: 1px solid rgba(34,211,238,0.35);
  box-shadow: 0 20px 40px rgba(0,0,0,0.5), 0 0 0 1px rgba(34,211,238,0.2);
  overflow: hidden;
  /* 删除动画 */
}
.dialog-content--cabinet { max-width: 700px; }

.dialog-glow {
  position: absolute;
  top: -50%; left: -20%; width: 140%; height: 140%;
  background: radial-gradient(circle at 30% 20%, rgba(34,211,238,0.12), transparent 70%);
  pointer-events: none;
  z-index: 0;
}

.dialog-header {
  position: relative;
  display: flex; justify-content: space-between; align-items: center;
  padding: 24px 28px 16px;
  background: rgba(34,211,238,0.05);
  border-bottom: 1px solid rgba(34,211,238,0.25);
  z-index: 1;
}
.dialog-header h3 {
  display: flex; align-items: center; flex-wrap: wrap; gap: 8px;
  margin: 0;
  font-size: 1.6rem; font-weight: 600;
  background: linear-gradient(135deg, #e0f2fe, #22d3ee);
  -webkit-background-clip: text; background-clip: text;
  color: transparent;
  text-shadow: 0 0 6px rgba(34,211,238,0.25);
}
.cabinet-name-badge {
  display: inline-block;
  background: rgba(34,211,238,0.2);
  border-radius: 60px;
  padding: 4px 14px;
  font-size: 1rem; font-weight: 700;
  color: #22d3ee;
  border: 1px solid rgba(34,211,238,0.6);
  letter-spacing: 0.5px;
  white-space: nowrap;
}
.dialog-close {
  background: rgba(255,255,255,0.06);
  border: none;
  font-size: 26px;
  color: #cbd5e1;
  cursor: pointer;
  width: 44px; height: 44px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
}
.dialog-close:hover {
  background: rgba(239,68,68,0.2);
  color: #f87171;
}

.dialog-body {
  position: relative;
  padding: 28px 28px 24px;
  display: flex; flex-direction: column;
  gap: 24px;
  z-index: 1;
  max-height: 70vh;
  overflow-y: auto;
}
.cabinet-edit-body { max-height: 65vh; }

/* ---------- 表单 ---------- */
.form-row--two-columns {
  display: flex; gap: 20px; flex-wrap: wrap;
}
.form-row--two-columns .form-field { flex:1; min-width:150px; }
.form-row--three-columns {
  display: flex; gap: 20px; flex-wrap: wrap;
}
.form-row--three-columns .form-field { flex:1; min-width:120px; }

.form-field { display: flex; flex-direction: column; gap: 8px; }
.form-label {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; font-weight: 600; color: #cbd5e1;
  letter-spacing: 0.3px;
}
.label-icon { font-size: 16px; filter: drop-shadow(0 0 2px rgba(34,211,238,0.5)); }
.required-star { color: #f87171; margin-left: 4px; font-size: 12px; }

.form-input {
  background: rgba(5,15,20,0.7);
  border: 1.5px solid rgba(34,211,238,0.3);
  border-radius: 24px;
  padding: 12px 16px;
  font-size: 14px;
  color: #f1f5f9;
  outline: none;
  font-weight: 500;
  width: 100%;
}
.form-input:focus {
  border-color: #22d3ee;
  box-shadow: 0 0 0 4px rgba(34,211,238,0.15);
  background: rgba(10,25,35,0.85);
}

.input-with-unit { position: relative; display: flex; align-items: center; }
.input-with-unit .form-input { padding-right: 40px; }
.unit-suffix {
  position: absolute; right: 16px;
  color: #22d3ee; font-size: 13px; font-weight: 500;
  pointer-events: none;
}

.form-row--error .form-input {
  border-color: #f87171;
  box-shadow: 0 0 0 3px rgba(248,113,113,0.15);
}
.form-error {
  display: flex; align-items: center; gap: 6px;
  font-size: 11px; color: #f87171;
  margin-top: 2px; padding-left: 12px;
}

.advanced-settings-section {
  margin-top: 8px;
  border-top: 1px dashed rgba(34,211,238,0.3);
  padding-top: 20px;
}
.section-divider { text-align: center; margin-bottom: 16px; }
.divider-text {
  background: rgba(34,211,238,0.15);
  padding: 4px 12px; border-radius: 40px;
  font-size: 12px; color: #22d3ee;
}

/* ---------- 图片上传 ---------- */
.image-upload-card {
  background: rgba(5,15,20,0.6);
  border-radius: 28px;
  padding: 20px;
  border: 1px solid rgba(34,211,238,0.3);
}
.image-upload-card.form-row--error { border-color: #f87171; box-shadow: 0 0 0 2px rgba(248,113,113,0.2); }
.upload-preview-area { display: flex; justify-content: center; margin-bottom: 16px; }
.preview-image {
  position: relative; width: 150px; height: 150px;
  border-radius: 20px; overflow: hidden;
  background: rgba(0,0,0,0.3);
  border: 2px solid rgba(34,211,238,0.5);
  box-shadow: 0 8px 20px rgba(0,0,0,0.3);
}
.preview-image img { width:100%; height:100%; object-fit:contain; background:#0a1a1f; }
.remove-image-btn {
  position: absolute; top:8px; right:8px;
  width:28px; height:28px; border-radius:50%;
  background: rgba(0,0,0,0.7); border: none;
  color: #f87171; font-size:16px; cursor:pointer;
  display:flex; align-items:center; justify-content:center;
}
.remove-image-btn:hover { background: rgba(239,68,68,0.8); color:white; }

.preview-placeholder {
  width:150px; height:150px; border-radius:20px;
  background:rgba(0,0,0,0.4);
  border:2px dashed rgba(34,211,238,0.5);
  display:flex; flex-direction:column;
  align-items:center; justify-content:center; gap:12px;
  color:#94a3b8;
}
.placeholder-icon { font-size:48px; opacity:0.6; }
.placeholder-text { font-size:12px; }

.upload-actions { display:flex; gap:12px; justify-content:center; margin-bottom:16px; }
.upload-btn {
  display:inline-flex; align-items:center; gap:8px;
  background:rgba(34,211,238,0.15);
  border:1px solid #22d3ee; border-radius:60px;
  padding:8px 24px; color:#22d3ee;
  font-size:13px; font-weight:600; cursor:pointer;
}
.upload-btn:hover { background:rgba(34,211,238,0.35); }

/* ---------- 类型选择器 ---------- */
.type-selector {
  display: flex; gap: 16px; margin-top:4px;
}
.type-btn {
  flex:1;
  background:rgba(5,15,20,0.6);
  border:1.5px solid rgba(34,211,238,0.3);
  border-radius:40px;
  padding:10px 16px;
  font-size:14px; font-weight:600;
  color:#94a3b8; cursor:pointer;
}
.type-btn.active {
  background:rgba(34,211,238,0.2);
  border-color:#22d3ee;
  color:#22d3ee;
  box-shadow:0 0 6px rgba(34,211,238,0.25);
}
.type-btn:hover:not(.active) {
  background:rgba(34,211,238,0.1);
  border-color:rgba(34,211,238,0.6);
  color:#cbd5e1;
}

/* ---------- 二维码 ---------- */
.qrcode-area {
  background: rgba(0,0,0,0.35);
  border-radius: 28px;
  padding: 20px;
  text-align: center;
  border: 1px solid rgba(34,211,238,0.3);
}
.qrcode-preview { display:flex; flex-direction:column; align-items:center; gap:16px; }
.qrcode-img-wrapper {
  position:relative; display:inline-block;
  padding:8px; background:white; border-radius:24px;
  box-shadow:0 8px 20px rgba(0,0,0,0.4);
}
.qrcode-img-wrapper:hover { /* 无 scale */ }
.qrcode-overlay-glow {
  position:absolute; inset:-2px; border-radius:26px;
  background:linear-gradient(135deg,#22d3ee,#3b82f6);
  opacity:0.4; z-index:-1;
}
.qrcode-text {
  font-size:13px; color:#22d3ee;
  background:rgba(0,0,0,0.6);
  padding:8px 16px; border-radius:40px;
  font-family:monospace; word-break:break-all; max-width:100%;
  border:1px solid rgba(34,211,238,0.3);
}
.qrcode-copy-btn {
  display:inline-flex; align-items:center; gap:8px;
  background:rgba(34,211,238,0.15);
  border:1px solid #22d3ee; border-radius:60px;
  padding:8px 24px; color:#22d3ee;
  font-size:13px; font-weight:600; cursor:pointer;
}
.qrcode-copy-btn:hover { background:rgba(34,211,238,0.35); }

.qrcode-empty {
  display:flex; flex-direction:column; align-items:center; gap:16px; padding:12px 0;
}
.empty-icon { font-size:48px; opacity:0.6; filter:drop-shadow(0 0 4px rgba(34,211,238,0.3)); }
.empty-hint { color:#94a3b8; font-size:14px; }
.btn-generate-qr {
  display:inline-flex; align-items:center; gap:8px;
  background:linear-gradient(115deg,#1e6f9f,#0e4b6e);
  border:none; border-radius:60px; padding:12px 28px;
  font-size:14px; font-weight:600; color:white;
  cursor:pointer; box-shadow:0 4px 12px rgba(0,0,0,0.3);
}
.btn-generate-qr:hover {
  background:linear-gradient(115deg,#2294cc,#0f5a82);
  box-shadow:0 8px 20px rgba(34,211,238,0.3);
}

/* ---------- 弹窗底部按钮 ---------- */
.dialog-footer--editable {
  display: flex; gap: 16px;
  padding: 16px 28px 28px;
  justify-content: center;
  background: rgba(0,0,0,0.25);
  border-top: 1px solid rgba(34,211,238,0.15);
  z-index: 1; position: relative;
}

.dialog-cancel-btn, .dialog-save-btn {
  flex: 1;
  padding: 14px 0;
  border-radius: 60px;
  font-weight: 700;
  font-size: 16px;
  cursor: pointer;
  border: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.dialog-cancel-btn {
  background: rgba(100,116,139,0.2);
  color: #cbd5e1;
  border: 1px solid rgba(100,116,139,0.6);
}
.dialog-cancel-btn:hover { background: rgba(100,116,139,0.4); }
.dialog-save-btn {
  background: linear-gradient(115deg, #22d3ee, #1e88e5);
  color: white;
  box-shadow: 0 6px 14px rgba(34,211,238,0.25);
}
.dialog-save-btn:hover { filter: brightness(1.02); box-shadow: 0 8px 20px rgba(34,211,238,0.3); }

.dialog-delete-btn {
  flex: 0.5;
  padding: 10px 0;
  border-radius: 60px;
  font-weight: 500;
  font-size: 13px;
  cursor: pointer;
  background: rgba(100,116,139,0.2);
  color: #94a3b8;
  border: 1px solid rgba(100,116,139,0.4);
}
.dialog-delete-btn:hover {
  background: rgba(239,68,68,0.2);
  color: #f87171;
  border-color: rgba(248,113,113,0.6);
}

/* ---------- Toast ---------- */
.toast-message {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0,0,0,0.9);
  color: #22d3ee;
  padding: 12px 24px;
  border-radius: 60px;
  font-size: 14px;
  font-weight: 500;
  border: 1px solid #22d3ee;
  z-index: 2147483647;
  pointer-events: none;
  white-space: nowrap;
  /* 删除动画 */
}

/* ---------- 确认弹窗 ---------- */
.confirm-overlay {
  position: fixed;
  top:0; left:0; width:100%; height:100%;
  background: rgba(0,0,0,0.6);
  display: flex; align-items:center; justify-content:center;
  z-index: 20000;
}
.confirm-dialog {
  background: rgba(20,35,45,0.98);
  border-radius: 32px;
  border: 1px solid rgba(34,211,238,0.4);
  width: 300px; max-width:80%;
  overflow: hidden;
  /* 删除动画 */
}
.confirm-header { padding:20px 20px 8px; font-size:18px; font-weight:600; color:#f87171; }
.confirm-body { padding:0 20px 20px; color:#cbd5e1; font-size:14px; }
.confirm-footer { display:flex; border-top:1px solid rgba(34,211,238,0.2); }
.confirm-footer button {
  flex:1; padding:14px 0; background:transparent; border:none;
  font-size:14px; font-weight:500; cursor:pointer;
}
.confirm-cancel { color:#94a3b8; border-right:1px solid rgba(34,211,238,0.2); }
.confirm-cancel:hover { background:rgba(100,116,139,0.2); }
.confirm-ok { color:#f87171; }
.confirm-ok:hover { background:rgba(239,68,68,0.15); }

/* ---------- 柜子编辑 ---------- */
.toggle-switch-cabinet {
  position: relative;
  width: 52px; height: 28px;
  background: rgba(100,116,139,0.5);
  border-radius: 30px;
  border: 1px solid rgba(34,211,238,0.3);
  cursor: pointer;
  flex-shrink: 0;
  display: inline-block;
}
.toggle-switch-cabinet.active {
  background: #22d3ee;
  box-shadow: 0 0 6px rgba(34,211,238,0.4);
}
.toggle-switch-cabinet .toggle-knob {
  position: absolute;
  top:2px; left:2px;
  width:22px; height:22px;
  background:white;
  border-radius:50%;
  box-shadow:0 1px 3px rgba(0,0,0,0.2);
}
.toggle-switch-cabinet.active .toggle-knob { transform: translateX(24px); }
.field-hint { font-size:11px; color:#6b7280; margin-top:4px; }

.comm-config-row {
  display: grid; grid-template-columns:1fr 1fr; gap:24px;
  margin:16px 0 8px;
}
.comm-card {
  background: rgba(5,15,20,0.4);
  border-radius: 28px;
  border: 1px solid rgba(34,211,238,0.25);
  overflow: hidden;
}
.comm-card-header {
  display:flex; align-items:center; gap:10px;
  padding:14px 20px;
  background:rgba(34,211,238,0.08);
  border-bottom:1px solid rgba(34,211,238,0.2);
}
.comm-icon { font-size:20px; filter:drop-shadow(0 0 2px rgba(34,211,238,0.5)); }
.comm-title { font-size:15px; font-weight:700; color:#22d3ee; letter-spacing:0.5px; }
.comm-card-body { padding:20px; display:flex; flex-direction:column; gap:20px; }

/* ---------- 滚动条 ---------- */
.hardware-detail-container::-webkit-scrollbar,
.upper-area::-webkit-scrollbar,
.dialog-body::-webkit-scrollbar {
  width:5px;
}
.hardware-detail-container::-webkit-scrollbar-track,
.upper-area::-webkit-scrollbar-track,
.dialog-body::-webkit-scrollbar-track {
  background:rgba(0,0,0,0.3); border-radius:4px;
}
.hardware-detail-container::-webkit-scrollbar-thumb,
.upper-area::-webkit-scrollbar-thumb,
.dialog-body::-webkit-scrollbar-thumb {
  background:#22d3ee; border-radius:4px;
}

/* ---------- 导出确认弹窗 ---------- */
.export-confirm-dialog {
  width:380px; max-width:90%;
  background: linear-gradient(145deg, rgba(20,35,45,0.98), rgba(10,20,28,0.98));
  border-radius: 32px;
  border: 1px solid rgba(34,211,238,0.4);
  box-shadow: 0 16px 30px rgba(0,0,0,0.5), 0 0 0 1px rgba(34,211,238,0.2);
  overflow: hidden;
}
.export-confirm-dialog .confirm-header {
  padding:24px 24px 8px; font-size:20px; font-weight:700;
  background: linear-gradient(135deg, #22d3ee, #60a5fa);
  -webkit-background-clip: text; background-clip: text;
  color: transparent;
  text-align:center; letter-spacing:1px;
}
.export-confirm-dialog .confirm-body { padding:8px 24px 20px; text-align:center; }
.export-confirm-dialog .confirm-body p { margin:12px 0; color:#cbd5e1; font-size:14px; }
.export-summary {
  background:rgba(34,211,238,0.08);
  border-radius:20px; padding:16px;
  margin:16px 0; border:1px solid rgba(34,211,238,0.2);
}
.export-summary-item {
  display:flex; align-items:center; justify-content:center; gap:12px;
  padding:8px 0; font-size:14px; color:#e0f2fe;
  border-bottom:1px dashed rgba(34,211,238,0.15);
}
.export-summary-item:last-child { border-bottom:none; }
.export-summary-item .summary-icon { font-size:18px; filter:drop-shadow(0 0 2px rgba(34,211,238,0.5)); }
.export-note {
  font-size:12px !important; color:#94a3b8 !important;
  background:rgba(0,0,0,0.3); padding:10px 12px; border-radius:16px;
  margin-top:12px !important;
}
.export-confirm-dialog .confirm-footer { display:flex; border-top:1px solid rgba(34,211,238,0.2); }
.export-confirm-dialog .confirm-footer button {
  flex:1; padding:16px 0; font-size:15px; font-weight:600;
  display:flex; align-items:center; justify-content:center; gap:8px;
}
.export-confirm-dialog .confirm-cancel {
  background:rgba(100,116,139,0.15); color:#94a3b8;
  border-right:1px solid rgba(34,211,238,0.2);
}
.export-confirm-dialog .confirm-cancel:hover { background:rgba(100,116,139,0.3); color:#cbd5e1; }
.export-ok-btn {
  background: linear-gradient(115deg, #22d3ee, #1e88e5);
  color: white !important;
  text-shadow:0 0 2px rgba(0,0,0,0.2);
}
.export-ok-btn:hover { filter:brightness(1.05); box-shadow:0 4px 12px rgba(34,211,238,0.3); }

/* ---------- 导出按钮禁用状态 ---------- */
.dropdown-item.action-item:disabled { opacity:0.5; cursor:not-allowed; pointer-events:none; }

/* ---------- 响应式 ---------- */
@media (max-width: 768px) {
  .detail-title { font-size:20px; }
  .summary-value { font-size:24px; }
  .summary-item { min-width:70px; }
  .nav-btn-left, .nav-btn-right { padding:8px 16px; }
  .temp-card, .humidity-card { padding:3px 10px; top:4px; }
  .card-icon { font-size:14px; }
  .card-value { font-size:14px; min-width:35px; }
  .card-label { font-size:9px; padding:1px 4px; }
  .toast-message { white-space:normal; text-align:center; max-width:80vw; }
  .dialog-content--editable, .dialog-content--cabinet { max-width:94%; margin:16px; }
  .dialog-header h3 { font-size:1.3rem; }
  .dialog-body { padding:20px 20px 16px; gap:20px; }
  .form-row--two-columns, .form-row--three-columns { flex-direction:column; gap:16px; }
  .form-field { min-width:auto; }
  .cabinet-name-badge { font-size:0.85rem; padding:2px 10px; }
  .preview-image, .preview-placeholder { width:120px; height:120px; }
  .upload-actions { flex-wrap:wrap; justify-content:center; }
  .type-selector { gap:12px; }
  .type-btn { padding:8px 12px; font-size:13px; }
  .comm-config-row { grid-template-columns:1fr; gap:20px; }
  .comm-card-body { padding:16px; }
  .comm-card-header { padding:12px 16px; }
  .dialog-footer--editable { flex-wrap:wrap; }
  .dialog-cancel-btn, .dialog-save-btn, .dialog-delete-btn { padding:10px 0; font-size:14px; }
}

@media (max-width: 480px) {
  .cabinet-item { width:260px !important; }
  .nav-btn-left, .nav-btn-right { padding:6px 12px; }
  .temp-card, .humidity-card { padding:2px 8px; gap:4px; }
  .card-icon { font-size:12px; }
  .card-value { font-size:12px; min-width:30px; }
  .card-label { font-size:8px; }
  .dialog-body { padding:18px 16px; }
  .dialog-footer--editable { padding:12px 20px 24px; gap:12px; }
  .dialog-cancel-btn, .dialog-save-btn { padding:10px 0; font-size:14px; }
  .preview-image, .preview-placeholder { width:100px; height:100px; }
  .type-selector { gap:8px; }
  .type-btn { padding:6px 10px; font-size:12px; }
}
</style>

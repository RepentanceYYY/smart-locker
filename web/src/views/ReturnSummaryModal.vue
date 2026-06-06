<template>
  <Teleport to="body">
    <div v-if="visible" class="modal-overlay">
      <div class="modal-container">
        <div class="modal-header">
          <h3>📋 本次归还详情</h3>
          <button class="close-btn" @click="handleClose">✕</button>
        </div>

        <div class="modal-body">
          <!-- 照片显示区域 -->
          <div v-if="photoData" class="photo-section">
            <div class="section-title">现场照片</div>
            <div class="photo-preview">
              <img :src="photoData" alt="拍摄照片" />
            </div>
          </div>

          <!-- 归还记录列表 -->
          <div class="record-section">
            <div class="section-title">归还物品清单</div>
            <div v-if="returnItems.length === 0" class="empty-message">
              暂无归还记录
            </div>
            <div v-else class="record-list">
              <div class="list-header">
                <span>柜子名称</span>
                <span>格口号</span>
                <span>工具名称</span>
                <span>归还时间</span>
              </div>
              <div class="list-scroll">
                <div v-for="(item, idx) in returnItems" :key="idx" class="list-row">
                  <span>{{ item.cabinetTitle }}</span>
                  <span>{{ item.cellNumber }}</span>
                  <span>{{ item.toolName }}</span>
                  <span>{{ item.returnTime }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 归还人信息表单（无预计归还时间） -->
          <div class="form-section">
            <div class="section-title">归还人信息</div>

            <div class="form-group">
              <label>归还人姓名 <span class="optional">(选填)</span></label>
              <input
                  type="text"
                  v-model="returnerName"
                  placeholder="请输入归还人姓名（选填）"
                  class="form-input"
              />
            </div>

            <div class="form-group">
              <label>工号/卡号 <span class="optional">(选填)</span></label>
              <input
                  type="text"
                  v-model="returnerNumber"
                  placeholder="请输入工号或卡号（选填）"
                  class="form-input"
              />
            </div>

            <div class="form-group">
              <label>备注</label>
              <textarea
                  v-model="remark"
                  placeholder="请输入备注信息（选填）"
                  class="form-textarea"
                  rows="2"
              ></textarea>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="cancel-btn" @click="handleClose">取 消</button>
          <button class="submit-btn" @click="handleSubmit">确认归还</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue'

interface ReturnItem {
  cabinetTitle: string
  cellNumber: string
  toolName: string
  returnTime: string
}

interface Props {
  visible: boolean
  returnItems: ReturnItem[]
  photoData?: string   // base64 照片数据
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'submit', data: {
    returnItems: ReturnItem[]
    returnerName: string
    returnerNumber: string
    remark: string
    photoData: string
  }): void
}>()

const returnerName = ref('')
const returnerNumber = ref('')
const remark = ref('')

function handleClose() {
  emit('update:visible', false)
}

function handleSubmit() {
  let finalPhotoData = props.photoData || ''
  if (!finalPhotoData) {
    const storedData = sessionStorage.getItem('toolOperationData')
    if (storedData) {
      try {
        const data = JSON.parse(storedData)
        finalPhotoData = data.imageData || ''
      } catch (e) {
        console.error(e)
      }
    }
  }

  emit('submit', {
    returnItems: props.returnItems,
    returnerName: returnerName.value.trim(),
    returnerNumber: returnerNumber.value.trim(),
    remark: remark.value,
    photoData: finalPhotoData,
  })

  emit('update:visible', false)
}

// 每次打开弹窗时重置表单
watch(() => props.visible, (newVal) => {
  if (newVal) {
    returnerName.value = ''
    returnerNumber.value = ''
    remark.value = ''
  }
})
</script>

<style scoped>
/* 样式完全复用 BorrowSummaryModal，此处省略，实际使用时复制其样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 20000;
  animation: fadeIn 0.2s ease;
}

.modal-container {
  background: rgba(30, 41, 59, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 32px;
  width: 90%;
  max-width: 900px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 25px 40px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(34, 211, 238, 0.3);
  animation: slideUp 0.3s cubic-bezier(0.2, 0.9, 0.4, 1.1);
  overflow: hidden;
  border: 1px solid rgba(34, 211, 238, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  background: rgba(34, 211, 238, 0.08);
  border-bottom: 1px solid rgba(34, 211, 238, 0.2);
}

.modal-header h3 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: #c2f0e0;
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.8rem;
  cursor: pointer;
  color: #94a3b8;
  transition: all 0.2s;
  line-height: 1;
  padding: 0 8px;
  border-radius: 40px;
}

.close-btn:hover {
  color: #f87171;
  background: rgba(255, 255, 255, 0.08);
  transform: scale(1.05);
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  background: rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #a5f3fc;
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 3px solid #22d3ee;
}

.photo-section {
  background: rgba(0, 0, 0, 0.25);
  border-radius: 20px;
  padding: 12px;
}

.photo-preview {
  width: 100%;
  max-width: 300px;
  margin: 0 auto;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(34, 211, 238, 0.4);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.photo-preview img {
  width: 100%;
  height: auto;
  display: block;
  object-fit: cover;
}

.record-section {
  background: rgba(0, 0, 0, 0.25);
  border-radius: 20px;
  padding: 12px;
}

.empty-message {
  text-align: center;
  padding: 32px 20px;
  color: #94a3b8;
  font-size: 0.9rem;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 16px;
}

.record-list {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.list-header {
  display: grid;
  grid-template-columns: 1fr 0.8fr 1.2fr 1.5fr;
  gap: 16px;
  padding: 10px 16px;
  background: rgba(34, 211, 238, 0.12);
  border-radius: 16px;
  font-weight: 700;
  font-size: 0.85rem;
  color: #a5f3fc;
  border-bottom: 1px solid rgba(34, 211, 238, 0.3);
  margin-bottom: 12px;
}

.list-scroll {
  max-height: 260px;
  overflow-y: auto;
  padding-right: 4px;
}

.list-row {
  display: grid;
  grid-template-columns: 1fr 0.8fr 1.2fr 1.5fr;
  gap: 16px;
  padding: 10px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  font-size: 0.85rem;
  color: #e2e8f0;
  transition: background 0.2s ease;
  border-radius: 12px;
}

.list-row:hover {
  background: rgba(34, 211, 238, 0.08);
}

.list-row span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.form-section {
  background: rgba(0, 0, 0, 0.25);
  border-radius: 20px;
  padding: 12px 16px 16px;
}

.form-group {
  margin-bottom: 18px;
}

.form-group label {
  display: block;
  color: #c2f0e0;
  margin-bottom: 6px;
  font-weight: 500;
  font-size: 0.9rem;
}

.optional {
  color: #94a3b8;
  font-weight: normal;
  font-size: 0.8rem;
  margin-left: 4px;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 10px 14px;
  background: rgba(0, 0, 0, 0.45);
  border: 1px solid rgba(34, 211, 238, 0.3);
  border-radius: 14px;
  color: #e2e8f0;
  font-size: 0.9rem;
  transition: all 0.2s;
  font-family: inherit;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: #22d3ee;
  background: rgba(0, 0, 0, 0.7);
  box-shadow: 0 0 0 2px rgba(34, 211, 238, 0.2);
}

.modal-footer {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  border-top: 1px solid rgba(34, 211, 238, 0.15);
  background: rgba(0, 0, 0, 0.25);
}

.cancel-btn,
.submit-btn {
  padding: 10px 28px;
  border: none;
  border-radius: 40px;
  font-weight: 600;
  font-size: 0.95rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.cancel-btn {
  background: rgba(100, 116, 139, 0.8);
  color: white;
}

.cancel-btn:hover {
  background: rgba(100, 116, 139, 1);
  transform: translateY(-2px);
}

.submit-btn {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: white;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.3);
}

.submit-btn:hover {
  transform: translateY(-2px);
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  box-shadow: 0 6px 16px rgba(245, 158, 11, 0.4);
}

.cancel-btn:active,
.submit-btn:active {
  transform: translateY(1px);
}

.list-scroll::-webkit-scrollbar,
.modal-body::-webkit-scrollbar {
  width: 5px;
}

.list-scroll::-webkit-scrollbar-track,
.modal-body::-webkit-scrollbar-track {
  background: #1e293b;
  border-radius: 4px;
}

.list-scroll::-webkit-scrollbar-thumb,
.modal-body::-webkit-scrollbar-thumb {
  background: #22d3ee;
  border-radius: 4px;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 680px) {
  .modal-container { width: 95%; max-height: 90vh; }
  .list-header, .list-row {
    grid-template-columns: 1fr 0.7fr 1fr 1.3fr;
    gap: 10px;
    font-size: 0.75rem;
    padding: 8px 12px;
  }
  .modal-header h3 { font-size: 1.2rem; }
  .modal-body { padding: 16px; gap: 16px; }
  .section-title { font-size: 1rem; }
  .photo-preview { max-width: 220px; }
  .form-group label { font-size: 0.85rem; }
  .form-input, .form-textarea { padding: 8px 12px; font-size: 0.85rem; }
  .cancel-btn, .submit-btn { padding: 8px 20px; font-size: 0.85rem; }
}

@media (max-width: 480px) {
  .list-header, .list-row {
    grid-template-columns: 1fr 0.6fr 0.9fr 1.2fr;
    gap: 8px;
    font-size: 0.7rem;
    padding: 6px 10px;
  }
  .modal-header { padding: 14px 18px; }
  .modal-header h3 { font-size: 1.1rem; }
  .close-btn { font-size: 1.5rem; }
  .modal-footer { padding: 12px 18px; gap: 12px; }
  .form-group { margin-bottom: 14px; }
  .photo-preview { max-width: 180px; }
}
</style>

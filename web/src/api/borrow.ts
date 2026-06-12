// src/api/borrow.ts
import request from '@/utils/request'

export interface BorrowItem {
    cabinetId: number
    cabinetName: string
    cellId: number
    cellNumber: string
    toolName: string
    borrowTime: string
}

export interface SubmitBorrowParams {
    borrowItems: BorrowItem[]
    borrowerName: string
    borrowerNumber: string
    expectedReturnTime: string
    remark: string
    photoFile?: File   // 改为 File 对象
}

/**
 * 提交领用记录（同时上传照片文件）
 * @param data 领用数据 + 照片文件
 * @returns Promise<any>
 */
export const submitBorrowRecords = async (data: SubmitBorrowParams) => {
    try {
        const formData = new FormData()
        // 将 JSON 数据转为字符串放入 formData
        formData.append('data', JSON.stringify({
            borrowItems: data.borrowItems,
            borrowerName: data.borrowerName,
            borrowerNumber: data.borrowerNumber,
            expectedReturnTime: data.expectedReturnTime,
            remark: data.remark
        }))
        // 添加照片文件（如果有）
        if (data.photoFile) {
            formData.append('photo', data.photoFile, 'borrow_photo.jpg')
        }

        const response = await request.post('/api/borrow/records', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
        return response.data
    } catch (error) {
        console.error('提交领用记录失败:', error)
        throw error
    }
}

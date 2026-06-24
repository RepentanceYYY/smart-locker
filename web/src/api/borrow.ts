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
    borrowerPhoto:string
}

/**
 * 提交领用记录
 * @param data 领用数据
 * @returns Promise<any>
 */
export const submitBorrowRecords = async (data: SubmitBorrowParams) => {
    try {
        const response = await request.post('/api/borrow/records',data)
        return response.data
    } catch (error) {
        console.error('提交领用记录失败:', error)
        throw error
    }
}

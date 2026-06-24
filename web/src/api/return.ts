import request from '@/utils/request'

export interface ReturnItem {
    cabinetId: number
    cabinetName: string
    cellId: number
    cellNumber: string
    toolName: string
    returnTime: string
}

export interface SubmitReturnParams {
    returnItems: ReturnItem[]
    returnerName: string
    returnerNumber: string
    remark: string
    borrowerPhoto: string
}

export const submitReturnRecords = async (params: SubmitReturnParams) => {

    const response = await request.post('/api/return/records', params)
    return response.data
}

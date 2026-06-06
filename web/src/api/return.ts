import axios from 'axios'

export interface ReturnItem {
    cabinetId: number
    cabinetName: string
    cellId: number
    cellNumber: string
    toolName: string
    returnTime: string
}

export interface SubmitReturnParams {
    returnItems: ReturnItem[]   // 注意：这里需要 ReturnItem[]，而不是 ReturnRecord[]
    returnerName: string
    returnerNumber: string
    remark: string
    photoFile?: File            // 可选属性
}

export async function submitReturnRecords(params: SubmitReturnParams) {
    const formData = new FormData()
    // 将 JSON 数据转为字符串
    formData.append('data', JSON.stringify({
        returnItems: params.returnItems,
        returnerName: params.returnerName,
        returnerNumber: params.returnerNumber,
        remark: params.remark
    }))
    if (params.photoFile) {
        formData.append('photo', params.photoFile, 'return_photo.jpg')
    }

    const response = await axios.post('/api/return/records', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    })
    return response.data
}

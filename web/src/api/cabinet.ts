// api/cabinet.ts
import request from '@/utils/request'

/**
 * 获取柜子配置列表
 * @returns {Promise<any[]>} 返回柜子配置数组
 */
export const fetchCabinetList = async (): Promise<any[]> => {
    return await request.get('/api/cabinet/list').then(response => {
        let rawData = response.data

        // 处理可能的数据包装格式
        if (rawData && typeof rawData === 'object' && !Array.isArray(rawData) && 'data' in rawData) {
            rawData = rawData.data
        }

        // 验证数据格式
        if (!Array.isArray(rawData)) {
            console.error('柜子数据格式错误，期望数组，实际得到:', rawData)
            return []
        }

        return rawData
    })
}

/**
 * 更新柜子配置
 * @param id 柜子ID
 * @param data 柜子配置数据
 * @returns {Promise<any>}
 */
export const updateCabinet = async (id: number, data: any): Promise<any> => {
    return await request.put(`/api/cabinet/${id}`, data).then(response => response.data)
}

/**
 * 创建柜子
 */
export const createCabinet = async (data: any) => {
    return await request.post('/api/cabinet', data).then(response => response.data)
}

/**
 * 删除柜子
 */
export const deleteCabinet = async (id: number) => {
    return request.delete(`/api/cabinet/${id}`).then(response => response.data)
}

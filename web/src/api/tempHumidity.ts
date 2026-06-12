import request from '@/utils/request'

/**
 * 温湿度日志数据结构
 */
export interface TempHumidityLog {
    cabinetTitle: string
    temperature: string
    humidity: String
    recordTime: string
}

/**
 * 获取温湿度日志列表
 * @param limit 查询条数，默认5条
 * @returns {Promise<TempHumidityLog[]>}
 */
export async function fetchTempHumidityLogs(limit: number = 5): Promise<TempHumidityLog[]> {
    try {
        const response = await request.get('/api/tempHumidity/logs', {
            params: { limit }
        })

        let rawData = response.data

        // 处理可能的数据包装格式（兼容 {code, data} 或直接返回 data）
        if (rawData && typeof rawData === 'object' && 'data' in rawData) {
            rawData = rawData.data
        }

        if (Array.isArray(rawData)) {
            return rawData
        }

        console.error('温湿度日志数据格式错误，期望数组:', rawData)
        return []
    } catch (error) {
        console.error('获取温湿度日志失败:', error)
        throw error
    }
}


export async function searchTempHumidityLogs(params: Record<string, any>): Promise<TempHumidityLog[]> {
    try {
        const response = await request.get('/api/tempHumidity/logs/search', { params })
        let rawData = response.data
        if (rawData && typeof rawData === 'object' && 'data' in rawData) {
            rawData = rawData.data
        }
        if (Array.isArray(rawData)) {

            return rawData.map(item => ({
                ...item,
                temperature: item.temperature,
                humidity: item.humidity
            }))
        }
        return []
    } catch (error) {
        console.error('筛选温湿度日志失败:', error)
        throw error
    }
}

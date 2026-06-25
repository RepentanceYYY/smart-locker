import request from '@/utils/request'
import type { LogListDTO, PageResult } from "@/api/log.ts";

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

export async function searchTempHumidityLogsByPage(
    page: number,
    size: number,
    params: {
        cabinetTitle?: string,
        endTime?: string
    } = {}
): Promise<PageResult<TempHumidityLog>> {
    try {

        const response = await request.get(`/api/tempHumidity/logs/${page}/${size}`, { params })

        let rawData = response.data

        // 剥离最外层的 Axios 包装或统一响应体包装（如 Result.success(data)）
        if (rawData && typeof rawData === 'object' && 'data' in rawData) {
            rawData = rawData.data
        }

        // 核心校验：判断结构中是否包含 records 并且 records 是数组
        if (rawData && typeof rawData === 'object' && Array.isArray(rawData.records)) {
            return rawData as PageResult<TempHumidityLog>
        }

        console.error('日志列表数据格式错误，期望分页对象:', rawData)
        return { records: [], total: 0, size, current: page, pages: 0 }
    } catch (error) {
        console.error('获取列表失败:', error)
        throw error
    }
}

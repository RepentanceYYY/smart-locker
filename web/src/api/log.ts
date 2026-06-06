// api/log.ts
import axios from 'axios'

/**
 * 日志概览数据结构
 */
export interface LogOverviewData {
    totalLogs: number
    unreturnedCount: number
    unreturnedList: UnreturnedItem[]
}

export interface UnreturnedItem {
    cabinetTitle: string
    cellNumber: number
    toolName: string
    borrowerPhoto: string
    borrowTime: string
}

export interface LogListDTO {
    id: number
    cabinetId: number
    cabinetTitle: string
    cellNumber: number
    toolName: string
    borrowerPhoto: string
    borrowerName: string
    borrowerNumber: string
    borrowTime: string
    borrowRemark: string
    returnPhoto: string
    returnName: string
    returnNumber: string
    returnTime: string
    returnRemark: string
    expectedReturnTime: string
}

/**
 * 获取日志概览（总数、未归还数、未归还列表）
 * @returns {Promise<LogOverviewData>}
 */
export async function fetchLogOverview(): Promise<LogOverviewData> {
    try {
        const response = await axios.get('/api/log/overview')
        let rawData = response.data

        // 处理可能的数据包装格式（兼容 {code, data} 或直接返回 data）
        if (rawData && typeof rawData === 'object' && 'data' in rawData) {
            rawData = rawData.data
        }

        // 验证数据格式
        if (!rawData || typeof rawData !== 'object') {
            console.error('日志数据格式错误，期望对象，实际得到:', rawData)
            return {
                totalLogs: 0,
                unreturnedCount: 0,
                unreturnedList: []
            }
        }

        return {
            totalLogs: rawData.totalLogs ?? 0,
            unreturnedCount: rawData.unreturnedCount ?? 0,
            unreturnedList: Array.isArray(rawData.unreturnedList) ? rawData.unreturnedList : []
        }
    } catch (error) {
        console.error('获取日志概览失败:', error)
        throw error
    }
}

/**
 * 获取全部日志列表（无分页，支持筛选）
 * @param params 查询参数
 * @returns {Promise<LogListDTO[]>}
 */
export async function fetchAllLogList(params: {
    borrowerName?: string
    toolName?: string
    status?: number
    startTime?: string
    endTime?: string
} = {}): Promise<LogListDTO[]> {
    try {
        const response = await axios.get('/api/log/listAll', { params })
        let rawData = response.data

        if (rawData && typeof rawData === 'object' && 'data' in rawData) {
            rawData = rawData.data
        }

        if (Array.isArray(rawData)) {
            return rawData
        }
        console.error('日志列表数据格式错误，期望数组:', rawData)
        return []
    } catch (error) {
        console.error('获取全部日志列表失败:', error)
        throw error
    }
}

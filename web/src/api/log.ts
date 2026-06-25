// api/log.ts
import request from '@/utils/request'

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
//分页
export interface PageResult<T> {
    records: T[];
    total: number;
    size: number;
    current: number;
    pages: number;
}
/**
 * 获取日志概览（总数、未归还数、未归还列表）
 * @returns {Promise<LogOverviewData>}
 */
export async function fetchLogOverview(): Promise<LogOverviewData> {
    try {
        const response = await request.get('/api/log/overview')
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
        const response = await request.get('/api/log/listAll', { params })
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

/**
 * 获取分页日志列表
 * @param page 页码
 * @param size 每页数量
 * @returns {Promise<PageResult<LogListDTO>>} 
 */
export async function fetchLogList(
    page: number, 
    size: number, 
    params: {
        borrowerName?: string,
        toolName?: string,
        status?: number,
        startTime?: string,
        endTime?: string
    } = {} 
): Promise<PageResult<LogListDTO>> {
    try {
       
        const response = await request.get(`/api/logs/${page}/${size}`, { params })
        
        let rawData = response.data

        // 剥离最外层的 Axios 包装或统一响应体包装（如 Result.success(data)）
        if (rawData && typeof rawData === 'object' && 'data' in rawData) {
            rawData = rawData.data
        }

        // 核心校验：判断结构中是否包含 records 并且 records 是数组
        if (rawData && typeof rawData === 'object' && Array.isArray(rawData.records)) {
            return rawData as PageResult<LogListDTO>
        }

        console.error('日志列表数据格式错误，期望分页对象:', rawData)
        return { records: [], total: 0, size, current: page, pages: 0 }
    } catch (error) {
        console.error('获取列表失败:', error)
        throw error
    }
}


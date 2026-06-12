// api/cell.ts
import request from '@/utils/request'

// ================== DTO 定义 ==================
// 更新格口配置的请求体（对应后端 CellConfig 实体）
export interface UpdateCellDTO {
    id?: number | null               // 新增时可以不传
    cabinetId: number
    rowNum: number
    type: 'cell' | 'image'
    columns: string
    height: string
    colSpan: number
    rowSpan: number
    number?: number | null
    toolName?: string | null
    isEmpty?: boolean | string
    imageUrl?: string | null
    label?: string | null
    macAddress?: string | null
    qrcodeContent?: string | null
}


// ================== API 函数 ==================
/**
 * 更新格口配置（根据 id 全量或部分更新）
 * @param data 格口配置 DTO
 */
export const updateCellConfig = (data: UpdateCellDTO) => {
    return request.put('/api/cellConfig/update', data)
}


/**
 * 新增格口配置
 * @param data 新增格口 DTO
 */
export const addCellConfig = (data: UpdateCellDTO) => {
    return request.post('/api/cellConfig/add', data)
}

/**
 * 上传图片文件
 * @param file 图片文件
 * @returns 服务器返回的图片访问路径
 */
export const uploadImage = (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<{ code: number; data: string; message: string }>(
        '/api/cellConfig/uploadImage',
        formData,
        {
            headers: { 'Content-Type': 'multipart/form-data' }
        }
    )
}

/**
 * 删除格口配置（根据ID）
 * @param id 格口配置ID
 */
export const deleteCellConfig = (id: number) => {
    return request.delete(`/api/cellConfig/delete/${id}`)
}

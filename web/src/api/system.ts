import request from '@/utils/request'

export interface SystemConfig {
    systemName: string
    engName: string
    systemCode: string
    location: string
    adminPwd: string
    borrowPeriod: string
    autoReturnTimeoutMinutes: number
    tempHumidityLogInterval: number
    enableFaceCapture: number   // 0-关闭，1-开启
    silentLivenessEnabled:number 
    baiduFaceLicenseKey:string 
}

export async function fetchSystemConfig(): Promise<SystemConfig> {
    const { data } = await request.get('/api/systemConfig')
    return data
}

export async function updateSystemConfig(config: SystemConfig): Promise<void> {
    await request.put('/api/systemConfig', config)
}

export async function resetSystemConfig(): Promise<void> {
    await request.post('/api/systemConfig/reset')
}

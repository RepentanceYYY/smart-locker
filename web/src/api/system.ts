import request from '@/utils/request'

export interface SystemConfig {
    systemName: string
    engName: string
    systemCode: string
    location: string
    adminPwd: string
    borrowPeriod: string
    autoReturnTimeoutMinutes: number
    tempHumidityLogInterval:number
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

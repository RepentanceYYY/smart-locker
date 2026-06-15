import axios from 'axios'

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
}

export async function fetchSystemConfig(): Promise<SystemConfig> {
    const { data } = await axios.get('/api/systemConfig')
    return data
}

export async function updateSystemConfig(config: SystemConfig): Promise<void> {
    await axios.put('/api/systemConfig', config)
}

export async function resetSystemConfig(): Promise<void> {
    await axios.post('/api/systemConfig/reset')
}

// src/stores/systemConfig.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { fetchSystemConfig, updateSystemConfig, resetSystemConfig, type SystemConfig } from '@/api/system'

export const useSystemConfigStore = defineStore('systemConfig', () => {
    // 状态
    const config = ref<SystemConfig>({
        systemName: '',
        engName: '',
        systemCode: '',
        location: '',
        adminPwd: '',
        borrowPeriod: '',
        autoReturnTimeoutMinutes: 5,
        tempHumidityLogInterval: 5,
        enableFaceCapture: 0      // 默认关闭
    })

    const loaded = ref(false)
    const loading = ref(false)

    // 计算属性
    const systemName = computed(() => config.value.systemName)
    const engName = computed(() => config.value.engName)
    const systemCode = computed(() => config.value.systemCode)
    const location = computed(() => config.value.location)
    const adminPwd = computed(() => config.value.adminPwd)
    const borrowPeriod = computed(() => config.value.borrowPeriod)
    const autoReturnTimeoutMinutes = computed(() => config.value.autoReturnTimeoutMinutes)
    const tempHumidityLogInterval = computed(() => config.value.tempHumidityLogInterval)
    const enableFaceCapture = computed(() => config.value.enableFaceCapture)

    // 初始化加载配置（只调用一次）
    async function loadConfig() {
        if (loaded.value) return
        if (loading.value) return

        loading.value = true
        try {
            const data = await fetchSystemConfig()
            config.value = data
            loaded.value = true
            console.log('系统配置加载完成:', config.value)
        } catch (error) {
            console.error('加载系统配置失败:', error)
            throw error
        } finally {
            loading.value = false
        }
    }

    // 更新配置（保存到后端并更新本地）
    async function updateConfig(newConfig: Partial<SystemConfig>) {
        const updatedConfig = { ...config.value, ...newConfig }
        try {
            await updateSystemConfig(updatedConfig)
            config.value = updatedConfig
            return true
        } catch (error) {
            console.error('更新系统配置失败:', error)
            throw error
        }
    }

    // 更新单个字段
    async function updateConfigField<K extends keyof SystemConfig>(key: K, value: SystemConfig[K]) {
        return updateConfig({ [key]: value } as Partial<SystemConfig>)
    }

    // 重置配置
    async function resetConfig() {
        try {
            await resetSystemConfig()
            // 重新加载配置
            loaded.value = false
            await loadConfig()
            return true
        } catch (error) {
            console.error('重置系统配置失败:', error)
            throw error
        }
    }

    // 手动刷新配置（用于需要强制刷新的场景）
    async function refreshConfig() {
        loaded.value = false
        await loadConfig()
    }

    return {
        // 状态
        config,
        loaded,
        loading,
        // 计算属性
        systemName,
        engName,
        systemCode,
        location,
        adminPwd,
        borrowPeriod,
        autoReturnTimeoutMinutes,
        tempHumidityLogInterval,
        enableFaceCapture,
        // 方法
        loadConfig,
        updateConfig,
        updateConfigField,
        resetConfig,
        refreshConfig
    }
})

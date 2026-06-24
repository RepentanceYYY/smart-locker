import { defineStore } from "pinia";
import { ref } from "vue";

export const useDehumidifierStore = defineStore('dehumidifier', () => {
    // websocket - 温湿度推送（服务端主动推送）
    const dehumidifierWsUrl = `${import.meta.env.VITE_WS_BASE_URL}/dehumidifier`
    let dehumidifierSocket: WebSocket | null = null
    const dehumidifierWsConnected = ref(false)
    let allowDehumidifierReconnect = true

    interface CabinetEnvData {
        temperature: number
        humidity: number
    }
    
    // 柜子温湿度Map
    const cabinetEnvMap = ref<Record<number, CabinetEnvData>>({});

    /**
     * 连接除湿机WebSocket（接收温湿度推送）
     */
    const connectDehumidifierWebSocket = async () => {
        // 建立连接时，确保允许重连标志为 true
        allowDehumidifierReconnect = true
        
        dehumidifierSocket = new WebSocket(dehumidifierWsUrl)
        dehumidifierSocket.onopen = () => {
            console.log('除湿机WebSocket 连接成功')
            dehumidifierWsConnected.value = true
        }
        dehumidifierSocket.onmessage = async (event) => {
            try {
                const message = JSON.parse(event.data)
                handleDehumidifierMessage(message)
            } catch (e) {
                console.error('解析除湿机消息失败', e)
            }
        }
        dehumidifierSocket.onerror = (error) => {
            console.error('除湿机WebSocket 错误', error)
        }
        dehumidifierSocket.onclose = () => {
            console.log('除湿机WebSocket 连接关闭')
            dehumidifierWsConnected.value = false
            // 尝试重连
            if (allowDehumidifierReconnect) {
                setTimeout(() => connectDehumidifierWebSocket(), 3000)
            }
        }
    }

    /**
     * 主动关闭除湿机WebSocket
     */
    const closeDehumidifierWebSocket = () => {
        // 1. 阻止后续的自动重连
        allowDehumidifierReconnect = false
        
        // 2. 如果当前有连接，则主动关闭
        if (dehumidifierSocket) {
            // 移除所有事件监听，防止触发原有的 onclose 逻辑导致意外行为（虽然有 allow 拦截，但移除更稳妥）
            dehumidifierSocket.onopen = null
            dehumidifierSocket.onmessage = null
            dehumidifierSocket.onerror = null
            dehumidifierSocket.onclose = null
            
            dehumidifierSocket.close()
            dehumidifierSocket = null
        }
        
        dehumidifierWsConnected.value = false
        console.log('除湿机WebSocket 已主动关闭并清理')
    }

    /**
     * 处理除湿机推送的消息
     */
    const handleDehumidifierMessage = (msg: any) => {
        const { action, code, data } = msg || {}

        if (action === 'pushRealtimeTemperatureHumidity' && code === 200) {
            const realtimeData = data?.realtimeTemperatureHumidity

            if (realtimeData && typeof realtimeData === 'object') {
                Object.keys(realtimeData).forEach(key => {
                    const cabinetId = parseInt(key);
                    const thData = realtimeData[key];

                    cabinetEnvMap.value[cabinetId] = {
                        temperature: thData.temperature,
                        humidity: thData.humidity
                    };
                });
            }
        }
    }

    return {
        connectDehumidifierWebSocket,
        closeDehumidifierWebSocket, // 暴露给外部调用
        cabinetEnvMap
    }
})
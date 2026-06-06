// src/composables/useCountdown.ts
import { ref, onUnmounted } from 'vue'

import { useSystemConfigStore } from '@/stores/systemConfig'
const systemConfigStore = useSystemConfigStore()
// 统一倒计时配置
export const COUNTDOWN_CONFIG = {
    // 倒计时时长（秒）
    DURATION: systemConfigStore.autoReturnTimeoutMinutes * 60,
    // 是否自动开始
    AUTO_START: true,
    // 操作后是否重置计时
    RESET_ON_OPERATION: true
} as const

export interface CountdownOptions {
    // 自定义倒计时时长（可选，默认使用统一配置）
    duration?: number
    // 是否自动开始（可选，默认使用统一配置）
    autoStart?: boolean
    // 超时回调
    onTimeout?: () => void
    // 每次 tick 回调
    onTick?: (secondsLeft: number) => void
    // 操作后是否重置计时（可选，默认使用统一配置）
    resetOnOperation?: boolean
}

export function useCountdown(options: CountdownOptions = {}) {
    const {
        duration: customDuration,
        autoStart: customAutoStart,
        onTimeout,
        onTick,
        resetOnOperation: customResetOnOperation
    } = options

    // 确定倒计时时长
    let duration = customDuration !== undefined ? customDuration : COUNTDOWN_CONFIG.DURATION
    // 确定是否自动开始
    const autoStart = customAutoStart !== undefined ? customAutoStart : COUNTDOWN_CONFIG.AUTO_START
    // 确定操作后是否重置
    const resetOnOperation = customResetOnOperation !== undefined ? customResetOnOperation : COUNTDOWN_CONFIG.RESET_ON_OPERATION

    const secondsLeft = ref(duration)
    const isActive = ref(false)
    let timer: ReturnType<typeof setInterval> | null = null
    let lastOperationTime = 0

    // 停止倒计时
    function stop() {
        if (timer) {
            clearInterval(timer)
            timer = null
        }
        isActive.value = false
    }

    // 开始倒计时
    function start() {
        if (timer) {
            stop()
        }

        if (secondsLeft.value <= 0) {
            secondsLeft.value = duration
        }

        isActive.value = true
        lastOperationTime = Date.now()

        timer = setInterval(() => {
            if (secondsLeft.value > 0) {
                secondsLeft.value--
                onTick?.(secondsLeft.value)

                if (secondsLeft.value === 0) {
                    stop()
                    onTimeout?.()
                }
            } else {
                stop()
            }
        }, 1000)
    }

    // 重置倒计时（重置到初始时长）
    function reset() {
        stop()
        secondsLeft.value = duration
        if (autoStart) {
            start()
        }
    }

    // 重新开始（重置并开始）
    function restart() {
        reset()
        if (!autoStart) {
            start()
        }
    }

    // 操作触发：重置计时
    function handleOperation() {
        if (resetOnOperation) {
            const now = Date.now()
            // 防止短时间内多次重置（500ms内只重置一次）
            if (now - lastOperationTime > 500) {
                lastOperationTime = now
                if (isActive.value) {
                    restart()
                } else if (autoStart) {
                    start()
                }
            }
        }
    }

    // 暂停倒计时
    function pause() {
        if (timer) {
            clearInterval(timer)
            timer = null
        }
        isActive.value = false
    }

    // 更新倒计时时长
    function updateDuration(newDuration: number) {
        if (newDuration > 0) {
            // 重新创建 duration 属性（由于 duration 是 const，这里使用技巧）
            // 实际上我们需要重新初始化，但为了保持 API 一致，我们重启倒计时
            const wasActive = isActive.value
            stop()
            // @ts-ignore - 动态修改 duration
            duration = newDuration
            secondsLeft.value = newDuration
            if (wasActive && autoStart) {
                start()
            }
        }
    }

    // 清理
    function cleanup() {
        stop()
    }

    // 自动清理
    onUnmounted(() => {
        cleanup()
    })

    // 如果自动开始，立即启动
    if (autoStart) {
        start()
    }

    return {
        secondsLeft,      // 剩余秒数
        isActive,         // 是否激活中
        duration,         // 总时长
        start,            // 开始
        stop,             // 停止
        pause,            // 暂停
        reset,            // 重置
        restart,          // 重新开始
        handleOperation,  // 操作处理（自动重置）
        updateDuration,   // 更新倒计时时长
        cleanup           // 清理
    }
}

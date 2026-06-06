package com.tairui.server.common.exception;

public class DeviceTimeoutException extends RuntimeException {

    public DeviceTimeoutException() {
        super("设备响应超时");
    }

    public DeviceTimeoutException(String message) {
        super(message);
    }

    public DeviceTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeviceTimeoutException(String deviceId, long timeoutMs) {
        super("设备响应超时: deviceId=" + deviceId + ", timeout=" + timeoutMs + "ms");
    }
}

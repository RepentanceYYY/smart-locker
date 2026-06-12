package com.tairui.server.common.exception;

import lombok.Getter;

/**
 * 客户端/业务逻辑异常（通常对应 HTTP 4xx 状态码）
 * 用于处理：前端参数错误、越权访问、业务规则不匹配
 */
@Getter
public class ClientException extends RuntimeException {

    // 自定义错误码
    private final Integer code;

    public ClientException(String message) {
        super(message);
        this.code = 400;
    }

    public ClientException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public ClientException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}

package com.tairui.server.common.handler;

import com.tairui.server.common.Result;
import com.tairui.server.common.exception.DeviceTimeoutException;
import com.tairui.server.common.exception.ServerException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NullPointerException.class)
    public Result handleNullPointerException(NullPointerException ex) {
        return Result.error(500, "系统出现空指针异常");
    }

    @ExceptionHandler(ServerException.class)
    public Result handleServerException(ServerException ex) {
        return Result.error(500, ex.getMessage());
    }

    @ExceptionHandler(DeviceTimeoutException.class)
    public Result handleServerException(DeviceTimeoutException ex) {
        return Result.error(504, ex.getMessage());
    }
}

package com.tairui.server.common.handler;

import com.tairui.server.common.Result;
import com.tairui.server.common.exception.ClientException;
import com.tairui.server.common.exception.DeviceTimeoutException;
import com.tairui.server.common.exception.ServerException;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLIntegrityConstraintViolationException;

@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

    /**
     * 捕获自定义客户端异常
     */
    @ExceptionHandler(ClientException.class)
    public Result<Void> handleClientException(ClientException ex) {
        log.warn("客户端请求异常 -> 状态码: {}, 原因: {}", ex.getCode(), ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }

    /**
     * 捕获 HTTP 消息不可读异常（通常是前端传的 JSON 格式坏了，或者漏了逗号括号）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("客户端请求的 JSON 格式错误: {}", ex.getMessage());
        return Result.error(400, "请求体 JSON 格式错误，请检查逗号或括号是否闭合");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result handleNoResourceFoundException(NoResourceFoundException ex) {
        log.warn("请求路径不存在或缺少必要的路径参数: {}", ex.getMessage());
        return Result.error(404, "请求路径不存在或缺少必要的路径参数");
    }

    /**
     * 捕获自定义服务异常
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(ServerException.class)
    public Result handleServerException(ServerException ex) {
        return Result.error(500, ex.getMessage());
    }

    /**
     * 捕获 HTTP 请求方式错误
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {

        log.warn("不支持的请求方式: {}", ex.getMethod());
        return Result.error(405, "不支持的请求方式：" + ex.getMethod());
    }

    /**
     * 捕获参数类型转换失败
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {

        log.warn("参数类型不匹配, 参数名: {}, 期望类型: {}", ex.getName(), ex.getRequiredType());
        return Result.error(400, String.format("参数 '%s' 类型不匹配", ex.getName()));
    }

    /**
     * 捕获数据库操作异常
     */
    @ExceptionHandler(DataAccessException.class)
    public Result<Void> handleDataAccessException(DataAccessException ex) {

        log.error("数据库操作异常: ", ex);

        if (ex.getCause() instanceof SQLIntegrityConstraintViolationException) {
            return Result.error(409, "数据已存在，请勿重复操作");
        }

        return Result.error(500, "数据库服务异常");
    }

    /**
     * 捕获 JSON 参数校验异常 (针对 @RequestBody 上的 @Validated 触发)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        StringBuilder sb = new StringBuilder();

        // 遍历所有的校验失败信息，拼接成一句话
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            sb.append(fieldError.getDefaultMessage()).append("; ");
        }

        log.warn("参数校验失败: {}", sb);

        return Result.error(400, sb.toString());
    }

    /**
     * 捕获所有未定义捕获的运行时异常/系统异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {

        log.error("系统未知崩溃，异常信息: ", ex);

        return Result.error(500, "系统开小差了，请稍后再试");
    }
}

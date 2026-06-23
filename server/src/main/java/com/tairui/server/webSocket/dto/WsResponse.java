package com.tairui.server.webSocket.dto;

import lombok.Data;

@Data
public class WsResponse<T> {
    private String action;
    private String requestId;
    private Long timestamp;
    private Integer code;
    private String message;
    private T data;

    public static <T> WsResponse<T> success(String action, T data) {
        WsResponse<T> response = new WsResponse<>();
        response.setAction(action);
        response.setCode(200);
        response.setMessage("success");
        response.setTimestamp(System.currentTimeMillis());
        response.setData(data);
        return response;
    }

    public static <T> WsResponse<T> success(String action, String message, T data) {
        WsResponse<T> response = new WsResponse<>();
        response.setAction(action);
        response.setCode(200);
        response.setMessage(message);
        response.setTimestamp(System.currentTimeMillis());
        response.setData(data);
        return response;
    }

    public static <T> WsResponse<T> fail(String action, Integer code, String message) {
        WsResponse<T> response = new WsResponse<>();
        response.setAction(action);
        response.setCode(code);
        response.setMessage(message);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    /**
     * 表示指令已被接收，正在处理中
     */
    public static <T> WsResponse<T> progress(String action, String message, T data) {
        WsResponse<T> response = new WsResponse<>();
        response.setAction(action);
        response.setCode(201);
        response.setMessage(message != null ? message : "processing");
        response.setTimestamp(System.currentTimeMillis());
        response.setData(data);
        return response;
    }

    public static <T> WsResponse<T> progress(String action, String message) {
        return progress(action, message, null);
    }

    public static <T> WsResponse<T> response(String action, Integer code, String message, T data) {
        WsResponse<T> response = new WsResponse<>();
        response.setAction(action);
        response.setCode(code);
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
}

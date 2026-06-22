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
}

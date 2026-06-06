package com.tairui.server.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result success(int code, String message, T data) {
        return new Result(code, message, data);
    }

    public static <T> Result success(String message, T data) {
        return new Result(200, message, data);
    }

    public static <T> Result success(T data) {
        return new Result(200, "success", data);
    }

    public static <T> Result success() {
        return new Result(200, "success", null);
    }

    public static <T> Result error(int code, String message, T data) {
        return new Result(code, message, data);
    }

    public static <T> Result error(int code, String message) {
        return new Result(code, message, null);
    }

    public static <T> Result error(String message) {
        return new Result(404, message, null);
    }

    public static <T> Result error() {
        return new Result(404, "error", null);
    }
}

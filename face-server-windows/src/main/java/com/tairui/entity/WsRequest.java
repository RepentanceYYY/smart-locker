package com.tairui.entity;

import lombok.Data;

@Data
public class WsRequest<T> {
    private String action;
    private String requestId;
    private Long timestamp;
    private T data;
}

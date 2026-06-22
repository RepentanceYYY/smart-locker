package com.tairui.server.webSocket.handle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tairui.server.deviceService.QianMingLockDeviceServiceManager;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Log4j2
public class BoxAddressConfigHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    @Autowired
    private QianMingLockDeviceServiceManager qianMingLockDeviceServiceManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("Face WebSocket 连接建立: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        log.info("Face WebSocket 连接关闭: {}", session.getId());
    }
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception{

    }
}

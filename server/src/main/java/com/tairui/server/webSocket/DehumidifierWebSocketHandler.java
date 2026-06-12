package com.tairui.server.webSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tairui.server.device.dehumidifier.ThData;
import com.tairui.server.deviceService.DehumidifierDeviceServiceManager;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Log4j2
public class DehumidifierWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    @Autowired
    private DehumidifierDeviceServiceManager dehumidifierDeviceServiceManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("WebSocket 连接建立: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
        String type = (String) msg.get("type");
        Map<String, Object> data = (Map<String, Object>) msg.get("data");

        switch (type) {
            case "getRealtimeTemperatureHumidity":
                handleGetRealtimeTemperatureHumidity(session, data);
                break;
            default:
                sendError(session, "未知消息类型: " + type);
        }
    }

    private void handleGetRealtimeTemperatureHumidity(WebSocketSession session, Map<String, Object> data) throws IOException {

        Map<Integer, ThData> realtimeTemperatureHumidity = dehumidifierDeviceServiceManager.getRealtimeTemperatureHumidity();
        Map<String, Object> result = Map.of(
                "realtimeTemperatureHumidity", realtimeTemperatureHumidity
        );
        sendResponse(session, "getRealtimeTemperatureHumidity", 200, "最新温湿度获取成功", result);
    }

    private void sendError(WebSocketSession session, String errorMsg) throws IOException {
        sendResponse(session, "error", 500, errorMsg, null);
    }

    private void sendResponse(WebSocketSession session, String type, int code, String message, Map<String, Object> data) throws IOException {
        Map<String, Object> response = Map.of(
                "type", type,
                "code", code,
                "message", message,
                "data", data == null ? Map.of() : data
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }
}

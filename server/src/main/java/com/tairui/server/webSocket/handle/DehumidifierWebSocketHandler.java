package com.tairui.server.webSocket.handle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tairui.server.device.dehumidifier.ThData;
import com.tairui.server.deviceService.DehumidifierDeviceServiceManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Log4j2
public class DehumidifierWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    @Autowired
    private DehumidifierDeviceServiceManager dehumidifierDeviceServiceManager;
    
    // 定时任务执行器，用于定期推送数据给前端
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::pushDataToAllClients, 2, 2, TimeUnit.SECONDS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("WebSocket 连接建立: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        log.info("WebSocket 连接关闭: {}", session.getId());
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

    /**
     * 处理获取实时温湿度请求（保留此方法以兼容前端主动请求）
     */
    private void handleGetRealtimeTemperatureHumidity(WebSocketSession session, Map<String, Object> data) throws IOException {
        Map<Integer, ThData> realtimeTemperatureHumidity = dehumidifierDeviceServiceManager.getRealtimeTemperatureHumidity();
        Map<String, Object> result = Map.of(
                "realtimeTemperatureHumidity", realtimeTemperatureHumidity
        );
        sendResponse(session, "getRealtimeTemperatureHumidity", 200, "最新温湿度获取成功", result);
    }

    /**
     * 定时推送数据给所有连接的客户端
     */
    private void pushDataToAllClients() {
        try {
            if (sessions.isEmpty()) {
                return; // 没有客户端连接，无需推送
            }
            // 直接从设备获取最新数据
            Map<Integer, ThData> latestThData = dehumidifierDeviceServiceManager.getRealtimeTemperatureHumidity();
            

            
            Map<String, Object> result = Map.of(
                    "realtimeTemperatureHumidity", latestThData
            );
            
            String message = objectMapper.writeValueAsString(Map.of(
                    "type", "pushRealtimeTemperatureHumidity",
                    "code", 200,
                    "message", "最新温湿度数据推送",
                    "data", result
            ));
            
            TextMessage textMessage = new TextMessage(message);
            
            // 向所有连接的客户端推送数据
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.error("向会话 {} 推送数据失败: {}", session.getId(), e.getMessage());
                    }
                }
            }
            
            log.debug("已向前端 {} 个客户端推送最新温湿度数据", sessions.size());
        } catch (Exception e) {
            log.error("推送温湿度数据时发生错误: {}", e.getMessage(), e);
        }
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
    
    /**
     * 销毁时关闭定时任务
     */
    @PreDestroy
    public void cleanup() {
        scheduler.shutdown();
        log.info("除湿机WebSocket定时推送任务已关闭");
    }
}

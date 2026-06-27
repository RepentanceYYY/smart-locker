package com.tairui.server.webSocket.handle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tairui.server.device.dehumidifier.ThData;
import com.tairui.server.deviceService.DehumidifierDeviceServiceManager;
import com.tairui.server.mapper.CabinetConfigMapper;
import com.tairui.server.webSocket.dto.WsRequest;
import com.tairui.server.webSocket.dto.WsResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
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

    /**
     * 定时任务线程池的核心线程数
     */
    private static final int THREAD_POOL_SIZE = 1;
    /**
     * 定时推送任务的初始延迟时间（秒）
     */
    private static final long PUSH_INITIAL_DELAY = 2L;
    /**
     * 定时推送任务的执行周期频率（秒）
     */
    private static final long PUSH_PERIOD = 2L;
    /**
     * 推送是否已暂停
     */
    @Getter
    private volatile boolean isPushPaused = false;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();


    @Autowired
    private DehumidifierDeviceServiceManager dehumidifierDeviceServiceManager;
    @Autowired
    private CabinetConfigMapper cabinetConfigMapper;

    // 定时任务执行器，用于定期推送数据给前端
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);


    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::pushDataToAllClients, PUSH_INITIAL_DELAY, PUSH_PERIOD, TimeUnit.SECONDS);
    }

    /**
     * 临时暂停定时推送任务（配置前调用）
     */
    public void pausePush() {
        this.isPushPaused = true;
        log.info("[WebSocket] 除湿机温湿度定时推送已临时暂停，等待配置更新...");
    }

    /**
     * 恢复定时推送任务（配置完整后调用）
     */
    public void resumePush() {
        this.isPushPaused = false;
        log.info("[WebSocket]除湿机温湿度定时推送已恢复正常。");
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
        WsRequest wsRequest;
        WsResponse wsResponse;
        try {
            wsRequest = objectMapper.readValue(payload, WsRequest.class);
        } catch (Exception e) {
            wsResponse = WsResponse.fail("invalid", 400, "不支持的JSON格式");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsResponse)));
            return;
        }

        switch (wsRequest.getAction()) {
            case "getRealtimeTemperatureHumidity":
                handleGetRealtimeTemperatureHumidity(session, wsRequest);
                break;
            default:
                wsResponse = WsResponse.fail(wsRequest.getAction(), 400, "未知消息类型: " + wsRequest.getAction());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsResponse)));
        }
    }

    /**
     * 处理获取实时温湿度请求
     */
    private void handleGetRealtimeTemperatureHumidity(WebSocketSession session, WsRequest wsRequest) throws IOException {
        Map<Integer, ThData> realtimeTemperatureHumidity = dehumidifierDeviceServiceManager.getRealtimeTemperatureHumidity();
        Map<String, Object> result = Map.of(
                "realtimeTemperatureHumidity", realtimeTemperatureHumidity
        );
        WsResponse wsResponse = WsResponse.success(wsRequest.getAction(), "最新温湿度获取成功", result);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsResponse)));
    }

    /**
     * 定时推送数据给所有连接的客户端
     */
    private void pushDataToAllClients() {
        try {
            if (sessions.isEmpty()) {
                return; // 没有客户端连接，无需推送
            }
            Long l = cabinetConfigMapper.selectCount(null);
            if (l <= 0) {
                log.debug("柜子配置列表未空，取消此次数据推送");
                return;
            }
            // 直接从设备获取最新数据
            Map<Integer, ThData> latestThData = dehumidifierDeviceServiceManager.getRealtimeTemperatureHumidity();

            if (latestThData.isEmpty()) {
                return;
            }


            Map<String, Object> result = Map.of(
                    "realtimeTemperatureHumidity", latestThData
            );

            // 向所有连接的客户端推送数据
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    try {
                        WsResponse pushResponse = WsResponse.success("pushRealtimeTemperatureHumidity", "最新温湿度数据推送", result);
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pushResponse)));
                    } catch (IOException e) {
                        log.error("向会话 {} 推送数据失败: {}", session.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("推送温湿度数据时发生错误: {}", e.getMessage(), e);
        }
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

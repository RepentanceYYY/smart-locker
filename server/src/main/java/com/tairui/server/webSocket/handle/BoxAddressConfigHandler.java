package com.tairui.server.webSocket.handle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tairui.server.deviceService.QianMingLockDeviceService;
import com.tairui.server.deviceService.QianMingLockDeviceServiceManager;
import com.tairui.server.webSocket.dto.BoxAddressConfig;
import com.tairui.server.webSocket.dto.WsRequest;
import com.tairui.server.webSocket.dto.WsResponse;
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
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        WsRequest wsRequest = null;
        WsResponse<?> wsResponse;

        QianMingLockDeviceService deviceService = null;
        String commPort = null;
        // 记录这个连接在执行前，是不是已经在全局缓存里了
        boolean isExistingConnection = false;

        try {
            try {
                wsRequest = objectMapper.readValue(payload, WsRequest.class);
            } catch (Exception e) {
                wsResponse = WsResponse.fail("invalid", 400, "不支持的JSON格式");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsResponse)));
                return;
            }

            BoxAddressConfig boxAddressConfig;
            try {
                boxAddressConfig = objectMapper.convertValue(wsRequest.getData(), BoxAddressConfig.class);
                commPort = boxAddressConfig.getCommunicationAddress();
            } catch (Exception e) {
                wsResponse = WsResponse.fail(wsRequest.getAction(), 400, "不支持的数据JSON格式");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsResponse)));
                return;
            }

            if (org.springframework.util.StringUtils.hasText(commPort)) {
                isExistingConnection = qianMingLockDeviceServiceManager.getDeviceServiceMap().containsKey(commPort);
            }

            // 获取或裸创服务，并尝试打开连接
            try {
                deviceService = qianMingLockDeviceServiceManager.getOrCreateDeviceServiceByRawArgs(
                        boxAddressConfig.getCommunicationType(), commPort);
                deviceService.open();

                // 推送中间状态给前端（进度/继续）
                wsResponse = WsResponse.progress(wsRequest.getAction(), "等待按按钮中，倒计时60秒");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsResponse)));
            } catch (Exception e) {
                wsResponse = WsResponse.fail(wsRequest.getAction(), 400, "初始化连接失败：" + e.getMessage());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsResponse)));
                return;
            }

            // 阻塞等待硬件响应结果（60秒超时）
            try {
                boolean setResult = deviceService.setBoxRangeSync(boxAddressConfig.getStartAddress(), boxAddressConfig.getEndAddress(), 60 * 1000);
                if (setResult) {
                    wsResponse = WsResponse.success(wsRequest.getAction(), "设置成功", null);
                } else {
                    wsResponse = WsResponse.fail(wsRequest.getAction(), 500, "硬件响应设置失败");
                }
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsResponse)));
            } catch (Exception e) {
                wsResponse = WsResponse.fail(wsRequest.getAction(), 500, "设置失败，原因: " + e.getMessage());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsResponse)));
            }

        } catch (Exception e) {
            log.error("WebSocket 通信未知异常: ", e);
            String action = (wsRequest != null) ? wsRequest.getAction() : "invalid";
            wsResponse = WsResponse.fail(action, 500, "服务器内部错误: " + e.getMessage());
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsResponse)));
            }
        } finally {
            if (deviceService != null) {
                if (isExistingConnection)
                    log.debug("端口 [{}] 属于系统已有全局长连接，保持通道存活。", commPort);
            } else {
                log.debug("端口 [{}] 属于临时调试/配置连接，执行安全关闭释放资源。", commPort);
                try {
                    deviceService.close();
                } catch (Exception e) {
                    log.error("释放临时硬件连接失败, 端口: {}", commPort, e);
                }
            }
        }
    }

}

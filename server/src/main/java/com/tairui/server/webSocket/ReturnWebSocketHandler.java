package com.tairui.server.webSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tairui.server.device.qianMingLock.QianMingLockDevice;
import com.tairui.server.deviceService.QianMingLockDeviceServiceManager;
import com.tairui.server.service.CabinetConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;


import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ReturnWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    private QianMingLockDeviceServiceManager qianMingLockDeviceServiceManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        System.out.println("WebSocket 连接建立: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
        String type = (String) msg.get("type");
        Map<String, Object> data = (Map<String, Object>) msg.get("data");

        switch (type) {
            case "openLock":
                handleOpenLock(session, data);
                break;
            case "closeAndCheck":
                handleCloseAndCheck(session, data);
                break;
            default:
                sendError(session, "未知消息类型: " + type);
        }
    }

    /**
     * 开锁处理器
     *
     * @param session
     * @param data
     * @throws Exception
     */
    private void handleOpenLock(WebSocketSession session, Map<String, Object> data) throws Exception {
        Integer cabinetId = (Integer) data.get("cabinetId");
        Integer cellId = (Integer) data.get("cellId");
        String cellNumber = (String) data.get("cellNumber");

        int boxNo;
        try {
            boxNo = Integer.parseInt(cellNumber);
        } catch (NumberFormatException e) {
            sendResponse(session, "openLock", 400, "格口号格式错误", null);
            return;
        }

        boolean success;
        try {
            success = qianMingLockDeviceServiceManager.openBoxSync(cellId, 3000);
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }

        if (success) {
            String toolName = getToolNameByCell(cabinetId, cellId, cellNumber);
            Map<String, Object> result = Map.of(
                    "cabinetId", cabinetId,
                    "cellId", cellId,
                    "cellNumber", cellNumber,
                    "toolName", toolName
            );
            sendResponse(session, "openLock", 200, "开锁成功", result);
        } else {
            sendResponse(session, "openLock", 500, "开锁失败，请检查硬件连接", null);
        }
    }

    private void handleCloseAndCheck(WebSocketSession session, Map<String, Object> data) throws Exception {
        Integer cabinetId = (Integer) data.get("cabinetId");
        Integer cellId = (Integer) data.get("cellId");
        String cellNumber = (String) data.get("cellNumber");
        String toolName = (String) data.get("toolName");

        int boxNo;
        try {
            boxNo = Integer.parseInt(cellNumber);
        } catch (NumberFormatException e) {
            sendResponse(session, "closeAndCheck", 400, "格口号格式错误", null);
            return;
        }
        boolean isOpen;
        try {
            QianMingLockDevice.BoxStatusData boxStatusData = qianMingLockDeviceServiceManager.queryBoxStatusSync(cellId, 500L);
            System.out.println(boxStatusData.toString());
            isOpen = boxStatusData.isOpen(boxNo);
            if (isOpen == true) {
                sendResponse(session, "closeAndCheck", 500, "关门失败，请联系管理员", null);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(session, "closeAndCheck", 500, "关门失败，请联系管理员", null);
            return;
        }

        // 是否有物品
        boolean hasGoods;
        try {
            QianMingLockDevice.BoxGoodsData boxGoodsData = qianMingLockDeviceServiceManager.queryGoodsStatusSync(cellId, 3000L);
            System.out.println(boxGoodsData.toString());
            hasGoods = boxGoodsData.hasGoods(boxNo);
        } catch (Exception e) {
            e.printStackTrace();
            hasGoods = false;
        }

        if (hasGoods) {
            String returnTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            Map<String, Object> result = Map.of(
                    "cabinetId", cabinetId,
                    "cellId", cellId,
                    "cellNumber", cellNumber,
                    "toolName", toolName,
                    "returnTime", returnTime
            );
            sendResponse(session, "closeAndCheck", 200, "归还成功", result);
        } else {
            sendResponse(session, "closeAndCheck", 204, "未检测到物品，请放入物品后重新关门", null);
        }
    }

    private String getToolNameByCell(Integer cabinetId, Integer cellId, String cellNumber) {
        // TODO: 从数据库或缓存中查询该格口对应的工具名称
        return "工具-" + cellNumber;
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

    private void sendError(WebSocketSession session, String errorMsg) throws IOException {
        sendResponse(session, "error", 500, errorMsg, null);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        System.out.println("WebSocket 连接关闭: " + session.getId());
    }
}

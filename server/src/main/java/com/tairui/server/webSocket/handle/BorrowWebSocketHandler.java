package com.tairui.server.webSocket.handle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tairui.server.deviceService.QianMingLockDeviceServiceManager;
import com.tairui.server.dto.CabinetFullDTO;
import com.tairui.server.service.CabinetConfigService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Log4j2
public class BorrowWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    private QianMingLockDeviceServiceManager qianMingLockDeviceServiceManager;
    @Autowired
    private CabinetConfigService cabinetConfigService;

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
            case "openLock":
                handleOpenLock(session, data);
                break;
            case "closeAndCheck":
                handleCloseAndCheck(session, data);
                break;
            case "checkAllLockStatus":
                handleAllCellLockStatus(session);
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

    /**
     * 获取指定格口的门锁状态和储物状态
     *
     * @param session
     * @param data
     * @throws Exception
     */
    private void handleCloseAndCheck(WebSocketSession session, Map<String, Object> data) throws Exception {
        /**
         * 响应码对照：
         * 400 请求数据错误
         * 409 锁状态查询失败，后续不检测是否储物
         * 500 检测到未关锁 后续不检测是否储物
         * 200 已关锁，没有物品
         * 204 已关锁，有物品
         *
         */

        Integer cabinetId = (Integer) data.get("cabinetId");
        Integer cellId = (Integer) data.get("cellId");
        String cellNumber = (String) data.get("cellNumber");
        String toolName = (String) data.get("toolName");

        try {
            boolean isOpen = qianMingLockDeviceServiceManager.querySingleBoxStatusSync(cellId, 500L);
            if (isOpen == true) {
                sendResponse(session, "closeAndCheck", 409, "锁未关", null);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(session, "closeAndCheck", 500, "查询锁状态失败", null);
            return;
        }
        Thread.sleep(200L);
        // 是否有物品
        boolean hasGoods;
        try {
            hasGoods = qianMingLockDeviceServiceManager.querySingleGoodsStatusSync(cellId, 3000L);
        } catch (Exception e) {
            e.printStackTrace();
            hasGoods = true;
        }
        String operationTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if (hasGoods) {

            Map<String, Object> result = Map.of(
                    "cabinetId", cabinetId,
                    "cellId", cellId,
                    "cellNumber", cellNumber,
                    "toolName", toolName
            );
            sendResponse(session, "closeAndCheck", 204, "已关锁，检测到物品", result);
        } else {
            Map<String, Object> result = Map.of(
                    "cabinetId", cabinetId,
                    "cellId", cellId,
                    "cellNumber", cellNumber,
                    "toolName", toolName,
                    "borrowTime", operationTime
            );
            sendResponse(session, "closeAndCheck", 200, "已关锁，未检测到物品", result);
        }
    }

    /**
     * 检测是否所有柜子所有锁都已关闭
     *
     * @param session
     * @throws Exception
     */
    private void handleAllCellLockStatus(WebSocketSession session) throws Exception {

        List<CabinetFullDTO> cabinets = cabinetConfigService.getFullConfigList();
        // 所有柜子的所有格口都关锁了
        boolean allCabinetAllClosed = true;

        for (CabinetFullDTO cabinet : cabinets) {

            if (cabinet.getRows() == null || cabinet.getRows().isEmpty()) continue;

            List<CabinetFullDTO.CellConfigDTO> cells = cabinet.getRows().stream()
                    .filter(Objects::nonNull)
                    .filter(row -> row.getCells() != null)
                    .flatMap(row -> row.getCells().stream())
                    .filter(Objects::nonNull)
                    .filter(cell -> "cell".equalsIgnoreCase(cell.getType()))
                    .filter(cell -> cell.getMacAddress() != null)
                    .toList();

            if (cells.isEmpty()) {
                continue;
            }
            List<Integer> macList = cells.stream()
                    .map(CabinetFullDTO.CellConfigDTO::getMacAddress)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .filter(s -> s.matches("\\d+"))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
            Boolean allClosed = false;
            try {
                allClosed = qianMingLockDeviceServiceManager.isAllActiveCellsClosed(cabinet.getId(), macList);
            } catch (Exception e) {
                sendResponse(session, "checkAllLockStatus", 501, e.getMessage(), null);
                return;
            }
            if (allClosed == false) {
                allCabinetAllClosed = false;
            }
        }

        if (allCabinetAllClosed == true) {
            sendResponse(session, "checkAllLockStatus", 200, "所有锁都已关闭", null);
        } else {
            sendResponse(session, "checkAllLockStatus", 500, "部分锁未关闭", null);
        }
    }

    private void sendError(WebSocketSession session, String errorMsg) throws IOException {
        sendResponse(session, "error", 500, errorMsg, null);
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

}

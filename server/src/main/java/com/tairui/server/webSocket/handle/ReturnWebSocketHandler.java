package com.tairui.server.webSocket.handle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tairui.server.deviceService.QianMingLockDeviceServiceManager;
import com.tairui.server.dto.CabinetFullDTO;
import com.tairui.server.service.CabinetConfigService;
import com.tairui.server.webSocket.dto.WsRequest;
import com.tairui.server.webSocket.dto.WsResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;


import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Log4j2
public class ReturnWebSocketHandler extends TextWebSocketHandler {

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
            case "openLock":
                handleOpenLock(session, wsRequest);
                break;
            case "closeAndCheck":
                handleCloseAndCheck(session, wsRequest);
                break;
            case "checkAllLockStatus":
                handleAllCellLockStatus(session, wsRequest);
                break;
            default:
                wsResponse = WsResponse.fail(wsRequest.getAction(), 400, "未知消息类型: " + wsRequest.getAction());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsResponse)));
        }
    }

    /**
     * 开锁处理器
     *
     * @param session
     * @param wsRequest
     * @throws Exception
     */
    private void handleOpenLock(WebSocketSession session, WsRequest wsRequest) throws Exception {

        Map<String, Object> data = objectMapper.convertValue(wsRequest.getData(), HashMap.class);

        Integer cabinetId = (Integer) data.get("cabinetId");
        Integer cellId = (Integer) data.get("cellId");
        String cellNumber = (String) data.get("cellNumber");

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
            WsResponse wsResponse = WsResponse.success(wsRequest.getAction(), "开锁成功", result);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsResponse)));
        } else {
            WsResponse wsResponse = WsResponse.fail(wsRequest.getAction(), 500, "开锁失败，请检查硬件连接");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsResponse)));
        }
    }

    /**
     * 获取指定格口的门锁状态和储物状态
     *
     * @param session
     * @param wsRequest
     * @throws Exception
     */
    private void handleCloseAndCheck(WebSocketSession session, WsRequest wsRequest) throws Exception {
        /**
         * 响应码对照：
         * 400 请求数据错误
         * 409 锁状态查询失败，后续不检测是否储物
         * 500 检测到未关锁 后续不检测是否储物
         * 200 已关锁，有物
         * 204 已关锁，没有物品
         *
         */
        Map<String, Object> data = objectMapper.convertValue(wsRequest.getData(), HashMap.class);
        Integer cabinetId = (Integer) data.get("cabinetId");
        Integer cellId = (Integer) data.get("cellId");
        String cellNumber = (String) data.get("cellNumber");
        String toolName = (String) data.get("toolName");
        WsResponse wsResponse;
        try {
            boolean isOpen = qianMingLockDeviceServiceManager.querySingleBoxStatusSync(cellId, 500L);
            if (isOpen == true) {
                wsResponse = WsResponse.fail(wsRequest.getAction(), 409, "锁未关");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsResponse)));
                return;
            }
        } catch (Exception e) {
            log.error("锁状态查询失败，原因:{}", e.getMessage());
            wsResponse = WsResponse.fail(wsRequest.getAction(), 409, "查询锁状态失败");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsResponse)));
            return;
        }
        Thread.sleep(200L);
        // 是否有物品
        boolean hasGoods;
        try {
            hasGoods = qianMingLockDeviceServiceManager.querySingleGoodsStatusSync(cellId, 3000L);
        } catch (Exception e) {
            log.error("物品状态查询失败，原因:{}", e.getMessage());
            hasGoods = false;
        }

        String operationTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Map<String, Object> result = Map.of(
                "cabinetId", cabinetId,
                "cellId", cellId,
                "cellNumber", cellNumber,
                "toolName", toolName,
                "borrow", operationTime
        );

        if (hasGoods) {
            wsResponse = WsResponse.response(wsRequest.getAction(), 200, "已关锁，检测到物品", result);
        } else {
            wsResponse = WsResponse.response(wsRequest.getAction(), 204, "已关锁，检测到物品", result);
        }

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsResponse)));
    }

    /**
     * 检测是否所有柜子所有锁都已关闭
     *
     * @param session
     * @throws Exception
     */
    private void handleAllCellLockStatus(WebSocketSession session, WsRequest wsRequest) throws Exception {

        List<CabinetFullDTO> cabinets = cabinetConfigService.getFullConfigList();
        // 所有柜子的所有格口都关锁了
        boolean allCabinetAllClosed = true;
        WsResponse wsResponse;

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

            Boolean allClosed;
            try {
                allClosed = qianMingLockDeviceServiceManager.isAllActiveCellsClosed(cabinet.getId(), macList);
            } catch (Exception e) {
                wsResponse = WsResponse.fail(wsRequest.getAction(), 501, "硬件连接异常，无法完成");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsResponse)));
                return;
            }

            if (allClosed == false) {
                allCabinetAllClosed = false;
            }
        }

        if (allCabinetAllClosed == true) {
            wsResponse = WsResponse.success(wsRequest.getAction(), "所有锁都已关闭", null);
        } else {
            wsResponse = WsResponse.fail(wsRequest.getAction(), 500, "部分锁未关闭");
        }
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsResponse)));
    }

    private String getToolNameByCell(Integer cabinetId, Integer cellId, String cellNumber) {
        // TODO: 从数据库或缓存中查询该格口对应的工具名称
        return "工具-" + cellNumber;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        log.info("WebSocket 连接关闭: {}", session.getId());
    }
}

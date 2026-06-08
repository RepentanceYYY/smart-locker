package com.tairui.server.webSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tairui.server.device.qianMingLock.QianMingLockDevice;
import com.tairui.server.deviceService.QianMingLockDeviceServiceManager;
import com.tairui.server.dto.CabinetFullDTO;
import com.tairui.server.service.CabinetConfigService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

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
         * 200 已关锁，有物
         * 204 已关锁，没有物品
         */

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
            Map<String, Object> result = Map.of(
                    "cabinetId", cabinetId,
                    "cellId", cellId,
                    "cellNumber", cellNumber,
                    "toolName", toolName
            );
            sendResponse(session, "closeAndCheck", 204, "未检测到物品，请放入物品后重新关门", result);
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

            CabinetFullDTO.CellConfigDTO firstCell = cabinet.getRows().stream()
                    .filter(row -> row.getCells() != null)
                    // 1. 把每一行里的 List<CellConfigDTO> 展平成一个格子流
                    .flatMap(row -> row.getCells().stream())
                    // 2. 过滤出符合条件的格子
                    .filter(cell -> "cell".equalsIgnoreCase(cell.getType()) && cell.getMacAddress() != null)
                    // 3. 拿到第一个符合条件的
                    .findFirst()
                    // 4. 如果没有符合条件的格子，则返回 null
                    .orElse(null);

            if (firstCell == null) {
                continue;
            }

            Boolean allClosed = qianMingLockDeviceServiceManager.isAllClosed(cabinet.getId(), Integer.parseInt(firstCell.getMacAddress()));
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

    /**
     * 根据柜子配置，计算并获取所有涉及到的板子首个 macAddress 集合
     */
    public List<Integer> getUniqueMacAddressList(CabinetFullDTO cabinet) {
        // 使用 TreeSet 自动去重且按 MAC 地址从小到大排序
        TreeSet<Integer> uniqueMacs = new TreeSet<>();

        if (cabinet == null || cabinet.getRows() == null) {
            return new ArrayList<>();
        }

        // 1. 深度遍历柜子的每一行、每一个格子
        for (CabinetFullDTO.RowConfig row : cabinet.getRows()) {
            if (row.getCells() == null) continue;

            for (CabinetFullDTO.CellConfigDTO cell : row.getCells()) {
                // 2. 排除图片格子，只处理合法的普通格子
                if ("cell".equalsIgnoreCase(cell.getType()) && cell.getMacAddress() != null) {
                    try {
                        int rawMac = Integer.parseInt(cell.getMacAddress().trim());
                        int baseMac = ((rawMac - 1) / 16) * 16 + 1;

                        uniqueMacs.add(baseMac);
                    } catch (NumberFormatException e) {
                        // 预防前端或数据库录入了非数字的异常 MAC 地址
                        log.error("解析格子 MAC 地址失败: {}", cell.getMacAddress());
                    }
                }
            }
        }

        // 4. 转回 List 返回
        return new ArrayList<>(uniqueMacs);
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
        log.info("WebSocket 连接关闭: {}", session.getId());
    }
}

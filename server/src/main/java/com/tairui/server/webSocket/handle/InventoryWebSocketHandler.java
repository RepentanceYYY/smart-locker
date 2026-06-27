package com.tairui.server.webSocket.handle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tairui.server.deviceService.QianMingLockDeviceServiceManager;
import com.tairui.server.dto.BorrowRecordSubmitDTO;
import com.tairui.server.dto.CabinetFullDTO;
import com.tairui.server.dto.ReturnRecordSubmitDTO;
import com.tairui.server.service.CabinetConfigService;
import com.tairui.server.service.SysOperLogService;
import com.tairui.server.webSocket.dto.WsRequest;
import com.tairui.server.webSocket.dto.WsResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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
public class InventoryWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    private QianMingLockDeviceServiceManager qianMingLockDeviceServiceManager;
    @Autowired
    private CabinetConfigService cabinetConfigService;
    @Autowired
    private SysOperLogService sysOperLogService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("WebSocket 连接建立: {}", session.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
        List<String> errorCabinetTitle = new ArrayList<>();

        List<CabinetFullDTO> cabinets = cabinetConfigService.getFullConfigList();

        List<BorrowRecordSubmitDTO.BorrowItemDTO> borrowItems = new ArrayList<>();
        List<ReturnRecordSubmitDTO.ReturnItemDTO> returnItems = new ArrayList<>();

        String currentTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        int unconfiguredLockerCount = 0;

        for (CabinetFullDTO cabinet : cabinets) {

            if (cabinet.getRows() == null || cabinet.getRows().isEmpty()) {
                unconfiguredLockerCount += 1;
                continue;
            }

            // cells 只做一次完整清洗（去掉重复判断）
            List<CabinetFullDTO.CellConfigDTO> cells = cabinet.getRows().stream()
                    .filter(Objects::nonNull)
                    .filter(row -> row.getCells() != null)
                    .flatMap(row -> row.getCells().stream())
                    .filter(Objects::nonNull)
                    .filter(cell -> "cell".equalsIgnoreCase(cell.getType()))
                    .filter(cell -> StringUtils.hasText(cell.getMacAddress()))
                    .filter(cell -> cell.getMacAddress().matches("\\d+"))
                    .toList();

            if (cells.isEmpty()) {
                continue;
            }

            // MAC列表（不重复trim/校验）
            List<Integer> macList = cells.stream()
                    .map(CabinetFullDTO.CellConfigDTO::getMacAddress)
                    .map(Integer::parseInt)
                    .toList();

            Map<Integer, Boolean> goodsStatusMap = new HashMap<>();
            try {
                goodsStatusMap = qianMingLockDeviceServiceManager.querySingleCabinetGoodsStatusSync(cabinet.getId(), macList, 500L);
            } catch (Exception e) {
                log.error("查询单个柜子所有有效格口的储物状态出现异常，消息:{}", e.getMessage());
                errorCabinetTitle.add(cabinet.getTitle());
                continue;
            }

            for (CabinetFullDTO.CellConfigDTO cell : cells) {

                Integer macNo = Integer.parseInt(cell.getMacAddress());

                boolean hasGoods = goodsStatusMap.getOrDefault(macNo, false);
                boolean isEmpty = Boolean.TRUE.equals(cell.getIsEmpty());

                // 归还
                if (isEmpty && hasGoods) {

                    ReturnRecordSubmitDTO.ReturnItemDTO item = new ReturnRecordSubmitDTO.ReturnItemDTO();
                    item.setCabinetId(cabinet.getId());
                    item.setCabinetName(cabinet.getTitle());
                    item.setCellId(cell.getId());
                    item.setCellNumber(cell.getNumber());
                    item.setToolName(cell.getToolName());
                    item.setReturnTime(currentTime);

                    returnItems.add(item);
                }

                // 领用
                else if (!isEmpty && !hasGoods) {

                    BorrowRecordSubmitDTO.BorrowItemDTO item = new BorrowRecordSubmitDTO.BorrowItemDTO();
                    item.setCabinetId(cabinet.getId());
                    item.setCabinetName(cabinet.getTitle());
                    item.setCellId(cell.getId());
                    item.setCellNumber(cell.getNumber());
                    item.setToolName(cell.getToolName());
                    item.setBorrowTime(currentTime);

                    borrowItems.add(item);
                }
            }
        }


        if (!borrowItems.isEmpty()) {
            BorrowRecordSubmitDTO borrowRecordSubmitDTO = new BorrowRecordSubmitDTO();
            borrowRecordSubmitDTO.setBorrowItems(borrowItems);
            sysOperLogService.saveBorrowRecordsWithPhoto(borrowRecordSubmitDTO);
        }
        if (!returnItems.isEmpty()) {
            ReturnRecordSubmitDTO returnRecordSubmitDTO = new ReturnRecordSubmitDTO();
            returnRecordSubmitDTO.setReturnItems(returnItems);
            sysOperLogService.saveReturnRecordsWithPhoto(returnRecordSubmitDTO);
        }

        Map<String, Object> result = Map.of(
                "borrowItems", borrowItems,
                "returnItems", returnItems
        );
        if (errorCabinetTitle.size() > 0 && errorCabinetTitle.size() == cabinets.size()) {
            wsResponse = WsResponse.fail(wsRequest.getAction(), 500, "盘点失败，请检查硬件连接");
        } else if (errorCabinetTitle.size() > 0) {
            String errorMessage = errorCabinetTitle.stream()
                    .filter(title -> title != null && !title.isEmpty())
                    .collect(Collectors.joining(","));
            errorMessage += "盘点失败，请检测硬件连接";
            wsResponse = WsResponse.response(wsRequest.getAction(), 206, errorMessage, result);
        } else if (cabinets.size() == unconfiguredLockerCount) {
            wsResponse = WsResponse.response(wsRequest.getAction(), 201, "未配置格口，盘点无效", result);
        } else {
            wsResponse = WsResponse.success(wsRequest.getAction(), "盘点完成", result);
        }

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsResponse)));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        log.info("WebSocket 连接关闭: {}", session.getId());
    }
}

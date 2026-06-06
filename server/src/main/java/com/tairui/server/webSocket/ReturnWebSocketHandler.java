package com.tairui.server.webSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tairui.server.device.core.CommDispatcher;
import com.tairui.server.device.core.SerialDispatcher;
import com.tairui.server.device.core.TcpClientDispatcher;
import com.tairui.server.device.channel.SerialChannel;
import com.tairui.server.device.channel.TcpClientChannel;
import com.tairui.server.device.qianMingLock.Controller;
import com.tairui.server.dto.CabinetFullDTO;
import com.tairui.server.service.CabinetConfigService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;



import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ReturnWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private Controller lockController;

    @Autowired
    private CabinetConfigService cabinetConfigService;

    @Value("${lock.simulation.mode:true}")
    private boolean simulationMode;

    public ReturnWebSocketHandler() {
        lockController = new Controller();
    }

    @PostConstruct
    public void init() throws Exception {
        lockController.setSimulationMode(simulationMode);

        if (simulationMode) {
            System.out.println("硬件控制器初始化完成，模拟模式开启");
            return;
        }

        // 真实模式：获取第一个柜子的锁板配置
        List<CabinetFullDTO> cabinets = cabinetConfigService.getFullConfigList();
        if (cabinets == null || cabinets.isEmpty()) {
            throw new Exception("没有找到柜子配置，无法初始化硬件");
        }
        CabinetFullDTO firstCab = cabinets.get(0);
        String lockCommType = firstCab.getLockCommType();
        String lockCommPort = firstCab.getLockCommPort();

        if (lockCommType == null || lockCommPort == null) {
            throw new Exception("锁板通讯配置不完整，请检查柜子配置");
        }

        CommDispatcher dispatcher = null;
        if ("485".equalsIgnoreCase(lockCommType)) {
            String[] parts = lockCommPort.split("@");
            if (parts.length != 2) {
                throw new Exception("串口配置格式错误，应为 端口@波特率，如 com1@115200");
            }
            String portName = parts[0];
            int baudRate;
            try {
                baudRate = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                throw new Exception("波特率格式错误: " + parts[1]);
            }
            SerialChannel channel = new SerialChannel(portName, baudRate);
            dispatcher = new SerialDispatcher(channel);
        } else if ("TCP".equalsIgnoreCase(lockCommType)) {
            String[] parts = lockCommPort.split(":");
            if (parts.length != 2) {
                throw new Exception("TCP配置格式错误，应为 IP:端口，如 192.168.1.2:8456");
            }
            String host = parts[0];
            int port;
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                throw new Exception("端口号格式错误: " + parts[1]);
            }
            TcpClientChannel channel = new TcpClientChannel(host, port);
            dispatcher = new TcpClientDispatcher(channel);
        } else {
            throw new Exception("不支持的锁板通讯类型: " + lockCommType);
        }

        lockController.setCommDispatcher(dispatcher);
        lockController.open();
        System.out.println("硬件控制器初始化完成，真实模式，通讯方式: " + lockCommType + "，配置: " + lockCommPort);
    }

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
            success = lockController.openBoxSync(boxNo, 3000);
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

        boolean hasGoods;
        try {
            if (simulationMode) {
                hasGoods = (toolName != null && !toolName.isEmpty());
            } else {
                Controller.BoxGoodsData goodsData = lockController.queryGoodsStatusSync(3000);
                hasGoods = goodsData.hasGoods(boxNo);
            }
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

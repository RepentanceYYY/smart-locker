package com.tairui.server.webSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tairui.server.face.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Log4j2
public class FaceWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 创建单线程线程池
    private final ExecutorService faceDetectExecutor = Executors.newSingleThreadExecutor();

    @Autowired
    private IFaceServer faceServer;

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
        Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
        String type = (String) msg.get("type");
        Map<String, Object> data = (Map<String, Object>) msg.get("data");

        switch (type) {
            case "getActivationStatus":
                handleGetActivationStatus(session, data);
                break;
            case "activate":
                faceDetectExecutor.submit(() -> {
                    try {
                        handleActivate(session, data);
                    } catch (Exception e) {
                        log.error("单线程执行激活授权异常", e);
                    }
                });
                handleActivate(session, data);
                break;
            case "detectFace":
                faceDetectExecutor.submit(() -> {
                    try {
                        handleDetectFace(session, data);
                    } catch (Exception e) {
                        log.error("单线程执行人脸检测异常", e);
                    }
                });
                break;
            default:
                sendError(session, "未知消息类型: " + type);
        }
    }

    /**
     * 处理获取激活状态
     */
    private void handleGetActivationStatus(WebSocketSession session, Map<String, Object> data) throws IOException {
        // 示例：直接返回状态
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("activated", true);
        sendResponse(session, "getActivationStatus", 200, "success", resultData);
    }

    /**
     * 处理激活
     */
    private void handleActivate(WebSocketSession session, Map<String, Object> data) throws IOException {
        // 如果激活也是耗时且单线程的操作，也可以考虑移入线程池。这里先保持原样。
    }

    /**
     * 处理人脸检测
     */
    private void handleDetectFace(WebSocketSession session, Map<String, Object> data) throws IOException {

        if (!session.isOpen()) {
            return;
        }

        Object faceImageObj = data.get("faceImage");
        FaceImage faceImage = objectMapper.convertValue(faceImageObj, FaceImage.class);

        if (faceImage == null || !StringUtils.hasText(faceImage.getRgbBase64())) {
            sendResponse(session, "detectFace", 400, "图片帧未传输", null);
            return;
        }

        try {
            String url = faceServer.faceDetect(faceImage);

            Map<String, Object> resultData = new HashMap<>();
            resultData.put("url", url);

            if (session.isOpen()) {
                sendResponse(session, "detectFace", 200, "success", resultData);
            }
        } catch (Exception e) {
            log.error("人脸识别算法调用失败", e);
            if (session.isOpen()) {
                sendResponse(session, "detectFace", 500, e.getMessage(), null);
            }
        }
    }

    private void sendError(WebSocketSession session, String errorMsg) throws IOException {
        sendResponse(session, "error", 500, errorMsg, null);
    }

    private void sendResponse(WebSocketSession session, String type, int code, String message, Map<String, Object> data) throws IOException {
        // 规避单例并发时容易踩坑的 Map.of 传 null 问题
        Map<String, Object> response = new HashMap<>();
        response.put("type", type);
        response.put("code", code);
        response.put("message", message);
        response.put("data", data == null ? Map.of() : data);

        synchronized (session) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            }
        }
    }

    /**
     * 优雅销毁线程池
     */
    @PreDestroy
    public void destroy() {
        faceDetectExecutor.shutdown();
    }
}
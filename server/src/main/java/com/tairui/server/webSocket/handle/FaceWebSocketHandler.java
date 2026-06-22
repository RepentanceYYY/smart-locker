package com.tairui.server.webSocket.handle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tairui.server.face.*;
import com.tairui.server.webSocket.dto.WsRequest;
import com.tairui.server.webSocket.dto.WsResponse;
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
            case "getActivationStatus":
                faceDetectExecutor.submit(() -> {
                    try {
                        handleGetActivationStatus(session, wsRequest);
                    } catch (Exception e) {
                        log.error("单线程执行激活授权异常", e);
                    }
                });
                break;
            case "activate":
                faceDetectExecutor.submit(() -> {
                    try {
                        handleActivate(session, wsRequest);
                    } catch (Exception e) {
                        log.error("单线程执行激活授权异常", e);
                    }
                });
                break;
            case "detectFace":
                faceDetectExecutor.submit(() -> {
                    try {
                        handleDetectFace(session, wsRequest);
                    } catch (Exception e) {
                        log.error("单线程执行人脸检测异常", e);
                    }
                });
                break;
            default:
                wsResponse = WsResponse.fail(wsRequest.getAction(), 400, "未知消息类型");

        }

    }

    /**
     * 处理获取激活状态
     */
    private void handleGetActivationStatus(WebSocketSession session, WsRequest wsRequest) throws Exception {
        WsResponse activationStatus = faceServer.getActivationStatus(wsRequest);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(activationStatus)));
    }

    /**
     * 处理激活
     */
    private void handleActivate(WebSocketSession session, WsRequest wsRequest) throws IOException {

    }

    /**
     * 处理人脸检测
     */
    private void handleDetectFace(WebSocketSession session, WsRequest wsRequest) throws IOException {

        if (!session.isOpen()) {
            return;
        }
        FaceImage faceImage;
        try {
            faceImage = objectMapper.convertValue(wsRequest.getData(), FaceImage.class);
        } catch (Exception ex) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(WsResponse.fail(wsRequest.getAction(), 400, "人脸信息JSON格式错误"))));
            return;
        }

        if (faceImage == null || !StringUtils.hasText(faceImage.getRgbBase64())) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(WsResponse.fail(wsRequest.getAction(), 400, "RGB图片帧未传输"))));
            return;
        }
        if (Boolean.TRUE.equals(faceImage.getSilentLivenessEnabled()) && StringUtils.hasText(faceImage.getIrBase64())) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(WsResponse.fail(wsRequest.getAction(), 400, "当前已启动活体检测，但未传输IrBase64"))));
            return;
        }


        try {
            WsResponse wsResponse = faceServer.faceDetect(wsRequest);

            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(wsResponse)));
            }
        } catch (Exception e) {
            log.error("人脸识别算法调用失败", e);
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(WsResponse.fail(wsRequest.getAction(), 500, e.getMessage()))));
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
package com.tairui.server.webSocket;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ReturnWebSocketHandler returnWebSocketHandler;
    private final BorrowWebSocketHandler borrowWebSocketHandler;
    private final InventoryWebSocketHandler inventoryWebSocketHandler;
    private final DehumidifierWebSocketHandler dehumidifierWebSocketHandler;
    private final FaceWebSocketHandler faceWebSocketHandler;

    public WebSocketConfig(ReturnWebSocketHandler returnWebSocketHandler, BorrowWebSocketHandler borrowWebSocketHandler, InventoryWebSocketHandler inventoryWebSocketHandler, DehumidifierWebSocketHandler dehumidifierWebSocketHandler, FaceWebSocketHandler faceWebSocketHandler) {
        this.returnWebSocketHandler = returnWebSocketHandler;
        this.borrowWebSocketHandler = borrowWebSocketHandler;
        this.inventoryWebSocketHandler = inventoryWebSocketHandler;
        this.dehumidifierWebSocketHandler = dehumidifierWebSocketHandler;
        this.faceWebSocketHandler = faceWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(returnWebSocketHandler, "/ws/return")
                .addHandler(borrowWebSocketHandler, "/ws/borrow")
                .addHandler(inventoryWebSocketHandler, "/ws/inventory")
                .addHandler(dehumidifierWebSocketHandler, "/ws/dehumidifier")
                .addHandler(faceWebSocketHandler, "/ws/face")
                .setAllowedOrigins("*");
    }
    /**
     * 配置 WebSocket 传输参数（设置最大接收 10MB）
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();

        // 1. 设置文本消息的最大缓存大小（10MB）
        container.setMaxTextMessageBufferSize(10 * 1024 * 1024);

        // 2. 设置二进制消息的最大缓存大小（10MB，通常传人脸图片或文件是用二进制）
        container.setMaxBinaryMessageBufferSize(10 * 1024 * 1024);

        return container;
    }
}

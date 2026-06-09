package com.tairui.server.webSocket;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ReturnWebSocketHandler returnWebSocketHandler;
    private final BorrowWebSocketHandler borrowWebSocketHandler;
    private final InventoryWebSocketHandler inventoryWebSocketHandler;

    public WebSocketConfig(ReturnWebSocketHandler returnWebSocketHandler, BorrowWebSocketHandler borrowWebSocketHandler, InventoryWebSocketHandler inventoryWebSocketHandler) {
        this.returnWebSocketHandler = returnWebSocketHandler;
        this.borrowWebSocketHandler = borrowWebSocketHandler;
        this.inventoryWebSocketHandler = inventoryWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(returnWebSocketHandler, "/ws/return")
                .addHandler(borrowWebSocketHandler,"/ws/borrow")
                .addHandler(inventoryWebSocketHandler,"/ws/inventory")
                .setAllowedOrigins("*");
    }
}

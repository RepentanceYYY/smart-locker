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

    public WebSocketConfig(ReturnWebSocketHandler returnWebSocketHandler,BorrowWebSocketHandler borrowWebSocketHandler) {
        this.returnWebSocketHandler = returnWebSocketHandler;
        this.borrowWebSocketHandler = borrowWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(returnWebSocketHandler, "/ws/return")
                .addHandler(borrowWebSocketHandler,"/ws/borrow")
                .setAllowedOrigins("*");
    }
}

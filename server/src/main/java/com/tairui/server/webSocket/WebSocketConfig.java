package com.tairui.server.webSocket;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ReturnWebSocketHandler returnWebSocketHandler;

    public WebSocketConfig(ReturnWebSocketHandler returnWebSocketHandler) {
        this.returnWebSocketHandler = returnWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(returnWebSocketHandler, "/ws/return")
                .setAllowedOrigins("*");
    }
}

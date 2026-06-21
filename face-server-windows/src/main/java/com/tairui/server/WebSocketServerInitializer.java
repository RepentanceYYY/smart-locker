package com.tairui.server;

import com.tairui.config.ServerConfig;
import com.tairui.handler.BinaryFrameHandler;
import com.tairui.handler.TextFrameHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

public class WebSocketServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) {
        ServerConfig serverConfig = ServerConfig.getInstance();

        ch.pipeline().addLast(new HttpServerCodec());
        ch.pipeline().addLast(new HttpObjectAggregator(20 * 1024 * 1024));
        ch.pipeline().addLast(new WebSocketServerProtocolHandler(
                serverConfig.getWebSocketPath(), null, true, 15 * 1024 * 1024
        ));
        ch.pipeline().addLast(new WebSocketFrameAggregator(15 * 1024 * 1024));
        ch.pipeline().addLast(new TextFrameHandler());
        ch.pipeline().addLast(new BinaryFrameHandler());
    }
}

package com.tairui.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tairui.entity.WsRequest;
import com.tairui.entity.WsResponse;
import com.tairui.handler.manager.ConnectionManager;
import com.tairui.handler.manager.FaceApiManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import com.tairui.utils.JsonUtils;

public class TextFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) throws JsonProcessingException {
        System.out.println("channelRead0当前所在线程Id:" + Thread.currentThread().getId());
        String originalText = frame.text();
        WsRequest wsRequest;

        try {
            // 返序列号
            wsRequest = JsonUtils.MAPPER.readValue(originalText, WsRequest.class);
        } catch (Exception e) {
            // JSON 非法，直接返回
            writeJson(ctx, WsResponse.fail("invalid", 400, "不支持的JSON格式"));
            return;
        }
        WsResponse wsResponse;
        switch (wsRequest.getAction()) {
            case "registerFaceIfNotExist":
                wsResponse = FaceHandler.registerFaceIfNotExist(wsRequest);
                break;
            case "activateSDK":
                wsResponse = FaceApiManager.activateSDK(wsRequest);
                break;
            case "queryActivationStatus":
                wsResponse = FaceApiManager.queryActivationStatus(wsRequest);
                break;
            default:
                wsResponse = WsResponse.fail(wsRequest.getAction(), 400, "不支持的action");
        }
        writeJson(ctx, wsResponse);

    }

    private void writeJson(ChannelHandlerContext ctx, Object obj) {
        try {
            String json = JsonUtils.MAPPER.writeValueAsString(obj);
            ctx.writeAndFlush(new TextWebSocketFrame(json));
        } catch (Exception e) {
            ctx.writeAndFlush(new TextWebSocketFrame("{\"code\":500,\"message\":\"serialize error\"}"));
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("客户端连接：" + ctx.channel().id());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("客户端断开：" + ctx.channel().id());
        ConnectionManager.all().forEach(ch -> {
            if (ch == ctx.channel()) {
                ConnectionManager.remove(ch.id().asLongText());
            }
        });
    }

}

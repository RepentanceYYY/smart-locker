package com.tairui.server.face;

import com.tairui.server.webSocket.dto.WsRequest;
import com.tairui.server.webSocket.dto.WsResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Log4j2
@ConditionalOnProperty(name = "face-detect-provider", havingValue = "ArcSoft")
public class ArcSoftFaceServer implements IFaceServer {
    @Override
    public WsResponse faceDetect(WsRequest wsRequest) throws Exception {
        return WsResponse.success(wsRequest.getAction(),null);
    }

    @Override
    public WsResponse activate(WsRequest wsRequest) throws Exception {
        return WsResponse.success(wsRequest.getAction(),null);
    }

    @Override
    public WsResponse getActivationStatus(WsRequest wsRequest) throws Exception {
        return WsResponse.success(wsRequest.getAction(),null);
    }

    @Override
    public WsResponse getBaiDuActivationStatus(WsRequest wsRequest) throws Exception {
        return WsResponse.success(wsRequest.getAction(),null);
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void destroy() {

    }
}

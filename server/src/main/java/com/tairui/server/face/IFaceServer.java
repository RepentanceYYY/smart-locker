package com.tairui.server.face;

import com.tairui.server.webSocket.dto.WsRequest;
import com.tairui.server.webSocket.dto.WsResponse;

public interface IFaceServer {

    public WsResponse faceDetect(WsRequest wsRequest) throws Exception;

    public WsResponse activate(WsRequest wsRequest)throws Exception;

    public WsResponse getActivationStatus(WsRequest wsRequest)throws Exception;

    public WsResponse getBaiDuActivationStatus(WsRequest wsRequest)throws Exception;

    public void start()throws Exception;
    public void destroy();
}

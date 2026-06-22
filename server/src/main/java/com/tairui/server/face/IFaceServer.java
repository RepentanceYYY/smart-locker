package com.tairui.server.face;

import java.util.Map;

public interface IFaceServer {

    public String faceDetect(FaceImage faceImage) throws Exception;

    public void activate(String sdkKey)throws Exception;

    public void activate(String appId, String sdkKey)throws Exception;

    public Map<String, String> getActivationStatus()throws Exception;

    public String getBaiDuActivationStatus()throws Exception;

    public void start()throws Exception;
}

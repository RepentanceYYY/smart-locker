package com.tairui.server.face;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Log4j2
@ConditionalOnProperty(name = "face-detect-provider", havingValue = "ArcSoft")
public class ArcSoftFaceServer implements IFaceServer {
    @Override
    public String faceDetect(FaceImage faceImage) throws Exception {
        return "";
    }

    @Override
    public void activate(String sdkKey) throws Exception {

    }

    @Override
    public void activate(String appId, String sdkKey) throws Exception {

    }

    @Override
    public Map<String, String> getActivationStatus() throws Exception {
        return Map.of();
    }

    @Override
    public String getBaiDuActivationStatus() throws Exception {
        return "";
    }

    @Override
    public void start() throws Exception {

    }
}

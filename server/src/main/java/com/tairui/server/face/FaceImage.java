package com.tairui.server.face;

import lombok.Data;

@Data
public class FaceImage {
    /**
     * 可见光base64
     */
    private String rgbBase64;
    /**
     * 红外base64
     */
    private String irBase64;
    /**
     * 是否启用了静默活体检测
     */
    private Boolean silentLivenessEnabled;
}

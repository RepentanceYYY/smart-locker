package com.tairui.server.face;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "face-threshold-config")
/**
 * 人脸检测阈值配置
 */
public class FaceThresholdConfig {
    /**
     * 最低活体置信度
     */
    private float liveScoreMin = 0.3f;
    /**
     * 最低嘴巴闭合置信度
     */
    private float mouthCloseScoreMin = 0.8f;
    /**
     * 最高闭眼置信度
     */
    private float eyeCloseMax = 0.1f;
    /**
     * 最高模糊置信度
     */
    private float blurMax = 0.25f;
    /**
     * 人脸识别最低相似度
     */
    private double similarity = 80d;
    /**
     * 最高面部遮挡置信度
     */
    private float faceOcclusion = 0.6f;
    /**
     * 最高左右偏头置信度
     */
    private  float headYamPoseMax = 20f;
    /**
     * 最高抬头低头置信度
     */
    private float headPitchPoseMax = 10f;
}

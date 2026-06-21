package com.tairui.entity.baidu;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@Data
public class FaceRecognitionResponse {

    /**
     * 是否出现异常
     */
    private int errno;

    /**
     * 消息
     */
    private String msg;

    /**
     * 检测结果
     */
    private FaceRecognitionData data;

    @Data
    public static class FaceRecognitionData {

        /**
         * 人脸 token
         */
        @JsonProperty("face_token")
        private String faceToken;

        @JsonProperty("log_id")
        private String logId;

        /**
         * 识别结果数量
         */
        @JsonProperty("result_num")
        private int resultNum;

        /**
         * 结果数组
         */
        private List<FaceRecognitionResult> result;
    }

    @Data
    public static class FaceRecognitionResult {

        /**
         * 组id
         */
        @JsonProperty("group_id")
        private String groupId;

        /**
         * 用户id（账号）
         */
        @JsonProperty("user_id")
        private String userId;

        /**
         * 相似得分(百分制)
         */
        private double score;
    }
}

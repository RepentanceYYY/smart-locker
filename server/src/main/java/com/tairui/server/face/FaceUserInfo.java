package com.tairui.server.face;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class FaceUserInfo {
    /**
     * 是否错误
     */
    private int errno;
    /**
     * 消息
     */
    private String msg;
    /**
     * 数据
     */
    private FaceUserInfoData data;

    /**
     * data 字段内部类
     */
    @Data
    public static class FaceUserInfoData {
        /**
         * 请求日志标识
         */
        @JsonProperty("log_id")
        private String logId;

        /**
         * 识别结果列表
         */
        private List<Object> result;

        /**
         * 组 id
         */
        @JsonProperty("group_id")
        private String groupId;

        /**
         * 人脸特征的唯一标识
         */
        @JsonProperty("face_token")
        private String faceToken;

        /**
         * 用户信息
         */
        @JsonProperty("user_info")
        private String userInfo;

        /**
         * 人脸首次注册时间
         */
        @JsonProperty("create_time")
        private String createTime;
    }

}

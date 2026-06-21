package com.tairui.entity.baidu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 人脸用户信息获取响应
 */
@Data
public class FaceUserResponse {
    private ResponseData data;
    private int errno;
    private String msg;

    @Data
    public static class ResponseData {
        @JsonProperty("log_id")
        private String logId;
        private List<Result> result = new ArrayList<>();

        @Data
        public static class Result {
            @JsonProperty("create_time")
            private String createTime;
            @JsonProperty("face_token")
            private String faceToken;
            @JsonProperty("group_id")
            private String groupId;
            @JsonProperty("user_info")
            private String userInfo;
        }
    }
}

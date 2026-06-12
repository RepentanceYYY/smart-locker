package com.tairui.server.dto;

import lombok.Data;

@Data
public class SystemConfigDTO {

    private String systemName;

    private String engName;

    private String systemCode;

    private String location;

    private String adminPwd;

    private String borrowPeriod;

    private Integer autoReturnTimeoutMinutes;

    private Integer tempHumidityLogInterval;

    private Integer enableFaceCapture;
}

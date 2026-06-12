package com.tairui.server.dto;

import lombok.Data;

/**
 * 温湿度日志数据传输对象
 */
@Data
public class TempHumidityLogDTO {
    /**
     * 柜子名称
     */
    private String cabinetTitle;

    /**
     * 温度（实际值，如 25.5）
     */
    private String temperature;

    /**
     * 湿度（实际值，如 65.0）
     */
    private String humidity;

    /**
     * 记录时间
     */
    private String recordTime;
}

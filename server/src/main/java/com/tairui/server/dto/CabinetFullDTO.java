// 文件位置：com/tairui/server/dto/CabinetFullDTO.java
package com.tairui.server.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CabinetFullDTO {
    private Integer id;
    private String title;
    private String width;
    private String height;
    private Boolean isDefault;          // 前端需要布尔值
    private String dehumidifierCommType;
    private String dehumidifierCommPort;
    private String dehumidifierAddr;
    private BigDecimal humidityMax;
    private BigDecimal humidityMin;
    private BigDecimal temperatureMax;
    private BigDecimal temperatureMin;
    private String lockCommType;
    private String lockCommPort;
    private String lockBoardAddr;
    private List<RowConfig> rows;       // 行配置列表

    @Data
    public static class RowConfig {
        private List<CellConfigDTO> cells;
    }

    @Data
    public static class CellConfigDTO {

        private Integer id;
        private String type;             // "cell" 或 "image"

        private Integer rowNum;
        private String columns;
        private String height;
        private Integer colSpan;
        private Integer rowSpan;
        // 普通格子字段
        private String number;
        private String toolName;
        private Boolean isEmpty;
        // 图片格子字段
        private String imageUrl;
        private String label;
        private String macAddress;

        private String  qrcodeContent;
    }
}

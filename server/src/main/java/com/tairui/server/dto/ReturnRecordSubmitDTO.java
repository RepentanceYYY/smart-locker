package com.tairui.server.dto;

import lombok.Data;

import java.util.List;

/**
 * @author Yhs
 * @date 2026/6/50249
 */
@Data
public class ReturnRecordSubmitDTO {
    private List<ReturnItemDTO> returnItems;
    private String returnerName;
    private String returnerNumber;
    private String remark;
    private String returnPhoto;

    @Data
    public static class ReturnItemDTO {
        private Integer cabinetId;
        private String cabinetName;
        private Integer cellId;
        private String cellNumber;
        private String toolName;
        private String returnTime;   // 格式: "yyyy-MM-dd HH:mm:ss" 或 ISO
    }
}

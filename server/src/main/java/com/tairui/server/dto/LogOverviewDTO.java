package com.tairui.server.dto;

import lombok.Data;
import java.util.List;

/**
 * 日志概览数据传输对象（包含内部类未归还记录项）
 */
@Data
public class LogOverviewDTO {
    private Long totalLogs;                           // 日志总数
    private Long unreturnedCount;                     // 未归还数量
    private List<UnreturnedItemDTO> unreturnedList;   // 未归还记录列表

    /**
     * 未归还记录项
     */
    @Data
    public static class UnreturnedItemDTO {
        private String cabinetTitle;    // 柜子名称
        private Integer cellNumber;     // 格口号
        private String toolName;        // 工具名称
        private String borrowerPhoto;   // 借用图片
        private String borrowTime;      // 借用时间
    }
}

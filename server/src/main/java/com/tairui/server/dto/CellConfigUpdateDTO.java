package com.tairui.server.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class CellConfigUpdateDTO {

    private Integer id;

    @NotNull(message = "柜子ID不能为空")
    private Integer cabinetId;

    @NotNull(message = "行号不能为空")
    @Min(value = 1, message = "行号必须大于0")
    private Integer rowNum;

    @NotNull(message = "类型不能为空")
    private String type; // "cell" 或 "image"

    @NotNull(message = "列宽配置不能为空")
    private String columns;

    @NotNull(message = "行高配置不能为空")
    private String height;

    @NotNull(message = "跨列数不能为空")
    @Min(value = 1, message = "跨列数至少为1")
    private Integer colSpan;

    @NotNull(message = "跨行数不能为空")
    @Min(value = 1, message = "跨行数至少为1")
    private Integer rowSpan;

    // 以下为普通格子字段（可为空）
    private Integer number;          // 格口号
    private String toolName;         // 工具名称
    private String isEmpty;          // 是否空闲（true/false）
    private String qrcodeContent;    // 二维码内容

    // 图片格子字段（可为空）
    private String imageUrl;
    private String label;

    // 其他
    private String macAddress;
}

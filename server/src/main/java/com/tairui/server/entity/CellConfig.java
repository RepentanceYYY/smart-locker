package com.tairui.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * <p>
 * 格口配置
 * </p>
 *
 * @author system
 * @since 2026-05-19
 */
@Getter
@Setter
@ToString
@TableName("cell_config")
public class CellConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 所属柜子
     */
    @TableField("cabinet_id")
    private Integer cabinetId;

    /**
     * 第几行
     */
    @TableField("row_num")
    private Integer rowNum;

    /**
     * 类型 cell格口/image 图片
     */
    @TableField("type")
    private String type;

    /**
     * 单元格占据的列宽 1fr
     */
    @TableField("columns")
    private String columns;

    /**
     * 单元格占据的行高 '60px'
     */
    @TableField("height")
    private String height;

    /**
     * 跨越的列数（整数）
     */
    @TableField("colSpan")
    private Integer colSpan;

    /**
     * 跨越的行数（整数）
     */
    @TableField("rowSpan")
    private Integer rowSpan;

    /**
     * 普通格子 (type='cell') 额外字段：格子编号
     */
    @TableField("number")
    private Integer number;

    /**
     * 普通格子 (type='cell') 额外字段：工具名称
     */
    @TableField("toolName")
    private String toolName;

    /**
     * 普通格子 (type='cell') 额外字段：是否空格子
     */
    @TableField("isEmpty")
    private String isEmpty;

    /**
     * 图片格子 (type='image') 额外字段：图片地址
     */
    @TableField("imageUrl")
    private String imageUrl;

    /**
     * 图片格子 (type='image') 额外字段：图片标签
     */
    @TableField("label")
    private String label;

    /**
     * 硬件地址 (mac_address)
     */
    @TableField("mac_address")
    private String macAddress;

    /**
     * 二维码内容 (qrcode_content)
     */
    @TableField("qrcode_content")
    private String  qrcodeContent;

}

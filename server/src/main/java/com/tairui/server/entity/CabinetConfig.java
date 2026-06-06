package com.tairui.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * <p>
 * 柜子配置
 * </p>
 *
 * @author system
 * @since 2026-05-19
 */
@Getter
@Setter
@ToString
@TableName("cabinet_config")
public class CabinetConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 柜子名称
     */
    @TableField("title")
    private String title;

    /**
     * 页面显示的柜子宽度默认270px
     */
    @TableField("width")
    private String width;

    /**
     * 柜子高度 默认auto
     */
    @TableField("height")
    private String height;

    /**
     * 这个柜子将作为初始展示  true/false
     */
    @TableField("isDefault")
    private String isDefault;

    /**
     * 通讯方式：485 / TCP
     */
    @TableField("dehumidifier_comm_type")
    private String dehumidifierCommType;

    /**
     * 通讯端口：485时为COM1等，TCP时为IP地址
     */
    @TableField("dehumidifier_comm_port")
    private String dehumidifierCommPort;

    /**
     * 除湿机地址
     */
    @TableField("dehumidifier_addr")
    private String dehumidifierAddr;

    /**
     * 湿度上限（%RH）
     */
    @TableField("humidity_max")
    private BigDecimal humidityMax;

    /**
     * 湿度下限（%RH）
     */
    @TableField("humidity_min")
    private BigDecimal humidityMin;

    /**
     * 温度上限（℃）
     */
    @TableField("temperature_max")
    private BigDecimal temperatureMax;

    /**
     * 温度下限（℃）
     */
    @TableField("temperature_min")
    private BigDecimal temperatureMin;

    /**
     * 锁板通讯方式 c485 或 TCP
     */
    @TableField("lock_comm_type")
    private String lockCommType;

    /**
     * 锁板通讯端口：485时为COM1等，TCP时为IP地址
     */
    @TableField("lock_comm_port")
    private String lockCommPort;

    /**
     * 锁板地址
     */
    @TableField("lock_board_addr")
    private String lockBoardAddr;
}

package com.tairui.server.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.math.BigDecimal;

/**
 * 柜子配置更新请求 DTO
 */
@Getter
@Setter
@ToString
public class CabinetUpdateDTO {

    /**
     * 柜子名称（唯一，不能为空）
     */
    private String title;

    /**
     * 页面显示的柜子宽度（如 280px, 320px, auto）
     */
    private String width;

    /**
     * 柜子高度（如 auto, 500px）
     */
    private String height;

    /**
     * 是否作为默认展示柜子
     */
    private Boolean isDefault;

    /**
     * 通讯方式：485 / TCP
     */
    private String dehumidifierCommType;

    /**
     * 通讯端口：485时为COM1等，TCP时为IP地址
     */
    private String dehumidifierCommPort;

    /**
     * 除湿机地址
     */
    private String dehumidifierAddr;

    /**
     * 湿度上限（%RH）
     */
    private BigDecimal humidityMax;

    /**
     * 湿度下限（%RH）
     */
    private BigDecimal humidityMin;

    /**
     * 温度上限（℃）
     */
    private BigDecimal temperatureMax;

    /**
     * 温度下限（℃）
     */
    private BigDecimal temperatureMin;

    /**
     * 锁板通讯方式 c485 或 TCP
     */
    private String lockCommType;

    /**
     * 锁板通讯端口：485时为COM1等，TCP时为IP地址
     */
    private String lockCommPort;

    /**
     * 锁板地址
     */
    private String lockBoardAddr;
}

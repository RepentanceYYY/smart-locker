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
 * 系统配置表
 * </p>
 *
 * @author system
 * @since 2026-06-02
 */
@Getter
@Setter
@ToString
@TableName("system_config")
public class SystemConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 系统名称
     */
    @TableField("system_name")
    private String systemName;

    /**
     * 英文名称
     */
    @TableField("eng_name")
    private String engName;

    /**
     * 系统编号
     */
    @TableField("system_code")
    private String systemCode;

    /**
     * 所属位置
     */
    @TableField("location")
    private String location;

    /**
     * 管理密码（建议存储哈希值）
     */
    @TableField("admin_pwd")
    private String adminPwd;

    /**
     * 使用周期（如：30天）
     */
    @TableField("borrow_period")
    private String borrowPeriod;

    /**
     * 长时间不操作返回主页（分钟）
     */
    @TableField("auto_return_timeout_minutes")
    private Integer autoReturnTimeoutMinutes;

    /**
     * 长温湿度记录间隔（分钟）
     */
    @TableField("temp_humidity_log_interval")
    private Integer tempHumidityLogInterval;

    /**
     * 是否开启抓拍人脸：0-关闭，1-开启';
     */
    @TableField("enable_face_capture")
    private Integer enableFaceCapture;


}

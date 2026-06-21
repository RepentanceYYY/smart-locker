package com.tairui.entity.db;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppSystemConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Integer id;

    /**
     * 系统名称
     */
    private String systemName;

    /**
     * 英文名称
     */
    private String engName;

    /**
     * 系统编号
     */
    private String systemCode;

    /**
     * 所属位置
     */
    private String location;

    /**
     * 管理密码
     */
    private String adminPwd;

    /**
     * 使用周期（如：30天）
     */
    private String borrowPeriod;

    /**
     * 长时间不操作返回主页（分钟）
     */
    private Integer autoReturnTimeoutMinutes;

    /**
     * 长温湿度记录间隔（分钟）
     */
    private Integer tempHumidityLogInterval;

    /**
     * 是否开启抓拍人脸：0-关闭，1-开启
     */
    private Boolean enableFaceCapture;

    /**
     * 是否启用静默活体检测
     */
    private Boolean silentLivenessEnabled;

    /**
     * 百度人脸SDK授权码
     */
    private String baiduFaceLicenseKey;
}

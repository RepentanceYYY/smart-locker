package com.tairui.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author system
 * @since 2026-06-11
 */
@Getter
@Setter
@ToString
@TableName("th_history")
public class ThHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 所属柜子
     */
    @TableField("cabinet_id")
    private Integer cabinetId;

    /**
     * 柜子名称
     */
    @TableField("cabinet_title")
    private String cabinetTitle;

    /**
     * 温度
     */
    @TableField("temperature")
    private String temperature;

    /**
     * 湿度
     */
    @TableField("humidity")
    private String humidity;

    /**
     * 记录入库时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}

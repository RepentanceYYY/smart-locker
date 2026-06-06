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
 * 日志表
 * </p>
 *
 * @author system
 * @since 2026-05-22
 */
@Getter
@Setter
@ToString
@TableName("sys_oper_log")
public class SysOperLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 柜子ID
     */
    @TableField("cabinet_id")
    private Integer cabinetId;

    /**
     * 柜子名称
     */
    @TableField("cabinet_title")
    private String cabinetTitle;

    /**
     * 格口id
     */
    @TableField("cell_id")
    private Integer cellId;

    /**
     * 格口编号
     */
    @TableField("cell_number")
    private Integer cellNumber;

    /**
     * 工具名称
     */
    @TableField("tool_name")
    private String toolName;

    /**
     * 领用照片
     */
    @TableField("borrower_photo")
    private String borrowerPhoto;

    /**
     * 领用人姓名
     */
    @TableField("borrower_name")
    private String borrowerName;

    /**
     * 工号/卡号
     */
    @TableField("borrower_number")
    private String borrowerNumber;

    /**
     * 领用时间
     */
    @TableField("borrow_time")
    private LocalDateTime borrowTime;

    /**
     * 领用说明
     */
    @TableField("borrow_remark")
    private String borrowRemark;

    /**
     * 归还照片
     */
    @TableField("return_photo")
    private String returnPhoto;

    /**
     * 领用人姓名
     */
    @TableField("return_name")
    private String returnName;

    /**
     * 工号/卡号
     */
    @TableField("return_number")
    private String returnNumber;

    /**
     * 归还时间
     */
    @TableField("return_time")
    private LocalDateTime returnTime;

    /**
     * 归还说明
     */
    @TableField("return_remark")
    private String returnRemark;

    /**
     * 预计归还时间
     */
    @TableField("expected_return_time")
    private LocalDateTime expectedReturnTime;
}

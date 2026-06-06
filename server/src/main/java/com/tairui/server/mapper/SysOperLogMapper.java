package com.tairui.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tairui.server.entity.SysOperLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 日志表 Mapper 接口
 * </p>
 *
 * @author system
 * @since 2026-05-22
 */
@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {

    void truncateTable();

}

package com.tairui.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tairui.server.entity.CabinetConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 柜子配置 Mapper 接口
 * </p>
 *
 * @author system
 * @since 2026-05-19
 */
@Mapper
public interface CabinetConfigMapper extends BaseMapper<CabinetConfig> {

    void truncateTable();
}

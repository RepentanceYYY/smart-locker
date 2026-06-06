package com.tairui.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tairui.server.entity.CellConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 格口配置 Mapper 接口
 * </p>
 *
 * @author system
 * @since 2026-05-19
 */
@Mapper
public interface CellConfigMapper extends BaseMapper<CellConfig> {

    void truncateTable();
}

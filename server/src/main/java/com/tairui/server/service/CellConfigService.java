package com.tairui.server.service;

import com.tairui.server.dto.CellConfigUpdateDTO;
import com.tairui.server.entity.CellConfig;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 格口配置 服务类
 * </p>
 *
 * @author system
 * @since 2026-05-19
 */
public interface CellConfigService extends IService<CellConfig> {
    /**
     * 将指定格口标记为空闲（isEmpty = true）
     * @param cellId 格口ID
     */
    void updateCellEmpty(Long cellId, String isEmpty);

    /**
     * 根据 DTO 更新格口配置（包含唯一性校验）
     * @param dto 更新参数
     */
    void updateCell(CellConfigUpdateDTO dto);

    /**
     * 新增格口配置
     * @param dto 新增参数
     */
    void addCell(CellConfigUpdateDTO dto);

    /**
     * 删除格口配置（根据ID）
     * @param id 格口ID
     */
    void deleteCell(Integer id);

    void truncateCellTable();
}

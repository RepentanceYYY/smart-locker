package com.tairui.server.service;

import com.tairui.server.dto.CabinetFullDTO;
import com.tairui.server.dto.CabinetUpdateDTO;
import com.tairui.server.entity.CabinetConfig;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 柜子配置 服务类
 * </p>
 *
 * @author system
 * @since 2026-05-19
 */
public interface CabinetConfigService extends IService<CabinetConfig> {
    List<CabinetFullDTO> getFullConfigList();   // 获取所有柜子完整配置

    void updateCabinet(Integer id, CabinetUpdateDTO updateDTO);

    void createCabinet(CabinetUpdateDTO createDTO);

    void deleteCabinet(Integer id);

    void truncateCabinetTable();
}

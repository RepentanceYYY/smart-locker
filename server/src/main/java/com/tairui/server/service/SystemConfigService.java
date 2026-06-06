package com.tairui.server.service;

import com.tairui.server.dto.SystemConfigDTO;
import com.tairui.server.entity.SystemConfig;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 系统配置表 服务类
 * </p>
 *
 * @author system
 * @since 2026-06-02
 */
public interface SystemConfigService extends IService<SystemConfig> {

    SystemConfigDTO getConfig();

    void updateConfig(SystemConfigDTO dto);

    void resetConfig();

}

package com.tairui.server.service.impl;

import com.tairui.server.dto.SystemConfigDTO;
import com.tairui.server.entity.SystemConfig;
import com.tairui.server.mapper.CabinetConfigMapper;
import com.tairui.server.mapper.CellConfigMapper;
import com.tairui.server.mapper.SysOperLogMapper;
import com.tairui.server.mapper.SystemConfigMapper;
import com.tairui.server.service.SystemConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 系统配置表 服务实现类
 * </p>
 *
 * @author system
 * @since 2026-06-02
 */
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    @Autowired
    private CellConfigMapper cellConfigMapper;
    @Autowired
    private SysOperLogMapper sysOperLogMapper;
    @Autowired
    private CabinetConfigMapper cabinetConfigMapper;

    // 约定配置表中只有一条数据，id = 1
    private static final Integer CONFIG_ID = 1;
    @Override
    public SystemConfigDTO getConfig() {
        SystemConfig config = baseMapper.selectById(CONFIG_ID);
        if (config == null) {
            config = createDefaultConfig();
            baseMapper.insert(config);
        }
        SystemConfigDTO dto = new SystemConfigDTO();
        BeanUtils.copyProperties(config, dto);
        return dto;
    }

    @Override
    public void updateConfig(SystemConfigDTO dto) {
        SystemConfig config = baseMapper.selectById(CONFIG_ID);
        if (config == null) {
            config = createDefaultConfig();
        }
        // 将 dto 的非空字段拷贝到 entity（可根据需求控制）
        BeanUtils.copyProperties(dto, config);
        config.setId(CONFIG_ID);
        baseMapper.insertOrUpdate(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetConfig() {
        // 先重置配置（事务保护）
        SystemConfig defaultConfig = createDefaultConfig();
        defaultConfig.setId(CONFIG_ID);
        baseMapper.insertOrUpdate(defaultConfig);
        cabinetConfigMapper.truncateTable();
        cellConfigMapper.truncateTable();
        sysOperLogMapper.truncateTable();
    }
    private SystemConfig createDefaultConfig() {
        SystemConfig config = new SystemConfig();
        config.setSystemName("智能工具柜系统");
        config.setEngName("Smart Cabinet System");
        config.setSystemCode("SC-001");
        config.setLocation("A区 工具库");
        config.setAdminPwd("admin");
        config.setBorrowPeriod("7");
        config.setAutoReturnTimeoutMinutes(5);
        config.setTempHumidityLogInterval(5);
        return config;
    }

}

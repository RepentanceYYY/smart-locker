package com.tairui.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tairui.server.common.exception.ClientException;
import com.tairui.server.common.exception.ServerException;
import com.tairui.server.deviceService.DehumidifierDeviceService;
import com.tairui.server.deviceService.DehumidifierDeviceServiceManager;
import com.tairui.server.deviceService.QianMingLockDeviceService;
import com.tairui.server.deviceService.QianMingLockDeviceServiceManager;
import com.tairui.server.dto.CabinetFullDTO;
import com.tairui.server.dto.CabinetUpdateDTO;
import com.tairui.server.entity.CabinetConfig;
import com.tairui.server.entity.CellConfig;
import com.tairui.server.mapper.CabinetConfigMapper;
import com.tairui.server.service.CabinetConfigService;
import com.tairui.server.service.CellConfigService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
public class CabinetConfigServiceImpl extends ServiceImpl<CabinetConfigMapper, CabinetConfig> implements CabinetConfigService {

    @Autowired
    private CellConfigService cellConfigService;

    @Autowired
    private CabinetConfigMapper cabinetConfigMapper;

    @Autowired
    private QianMingLockDeviceServiceManager qianMingLockDeviceServiceManager;

    @Autowired
    private DehumidifierDeviceServiceManager dehumidifierDeviceServiceManager;

    @Override
    public List<CabinetFullDTO> getFullConfigList() {
        // 1. 查询所有柜子
        List<CabinetConfig> cabinets = this.list();
        if (cabinets.isEmpty()) return Collections.emptyList();

        // 2. 批量查询所有单元格，按柜子ID分组
        LambdaQueryWrapper<CellConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(CellConfig::getCabinetId, CellConfig::getRowNum, CellConfig::getId);
        List<CellConfig> allCells = cellConfigService.list(wrapper);
        Map<Integer, List<CellConfig>> cellGroupMap = allCells.stream()
                .collect(Collectors.groupingBy(CellConfig::getCabinetId));

        // 3. 组装完整 DTO
        List<CabinetFullDTO> result = new ArrayList<>();
        for (CabinetConfig cab : cabinets) {
            CabinetFullDTO dto = new CabinetFullDTO();
            BeanUtil.copyProperties(cab, dto);
            // 转换 isDefault（数据库存储 "true"/"false" 字符串）
            dto.setIsDefault("true".equalsIgnoreCase(cab.getIsDefault()));
            // 处理宽度高度默认值
            if (!StringUtils.hasText(dto.getWidth())) dto.setWidth("300px");
            if (!StringUtils.hasText(dto.getHeight())) dto.setHeight("auto");

            // 获取当前柜子的单元格列表，并按 row_num 分组
            List<CellConfig> cellsOfCab = cellGroupMap.getOrDefault(cab.getId(), Collections.emptyList());
            Map<Integer, List<CellConfig>> rowMap = cellsOfCab.stream()
                    .collect(Collectors.groupingBy(CellConfig::getRowNum, TreeMap::new, Collectors.toList()));

            // 构建 rows
            List<CabinetFullDTO.RowConfig> rows = new ArrayList<>();
            for (List<CellConfig> rowCells : rowMap.values()) {
                CabinetFullDTO.RowConfig rowConfig = new CabinetFullDTO.RowConfig();
                List<CabinetFullDTO.CellConfigDTO> cellDTOs = new ArrayList<>();
                for (CellConfig cell : rowCells) {
                    CabinetFullDTO.CellConfigDTO cellDTO = new CabinetFullDTO.CellConfigDTO();
                    BeanUtil.copyProperties(cell, cellDTO);
                    // 类型判断
                    cellDTO.setType("image".equals(cell.getType()) ? "image" : "cell");
                    // 转换 isEmpty
                    if (cellDTO.getType().equals("cell")) {
                        cellDTO.setIsEmpty("true".equalsIgnoreCase(cell.getIsEmpty()));
                    }
                    // number 可能为整数，前端需要字符串
                    if (cell.getNumber() != null) cellDTO.setNumber(String.valueOf(cell.getNumber()));
                    cellDTOs.add(cellDTO);
                }
                rowConfig.setCells(cellDTOs);
                rows.add(rowConfig);
            }
            dto.setRows(rows);
            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCabinet(Integer id, CabinetUpdateDTO updateDTO) {
        // 检查柜子是否存在
        CabinetConfig existing = this.getById(id);
        if (existing == null) {
            throw new ClientException("柜子不存在，id=" + id);
        }

        // 保存旧的端口信息，用于后续比对和硬件回滚
        String oldLockCommPort = existing.getLockCommPort();
        String oldDehumidifierCommPort = existing.getDehumidifierCommPort();

        // 检查名称及设备的唯一性（排除自身）
        if (updateDTO.getTitle() != null && !updateDTO.getTitle().equals(existing.getTitle())) {
            LambdaQueryWrapper<CabinetConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CabinetConfig::getTitle, updateDTO.getTitle());
            if (this.count(wrapper) > 0) {
                throw new ClientException("柜子名称已存在，请使用唯一名称");
            }
        }

        // 检查新除湿机端口+地址是否与别的柜子冲突
        String targetDehumidifierPort = updateDTO.getDehumidifierCommPort() != null ? updateDTO.getDehumidifierCommPort() : existing.getDehumidifierCommPort();
        String targetDehumidifierAddr = updateDTO.getDehumidifierAddr() != null ? updateDTO.getDehumidifierAddr() : existing.getDehumidifierAddr();
        if (!targetDehumidifierPort.equals(oldDehumidifierCommPort) || !targetDehumidifierAddr.equals(existing.getDehumidifierAddr())) {
            LambdaQueryWrapper<CabinetConfig> dehumidifierWrapper = new LambdaQueryWrapper<>();
            dehumidifierWrapper.ne(CabinetConfig::getId, id)
                    .eq(CabinetConfig::getDehumidifierCommPort, targetDehumidifierPort)
                    .eq(CabinetConfig::getDehumidifierAddr, targetDehumidifierAddr);
            if (this.count(dehumidifierWrapper) > 0) {
                throw new ClientException(String.format("其他柜子已占用通信端口[%s]下的除湿机地址[%s]", targetDehumidifierPort, targetDehumidifierAddr));
            }
        }

        // 构建并执行数据库更新
        CabinetConfig updateEntity = new CabinetConfig();
        updateEntity.setId(id);
        if (updateDTO.getTitle() != null) updateEntity.setTitle(updateDTO.getTitle());
        if (updateDTO.getWidth() != null) updateEntity.setWidth(updateDTO.getWidth());
        if (updateDTO.getHeight() != null) updateEntity.setHeight(updateDTO.getHeight());
        if (updateDTO.getIsDefault() != null) {
            updateEntity.setIsDefault(updateDTO.getIsDefault() ? "true" : "false");
        }
        if (updateDTO.getDehumidifierCommType() != null)
            updateEntity.setDehumidifierCommType(updateDTO.getDehumidifierCommType());
        if (updateDTO.getDehumidifierCommPort() != null)
            updateEntity.setDehumidifierCommPort(updateDTO.getDehumidifierCommPort());
        if (updateDTO.getDehumidifierAddr() != null) updateEntity.setDehumidifierAddr(updateDTO.getDehumidifierAddr());
        if (updateDTO.getHumidityMax() != null) updateEntity.setHumidityMax(updateDTO.getHumidityMax());
        if (updateDTO.getHumidityMin() != null) updateEntity.setHumidityMin(updateDTO.getHumidityMin());
        if (updateDTO.getTemperatureMax() != null) updateEntity.setTemperatureMax(updateDTO.getTemperatureMax());
        if (updateDTO.getTemperatureMin() != null) updateEntity.setTemperatureMin(updateDTO.getTemperatureMin());
        if (updateDTO.getLockCommType() != null) updateEntity.setLockCommType(updateDTO.getLockCommType());
        if (updateDTO.getLockCommPort() != null) updateEntity.setLockCommPort(updateDTO.getLockCommPort());
        if (updateDTO.getLockBoardAddr() != null) updateEntity.setLockBoardAddr(updateDTO.getLockBoardAddr());

        boolean updated = this.updateById(updateEntity);
        if (!updated) {
            throw new ServerException("更新柜子配置失败");
        }

        // 默认柜子排他性更新
        if (updateDTO.getIsDefault() != null && updateDTO.getIsDefault()) {
            LambdaUpdateWrapper<CabinetConfig> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.ne(CabinetConfig::getId, id)
                    .eq(CabinetConfig::getIsDefault, "true")
                    .set(CabinetConfig::getIsDefault, "false");
            this.update(updateWrapper);
        }

        // 硬件连接平滑更新与回滚
        // 获取更新后的完整最新配置实体，用于传入硬件 ServiceManager
        CabinetConfig fullLatestEntity = this.getById(id);

        // 判断锁板和除湿机端口/设备地址是否发生改变
        boolean isLockPortChanged = updateDTO.getLockCommPort() != null && !updateDTO.getLockCommPort().equals(oldLockCommPort);

        // 除湿机：物理链路变更条件（端口变了，或者通信地址变了）
        boolean isDehumidifierLinkChanged = (updateDTO.getDehumidifierCommPort() != null && !updateDTO.getDehumidifierCommPort().equals(oldDehumidifierCommPort))
                || (updateDTO.getDehumidifierAddr() != null && !updateDTO.getDehumidifierAddr().equals(existing.getDehumidifierAddr()));

        // 除湿机：阈值变更条件（最大或最小湿度发生变化，且传入了新值）
        boolean isHumidityThresholdChanged = (updateDTO.getHumidityMax() != null && !updateDTO.getHumidityMax().equals(existing.getHumidityMax()))
                || (updateDTO.getHumidityMin() != null && !updateDTO.getHumidityMin().equals(existing.getHumidityMin()));

        // 状态记录：用于在 catch 中识别哪些“新动作”成功了，需要做逆向清除
        boolean newLockRegistered = false;
        boolean newDehumidifierRegistered = false;

        // 状态记录：用于暂存被断开的老设备实例，若新逻辑失败，需要将它们“死灰复燃”
        QianMingLockDeviceService oldLockServiceBackup = null;
        DehumidifierDeviceService oldDehumidifierServiceBackup = null;

        try {
            // ==========================================
            // 1. 锁板链路更新
            // ==========================================
            if (isLockPortChanged) {
                log.info("检测到锁板端口变更: {} -> {}", oldLockCommPort, fullLatestEntity.getLockCommPort());

                // 暂存并断开旧的连接（注意：为了防止新端口占用冲突，必须先断开老连接释放物理串口）
                if (oldLockCommPort != null) {
                    oldLockServiceBackup = qianMingLockDeviceServiceManager.getDeviceServiceMap().remove(oldLockCommPort);
                    if (oldLockServiceBackup != null) {
                        try {
                            oldLockServiceBackup.close();
                        } catch (Exception e) {
                            log.warn("断开老锁板连接时发生异常(忽略): ", e);
                        }
                    }
                }

                // 建立新连接
                QianMingLockDeviceService newLockService = qianMingLockDeviceServiceManager.addDeviceServiceByNewCabinetConfig(fullLatestEntity);
                if (newLockService.getCommDispatcher() != null && !qianMingLockDeviceServiceManager.getDeviceServiceMap().containsKey(fullLatestEntity.getLockCommPort())) {
                    newLockService.open();
                    qianMingLockDeviceServiceManager.getDeviceServiceMap().put(fullLatestEntity.getLockCommPort(), newLockService);
                    newLockRegistered = true;
                    log.info("{} 新锁板硬件连接成功并缓存", fullLatestEntity.getTitle());
                }
            }

            // ==========================================
            // 2. 除湿机链路与参数更新
            // ==========================================
            if (isDehumidifierLinkChanged) {
                // 【情况 A】通信端口或设备地址发生改变 -> 走原本的“重连并重置参数”逻辑
                log.info("检测到除湿机物理链路变更，重连硬件通道...");

                // 暂存并断开旧的连接
                if (oldDehumidifierCommPort != null) {
                    oldDehumidifierServiceBackup = dehumidifierDeviceServiceManager.getDeviceServiceMap().remove(oldDehumidifierCommPort);
                    if (oldDehumidifierServiceBackup != null) {
                        try {
                            oldDehumidifierServiceBackup.close();
                        } catch (Exception e) {
                            log.warn("断开老除湿机连接时发生异常(忽略): ", e);
                        }
                    }
                }

                // 建立新连接
                DehumidifierDeviceService newDehumidifierService = dehumidifierDeviceServiceManager.addDeviceServiceByNewCabinetConfig(fullLatestEntity);
                if (!dehumidifierDeviceServiceManager.getDeviceServiceMap().containsKey(fullLatestEntity.getDehumidifierCommPort())) {
                    newDehumidifierService.open();
                    // 使用最新数据库实体的值下发
                    newDehumidifierService.setHumidityControlStart(fullLatestEntity.getHumidityMax().intValue());
                    newDehumidifierService.setHumidityControlStop(fullLatestEntity.getHumidityMin().intValue());
                    dehumidifierDeviceServiceManager.getDeviceServiceMap().put(fullLatestEntity.getDehumidifierCommPort(), newDehumidifierService);
                    newDehumidifierRegistered = true;
                    log.info("{} 新除湿机硬件连接成功，并已同步新阈值并缓存", fullLatestEntity.getTitle());
                }
            } else if (isHumidityThresholdChanged) {
                // 【情况 B】链路没变，但温湿度阈值变了 -> 动态同步到当前运行中的硬件服务
                log.info("除湿机硬件链路未变，检测到湿度阈值变更，开始动态下发到硬件设备...");

                // 从缓存 Map 中获取当前正在运行的 Service 实例
                DehumidifierDeviceService currentService = dehumidifierDeviceServiceManager.getDeviceServiceMap().get(oldDehumidifierCommPort);
                if (currentService != null) {
                    // 直接调用硬件接口下发指令
                    currentService.setAddress(Integer.parseInt(fullLatestEntity.getDehumidifierAddr()));
                    currentService.setHumidityControlStart(fullLatestEntity.getHumidityMax().intValue());
                    currentService.setHumidityControlStop(fullLatestEntity.getHumidityMin().intValue());
                    log.info("{} 除湿机硬件参数动态同步成功。Start: {}, Stop: {}",
                            fullLatestEntity.getTitle(), fullLatestEntity.getHumidityMax(), fullLatestEntity.getHumidityMin());
                } else {
                    log.warn("未在缓存中找到端口 [{}] 对应的运行中除湿机实例，跳过动态参数同步", oldDehumidifierCommPort);
                }
            }

        } catch (Exception e) {
            log.error("==== 硬件链路更新失败！开始执行人工补偿机制（硬件回滚） ====");

            // 【新连接的清理】
            if (newLockRegistered) {
                try {
                    qianMingLockDeviceServiceManager.removeDeviceServiceByCommPort(fullLatestEntity.getLockCommPort());
                    log.warn("【更新回滚】已强制关闭并清理新锁板连接: {}", fullLatestEntity.getLockCommPort());
                } catch (Exception ex) {
                    log.error("回滚新锁板连接次生异常: ", ex);
                }
            }
            if (newDehumidifierRegistered) {
                try {
                    dehumidifierDeviceServiceManager.removeDeviceServiceByCommPort(fullLatestEntity.getDehumidifierCommPort());
                    log.warn("【更新回滚】已强制关闭并清理新除湿机连接: {}", fullLatestEntity.getDehumidifierCommPort());
                } catch (Exception ex) {
                    log.error("回滚新除湿机连接次生异常: ", ex);
                }
            }

            // 老连接的恢复
            if (isLockPortChanged && oldLockServiceBackup != null && oldLockCommPort != null) {
                try {
                    oldLockServiceBackup.open();
                    qianMingLockDeviceServiceManager.getDeviceServiceMap().put(oldLockCommPort, oldLockServiceBackup);
                    log.info("【更新回滚】已成功恢复原锁板物理连接及缓存: {}", oldLockCommPort);
                } catch (Exception ex) {
                    log.error("关键错误：恢复原锁板物理连接失败！老链路已断开！", ex);
                }
            }

            // 优化了除湿机回滚：只有在链路真正变化、引发了老链路断开时，才需要盲滚恢复老连接
            if (isDehumidifierLinkChanged && oldDehumidifierServiceBackup != null && oldDehumidifierCommPort != null) {
                try {
                    oldDehumidifierServiceBackup.open();
                    dehumidifierDeviceServiceManager.getDeviceServiceMap().put(oldDehumidifierCommPort, oldDehumidifierServiceBackup);
                    log.info("【更新回滚】已成功恢复原除湿机物理连接及缓存: {}", oldDehumidifierCommPort);
                } catch (Exception ex) {
                    log.error("关键错误：恢复原除湿机物理连接失败！老链路已断开！", ex);
                }
            }

            // 抛出异常触发 Spring 数据库回滚
            throw new ClientException("更新柜子硬件连接失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCabinet(CabinetUpdateDTO createDTO) {
        // 业务唯一性检查
        LambdaQueryWrapper<CabinetConfig> titleWrapper = new LambdaQueryWrapper<>();
        titleWrapper.eq(CabinetConfig::getTitle, createDTO.getTitle());
        if (this.count(titleWrapper) > 0) {
            throw new ClientException("柜子名称已存在，请使用唯一名称");
        }

        LambdaQueryWrapper<CabinetConfig> dehumidifierWrapper = new LambdaQueryWrapper<>();
        dehumidifierWrapper.eq(CabinetConfig::getDehumidifierCommPort, createDTO.getDehumidifierCommPort())
                .eq(CabinetConfig::getDehumidifierAddr, createDTO.getDehumidifierAddr());
        if (this.count(dehumidifierWrapper) > 0) {
            throw new ClientException(String.format("通信端口[%s]下已存在地址为[%s]的除湿机，不可重复添加",
                    createDTO.getDehumidifierCommPort(), createDTO.getDehumidifierAddr()));
        }

        // 构建并保存数据库
        CabinetConfig entity = new CabinetConfig();
        entity.setTitle(createDTO.getTitle());
        entity.setWidth(createDTO.getWidth());
        entity.setHeight(createDTO.getHeight());

        boolean isDefaultCabinet = createDTO.getIsDefault() != null && createDTO.getIsDefault();
        entity.setIsDefault(isDefaultCabinet ? "true" : "false");

        entity.setDehumidifierCommType(createDTO.getDehumidifierCommType());
        entity.setDehumidifierCommPort(createDTO.getDehumidifierCommPort());
        entity.setDehumidifierAddr(createDTO.getDehumidifierAddr());
        entity.setHumidityMax(createDTO.getHumidityMax());
        entity.setHumidityMin(createDTO.getHumidityMin());
        entity.setTemperatureMax(createDTO.getTemperatureMax());
        entity.setTemperatureMin(createDTO.getTemperatureMin());
        entity.setLockCommType(createDTO.getLockCommType());
        entity.setLockCommPort(createDTO.getLockCommPort());
        entity.setLockBoardAddr(createDTO.getLockBoardAddr());

        boolean saved = this.save(entity);
        if (!saved) {
            throw new ServerException("创建柜子失败，数据库保存异常");
        }

        if (isDefaultCabinet) {
            LambdaUpdateWrapper<CabinetConfig> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.ne(CabinetConfig::getId, entity.getId())
                    .eq(CabinetConfig::getIsDefault, "true")
                    .set(CabinetConfig::getIsDefault, "false");
            this.update(updateWrapper);
        }

        // 用于记录两端设备最终是否有成功执行过逻辑放入过 Map 缓存，以便回滚清理
        boolean lockRegistered = false;
        boolean dehumidifierRegistered = false;

        QianMingLockDeviceService lockService;
        DehumidifierDeviceService dehumidifierService;

        try {
            // 获取锁板纯净对象（此时未 open，未入 Map）
            lockService = qianMingLockDeviceServiceManager.addDeviceServiceByNewCabinetConfig(entity);

            // 尝试启动锁板连接
            try {
                if (lockService.getCommDispatcher() != null && !qianMingLockDeviceServiceManager.getDeviceServiceMap().containsKey(entity.getLockCommPort())) {
                    lockService.open();
                    qianMingLockDeviceServiceManager.getDeviceServiceMap().put(entity.getLockCommPort(), lockService);
                    lockRegistered = true; // 标记锁板已经成功注册并建立了连接
                    log.info("{} 锁板硬件连接成功，并已缓存", entity.getTitle());
                }
            } catch (Exception e) {
                throw new RuntimeException("锁板建立物理连接失败: " + e.getMessage());
            }

            try {
                dehumidifierService = dehumidifierDeviceServiceManager.addDeviceServiceByNewCabinetConfig(entity);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            // 尝试启动除湿机连接
            try {
                if (!dehumidifierDeviceServiceManager.getDeviceServiceMap().containsKey(entity.getDehumidifierCommPort())) {
                    dehumidifierService.open();
                    dehumidifierDeviceServiceManager.getDeviceServiceMap().put(entity.getDehumidifierCommPort(), dehumidifierService);
                    dehumidifierRegistered = true; // 标记除湿机已经成功注册并建立了连接
                    log.info("{} 除湿机硬件连接成功，并已缓存", entity.getTitle());
                }
            } catch (Exception e) {
                throw new RuntimeException("除湿机建立物理连接失败: " + e.getMessage());
            }
            // 尝试设置除湿机控湿开始停止值
            try {
                dehumidifierService.setAddress(Integer.parseInt(createDTO.getDehumidifierAddr()));
                dehumidifierService.setHumidityControlStart(createDTO.getHumidityMax().intValue());
            } catch (Exception e) {
                throw e;
            }
            try {
                dehumidifierService.setAddress(Integer.parseInt(createDTO.getDehumidifierAddr()));
                dehumidifierService.setHumidityControlStop(createDTO.getHumidityMin().intValue());
            } catch (Exception e) {
                throw e;
            }

        } catch (Exception e) {
            log.error("[createCabinet]:硬件链路建立失败！开始执行人工补偿机制（硬件回滚）");

            // 补偿机制：如果锁板刚才顺利注册成功了，但因为除湿机垮了，必须立刻把锁板断开并从缓存踢出！
            if (lockRegistered) {
                try {
                    qianMingLockDeviceServiceManager.removeDeviceServiceByCommPort(entity.getLockCommPort());
                    log.warn("[系统回滚]由于后续设备初始化失败，已强制关闭并清理锁板连接: {}", entity.getLockCommPort());
                } catch (Exception ex) {
                    log.error("回滚锁板连接时发生次生异常: ", ex);
                }
            }

            // 补偿机制：如果除湿机注册成功了
            if (dehumidifierRegistered) {
                try {
                    dehumidifierDeviceServiceManager.removeDeviceServiceByCommPort(entity.getDehumidifierCommPort());
                    log.warn("【系统回滚】由于后续设备初始化失败，已强制关闭并清理除湿机连接: {}", entity.getDehumidifierCommPort());
                } catch (Exception ex) {
                    log.error("回滚除湿机连接时发生次生异常: ", ex);
                }
            }

            // 重新抛出客户端能识别的异常，促使 Spring 触发数据库 @Transactional 回滚
            throw new ClientException("创建柜子失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCabinet(Integer id) {
        CabinetConfig cabinet = this.getById(id);
        if (cabinet == null) {
            throw new ClientException("柜子不存在，id=" + id);
        }

        // 删除该柜子下所有单元格
        LambdaQueryWrapper<CellConfig> cellWrapper = new LambdaQueryWrapper<>();
        cellWrapper.eq(CellConfig::getCabinetId, id);
        cellConfigService.remove(cellWrapper);

        // 删除柜子本身
        boolean removed = this.removeById(id);
        if (!removed) {
            throw new ServerException("删除柜子失败");
        }

        qianMingLockDeviceServiceManager.afterDeleteCabinetConfigData(cabinet.getId(), cabinet.getLockCommPort());
        dehumidifierDeviceServiceManager.afterDeleteCabinetConfigData(cabinet.getId(), cabinet.getDehumidifierCommPort());

        // 如果删除的是默认柜子，则将剩余第一个柜子设为默认
        if ("true".equalsIgnoreCase(cabinet.getIsDefault())) {
            LambdaQueryWrapper<CabinetConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.last("limit 1");
            CabinetConfig anyRemaining = this.getOne(wrapper);
            if (anyRemaining != null) {
                anyRemaining.setIsDefault("true");
                this.updateById(anyRemaining);
            }
        }
    }


    @Override
    public void truncateCabinetTable() {
        cabinetConfigMapper.truncateTable();
    }
}

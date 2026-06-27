package com.tairui.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tairui.server.common.exception.ClientException;
import com.tairui.server.common.exception.ServerException;
import com.tairui.server.config.TemperatureHumidityScheduleConfig;
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
import com.tairui.server.webSocket.handle.DehumidifierWebSocketHandler;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
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

    @Autowired
    private DehumidifierWebSocketHandler dehumidifierWebSocketHandler;

    @Autowired
    private TemperatureHumidityScheduleConfig temperatureHumidityScheduleConfig;

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

        dehumidifierWebSocketHandler.pausePush();
        temperatureHumidityScheduleConfig.pauseSchedule();

        try {
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

            // 检查新除湿机端口+设备地址是否与别的柜子冲突
            String targetDehumidifierPort = updateDTO.getDehumidifierCommPort() != null ? updateDTO.getDehumidifierCommPort() : existing.getDehumidifierCommPort();
            String targetDehumidifierAddr = updateDTO.getDehumidifierAddr() != null ? updateDTO.getDehumidifierAddr() : existing.getDehumidifierAddr();
            if (!targetDehumidifierPort.equals(oldDehumidifierCommPort) || !targetDehumidifierAddr.equals(existing.getDehumidifierAddr())) {
                LambdaQueryWrapper<CabinetConfig> dehumidifierWrapper = new LambdaQueryWrapper<>();
                dehumidifierWrapper.ne(CabinetConfig::getId, id)
                        .eq(CabinetConfig::getDehumidifierCommPort, targetDehumidifierPort)
                        .eq(CabinetConfig::getDehumidifierAddr, targetDehumidifierAddr);
                if (this.count(dehumidifierWrapper) > 0) {
                    throw new ClientException(String.format("其他柜子已占用通信端口[%s]下的除湿机设备地址[%s]", targetDehumidifierPort, targetDehumidifierAddr));
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
            if (updateDTO.getDehumidifierAddr() != null)
                updateEntity.setDehumidifierAddr(updateDTO.getDehumidifierAddr());
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

            // 重新获取更新后的完整最新配置实体
            CabinetConfig fullLatestEntity = this.getById(id);

            // 锁板通信端口改变
            boolean isLockPortChanged = updateDTO.getLockCommPort() != null && !updateDTO.getLockCommPort().equals(oldLockCommPort);
            // 除湿机通信端口改变
            boolean isDehumidifierPortChanged = updateDTO.getDehumidifierCommPort() != null && !updateDTO.getDehumidifierCommPort().equals(oldDehumidifierCommPort);
            // 除湿机设备地址改变
            boolean isDehumidifierAddrChanged = updateDTO.getDehumidifierAddr() != null && !updateDTO.getDehumidifierAddr().equals(existing.getDehumidifierAddr());
            // 除湿阈值改变
            boolean isHumidityThresholdChanged = (updateDTO.getHumidityMax() != null && !updateDTO.getHumidityMax().equals(existing.getHumidityMax()))
                    || (updateDTO.getHumidityMin() != null && !updateDTO.getHumidityMin().equals(existing.getHumidityMin()));

            boolean newLockRegistered = false;
            boolean newDehumidifierRegistered = false;

            QianMingLockDeviceService oldLockServiceBackup = null;
            DehumidifierDeviceService oldDehumidifierServiceBackup = null;

            // 标记原连接是否在本次修改中被真正 remove 和 close 掉了
            boolean oldDehumidifierPortClosed = false;
            boolean oldLockPortClosed = false;

            try {
                // ==========================================
                // 1. 先进行除湿机（分子筛）链路与参数更新
                // ==========================================
                if (isDehumidifierPortChanged) {
                    // 情况 A：物理端口变了，需要进行多租户引用计算安全卸载
                    log.info("检测到除湿机物理端口变更，进行安全引用计数释放检查...");

                    if (oldDehumidifierCommPort != null) {
                        LambdaQueryWrapper<CabinetConfig> checkWrapper = new LambdaQueryWrapper<>();
                        checkWrapper.ne(CabinetConfig::getId, id).eq(CabinetConfig::getDehumidifierCommPort, oldDehumidifierCommPort);
                        long othersUsingPort = this.count(checkWrapper);

                        if (othersUsingPort == 0) {
                            // 独苗使用，可以安全移除销毁
                            oldDehumidifierServiceBackup = dehumidifierDeviceServiceManager.getDeviceServiceMap().remove(oldDehumidifierCommPort);
                            if (oldDehumidifierServiceBackup != null) {
                                try {
                                    oldDehumidifierServiceBackup.close();
                                    oldDehumidifierPortClosed = true;
                                    log.info("原除湿机端口 [{}] 已无其它柜子使用，安全关闭释放物理链路资源。", oldDehumidifierCommPort);
                                } catch (Exception e) {
                                    log.warn("断开老除湿机物理连接失败(忽略): ", e);
                                }
                            }
                        } else {
                            // 还有其它设备在使用，绝对不能断开物理底层链路
                            oldDehumidifierServiceBackup = dehumidifierDeviceServiceManager.getDeviceServiceMap().get(oldDehumidifierCommPort);
                            log.info("原除湿机端口 [{}] 仍有其它 {} 个柜子共用，不关闭链路，保持实例运作。", oldDehumidifierCommPort, othersUsingPort);
                        }
                    }

                    // 建立新端口下的连接
                    DehumidifierDeviceService newDehumidifierService = dehumidifierDeviceServiceManager.addDeviceServiceByNewCabinetConfig(fullLatestEntity);
                    if (!dehumidifierDeviceServiceManager.getDeviceServiceMap().containsKey(fullLatestEntity.getDehumidifierCommPort())) {
                        newDehumidifierService.open();
                        int humidifierDeviceAddress = Integer.parseInt(fullLatestEntity.getDehumidifierAddr());
                        newDehumidifierService.setAddress(humidifierDeviceAddress);
                        newDehumidifierService.setHumidityControlStart(fullLatestEntity.getHumidityMax().intValue());
                        newDehumidifierService.setHumidityControlStop(fullLatestEntity.getHumidityMin().intValue());
                        dehumidifierDeviceServiceManager.getDeviceServiceMap().put(fullLatestEntity.getDehumidifierCommPort(), newDehumidifierService);
                        newDehumidifierRegistered = true;
                        log.info("{} 迁往新除湿机端口连接建立成功，并同步了阈值", fullLatestEntity.getTitle());
                    }
                } else if (isDehumidifierAddrChanged || isHumidityThresholdChanged) {
                    // 情况 B：物理链路没有发生任何变化，仅仅只是改了同一个总线上的设备地址或者控湿阈值
                    log.info("除湿机物理端口未变。检测到[设备地址]或[湿度阈值]发生变更，执行免断开动态同步...");

                    DehumidifierDeviceService currentService = dehumidifierDeviceServiceManager.getDeviceServiceMap().get(oldDehumidifierCommPort);
                    if (currentService != null) {
                        // 直接复用旧连接服务，下发更新设备地址以及新的启停温湿度参数
                        currentService.setAddress(Integer.parseInt(fullLatestEntity.getDehumidifierAddr()));
                        currentService.setHumidityControlStart(fullLatestEntity.getHumidityMax().intValue());
                        currentService.setHumidityControlStop(fullLatestEntity.getHumidityMin().intValue());
                        log.info("{} 除湿机总线动态变更完成。当前地址: {}, Start: {}, Stop: {}",
                                fullLatestEntity.getTitle(), fullLatestEntity.getDehumidifierAddr(), fullLatestEntity.getHumidityMax(), fullLatestEntity.getHumidityMin());
                    } else {
                        log.warn("未在缓存中发现端口 [{}] 激活的除湿机实例，跳过动态同步", oldDehumidifierCommPort);
                    }
                }

                // ==========================================
                // 2. 再进行锁板链路更新
                // ==========================================
                if (isLockPortChanged) {
                    log.info("检测到锁板物理端口变更，进行安全引用计数释放检查...");

                    if (oldLockCommPort != null) {
                        LambdaQueryWrapper<CabinetConfig> checkWrapper = new LambdaQueryWrapper<>();
                        checkWrapper.ne(CabinetConfig::getId, id).eq(CabinetConfig::getLockCommPort, oldLockCommPort);
                        long othersUsingPort = this.count(checkWrapper);

                        if (othersUsingPort == 0) {
                            // 独苗使用，可以安全关闭
                            oldLockServiceBackup = qianMingLockDeviceServiceManager.getDeviceServiceMap().remove(oldLockCommPort);
                            if (oldLockServiceBackup != null) {
                                try {
                                    oldLockServiceBackup.close();
                                    oldLockPortClosed = true;
                                    log.info("原锁板端口 [{}] 已无其它柜子共用，安全关闭物理通道。", oldLockCommPort);
                                } catch (Exception e) {
                                    log.warn("断开老锁板物理连接失败(忽略): ", e);
                                }
                            }
                        } else {
                            // 还有其它柜子，不能关闭总线
                            oldLockServiceBackup = qianMingLockDeviceServiceManager.getDeviceServiceMap().get(oldLockCommPort);
                            log.info("原锁板端口 [{}] 仍有其它 {} 个柜子共用总线，保持通道状态不变。", oldLockCommPort, othersUsingPort);
                        }
                    }

                    // 建立新连接
                    QianMingLockDeviceService newLockService = qianMingLockDeviceServiceManager.addDeviceServiceByNewCabinetConfig(fullLatestEntity);
                    if (newLockService.getCommDispatcher() != null && !qianMingLockDeviceServiceManager.getDeviceServiceMap().containsKey(fullLatestEntity.getLockCommPort())) {
                        newLockService.open();
                        qianMingLockDeviceServiceManager.getDeviceServiceMap().put(fullLatestEntity.getLockCommPort(), newLockService);
                        newLockRegistered = true;
                        log.info("{} 新锁板物理链路初始化成功并缓存", fullLatestEntity.getTitle());
                    }
                }

            } catch (Exception e) {
                log.error("==== 硬件链路更新失败！开始执行人工补偿机制（硬件回滚） ====");

                // 【清理新注册成功的误加项】
                if (newDehumidifierRegistered) {
                    try {
                        dehumidifierDeviceServiceManager.removeDeviceServiceByCommPort(fullLatestEntity.getDehumidifierCommPort());
                    } catch (Exception ex) {
                        log.error("回滚清理新除湿机异常: ", ex);
                    }
                }
                if (newLockRegistered) {
                    try {
                        qianMingLockDeviceServiceManager.removeDeviceServiceByCommPort(fullLatestEntity.getLockCommPort());
                    } catch (Exception ex) {
                        log.error("回滚清理新锁板异常: ", ex);
                    }
                }

                // 【还原已被破坏的老服务通道（仅在它曾被真正close移除的情况下恢复）】
                if (isDehumidifierPortChanged && oldDehumidifierPortClosed && oldDehumidifierServiceBackup != null && oldDehumidifierCommPort != null) {
                    try {
                        oldDehumidifierServiceBackup.open();
                        dehumidifierDeviceServiceManager.getDeviceServiceMap().put(oldDehumidifierCommPort, oldDehumidifierServiceBackup);
                        log.info("【更新回滚】已重新拉起并还原原除湿机物理链路: {}", oldDehumidifierCommPort);
                    } catch (Exception ex) {
                        log.error("关键严重缺陷：原除湿机物理连接回滚恢复失败！", ex);
                    }
                }

                if (isLockPortChanged && oldLockPortClosed && oldLockServiceBackup != null && oldLockCommPort != null) {
                    try {
                        oldLockServiceBackup.open();
                        qianMingLockDeviceServiceManager.getDeviceServiceMap().put(oldLockCommPort, oldLockServiceBackup);
                        log.info("【更新回滚】已重新拉起并还原原锁板物理链路: {}", oldLockCommPort);
                    } catch (Exception ex) {
                        log.error("关键严重缺陷：原锁板物理连接回滚恢复失败！", ex);
                    }
                }

                throw new ClientException("更新柜子硬件连接失败：" + e.getMessage());
            }
        } finally {
            dehumidifierWebSocketHandler.resumePush();
            temperatureHumidityScheduleConfig.resumeSchedule();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCabinet(CabinetUpdateDTO createDTO) {

        dehumidifierWebSocketHandler.pausePush();
        temperatureHumidityScheduleConfig.pauseSchedule();

        try {
            // 业务唯一性检查
            LambdaQueryWrapper<CabinetConfig> titleWrapper = new LambdaQueryWrapper<>();
            titleWrapper.eq(CabinetConfig::getTitle, createDTO.getTitle());
            if (this.count(titleWrapper) > 0) {
                throw new ClientException("柜子名称已存在，请使用唯一名称");
            }
            // 分子筛 通信地址和设备地址联合唯一性检查
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
            // 锁板连接成功标志位(如果原来没有其他柜子使用，并且当前连接成功)
            boolean lockRegistered = false;
            // 除湿机连接成功标志位(如果原来没有其他柜子使用，并且当前连接成功)
            boolean dehumidifierRegistered = false;

            QianMingLockDeviceService lockService;
            DehumidifierDeviceService dehumidifierService;

            try {
                // 除湿机设置
                dehumidifierService = dehumidifierDeviceServiceManager.addDeviceServiceByNewCabinetConfig(entity);

                if (!dehumidifierDeviceServiceManager.getDeviceServiceMap().containsKey(entity.getDehumidifierCommPort())) {
                    try {
                        dehumidifierService.open();
                    } catch (IOException e) {
                        throw new RuntimeException("除湿机设备连接建立失败:" + e.getMessage());
                    }
                    dehumidifierDeviceServiceManager.getDeviceServiceMap().put(entity.getDehumidifierCommPort(), dehumidifierService);
                    dehumidifierRegistered = true;
                    log.info("{} 除湿机硬件连接成功，并已缓存", entity.getTitle());
                }

                dehumidifierService.setAddress(Integer.parseInt(createDTO.getDehumidifierAddr()));
                dehumidifierService.setHumidityControlStart(createDTO.getHumidityMax().intValue());
                dehumidifierService.setAddress(Integer.parseInt(createDTO.getDehumidifierAddr()));
                dehumidifierService.setHumidityControlStop(createDTO.getHumidityMin().intValue());
                // 锁板设置
                lockService = qianMingLockDeviceServiceManager.addDeviceServiceByNewCabinetConfig(entity);

                if (lockService.getCommDispatcher() != null && !qianMingLockDeviceServiceManager.getDeviceServiceMap().containsKey(entity.getLockCommPort())) {
                    try {
                        lockService.open();
                    } catch (IOException e) {
                        throw new RuntimeException("锁板设备连接建立失败:" + e.getMessage());
                    }
                    qianMingLockDeviceServiceManager.getDeviceServiceMap().put(entity.getLockCommPort(), lockService);
                    lockRegistered = true;
                    log.info("{} 锁板硬件连接成功，并已缓存", entity.getTitle());
                }

            } catch (Exception e) {
                log.error("[createCabinet]:硬件链路建立失败！开始执行人工补偿机制（硬件回滚）");
                // 如果是新存入的，原来没有存在过
                if (dehumidifierRegistered) {
                    try {
                        dehumidifierDeviceServiceManager.removeDeviceServiceByCommPort(entity.getDehumidifierCommPort());
                        log.warn("【系统回滚】由于后续设备初始化失败，已强制关闭并清理除湿机连接: {}", entity.getDehumidifierCommPort());
                    } catch (Exception ex) {
                        log.error("回滚除湿机连接时发生次生异常: ", ex);
                    }
                }
                // 如果是新存入的，原来没有存在过
                if (lockRegistered) {
                    try {
                        qianMingLockDeviceServiceManager.removeDeviceServiceByCommPort(entity.getLockCommPort());
                        log.warn("[系统回滚]由于后续设备初始化失败，已强制关闭并清理锁板连接: {}", entity.getLockCommPort());
                    } catch (Exception ex) {
                        log.error("回滚锁板连接时发生次生异常: ", ex);
                    }
                }

                throw new ClientException("Error：" + e.getMessage());
            }
        } finally {
            dehumidifierWebSocketHandler.resumePush();
            temperatureHumidityScheduleConfig.resumeSchedule();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCabinet(Integer id) {
        dehumidifierWebSocketHandler.pausePush();
        temperatureHumidityScheduleConfig.pauseSchedule();

        try {
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

            if ("true".equalsIgnoreCase(cabinet.getIsDefault())) {
                LambdaQueryWrapper<CabinetConfig> wrapper = new LambdaQueryWrapper<>();
                wrapper.last("limit 1");
                CabinetConfig anyRemaining = this.getOne(wrapper);
                if (anyRemaining != null) {
                    anyRemaining.setIsDefault("true");
                    this.updateById(anyRemaining);
                }
            }
        } finally {
            dehumidifierWebSocketHandler.resumePush();
            temperatureHumidityScheduleConfig.resumeSchedule();
        }
    }

    @Override
    public void truncateCabinetTable() {
        cabinetConfigMapper.truncateTable();
    }
}
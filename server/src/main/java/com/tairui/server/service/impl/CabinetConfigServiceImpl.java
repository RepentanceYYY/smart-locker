// 文件位置：com/tairui/server/service/impl/CabinetConfigServiceImpl.java
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
        // 1. 检查柜子是否存在
        CabinetConfig existing = this.getById(id);
        if (existing == null) {
            throw new RuntimeException("柜子不存在，id=" + id);
        }

        // 2. 检查名称唯一性（排除自身）
        if (updateDTO.getTitle() != null && !updateDTO.getTitle().equals(existing.getTitle())) {
            LambdaQueryWrapper<CabinetConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CabinetConfig::getTitle, updateDTO.getTitle());
            long count = this.count(wrapper);
            if (count > 0) {
                throw new RuntimeException("柜子名称已存在，请使用唯一名称");
            }
        }

        // 3. 构建更新实体（必须设置 id）
        CabinetConfig updateEntity = new CabinetConfig();
        updateEntity.setId(id);   // 关键：设置要更新的记录 ID
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

        // 4. 执行更新
        boolean updated = this.updateById(updateEntity);
        if (!updated) {
            throw new RuntimeException("更新柜子配置失败");
        }

        // 5. 如果当前柜子被设置为默认柜子，将其他柜子的 isDefault 设为 false
        if (updateDTO.getIsDefault() != null && updateDTO.getIsDefault()) {
            LambdaQueryWrapper<CabinetConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.ne(CabinetConfig::getId, id);
            wrapper.eq(CabinetConfig::getIsDefault, "true");
            List<CabinetConfig> otherDefaults = this.list(wrapper);
            for (CabinetConfig cab : otherDefaults) {
                cab.setIsDefault("false");
                this.updateById(cab);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCabinet(CabinetUpdateDTO createDTO) {
        // ==================== 1. 参数校验 ====================
        if (createDTO.getTitle() == null || createDTO.getTitle().trim().isEmpty()) {
            throw new ClientException("柜子名称不能为空");
        }
        if (createDTO.getWidth() == null || createDTO.getWidth().trim().isEmpty()) {
            throw new ClientException("柜子宽度不能为空");
        }
        if (createDTO.getHeight() == null || createDTO.getHeight().trim().isEmpty()) {
            throw new ClientException("柜子高度不能为空");
        }
        if (createDTO.getDehumidifierCommPort() == null || createDTO.getDehumidifierCommPort().trim().isEmpty()) {
            throw new ClientException("除湿机通讯端口不能为空");
        }
        if (createDTO.getDehumidifierAddr() == null || createDTO.getDehumidifierAddr().trim().isEmpty()) {
            throw new ClientException("除湿机地址不能为空");
        }
        if (createDTO.getLockCommPort() == null || createDTO.getLockCommPort().trim().isEmpty()) {
            throw new ClientException("锁板通讯端口不能为空");
        }

        // ==================== 2. 业务唯一性检查 ====================
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

        // ==================== 3. 构建并保存数据库 ====================
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

        // ==================== 4. 核心变化：硬件连接精细化控制 ====================

        // 用于记录两端设备最终是否有成功执行过逻辑放入过 Map 缓存，以便回滚清理
        boolean lockRegistered = false;
        boolean dehumidifierRegistered = false;

        QianMingLockDeviceService lockService = null;
        DehumidifierDeviceService dehumidifierService = null;

        try {
            // Step 4.1: 获取锁板纯净对象（此时未 open，未入 Map）
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

            // 尝试启动除湿机连接
            try {
                dehumidifierService = dehumidifierDeviceServiceManager.addDeviceServiceByNewCabinetConfig(entity);
                if (!dehumidifierDeviceServiceManager.getDeviceServiceMap().containsKey(entity.getDehumidifierCommPort())) {
                    dehumidifierService.open();
                    dehumidifierDeviceServiceManager.getDeviceServiceMap().put(entity.getDehumidifierCommPort(), dehumidifierService);
                    dehumidifierRegistered = true; // 标记除湿机已经成功注册并建立了连接
                    log.info("{} 除湿机硬件连接成功，并已缓存", entity.getTitle());
                }
            } catch (Exception e) {
                throw new RuntimeException("除湿机建立物理连接失败: " + e.getMessage());
            }

        } catch (Exception e) {
            log.error("==== 硬件链路建立失败！开始执行人工补偿机制（硬件回滚） ====");

            // 补偿机制：如果锁板刚才顺利注册成功了，但因为除湿机垮了，必须立刻把锁板断开并从缓存踢出！
            if (lockRegistered) {
                try {
                    qianMingLockDeviceServiceManager.removeDeviceServiceByCommPort(entity.getLockCommPort());
                    log.warn("【系统回滚】由于后续设备初始化失败，已强制关闭并清理锁板连接: {}", entity.getLockCommPort());
                } catch (Exception ex) {
                    log.error("回滚锁板连接时发生次生异常: ", ex);
                }
            }

            // 补偿机制：如果除湿机注册成功了（虽然顺序在后，作为防御性代码也加上）
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

// 文件位置：com/tairui/server/service/impl/CabinetConfigServiceImpl.java
package com.tairui.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tairui.server.dto.CabinetFullDTO;
import com.tairui.server.dto.CabinetUpdateDTO;
import com.tairui.server.entity.CabinetConfig;
import com.tairui.server.entity.CellConfig;
import com.tairui.server.mapper.CabinetConfigMapper;
import com.tairui.server.service.CabinetConfigService;
import com.tairui.server.service.CellConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CabinetConfigServiceImpl extends ServiceImpl<CabinetConfigMapper, CabinetConfig> implements CabinetConfigService {

    @Autowired
    private CellConfigService cellConfigService;

    @Autowired
    private CabinetConfigMapper cabinetConfigMapper;

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
        if (updateDTO.getDehumidifierCommType() != null) updateEntity.setDehumidifierCommType(updateDTO.getDehumidifierCommType());
        if (updateDTO.getDehumidifierCommPort() != null) updateEntity.setDehumidifierCommPort(updateDTO.getDehumidifierCommPort());
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
        // 1. 参数校验
        if (createDTO.getTitle() == null || createDTO.getTitle().trim().isEmpty()) {
            throw new RuntimeException("柜子名称不能为空");
        }
        if (createDTO.getWidth() == null || createDTO.getWidth().trim().isEmpty()) {
            throw new RuntimeException("柜子宽度不能为空");
        }
        if (createDTO.getHeight() == null || createDTO.getHeight().trim().isEmpty()) {
            throw new RuntimeException("柜子高度不能为空");
        }
        if (createDTO.getDehumidifierCommPort() == null || createDTO.getDehumidifierCommPort().trim().isEmpty()) {
            throw new RuntimeException("通讯端口不能为空");
        }
        if (createDTO.getDehumidifierAddr() == null || createDTO.getDehumidifierAddr().trim().isEmpty()) {
            throw new RuntimeException("除湿机地址不能为空");
        }
        if (createDTO.getLockCommPort() == null || createDTO.getLockCommPort().trim().isEmpty()) {
            throw new RuntimeException("通讯端口不能为空");
        }
        // 2. 检查名称唯一性
        LambdaQueryWrapper<CabinetConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CabinetConfig::getTitle, createDTO.getTitle());
        long count = this.count(wrapper);
        if (count > 0) {
            throw new RuntimeException("柜子名称已存在，请使用唯一名称");
        }

        // 3. 构建实体对象
        CabinetConfig entity = new CabinetConfig();
        entity.setTitle(createDTO.getTitle());
        entity.setWidth(createDTO.getWidth());
        entity.setHeight(createDTO.getHeight());
        entity.setIsDefault(createDTO.getIsDefault() != null && createDTO.getIsDefault() ? "true" : "false");
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

        // 4. 保存
        boolean saved = this.save(entity);
        if (!saved) {
            throw new RuntimeException("创建柜子失败");
        }

        // 5. 如果当前柜子被设置为默认柜子，将其他柜子的 isDefault 设为 false
        if (createDTO.getIsDefault() != null && createDTO.getIsDefault()) {
            LambdaQueryWrapper<CabinetConfig> otherWrapper = new LambdaQueryWrapper<>();
            otherWrapper.ne(CabinetConfig::getId, entity.getId());
            otherWrapper.eq(CabinetConfig::getIsDefault, "true");
            List<CabinetConfig> otherDefaults = this.list(otherWrapper);
            for (CabinetConfig cab : otherDefaults) {
                cab.setIsDefault("false");
                this.updateById(cab);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCabinet(Integer id) {
        CabinetConfig cabinet = this.getById(id);
        if (cabinet == null) {
            throw new RuntimeException("柜子不存在，id=" + id);
        }

        // 删除该柜子下所有单元格
        LambdaQueryWrapper<CellConfig> cellWrapper = new LambdaQueryWrapper<>();
        cellWrapper.eq(CellConfig::getCabinetId, id);
        cellConfigService.remove(cellWrapper);

        // 删除柜子本身
        boolean removed = this.removeById(id);
        if (!removed) {
            throw new RuntimeException("删除柜子失败");
        }

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

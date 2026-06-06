package com.tairui.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tairui.server.dto.CellConfigUpdateDTO;
import com.tairui.server.entity.CellConfig;
import com.tairui.server.mapper.CabinetConfigMapper;
import com.tairui.server.mapper.CellConfigMapper;
import com.tairui.server.service.CellConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 格口配置 服务实现类
 * </p>
 *
 * @author system
 * @since 2026-05-19
 */
@Service
public class CellConfigServiceImpl extends ServiceImpl<CellConfigMapper, CellConfig> implements CellConfigService {

    @Autowired
    private CellConfigMapper cellConfigMapper;
    @Override
    public void updateCellEmpty( Long cellId, String isEmpty) {
        if ( cellId == null) {

        log.warn("cabinetId或cellId为空，无法更新格口状态");
        return;
    }
        // 根据 cabinetId 和 cellId 查询格口配置
        LambdaQueryWrapper<CellConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper .eq(CellConfig::getId, cellId);

        CellConfig cellConfig = this.getOne(wrapper);

        if (cellConfig == null) {
            return;
        }

        // 如果已经是空，则无需更新
        if (Boolean.TRUE.equals(cellConfig.getIsEmpty())) {
            return;
        }

        // 设置为空
        cellConfig.setIsEmpty(isEmpty);

        boolean updated = this.updateById(cellConfig);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCell(CellConfigUpdateDTO dto) {
        // 1. 校验存在性
        CellConfig existing = this.getById(dto.getId());
        if (existing == null) {
            throw new RuntimeException("格口不存在，id=" + dto.getId());
        }

        // 2. 唯一性校验：格口号
        if (dto.getNumber() != null && !dto.getNumber().equals(existing.getNumber())) {
            LambdaQueryWrapper<CellConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CellConfig::getCabinetId, dto.getCabinetId())
                    .eq(CellConfig::getNumber, dto.getNumber())
                    .ne(CellConfig::getId, dto.getId());
            long count = this.count(wrapper);
            if (count > 0) {
                throw new RuntimeException("柜子内格口号已存在，请使用唯一编号");
            }
        }

        // 3. 硬件地址唯一性校验（如有）
        if (dto.getMacAddress() != null && !dto.getMacAddress().isEmpty()) {
            LambdaQueryWrapper<CellConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CellConfig::getMacAddress, dto.getMacAddress())
                    .ne(CellConfig::getId, dto.getId());
            long count = this.count(wrapper);
            if (count > 0) {
                throw new RuntimeException("硬件地址已被其他格口使用");
            }
        }

        // 4. 拷贝 DTO 到实体并更新
        CellConfig updateEntity = new CellConfig();
        BeanUtils.copyProperties(dto, updateEntity);
        if (dto.getIsEmpty() != null) {
            updateEntity.setIsEmpty(dto.getIsEmpty());
        }
        boolean updated = this.updateById(updateEntity);
        if (!updated) {
            throw new RuntimeException("更新格口配置失败");
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCell(CellConfigUpdateDTO dto) {
        // 唯一性校验：格口号、硬件地址
        if (dto.getNumber() != null) {
            LambdaQueryWrapper<CellConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CellConfig::getCabinetId, dto.getCabinetId())
                    .eq(CellConfig::getNumber, dto.getNumber());
            long count = this.count(wrapper);
            if (count > 0) {
                throw new RuntimeException("柜子内格口号已存在");
            }
        }
        if (dto.getMacAddress() != null && !dto.getMacAddress().isEmpty()) {
            LambdaQueryWrapper<CellConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CellConfig::getMacAddress, dto.getMacAddress());
            long count = this.count(wrapper);
            if (count > 0) {
                throw new RuntimeException("硬件地址已被使用");
            }
        }

        CellConfig entity = new CellConfig();
        BeanUtils.copyProperties(dto, entity);
        if (dto.getIsEmpty() == null && dto.getType().equals("cell")) {
            entity.setIsEmpty("true");
        }

        boolean saved = this.save(entity);
        if (!saved) {
            throw new RuntimeException("新增格口失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCell(Integer id) {
        if (id == null) {
            throw new RuntimeException("格口ID不能为空");
        }
        // 检查是否存在
        CellConfig existing = this.getById(id);
        if (existing == null) {
            throw new RuntimeException("格口不存在，id=" + id);
        }
        // 执行删除
        boolean deleted = this.removeById(id);
        if (!deleted) {
            throw new RuntimeException("删除格口失败");
        }
    }

    @Override
    public void truncateCellTable() {
        cellConfigMapper.truncateTable();
    }



}

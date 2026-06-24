package com.tairui.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tairui.server.config.WebConfig;
import com.tairui.server.dto.BorrowRecordSubmitDTO;
import com.tairui.server.dto.LogListDTO;
import com.tairui.server.dto.LogOverviewDTO;
import com.tairui.server.dto.ReturnRecordSubmitDTO;
import com.tairui.server.entity.SysOperLog;
import com.tairui.server.mapper.SysOperLogMapper;
import com.tairui.server.service.CellConfigService;
import com.tairui.server.service.SysOperLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements SysOperLogService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Autowired
    private CellConfigService cellConfigService;


    @Autowired
    private SysOperLogMapper sysOperLogMapper;

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (!StringUtils.hasText(dateTimeStr)) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
        } catch (Exception e1) {
            try {
                return LocalDateTime.parse(dateTimeStr, ISO_FORMATTER);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBorrowRecordsWithPhoto(BorrowRecordSubmitDTO dto) {

        // 2. 校验借用列表
        if (dto.getBorrowItems() == null || dto.getBorrowItems().isEmpty()) {
            throw new RuntimeException("借用物品列表不能为空");
        }

        LocalDateTime expectedReturnTime = parseDateTime(dto.getExpectedReturnTime());
        String borrowerName = dto.getBorrowerName();
        String borrowerNumber = dto.getBorrowerNumber();
        String borrowRemark = dto.getRemark();

        List<SysOperLog> logs = new ArrayList<>();
        for (BorrowRecordSubmitDTO.BorrowItemDTO item : dto.getBorrowItems()) {
            SysOperLog log = new SysOperLog();
            log.setCabinetId(item.getCabinetId());
            log.setCabinetTitle(item.getCabinetName());
            log.setCellId(item.getCellId());
            if (StringUtils.hasText(item.getCellNumber())) {
                try {
                    log.setCellNumber(Integer.parseInt(item.getCellNumber()));
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
            log.setToolName(item.getToolName());
            log.setBorrowerPhoto(dto.getBorrowerPhoto());
            log.setBorrowerName(borrowerName);
            log.setBorrowerNumber(borrowerNumber);
            log.setBorrowRemark(borrowRemark);
            log.setExpectedReturnTime(expectedReturnTime);
            log.setBorrowTime(parseDateTime(item.getBorrowTime()));
            logs.add(log);
        }

        // 3. 批量保存借用记录
        boolean saved = this.saveBatch(logs);
        if (!saved) {
            throw new RuntimeException("保存借用记录失败");
        }

        // 4. 保存成功后，将对应格口标记为空闲
        for (BorrowRecordSubmitDTO.BorrowItemDTO item : dto.getBorrowItems()) {
            Long cellId = Long.valueOf(item.getCellId());
            if (cellId != null) {
                try {
                    cellConfigService.updateCellEmpty(cellId, "true");
                } catch (Exception e) {
                    throw new RuntimeException("更新格口状态失败", e);
                }
            }
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveReturnRecordsWithPhoto(ReturnRecordSubmitDTO dto) {

        // 2. 校验归还列表
        if (dto.getReturnItems() == null || dto.getReturnItems().isEmpty()) {
            throw new RuntimeException("归还物品列表不能为空");
        }

        String returnerName = dto.getReturnerName();
        String returnerNumber = dto.getReturnerNumber();
        String returnRemark = dto.getRemark();

        List<SysOperLog> logsToUpdate = new ArrayList<>();
        List<SysOperLog> logsToInsert = new ArrayList<>();

        for (ReturnRecordSubmitDTO.ReturnItemDTO item : dto.getReturnItems()) {
            // 查询是否存在未归还的领用记录（return_time IS NULL）
            LambdaQueryWrapper<SysOperLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysOperLog::getCabinetId, item.getCabinetId())
                    .eq(SysOperLog::getCellId, item.getCellId())
                    .isNull(SysOperLog::getReturnTime)
                    .orderByDesc(SysOperLog::getBorrowTime)
                    .last("LIMIT 1");

            SysOperLog existLog = this.getOne(queryWrapper);

            if (existLog != null) {
                // 存在领用记录 → 更新归还信息
                existLog.setReturnName(returnerName);
                existLog.setReturnNumber(returnerNumber);
                existLog.setReturnTime(parseDateTime(item.getReturnTime()));
                existLog.setReturnRemark(returnRemark);
                existLog.setReturnPhoto(dto.getReturnPhoto());
                logsToUpdate.add(existLog);
            } else {
                // 不存在领用记录 → 新增一条仅包含归还信息的记录
                SysOperLog newLog = new SysOperLog();
                newLog.setCabinetId(item.getCabinetId());
                newLog.setCabinetTitle(item.getCabinetName());
                newLog.setCellId(item.getCellId());
                if (StringUtils.hasText(item.getCellNumber())) {
                    try {
                        newLog.setCellNumber(Integer.parseInt(item.getCellNumber()));
                    } catch (NumberFormatException e) {
                        log.warn("格口号格式错误：{}", item.getCellNumber());
                    }
                }
                newLog.setToolName(item.getToolName());
                newLog.setReturnName(returnerName);
                newLog.setReturnNumber(returnerNumber);
                newLog.setReturnTime(parseDateTime(item.getReturnTime()));
                newLog.setReturnRemark(returnRemark);
                newLog.setReturnPhoto(dto.getReturnPhoto());
                logsToInsert.add(newLog);
            }
        }

        // 执行批量更新
        if (!logsToUpdate.isEmpty()) {
            boolean updated = this.updateBatchById(logsToUpdate);
            if (!updated) {
                throw new RuntimeException("更新归还记录失败");
            }
            log.info("成功更新 {} 条归还记录", logsToUpdate.size());
        }

        // 执行批量插入
        if (!logsToInsert.isEmpty()) {
            boolean inserted = this.saveBatch(logsToInsert);
            if (!inserted) {
                throw new RuntimeException("新增归还记录失败");
            }
            log.info("成功新增 {} 条归还记录", logsToInsert.size());
        }

        // 4. 归还成功后，将对应格口标记为非空（工具已放入）
        for (ReturnRecordSubmitDTO.ReturnItemDTO item : dto.getReturnItems()) {
            Long cellId = Long.valueOf(item.getCellId());
            if (cellId != null) {
                try {
                    // 注意：第二个参数 "false" 表示非空（因为借出时传 "true" 表示置空）
                    cellConfigService.updateCellEmpty(cellId, "false");
                } catch (Exception e) {
                    log.error("更新格口状态失败, cellId: {}", cellId, e);
                    throw new RuntimeException("更新格口状态失败", e);
                }
            }
        }
    }

    // ================= 新增：日志概览查询 =================
    @Override
    public LogOverviewDTO getLogOverview() {
        // 总数
        Long totalLogs = this.count();
        // 未归还数量（return_time is null）
        LambdaQueryWrapper<SysOperLog> unreturnedWrapper = new LambdaQueryWrapper<>();
        unreturnedWrapper.isNull(SysOperLog::getReturnTime);
        Long unreturnedCount = this.count(unreturnedWrapper);
        // 未归还记录列表
        LambdaQueryWrapper<SysOperLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNull(SysOperLog::getReturnTime)
                .orderByDesc(SysOperLog::getBorrowTime);
        List<SysOperLog> records = this.list(queryWrapper);
        List<LogOverviewDTO.UnreturnedItemDTO> unreturnedList = records.stream()
                .map(record -> {
                    LogOverviewDTO.UnreturnedItemDTO dto = new LogOverviewDTO.UnreturnedItemDTO();
                    dto.setCabinetTitle(record.getCabinetTitle());
                    dto.setCellNumber(record.getCellNumber());
                    dto.setToolName(record.getToolName());
                    dto.setBorrowerPhoto(record.getBorrowerPhoto());
                    if (record.getBorrowTime() != null) {
                        dto.setBorrowTime(record.getBorrowTime()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    }
                    return dto;
                })
                .collect(Collectors.toList());
        LogOverviewDTO result = new LogOverviewDTO();
        result.setTotalLogs(totalLogs);
        result.setUnreturnedCount(unreturnedCount);
        result.setUnreturnedList(unreturnedList);
        return result;
    }

    @Override
    public List<LogListDTO> getAllLogList(String borrowerName, String toolName, Integer status,
                                          String startTime, String endTime) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(borrowerName)) {
            wrapper.like(SysOperLog::getBorrowerName, borrowerName);
        }
        if (StringUtils.hasText(toolName)) {
            wrapper.like(SysOperLog::getToolName, toolName);
        }
        if (status != null) {
            if (status == 0) {
                wrapper.isNull(SysOperLog::getReturnTime);
            } else if (status == 1) {
                wrapper.isNotNull(SysOperLog::getReturnTime);
            } else if (status == 2) {
                wrapper.isNotNull(SysOperLog::getReturnTime)
                        .isNotNull(SysOperLog::getExpectedReturnTime)
                        .apply("return_time > expected_return_time");
            }
        }
        if (StringUtils.hasText(startTime)) {
            LocalDateTime start = LocalDate.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    .atStartOfDay();
            wrapper.ge(SysOperLog::getBorrowTime, start);
        }
        if (StringUtils.hasText(endTime)) {
            LocalDateTime end = LocalDate.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    .atTime(23, 59, 59);
            wrapper.le(SysOperLog::getBorrowTime, end);
        }

        // 排序：未归还优先，未归还按借用时间倒序，已归按归还时间倒序
        wrapper.last("ORDER BY " +
                "(return_time IS NULL) DESC, " +
                "CASE WHEN return_time IS NULL THEN borrow_time END DESC, " +
                "return_time DESC");

        List<SysOperLog> list = this.list(wrapper);
        return list.stream()
                .map(entity -> {
                    LogListDTO dto = new LogListDTO();
                    BeanUtils.copyProperties(entity, dto);  // 自动拷贝同名同类型属性
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void truncateLogTable() {
        sysOperLogMapper.truncateTable();
    }
}

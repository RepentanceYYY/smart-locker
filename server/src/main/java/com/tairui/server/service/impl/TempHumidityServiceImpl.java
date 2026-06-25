package com.tairui.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tairui.server.device.dehumidifier.ThData;
import com.tairui.server.deviceService.DehumidifierDeviceServiceManager;
import com.tairui.server.dto.TempHumidityLogDTO;
import com.tairui.server.entity.CabinetConfig;
import com.tairui.server.entity.ThHistory;
import com.tairui.server.mapper.CabinetConfigMapper;
import com.tairui.server.mapper.ThHistoryMapper;
import com.tairui.server.service.TempHumidityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 温湿度日志服务实现类
 */
@Slf4j
@Service
public class TempHumidityServiceImpl implements TempHumidityService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ThHistoryMapper thHistoryMapper;
    @Autowired
    private CabinetConfigMapper cabinetConfigMapper;
    @Autowired
    private DehumidifierDeviceServiceManager dehumidifierDeviceServiceManager;

    @Override
    public List<TempHumidityLogDTO> getRecentLogs(Integer limit) {
        // 构建查询条件：按创建时间降序，取最近 limit 条
        LambdaQueryWrapper<ThHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ThHistory::getCreateTime)
                .last("LIMIT " + limit);

        List<ThHistory> histories = thHistoryMapper.selectList(wrapper);

        List<TempHumidityLogDTO> result = new ArrayList<>();
        for (ThHistory history : histories) {
            TempHumidityLogDTO dto = new TempHumidityLogDTO();
            dto.setCabinetTitle(history.getCabinetTitle());


            if (history.getTemperature() != null) {
                dto.setTemperature(history.getTemperature());
            }
            if (history.getHumidity() != null) {
                dto.setHumidity(history.getHumidity());
            }

            // 格式化时间
            if (history.getCreateTime() != null) {
                dto.setRecordTime(history.getCreateTime().format(DATE_TIME_FORMATTER));
            }

            result.add(dto);
        }
        return result;
    }


    @Override
    public List<TempHumidityLogDTO> searchLogsByCondition(String cabinetTitle,
                                                          LocalDate startTime,
                                                          LocalDate endTime
    ) {
        LambdaQueryWrapper<ThHistory> wrapper = new LambdaQueryWrapper<>();

        if (cabinetTitle != null && !cabinetTitle.trim().isEmpty()) {
            wrapper.like(ThHistory::getCabinetTitle, cabinetTitle);
        }
        if (startTime != null) {
            wrapper.ge(ThHistory::getCreateTime, startTime.atStartOfDay());
        }
        if (endTime != null) {
            wrapper.le(ThHistory::getCreateTime, endTime.atTime(LocalTime.MAX));
        }
        wrapper.orderByDesc(ThHistory::getCreateTime);

        List<ThHistory> histories = thHistoryMapper.selectList(wrapper);
        List<TempHumidityLogDTO> result = new ArrayList<>();

        for (ThHistory history : histories) {
            TempHumidityLogDTO dto = new TempHumidityLogDTO();
            dto.setCabinetTitle(history.getCabinetTitle());
            // 直接 toString()，不做任何格式化
            dto.setTemperature(history.getTemperature() == null ? null : history.getTemperature().toString());
            dto.setHumidity(history.getHumidity() == null ? null : history.getHumidity().toString());
            if (history.getCreateTime() != null) {
                dto.setRecordTime(history.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            result.add(dto);
        }
        return result;
    }

    /**
     * 执行保存温湿度历史数据
     */
    @Override
    public void saveTemperatureHumidityHistory() {

        try {
            // 1. 柜子配置
            List<CabinetConfig> cabinetConfigs = cabinetConfigMapper.selectList(null);
            if (cabinetConfigs == null || cabinetConfigs.isEmpty()) {
                log.warn("暂无柜子配置，跳过温湿度历史记录");
                return;
            }

            // 2. 最新温湿度数据
            Map<Integer, ThData> thDataMap = dehumidifierDeviceServiceManager.getRealtimeTemperatureHumidity();

            if (thDataMap == null || thDataMap.isEmpty()) {
                log.warn("未获取到温湿度数据，跳过历史记录");
                return;
            }

            int successCount = 0;
            int failCount = 0;
            LocalDateTime now = LocalDateTime.now();

            for (CabinetConfig cabinet : cabinetConfigs) {
                try {
                    ThData thData = thDataMap.get(cabinet.getId());
                    if (thData == null) {
                        continue;
                    }

                    // 创建历史记录对象
                    ThHistory history = new ThHistory();
                    history.setCabinetId(cabinet.getId());
                    history.setCabinetTitle(cabinet.getTitle());
                    history.setTemperature(String.valueOf(thData.getTemperature()));
                    history.setHumidity(String.valueOf(thData.getHumidity()));
                    history.setCreateTime(now);

                    // 保存到数据库
                    thHistoryMapper.insert(history);
                    successCount++;


                } catch (Exception e) {
                    failCount++;

                }
            }

        } catch (Exception e) {
            log.error("保存温湿度历史数据失败: {}", e.getMessage(), e);
        }


    }

    /**
     * 获取温湿度历史数据列表分页
     */
    @Override
    public Page<TempHumidityLogDTO> getLogList(Integer page, Integer size, String cabinetTitle, String startTime, String endTime) {

        Page<ThHistory> thHistoryPage = new Page<>(page, size);
        LambdaQueryWrapper<ThHistory> queryWrapper = new LambdaQueryWrapper<>();

        CabinetConfig targetedCabinet = null;

        // 1. 柜子名称条件过滤
        if (StringUtils.hasText(cabinetTitle)) {
            List<CabinetConfig> cabinets = cabinetConfigMapper.selectList(
                    new LambdaQueryWrapper<CabinetConfig>()
                            .like(CabinetConfig::getTitle, cabinetTitle)
            );

            if (ObjectUtils.isEmpty(cabinets)) {
                return new Page<>(page, size);
            }

            targetedCabinet = cabinets.get(0);
            queryWrapper.eq(ThHistory::getCabinetId, targetedCabinet.getId());
        }

        // 2. 时间范围与排序
        queryWrapper.ge(StringUtils.hasText(startTime), ThHistory::getCreateTime, startTime)
                .le(StringUtils.hasText(endTime), ThHistory::getCreateTime, endTime + " 23:59:59");


        thHistoryMapper.selectPage(thHistoryPage, queryWrapper);

        final CabinetConfig finalCabinet = targetedCabinet;

        //3. 结果转换
        List<TempHumidityLogDTO> dtoList = thHistoryPage.getRecords().stream().map(entity -> {
            TempHumidityLogDTO dto = new TempHumidityLogDTO();

            BeanUtils.copyProperties(entity, dto);
            //返回展示创建时间
            if (entity.getCreateTime() != null) {
                dto.setRecordTime(entity.getCreateTime().toString());
            }

            if (finalCabinet != null && entity.getCabinetId().equals(finalCabinet.getId())) {
                dto.setCabinetTitle(finalCabinet.getTitle());
            } else {
                CabinetConfig cabinet = cabinetConfigMapper.selectById(entity.getCabinetId());
                if (cabinet != null) {
                    dto.setCabinetTitle(cabinet.getTitle());
                }
            }
            return dto;
        }).collect(Collectors.toList());

        Page<TempHumidityLogDTO> resultPage = new Page<>();
        resultPage.setCurrent(thHistoryPage.getCurrent());
        resultPage.setSize(thHistoryPage.getSize());
        resultPage.setTotal(thHistoryPage.getTotal());
        resultPage.setPages(thHistoryPage.getPages());
        resultPage.setRecords(dtoList);

        return resultPage;
    }
}

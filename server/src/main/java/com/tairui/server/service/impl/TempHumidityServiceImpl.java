package com.tairui.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tairui.server.dto.TempHumidityLogDTO;
import com.tairui.server.entity.ThHistory;
import com.tairui.server.mapper.ThHistoryMapper;
import com.tairui.server.service.TempHumidityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 温湿度日志服务实现类
 */
@Service
public class TempHumidityServiceImpl implements TempHumidityService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ThHistoryMapper thHistoryMapper;

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
}

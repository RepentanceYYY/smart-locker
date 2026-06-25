package com.tairui.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tairui.server.dto.TempHumidityLogDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * 温湿度日志服务接口
 */
public interface TempHumidityService {

    /**
     * 获取最近的温湿度日志
     * @param limit 查询条数
     * @return 温湿度日志列表
     */
    List<TempHumidityLogDTO> getRecentLogs(Integer limit);


    // 新增：多条件筛选查询
    List<TempHumidityLogDTO> searchLogsByCondition(String cabinetTitle,
                                                   LocalDate startTime,
                                                   LocalDate endTime
                                                  );

    Page<TempHumidityLogDTO> getLogList(Integer page, Integer size, String cabinetTitle, String startTime, String endTime);

    void saveTemperatureHumidityHistory();
}

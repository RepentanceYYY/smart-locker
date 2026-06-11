package com.tairui.server.controller;

import com.tairui.server.dto.TempHumidityLogDTO;
import com.tairui.server.service.TempHumidityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 温湿度日志前端控制器
 */
@RestController
@RequestMapping("/api/tempHumidity")
public class TempHumidityController {

    @Autowired
    private TempHumidityService tempHumidityService;

    /**
     * 获取温湿度日志列表
     * @param limit 查询条数，默认10条
     * @return 温湿度日志列表
     */
    @GetMapping("/logs")
    public Map<String, Object> getTempHumidityLogs(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        List<TempHumidityLogDTO> logs = tempHumidityService.getRecentLogs(limit);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", logs);
        return result;
    }

    // 新增：多条件筛选查询
    @GetMapping("/logs/search")
    public Map<String, Object> searchTempHumidityLogs(
            @RequestParam(required = false) String cabinetTitle,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endTime) {

        List<TempHumidityLogDTO> logs = tempHumidityService.searchLogsByCondition(
                cabinetTitle, startTime, endTime);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", logs);
        return result;
    }
}

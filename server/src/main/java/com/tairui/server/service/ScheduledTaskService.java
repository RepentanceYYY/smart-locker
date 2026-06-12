package com.tairui.server.service;

import com.tairui.server.device.dehumidifier.ThData;
import com.tairui.server.deviceService.DehumidifierDeviceServiceManager;
import com.tairui.server.entity.CabinetConfig;
import com.tairui.server.entity.ThHistory;
import com.tairui.server.mapper.CabinetConfigMapper;
import com.tairui.server.mapper.ThHistoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 定时任务服务
 */
@Slf4j
@Service
public class ScheduledTaskService {

    @Autowired
    private DehumidifierDeviceServiceManager dehumidifierDeviceServiceManager;
    
    @Autowired
    private CabinetConfigMapper cabinetConfigMapper;
    
    @Autowired
    private ThHistoryMapper thHistoryMapper;

    /**
     * 每5分钟执行一次，保存温湿度历史数据到数据库
     * cron表达式: 秒 分 时 日 月 周
     * 0 0/5 * * * ? 表示每5分钟的第0秒执行
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void saveTemperatureHumidityHistory() {
        log.info("========== 开始保存温湿度历史数据 ==========");
        
        try {
            // 1. 获取所有柜子配置
            List<CabinetConfig> cabinetConfigs = cabinetConfigMapper.selectList(null);
            if (cabinetConfigs == null || cabinetConfigs.isEmpty()) {
                log.warn("暂无柜子配置，跳过温湿度历史记录");
                return;
            }
            
            // 2. 获取最新温湿度数据
            Map<Integer, ThData> thDataMap = dehumidifierDeviceServiceManager.getRealtimeTemperatureHumidity();
            
            if (thDataMap == null || thDataMap.isEmpty()) {
                log.warn("未获取到温湿度数据，跳过历史记录");
                return;
            }
            
            // 3. 遍历所有柜子，保存历史记录
            int successCount = 0;
            int failCount = 0;
            LocalDateTime now = LocalDateTime.now();
            
            for (CabinetConfig cabinet : cabinetConfigs) {
                try {
                    ThData thData = thDataMap.get(cabinet.getId());
                    if (thData == null) {
                        log.debug("柜子 [{}] 无温湿度数据", cabinet.getTitle());
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
                    
                    log.debug("柜子 [{}] 温湿度历史记录成功: 温度={}°C, 湿度={}%", 
                            cabinet.getTitle(), thData.getTemperature(), thData.getHumidity());
                    
                } catch (Exception e) {
                    failCount++;
                    log.error("柜子 [{}] 温湿度历史记录失败: {}", cabinet.getTitle(), e.getMessage());
                }
            }
            
            log.info("温湿度历史记录完成 - 成功: {} 条, 失败: {} 条", successCount, failCount);
            
        } catch (Exception e) {
            log.error("保存温湿度历史数据失败: {}", e.getMessage(), e);
        }
        
        log.info("========== 温湿度历史数据保存结束 ==========");
    }
}

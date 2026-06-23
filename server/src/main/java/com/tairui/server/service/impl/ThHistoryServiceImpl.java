package com.tairui.server.service.impl;

import com.tairui.server.device.dehumidifier.ThData;
import com.tairui.server.deviceService.DehumidifierDeviceServiceManager;
import com.tairui.server.entity.CabinetConfig;
import com.tairui.server.entity.ThHistory;
import com.tairui.server.mapper.CabinetConfigMapper;
import com.tairui.server.mapper.ThHistoryMapper;
import com.tairui.server.service.ThHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@Slf4j
@Service
public class ThHistoryServiceImpl implements ThHistoryService {
    @Autowired
    private DehumidifierDeviceServiceManager dehumidifierDeviceServiceManager;

    @Autowired
    private CabinetConfigMapper cabinetConfigMapper;

    @Autowired
    private ThHistoryMapper thHistoryMapper;
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
}

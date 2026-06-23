package com.tairui.server.config;

import com.tairui.server.mapper.SystemConfigMapper;
import com.tairui.server.service.ThHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableScheduling
public class TemperatureHumidityScheduleConfig implements SchedulingConfigurer {

    @Autowired
    private ThHistoryService thHistoryService;

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {

        taskRegistrar.addTriggerTask(
                () -> thHistoryService.saveTemperatureHumidityHistory(),

                // 动态调整间隔时间
                triggerContext -> {
                    // 默认间隔时间：5分钟
                    long minutes = 5;
                    try {
                        Integer dbMinutes = systemConfigMapper.selectOne(null).getTempHumidityLogInterval();

                        if (dbMinutes != null && dbMinutes > 0) {
                            minutes = dbMinutes;
                        }
                    } catch (Exception e) {
                        log.error("从数据库获取定时任务间隔分钟数失败，将使用默认间隔(5分钟)", e);
                    }

                    // 使用 PeriodicTrigger，并指定单位为分钟（TimeUnit.MINUTES）
                    PeriodicTrigger periodicTrigger = new PeriodicTrigger(minutes, TimeUnit.MINUTES);
                    

                    // 只有在上一次实际执行时间为 null 时（代表刚启动），才设置初始延迟
                    if (triggerContext.lastActualExecutionTime() == null) {
                        periodicTrigger.setInitialDelay(minutes);
                    }

                    // 计算并返回下一次执行的绝对时间戳
                    return periodicTrigger.nextExecutionTime(triggerContext).toInstant();
                }
        );
    }
}
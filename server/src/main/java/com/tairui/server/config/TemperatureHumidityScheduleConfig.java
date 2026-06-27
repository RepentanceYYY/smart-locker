package com.tairui.server.config;

import com.tairui.server.mapper.SystemConfigMapper;
import com.tairui.server.service.TempHumidityService;
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
    private TempHumidityService tempHumidityService;

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    /**
     * 停状态开关
     */
    private volatile boolean isPaused = false;

    /**
     * 临时暂停
     */
    public void pauseSchedule() {
        this.isPaused = true;
        log.info("[定时记录] 除湿机温湿度历史记录定时任务已临时暂停...");
    }

    /**
     * 恢复运行
     */
    public void resumeSchedule() {
        this.isPaused = false;
        log.info("[定时记录] 除湿机温湿度历史记录定时任务已恢复运行。");
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {

        taskRegistrar.addTriggerTask(
                () -> {
                    if (isPaused) {
                        log.debug("由于系统正在调整硬件配置，本次温湿度历史记录数据写入已被跳过。");
                        return;
                    }
                    tempHumidityService.saveTemperatureHumidityHistory();
                },

                triggerContext -> {
                    long minutes = 5;
                    try {
                        Integer dbMinutes = systemConfigMapper.selectOne(null).getTempHumidityLogInterval();

                        if (dbMinutes != null && dbMinutes > 0) {
                            minutes = dbMinutes;
                        }
                    } catch (Exception e) {
                        log.error("从数据库获取定时任务间隔分钟数失败，将使用默认间隔(5分钟)", e);
                    }

                    PeriodicTrigger periodicTrigger = new PeriodicTrigger(minutes, TimeUnit.MINUTES);

                    if (triggerContext.lastActualExecutionTime() == null) {
                        periodicTrigger.setInitialDelay(minutes);
                    }

                    return periodicTrigger.nextExecutionTime(triggerContext).toInstant();
                }
        );
    }
}
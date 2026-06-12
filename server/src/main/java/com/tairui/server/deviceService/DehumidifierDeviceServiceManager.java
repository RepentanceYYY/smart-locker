package com.tairui.server.deviceService;

import com.tairui.server.device.channel.SerialChannel;
import com.tairui.server.device.channel.TcpClientChannel;
import com.tairui.server.device.core.CommDispatcher;
import com.tairui.server.device.dehumidifier.DehumidifierRunParam;
import com.tairui.server.device.dehumidifier.ThData;
import com.tairui.server.entity.CabinetConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 除湿机设备服务管理
 */
@Service
@Log4j2
public class DehumidifierDeviceServiceManager extends BaseDeviceServiceManager<DehumidifierDeviceService> {

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        super.init("dehumidifierCommType", "dehumidifierCommPort", "除湿机");
    }

    /**
     * 通过柜子配置id，获取设备对象
     */
    public DehumidifierDeviceService getDeviceServiceByCabinetId(Integer cabinetId) {
        return super.getDeviceServiceByCabinetId(cabinetId, "dehumidifierCommType", "dehumidifierCommPort");
    }

    /**
     * 通过柜子配置创建设备对象并建立连接
     */
    @Override
    protected DehumidifierDeviceService createDeviceServiceByCabinetConfig(CabinetConfig cabinetConfig, String commTypeField, String commPortField) {
        String commType = cabinetConfig.getDehumidifierCommType();
        String commPort = cabinetConfig.getDehumidifierCommPort();

        CommDispatcher dispatcher = createDispatcher(commType, commPort);
        DehumidifierDeviceService dehumidifierDeviceService = new DehumidifierDeviceService(Integer.parseInt(cabinetConfig.getDehumidifierAddr()));
        dehumidifierDeviceService.setWriteIntervalTime(50L);
        dehumidifierDeviceService.setCommDispatcher(dispatcher);
        dispatcher.addDevice(dehumidifierDeviceService);

        deviceServiceMap.put(commPort, dehumidifierDeviceService);
        openAndCacheDevice(dehumidifierDeviceService, cabinetConfig, commPort);
        
        return dehumidifierDeviceService;
    }

    /**
     * 关闭设备连接
     */
    @Override
    protected void closeDevice(DehumidifierDeviceService deviceService) throws Exception {
        deviceService.close();
    }

    /**
     * 获取通信类型
     */
    @Override
    protected String getCommType(CabinetConfig config, String fieldName) {
        return config.getDehumidifierCommType();
    }

    /**
     * 获取通信端口
     */
    @Override
    protected String getCommPort(CabinetConfig config, String fieldName) {
        return config.getDehumidifierCommPort();
    }

    /**
     * 从配置中获取通信类型（用于日志）
     */
    @Override
    protected String getCommTypeFromConfig(CabinetConfig config) {
        return config.getDehumidifierCommType();
    }

    /**
     * 删除柜子配置之后
     */
    public void afterDeleteCabinetConfigData(Integer cabinetConfigId, String commPort) {
        super.afterDeleteCabinetConfigData(cabinetConfigId, commPort, "dehumidifierCommPort");
    }

    /**
     * 获取所有柜子最新温湿度
     *
     * @return
     */
    public Map<Integer, ThData> getRealtimeTemperatureHumidity() {
        List<CabinetConfig> cabinetConfigs = cabinetConfigMapper.selectList(null);

        if (cabinetConfigs.isEmpty()) return new HashMap<>();

        Map<Integer, ThData> result = new HashMap<>();

        for (CabinetConfig cabinetConfig : cabinetConfigs) {
            try {
                DehumidifierDeviceService dehumidifierDeviceService = deviceServiceMap.computeIfAbsent(cabinetConfig.getDehumidifierCommPort(), key -> createDeviceServiceByCabinetConfig(cabinetConfig, "dehumidifierCommType", "dehumidifierCommPort"));
                dehumidifierDeviceService.setAddress(Integer.parseInt(cabinetConfig.getDehumidifierAddr()));
                DehumidifierRunParam dehumidifierRunParam = dehumidifierDeviceService.queryRunParam(0, 15);
                result.put(cabinetConfig.getId(), new ThData(dehumidifierRunParam.getAmbientTemperature(), dehumidifierRunParam.getAmbientHumidity()));
            } catch (Exception ex) {
                result.put(cabinetConfig.getId(), new ThData(0D, 0D));
                log.error("{}除湿机温湿度采集失败，原因：{}", cabinetConfig.getTitle(), ex.getMessage());
            }
        }

        return result;
    }
}

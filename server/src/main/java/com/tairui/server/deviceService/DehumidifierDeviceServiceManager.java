package com.tairui.server.deviceService;

import com.tairui.server.device.core.CommDispatcher;
import com.tairui.server.device.dehumidifier.DehumidifierRunParam;
import com.tairui.server.device.dehumidifier.ThData;
import com.tairui.server.entity.CabinetConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 除湿机设备服务管理
 */
@Service
@Log4j2
public class DehumidifierDeviceServiceManager extends BaseDeviceServiceManager<DehumidifierDeviceService> {

    @PostConstruct
    public void init() {
        super.init("除湿机");
    }

    public DehumidifierDeviceService getDeviceServiceByCabinetId(Integer cabinetId) {
        return super.getDeviceServiceByCabinetId(cabinetId, "除湿机");
    }

    /**
     * 如果存在则返回存在的，否则返回新的(新的不会存入Map)
     * @param cabinetConfig
     * @return
     */
    public DehumidifierDeviceService addDeviceServiceByNewCabinetConfig(CabinetConfig cabinetConfig) {
        return super.addDeviceServiceByNewCabinetConfig(cabinetConfig, "除湿机");
    }

    @Override
    protected String getCommType(CabinetConfig config) { return config.getDehumidifierCommType(); }

    @Override
    protected String getCommPort(CabinetConfig config) { return config.getDehumidifierCommPort(); }

    @Override
    protected DehumidifierDeviceService createDeviceInstance(CabinetConfig config, CommDispatcher dispatcher) {
        DehumidifierDeviceService service = new DehumidifierDeviceService(Integer.parseInt(config.getDehumidifierAddr()));
        service.setWriteIntervalTime(50L);
        service.setCommDispatcher(dispatcher);
        dispatcher.addDevice(service);
        return service;
    }

    @Override
    protected void doOpenConnection(DehumidifierDeviceService deviceService) throws Exception {
        deviceService.open(); // 显式调用，排查时按住 Ctrl 就能直接点进去
    }

    @Override
    protected void doCloseConnection(DehumidifierDeviceService deviceService) throws Exception {
        deviceService.close();
    }

    public void afterDeleteCabinetConfigData(Integer cabinetConfigId, String commPort) {
        super.afterDeleteCabinetConfigData(cabinetConfigId, commPort);
    }

    /**
     * 获取实时温湿度(所有柜子)
     */
    public Map<Integer, ThData> getRealtimeTemperatureHumidity() {
        List<CabinetConfig> cabinetConfigs = cabinetConfigMapper.selectList(null);
        Map<Integer, ThData> result = new HashMap<>();
        if (cabinetConfigs.isEmpty()) return result;

        for (CabinetConfig config : cabinetConfigs) {
            try {
                DehumidifierDeviceService service = this.getDeviceServiceByCabinetId(config.getId());

                synchronized (service) {
                    int address = Integer.parseInt(config.getDehumidifierAddr());
                    service.setAddress(address);

                    DehumidifierRunParam param = service.queryRunParam(0, 15);
                    result.put(config.getId(), new ThData(param.getAmbientTemperature(), param.getAmbientHumidity()));
                }

            } catch (Exception ex) {
                result.put(config.getId(), new ThData(0D, 0D));
                log.error("{}除湿机温湿度采集失败，原因：{}", config.getTitle(), ex.getMessage());
            }
        }
        return result;
    }
}

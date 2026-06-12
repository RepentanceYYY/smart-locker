package com.tairui.server.deviceService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tairui.server.device.channel.SerialChannel;
import com.tairui.server.device.channel.TcpClientChannel;
import com.tairui.server.device.core.CommDispatcher;
import com.tairui.server.device.core.SerialDispatcher;
import com.tairui.server.device.core.TcpClientDispatcher;
import com.tairui.server.device.dehumidifier.DehumidifierRunParam;
import com.tairui.server.device.dehumidifier.ThData;
import com.tairui.server.entity.CabinetConfig;
import com.tairui.server.mapper.CabinetConfigMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 除湿机设备服务管理
 */
@Service
@Log4j2
public class DehumidifierDeviceServiceManager {

    @Autowired
    private CabinetConfigMapper cabinetConfigMapper;

    /**
     * key:通信地址，value:设备服务
     */
    private final Map<String, DehumidifierDeviceService> dehumidifierDeviceServiceMap = new ConcurrentHashMap<>();

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        dehumidifierDeviceServiceMap.clear();
        List<CabinetConfig> cabinetConfigs = cabinetConfigMapper.selectList(null);
        if (cabinetConfigs.isEmpty()) {
            log.warn("暂无柜子配置数据，跳过除湿机初始化");
            return;
        }
        for (CabinetConfig cabinetConfig : cabinetConfigs) {
            try {
                String commPort = cabinetConfig.getDehumidifierCommPort();
                if (!StringUtils.hasText(cabinetConfig.getDehumidifierCommType())) {
                    throw new RuntimeException(cabinetConfig.getTitle() + "除湿机通信类型未配置");
                }
                if (!StringUtils.hasText(commPort)) {
                    throw new RuntimeException(cabinetConfig.getTitle() + "除湿机通信地址未配置");
                }

                if (dehumidifierDeviceServiceMap.containsKey(commPort)) {
                    log.info("{} 除湿机的通信地址 {} 已被其他柜子初始化，共享同一连接。", cabinetConfig.getTitle(), commPort);
                    continue;
                }

                // 创建并打开连接，并存入全局 Map 缓存
                this.createDeviceServiceByCabinetConfig(cabinetConfig);

            } catch (Exception e) {
                log.error("初始化柜子 [{}] 失败: {}", cabinetConfig.getTitle(), e.getMessage());
            }
        }
    }

    /**
     * 通过柜子配置id，获取设备对象
     */
    public DehumidifierDeviceService getDeviceServiceByCabinetId(Integer cabinetId) {
        CabinetConfig cabinetConfig = cabinetConfigMapper.selectById(cabinetId);
        if (cabinetConfig == null) {
            throw new RuntimeException("柜子配置不存在");
        }
        if (!StringUtils.hasText(cabinetConfig.getDehumidifierCommType())) {
            throw new RuntimeException(cabinetConfig.getTitle() + "通信类型未配置");
        }
        if (!StringUtils.hasText(cabinetConfig.getDehumidifierCommPort())) {
            throw new RuntimeException(cabinetConfig.getTitle() + "通信地址未配置");
        }
        return dehumidifierDeviceServiceMap.computeIfAbsent(cabinetConfig.getDehumidifierCommPort(), key -> createDeviceServiceByCabinetConfig(cabinetConfig));
    }

    /**
     * 通过柜子配置创建设备对象并建立连接
     */
    private DehumidifierDeviceService createDeviceServiceByCabinetConfig(CabinetConfig cabinetConfig) {

        CommDispatcher dispatcher;  // 通信调度器
        DehumidifierDeviceService dehumidifierDeviceService;    // 设备服务
        String commPort = cabinetConfig.getDehumidifierCommPort();

        if ("485".equalsIgnoreCase(cabinetConfig.getDehumidifierCommType())) {
            String[] parts = commPort.split("@");
            if (parts.length != 2) {
                throw new RuntimeException("串口配置格式错误，应为 端口@波特率，如 com1@115200");
            }
            String portName = parts[0];
            int baudRate;
            try {
                baudRate = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                throw new RuntimeException("波特率格式错误: " + parts[1]);
            }
            SerialChannel channel = new SerialChannel(portName, baudRate);
            dispatcher = new SerialDispatcher(channel);
        } else if ("TCP".equalsIgnoreCase(cabinetConfig.getDehumidifierCommType())) {
            String[] parts = commPort.split(":");
            if (parts.length != 2) {
                throw new RuntimeException("TCP配置格式错误，应为 IP:端口，如 192.168.1.2:8456");
            }
            String host = parts[0];
            int port;
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                throw new RuntimeException("端口号格式错误: " + parts[1]);
            }
            TcpClientChannel channel = new TcpClientChannel(host, port);
            dispatcher = new TcpClientDispatcher(channel);
        } else {
            throw new RuntimeException("不支持的除湿机通讯类型: " + cabinetConfig.getDehumidifierCommType());
        }

        dehumidifierDeviceService = new DehumidifierDeviceService(Integer.parseInt(cabinetConfig.getDehumidifierAddr()));
        dehumidifierDeviceService.setWriteIntervalTime(50L);
        dehumidifierDeviceService.setCommDispatcher(dispatcher);
        dispatcher.addDevice(dehumidifierDeviceService);

        log.info("除湿机硬件控制器初始化完成，真实模式，通讯方式: {}，地址: {}", cabinetConfig.getDehumidifierCommType(), commPort);
        try {
            dehumidifierDeviceService.open();
            log.info("{}除湿机使用的 {} 打开连接成功", cabinetConfig.getTitle(), commPort);

            dehumidifierDeviceServiceMap.put(commPort, dehumidifierDeviceService);

        } catch (IOException e) {
            log.error("{}除湿机使用的 {} 打开连接失败，原因:{}", cabinetConfig.getTitle(), commPort, e.getMessage());
        }
        return dehumidifierDeviceService;
    }

    /**
     * 删除柜子配置之后
     */
    public void afterDeleteCabinetConfigData(Integer cabinetConfigId, String commPort) {
        List<CabinetConfig> cabinetConfigs = cabinetConfigMapper.selectList(new LambdaQueryWrapper<CabinetConfig>().eq(CabinetConfig::getDehumidifierCommPort, commPort));
        if (cabinetConfigs.isEmpty()) {
            removeDeviceServiceByCommPort(commPort);
            return;
        }
        // 使用这个通信地址的其他柜子除湿机总数量
        long count = cabinetConfigs.stream()
                .filter(x -> !cabinetConfigId.equals(x.getId()))
                .filter(x -> commPort.equals(x.getDehumidifierCommPort()))
                .count();
        if (count <= 0) {
            removeDeviceServiceByCommPort(commPort);
        }
    }

    /**
     * 通过通信地址删除设备并关闭连接
     */
    public void removeDeviceServiceByCommPort(String commPort) {
        DehumidifierDeviceService remove = dehumidifierDeviceServiceMap.remove(commPort);
        if (remove != null) {
            try {
                remove.close();
            } catch (Exception e) {
                throw new RuntimeException("关闭除湿机设备连接失败", e);
            }
        }
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
                if (!StringUtils.hasText(cabinetConfig.getDehumidifierCommType())) {
                    throw new RuntimeException(cabinetConfig.getTitle() + "通信类型未配置");
                }
                if (!StringUtils.hasText(cabinetConfig.getDehumidifierCommPort())) {
                    throw new RuntimeException(cabinetConfig.getTitle() + "通信地址未配置");
                }
                DehumidifierDeviceService dehumidifierDeviceService = dehumidifierDeviceServiceMap.computeIfAbsent(cabinetConfig.getDehumidifierCommPort(), key -> createDeviceServiceByCabinetConfig(cabinetConfig));
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

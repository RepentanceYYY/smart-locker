package com.tairui.server.deviceService;

import com.tairui.server.device.channel.SerialChannel;
import com.tairui.server.device.channel.TcpClientChannel;
import com.tairui.server.device.core.CommDispatcher;
import com.tairui.server.device.core.SerialDispatcher;
import com.tairui.server.device.core.TcpClientDispatcher;
import com.tairui.server.entity.CabinetConfig;
import com.tairui.server.mapper.CabinetConfigMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备服务管理基类
 * 提供通用的设备初始化、连接管理、生命周期管理等能力
 *
 * @param <T> 设备服务类型
 */
@Log4j2
public abstract class BaseDeviceServiceManager<T> {

    @Autowired
    protected CabinetConfigMapper cabinetConfigMapper;

    /**
     * key:通信地址，value:设备服务
     */
    protected final Map<String, T> deviceServiceMap = new ConcurrentHashMap<>();

    /**
     * 初始化所有设备连接
     * 在子类中通过 @PostConstruct 调用
     */
    protected void init(String commTypeField, String commPortField, String deviceTitle) {
        deviceServiceMap.clear();
        List<CabinetConfig> cabinetConfigs = cabinetConfigMapper.selectList(null);
        if (cabinetConfigs.isEmpty()) {
            log.warn("暂无柜子配置数据，跳过{}初始化", deviceTitle);
            return;
        }
        for (CabinetConfig cabinetConfig : cabinetConfigs) {
            try {
                String commType = getCommType(cabinetConfig, commTypeField);
                String commPort = getCommPort(cabinetConfig, commPortField);

                if (!StringUtils.hasText(commType)) {
                    throw new RuntimeException(cabinetConfig.getTitle() + deviceTitle + "通信类型未配置");
                }
                if (!StringUtils.hasText(commPort)) {
                    throw new RuntimeException(cabinetConfig.getTitle() + deviceTitle + "通信地址未配置");
                }

                if (deviceServiceMap.containsKey(commPort)) {
                    log.info("{} {}的通信地址 {} 已被其他柜子初始化，共享同一连接。", cabinetConfig.getTitle(), deviceTitle, commPort);
                    continue;
                }

                // 创建并打开连接，并存入全局 Map 缓存
                this.createDeviceServiceByCabinetConfig(cabinetConfig, commTypeField, commPortField);

            } catch (Exception e) {
                log.error("初始化柜子 [{}] 失败: {}", cabinetConfig.getTitle(), e.getMessage());
            }
        }
    }

    /**
     * 通过柜子ID获取设备服务对象
     */
    protected T getDeviceServiceByCabinetId(Integer cabinetId, String commTypeField, String commPortField) {
        CabinetConfig cabinetConfig = cabinetConfigMapper.selectById(cabinetId);
        if (cabinetConfig == null) {
            throw new RuntimeException("柜子配置不存在");
        }
        String commType = getCommType(cabinetConfig, commTypeField);
        String commPort = getCommPort(cabinetConfig, commPortField);

        if (!StringUtils.hasText(commType)) {
            throw new RuntimeException(cabinetConfig.getTitle() + "通信类型未配置");
        }
        if (!StringUtils.hasText(commPort)) {
            throw new RuntimeException(cabinetConfig.getTitle() + "通信地址未配置");
        }
        return deviceServiceMap.computeIfAbsent(commPort, key -> createDeviceServiceByCabinetConfig(cabinetConfig, commTypeField, commPortField));
    }

    /**
     * 删除柜子配置后的清理工作
     */
    protected void afterDeleteCabinetConfigData(Integer cabinetConfigId, String commPort, String commPortField) {
        // 由于字段名是动态的，这里使用字符串比较
        List<CabinetConfig> cabinetConfigs = cabinetConfigMapper.selectList(null).stream()
                .filter(config -> commPort.equals(getCommPort(config, commPortField)))
                .toList();

        if (cabinetConfigs.isEmpty()) {
            removeDeviceServiceByCommPort(commPort);
            return;
        }
        // 使用这个通信地址的其他柜子总数量
        long count = cabinetConfigs.stream()
                .filter(x -> !cabinetConfigId.equals(x.getId()))
                .count();
        if (count <= 0) {
            removeDeviceServiceByCommPort(commPort);
        }
    }

    /**
     * 通过通信地址删除设备并关闭连接
     */
    protected void removeDeviceServiceByCommPort(String commPort) {
        T remove = deviceServiceMap.remove(commPort);
        if (remove != null) {
            try {
                closeDevice(remove);
            } catch (Exception e) {
                throw new RuntimeException("关闭设备连接失败", e);
            }
        }
    }

    /**
     * 创建通信调度器
     */
    protected CommDispatcher createDispatcher(String commType, String commPort) {
        if ("485".equalsIgnoreCase(commType)) {
            return createSerialDispatcher(commPort);
        } else if ("TCP".equalsIgnoreCase(commType)) {
            return createTcpDispatcher(commPort);
        } else {
            throw new RuntimeException("不支持的通讯类型: " + commType);
        }
    }

    /**
     * 创建串口调度器
     */
    private CommDispatcher createSerialDispatcher(String commPort) {
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
        return new SerialDispatcher(channel);
    }

    /**
     * 创建TCP调度器
     */
    private CommDispatcher createTcpDispatcher(String commPort) {
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
        return new TcpClientDispatcher(channel);
    }

    /**
     * 打建设备连接并缓存
     */
    protected void openAndCacheDevice(T deviceService, CabinetConfig cabinetConfig, String commPort) {
        try {
            deviceService.getClass().getMethod("open").invoke(deviceService);
            log.info("{}使用的 {} 打开连接成功", cabinetConfig.getTitle(), commPort);
            String commType = getCommTypeFromConfig(cabinetConfig);
            log.info("硬件控制器初始化完成，真实模式，通讯方式: {}，地址: {}", commType, commPort);
        } catch (Exception e) {
            log.error("{}使用的 {} 打开连接失败，原因:{}", cabinetConfig.getTitle(), commPort, e.getMessage());
        }
    }

    // ==================== 抽象方法，由子类实现 ====================

    /**
     * 通过柜子配置创建设备服务对象
     */
    protected abstract T createDeviceServiceByCabinetConfig(CabinetConfig cabinetConfig, String commTypeField, String commPortField);

    /**
     * 关闭设备连接
     */
    protected abstract void closeDevice(T deviceService) throws Exception;

    /**
     * 获取通信类型
     */
    protected abstract String getCommType(CabinetConfig config, String fieldName);

    /**
     * 获取通信端口
     */
    protected abstract String getCommPort(CabinetConfig config, String fieldName);

    /**
     * 从配置中获取通信类型（用于日志）
     */
    protected abstract String getCommTypeFromConfig(CabinetConfig config);
}

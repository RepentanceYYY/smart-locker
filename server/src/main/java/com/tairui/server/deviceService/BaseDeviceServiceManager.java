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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public abstract class BaseDeviceServiceManager<T> {

    @Autowired
    protected CabinetConfigMapper cabinetConfigMapper;

    /**
     * key:通信地址 (IP:Port 或 COM@Baud)，value:设备服务
     */
    protected final Map<String, T> deviceServiceMap = new ConcurrentHashMap<>();

    protected abstract String getCommType(CabinetConfig config);

    protected abstract String getCommPort(CabinetConfig config);

    protected abstract T createDeviceInstance(CabinetConfig config, CommDispatcher dispatcher);

    protected abstract void doOpenConnection(T deviceService) throws Exception;

    protected abstract void doCloseConnection(T deviceService) throws Exception;


    /**
     * 初始化所有设备连接
     */
    protected void init(String deviceTitle) {
        deviceServiceMap.clear();
        List<CabinetConfig> cabinetConfigs = cabinetConfigMapper.selectList(null);
        if (cabinetConfigs.isEmpty()) {
            log.warn("暂无柜子配置数据，跳过{}初始化", deviceTitle);
            return;
        }
        for (CabinetConfig config : cabinetConfigs) {
            try {
                String commPort = getCommPort(config);
                if (deviceServiceMap.containsKey(commPort)) {
                    log.info("{} {}的通信地址 {} 已被其他柜子初始化，共享同一连接。", config.getTitle(), deviceTitle, commPort);
                    continue;
                }
                // 统一由基类进行：构建 -> 开启连接 -> 塞入缓存
                setupAndCacheDevice(config, deviceTitle);
            } catch (Exception e) {
                log.error("初始化柜子 [{}] [{}] 失败: {}", config.getTitle(),deviceTitle, e.getMessage());
            }
        }
    }

    /**
     * 核心统一的构建、连接、缓存流程
     */
    private T setupAndCacheDevice(CabinetConfig config, String deviceTitle) throws Exception {
        String commType = getCommType(config);
        String commPort = getCommPort(config);

        if (!StringUtils.hasText(commType) || !StringUtils.hasText(commPort)) {
            throw new RuntimeException(config.getTitle() + deviceTitle + "通信配置不完整");
        }

        CommDispatcher dispatcher = createDispatcher(commType, commPort);
        T deviceService = createDeviceInstance(config, dispatcher);

        // 显式调用打开连接，拒绝反射
        doOpenConnection(deviceService);
        log.info("{}使用的 {} 打开连接成功。通讯方式: {}，地址: {}", config.getTitle(), deviceTitle, commType, commPort);

        deviceServiceMap.put(commPort, deviceService);
        return deviceService;
    }

    /**
     * 通过柜子ID获取或动态创建设备服务对象
     */
    protected T getDeviceServiceByCabinetId(Integer cabinetId, String deviceTitle) {
        CabinetConfig config = cabinetConfigMapper.selectById(cabinetId);
        if (config == null) throw new RuntimeException("柜子配置不存在ID: " + cabinetId);

        String commPort = getCommPort(config);
        return deviceServiceMap.computeIfAbsent(commPort, key -> {
            try {
                return setupAndCacheDevice(config, deviceTitle);
            } catch (Exception e) {
                throw new RuntimeException("动态创建设备服务失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 新增/修改配置时的校验逻辑（干净返回，不打开连接，不写Map）
     */
    protected T addDeviceServiceByNewCabinetConfig(CabinetConfig config, String deviceTitle) {
        String commType = getCommType(config);
        String commPort = getCommPort(config);

        if (!StringUtils.hasText(commType) || !StringUtils.hasText(commPort)) {
            throw new RuntimeException(config.getTitle() + deviceTitle + "通信配置不完整");
        }

        if (deviceServiceMap.containsKey(commPort)) {
            return deviceServiceMap.get(commPort);
        }

        if ("485".equalsIgnoreCase(commType)) {
            validateSerialPortConflict(commPort, deviceTitle);
        }

        CommDispatcher dispatcher = createDispatcher(commType, commPort);
        return createDeviceInstance(config, dispatcher);
    }

    /**
     * 删除配置后的清理
     */
    protected void afterDeleteCabinetConfigData(Integer cabinetConfigId, String commPort) {
        List<CabinetConfig> allConfigs = cabinetConfigMapper.selectList(null);
        // 检查是否还有其他柜子在使用这个通信端口
        long count = allConfigs.stream()
                .filter(config -> commPort.equals(getCommPort(config)) && !cabinetConfigId.equals(config.getId()))
                .count();

        if (count == 0) {
            removeDeviceServiceByCommPort(commPort);
        }
    }

    public void removeDeviceServiceByCommPort(String commPort) {
        T service = deviceServiceMap.remove(commPort);
        if (service != null) {
            try {
                doCloseConnection(service);
            } catch (Exception e) {
                log.error("关闭设备连接失败, port: {}", commPort, e);
            }
        }
    }

    /**
     * 关闭所有设备连接并清空缓存服务
     */
    public void shutdown() {

        Collection<T> services = deviceServiceMap.values();

        for (T service : services) {
            if (service != null) {
                try {
                    doCloseConnection(service);
                } catch (Exception e) {
                    log.error("安全关闭设备连接失败", e);
                }
            }
        }
        deviceServiceMap.clear();
    }

    /**
     * 创建调度器
     *
     * @param commType
     * @param commPort
     * @return
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
     *
     * @param commPort
     * @return
     */
    private CommDispatcher createSerialDispatcher(String commPort) {
        String[] parts = commPort.split("@");
        if (parts.length != 2) throw new RuntimeException("串口格式错误: " + commPort);
        return new SerialDispatcher(new SerialChannel(parts[0], Integer.parseInt(parts[1])));
    }

    /**
     * 创建Tcp调度器
     *
     * @param commPort
     * @return
     */
    private CommDispatcher createTcpDispatcher(String commPort) {
        String[] parts = commPort.split(":");
        if (parts.length != 2) throw new RuntimeException("TCP格式错误: " + commPort);
        return new TcpClientDispatcher(new TcpClientChannel(parts[0], Integer.parseInt(parts[1])));
    }

    /**
     * 校验是否有串口号相同，波特率不同的通信地址
     *
     * @param newCommPort
     * @param deviceTitle
     */
    protected void validateSerialPortConflict(String newCommPort, String deviceTitle) {
        String[] newParts = newCommPort.split("@");
        if (newParts.length != 2) return;
        String newPortName = newParts[0].trim();
        String newBaudRate = newParts[1].trim();

        for (String existCommPort : deviceServiceMap.keySet()) {
            if (existCommPort.contains("@")) {
                String[] existParts = existCommPort.split("@");
                if (existParts[0].trim().equalsIgnoreCase(newPortName) && !existParts[1].trim().equals(newBaudRate)) {
                    throw new RuntimeException(String.format("创建失败！%s串口 [%s] 已被使用，当前波特率 [%s] 与已有波特率 [%s] 不一致！",
                            deviceTitle, newPortName, newBaudRate, existParts[1].trim()));
                }
            }
        }
    }

    public Map<String, T> getDeviceServiceMap() {
        return deviceServiceMap;
    }
}

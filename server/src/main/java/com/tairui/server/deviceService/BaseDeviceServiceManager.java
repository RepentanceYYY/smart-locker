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
    public void removeDeviceServiceByCommPort(String commPort) {
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
     * 获取设备服务Map（供外部查询使用）
     */
    public Map<String, T> getDeviceServiceMap() {
        return deviceServiceMap;
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

    // ==================== 通用方法：添加/校验新设备服务 ====================

    /**
     * 添加/校验新柜子设备服务（供Service层创建/修改柜子时调用）
     * 1. 如果存在完全相同的通信端口（TCP的IP:Port，或485的COM@Baud），直接返回已有服务
     * 2. 如果是485，且使用了相同的物理串口名称（如COM1），但波特率不同，则拒绝通过
     * 3. 校验通过后，仅创建并返回对象，【不打开连接】，【不存入Map】
     *
     * @param cabinetConfig   新增或修改后的柜子配置
     * @param commTypeField   通信类型字段名
     * @param commPortField   通信端口字段名
     * @param deviceTitle     设备标题（用于错误提示）
     * @return 实例化后的设备服务对象
     */
    protected T addDeviceServiceByNewCabinetConfig(CabinetConfig cabinetConfig, String commTypeField, String commPortField, String deviceTitle) {
        String newCommType = getCommType(cabinetConfig, commTypeField);
        if (!StringUtils.hasText(newCommType)) {
            throw new RuntimeException(cabinetConfig.getTitle() + deviceTitle + "通信类型未配置");
        }
        
        String newCommPort = getCommPort(cabinetConfig, commPortField);
        if (!StringUtils.hasText(newCommPort)) {
            throw new RuntimeException(cabinetConfig.getTitle() + deviceTitle + "通信地址未配置");
        }

        // 逻辑 1：完全一模一样的配置已存在，直接返回现有服务
        if (deviceServiceMap.containsKey(newCommPort)) {
            log.info("{} {}的通信地址 {} 已存在，直接返回现有服务对象。", cabinetConfig.getTitle(), deviceTitle, newCommPort);
            return deviceServiceMap.get(newCommPort);
        }

        // 逻辑 2：如果是 485 模式，额外校验"同串口、不同波特率"的冲突情况
        if ("485".equalsIgnoreCase(newCommType)) {
            validateSerialPortConflict(newCommPort, deviceTitle);
        }

        return this.buildPureDeviceService(cabinetConfig, commTypeField, commPortField);
    }

    /**
     * 校验485串口冲突：同一物理串口不能使用不同波特率
     *
     * @param newCommPort 新的通信端口配置（格式：端口@波特率）
     * @param deviceTitle 设备标题
     */
    protected void validateSerialPortConflict(String newCommPort, String deviceTitle) {
        String[] newParts = newCommPort.split("@");
        if (newParts.length != 2) {
            throw new RuntimeException("串口配置格式错误，应为 端口@波特率，如 com1@115200");
        }
        String newPortName = newParts[0].trim();
        String newBaudRate = newParts[1].trim();

        // 遍历 Map 中现有的设备连接
        for (String existCommPort : deviceServiceMap.keySet()) {
            if (existCommPort.contains("@")) {
                String[] existParts = existCommPort.split("@");
                String existPortName = existParts[0].trim();
                String existBaudRate = existParts[1].trim();

                // 如果物理串口名字相同（忽略大小写，如 COM1 和 com1）
                if (existPortName.equalsIgnoreCase(newPortName)) {
                    // 物理串口相同，但波特率不同，视为不通过
                    if (!existBaudRate.equals(newBaudRate)) {
                        throw new RuntimeException(String.format(
                                "创建失败！%s串口 [%s] 已被使用，当前配置波特率 [%s] 与已有波特率 [%s] 不一致！",
                                deviceTitle, newPortName, newBaudRate, existBaudRate
                        ));
                    }
                }
            }
        }
    }

    /**
     * 纯粹构建设备对象和基础通道绑定，不触发 open()，不影响全局 map
     *
     * @param cabinetConfig 柜子配置
     * @param commTypeField 通信类型字段名
     * @param commPortField 通信端口字段名
     * @return 设备服务对象
     */
    protected T buildPureDeviceService(CabinetConfig cabinetConfig, String commTypeField, String commPortField) {
        String commType = getCommType(cabinetConfig, commTypeField);
        String commPort = getCommPort(cabinetConfig, commPortField);

        CommDispatcher dispatcher = createDispatcher(commType, commPort);
        T deviceService = createDeviceServiceWithoutCache(cabinetConfig, commTypeField, commPortField, dispatcher);

        return deviceService;
    }

    /**
     * 创建设备服务对象但不缓存（由子类实现具体的设备服务实例化逻辑）
     * 注意：此方法不会将设备放入 Map，也不会打开连接
     *
     * @param cabinetConfig 柜子配置
     * @param commTypeField 通信类型字段名
     * @param commPortField 通信端口字段名
     * @param dispatcher    通信调度器
     * @return 设备服务对象
     */
    protected abstract T createDeviceServiceWithoutCache(CabinetConfig cabinetConfig, String commTypeField, String commPortField, CommDispatcher dispatcher);

    public void reset(){

    }
}

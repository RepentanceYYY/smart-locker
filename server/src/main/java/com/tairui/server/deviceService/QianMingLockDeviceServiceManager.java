package com.tairui.server.deviceService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tairui.server.device.channel.SerialChannel;
import com.tairui.server.device.channel.TcpClientChannel;
import com.tairui.server.device.core.CommDispatcher;
import com.tairui.server.device.core.SerialDispatcher;
import com.tairui.server.device.core.TcpClientDispatcher;
import com.tairui.server.device.qianMingLock.QianMingLockDevice;
import com.tairui.server.entity.CabinetConfig;
import com.tairui.server.entity.CellConfig;
import com.tairui.server.mapper.CabinetConfigMapper;
import com.tairui.server.mapper.CellConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 千鸣锁设备服务管理
 */
@Service
public class QianMingLockDeviceServiceManager {
    @Autowired
    private CellConfigMapper cellConfigMapper;
    @Autowired
    private CabinetConfigMapper cabinetConfigMapper;
    @Value("${lock.simulation.mode:true}")
    private boolean simulationMode;
    /**
     * key:通信地址，value:设备服务
     */
    private final Map<String, QianMingLockDeviceService> qianmingLockDeviceServiceMap = new ConcurrentHashMap<>();

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        List<CabinetConfig> cabinetConfigs = cabinetConfigMapper.selectList(null);
        if (cabinetConfigs.isEmpty()) {
            System.out.println("暂无柜子配置数据，跳过初始化");
            return;
        }
        for (CabinetConfig cabinetConfig : cabinetConfigs) {
            try {
                String commPort = cabinetConfig.getLockCommPort();
                if (!StringUtils.hasText(cabinetConfig.getLockCommType())) {
                    throw new RuntimeException(cabinetConfig.getTitle() + "通信类型未配置");
                }
                if (!StringUtils.hasText(commPort)) {
                    throw new RuntimeException(cabinetConfig.getTitle() + "通信地址未配置");
                }

                if (qianmingLockDeviceServiceMap.containsKey(commPort)) {
                    System.out.println(cabinetConfig.getTitle() + " 的通信地址 " + commPort + " 已被其他柜子初始化，共享同一连接。");
                    continue;
                }

                // 创建并打开连接，并存入全局 Map 缓存
                this.createDeviceServiceByCabinetConfig(cabinetConfig);

            } catch (Exception e) {
                System.out.println("初始化柜子 [" + cabinetConfig.getTitle() + "] 失败: " + e.getMessage());
            }
        }
    }

    /**
     * 通过柜子配置id，获取设备对象
     */
    public QianMingLockDeviceService getDeviceServiceByCabinetId(Integer cabinetId) {
        CabinetConfig cabinetConfig = cabinetConfigMapper.selectById(cabinetId);
        if (cabinetConfig == null) {
            throw new RuntimeException("柜子配置不存在");
        }
        if (!StringUtils.hasText(cabinetConfig.getLockCommType())) {
            throw new RuntimeException(cabinetConfig.getTitle() + "通信类型未配置");
        }
        if (!StringUtils.hasText(cabinetConfig.getLockCommPort())) {
            throw new RuntimeException(cabinetConfig.getTitle() + "通信地址未配置");
        }
        return qianmingLockDeviceServiceMap.computeIfAbsent(cabinetConfig.getLockCommPort(), key -> createDeviceServiceByCabinetConfig(cabinetConfig));
    }

    /**
     * 通过格口配置获取设备对象
     */
    public QianMingLockDeviceService getDeviceServiceByCellConfig(CellConfig cellConfig) {
        if (cellConfig == null) {
            throw new RuntimeException("格口配置不存在");
        }
        if (Objects.isNull(cellConfig.getCabinetId())) {
            throw new RuntimeException("格口未绑定柜子");
        }
        return this.getDeviceServiceByCabinetId(cellConfig.getCabinetId());
    }

    /**
     * 通过柜子配置创建设备对象并建立连接
     */
    private QianMingLockDeviceService createDeviceServiceByCabinetConfig(CabinetConfig cabinetConfig) {
        CommDispatcher dispatcher = null;
        QianMingLockDeviceService qianMingLockDeviceService = null;
        String commPort = cabinetConfig.getLockCommPort();

        if ("485".equalsIgnoreCase(cabinetConfig.getLockCommType())) {
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
        } else if ("TCP".equalsIgnoreCase(cabinetConfig.getLockCommType())) {
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
            throw new RuntimeException("不支持的锁板通讯类型: " + cabinetConfig.getLockCommType());
        }

        qianMingLockDeviceService = new QianMingLockDeviceService();
        qianMingLockDeviceService.setCommDispatcher(dispatcher);
        dispatcher.setDeviceBase(qianMingLockDeviceService);
        qianMingLockDeviceService.setSimulationMode(simulationMode);

        System.out.println("硬件控制器初始化完成，真实模式，通讯方式: " + cabinetConfig.getLockCommType() + "，地址: " + commPort);
        try {
            qianMingLockDeviceService.open();
            System.out.println(cabinetConfig.getTitle() + "使用的 " + commPort + " 打开连接成功");

            qianmingLockDeviceServiceMap.put(commPort, qianMingLockDeviceService);

        } catch (IOException e) {
            System.out.println(cabinetConfig.getTitle() + "使用的 " + commPort + " 打开连接失败，原因:" + e.getMessage());
        }
        return qianMingLockDeviceService;
    }

    /**
     * 删除柜子配置之后
     */
    public void afterDeleteCabinetConfigData(Integer cabinetConfigId, String commPort) {
        List<CabinetConfig> cabinetConfigs = cabinetConfigMapper.selectList(new LambdaQueryWrapper<CabinetConfig>().eq(CabinetConfig::getLockCommPort, commPort));
        if (cabinetConfigs.isEmpty()) {
            removeDeviceServiceByCommPort(commPort);
            return;
        }
        // 使用这个通信地址的其他柜子总数量
        long count = cabinetConfigs.stream()
                .filter(x -> !cabinetConfigId.equals(x.getId()))
                .filter(x -> commPort.equals(x.getLockCommPort()))
                .count();
        if (count <= 0) {
            removeDeviceServiceByCommPort(commPort);
        }
    }

    /**
     * 通过通信地址删除设备并关闭连接
     */
    public void removeDeviceServiceByCommPort(String commPort) {
        QianMingLockDeviceService remove = qianmingLockDeviceServiceMap.remove(commPort);
        if (remove != null) {
            try {
                remove.close();
            } catch (Exception e) {
                throw new RuntimeException("关闭设备连接失败", e);
            }
        }
    }

    /**
     * 通过格口id打开箱门
     */
    public boolean openBoxSync(int cellConfigId, long timeout) {
        DeviceContext ctx = prepareDeviceContext(cellConfigId);
        try {
            return ctx.getDevice().openBoxSync(ctx.getMac(), timeout);
        } catch (Exception e) {
            throw new RuntimeException("格口开门失败，cellId=" + cellConfigId, e);
        }
    }

    /**
     * 查询指定格口号所在板子所有格口的箱门锁状态
     */
    public QianMingLockDevice.BoxStatusData queryBoxStatusSync(int cellConfigId, long timeout) throws Exception {
        DeviceContext ctx = prepareDeviceContext(cellConfigId);
        try {
            return ctx.getDevice().queryBoxStatusSync(ctx.getMac(), timeout);
        } catch (Exception e) {
            throw new RuntimeException("格口状态查询失败，cellId=" + cellConfigId, e);
        }
    }

    /**
     * 查询指定格口号所在板子所有的格口号的储物状态
     */
    public QianMingLockDevice.BoxGoodsData queryGoodsStatusSync(int cellConfigId, long timeout) {
        DeviceContext ctx = prepareDeviceContext(cellConfigId);
        try {
            return ctx.getDevice().queryGoodsStatusSync(ctx.getMac(), timeout);
        } catch (Exception e) {
            throw new RuntimeException("格口状态查询失败，cellId=" + cellConfigId, e);
        }
    }

    /**
     * 辅助方法
     */
    private DeviceContext prepareDeviceContext(int cellConfigId) {
        CellConfig cellConfig = cellConfigMapper.selectById(cellConfigId);
        if (cellConfig == null) {
            throw new RuntimeException("格口配置不存在: " + cellConfigId);
        }

        QianMingLockDeviceService deviceService = this.getDeviceServiceByCellConfig(cellConfig);

        try {
            deviceService.open();
        } catch (IOException e) {
            throw new RuntimeException("格口打开失败：设备连接失败，原因：" + e.getMessage(), e);
        }

        int mac;
        try {
            mac = Integer.parseInt(cellConfig.getMacAddress());
        } catch (Exception e) {
            throw new RuntimeException("格口打开失败：MAC地址格式不正确：" + cellConfig.getMacAddress(), e);
        }

        return new DeviceContext(deviceService, mac, cellConfig);
    }

    /**
     * 辅助类
     */
    private static class DeviceContext {
        private final QianMingLockDeviceService device;
        private final int mac;
        private final CellConfig cellConfig;

        public DeviceContext(QianMingLockDeviceService device, int mac, CellConfig cellConfig) {
            this.device = device;
            this.mac = mac;
            this.cellConfig = cellConfig;
        }

        public QianMingLockDeviceService getDevice() {
            return device;
        }

        public int getMac() {
            return mac;
        }

        public CellConfig getCellConfig() {
            return cellConfig;
        }
    }
}
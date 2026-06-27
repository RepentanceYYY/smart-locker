package com.tairui.server.deviceService;

import com.tairui.server.device.core.CommDispatcher;
import com.tairui.server.device.qianMingLock.QianMingLockDevice;
import com.tairui.server.entity.CabinetConfig;
import com.tairui.server.entity.CellConfig;
import com.tairui.server.mapper.CellConfigMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 千鸣锁设备服务管理
 */
@Service
@Log4j2
public class QianMingLockDeviceServiceManager extends BaseDeviceServiceManager<QianMingLockDeviceService> {

    @Autowired
    private CellConfigMapper cellConfigMapper;

    @Value("${lock.simulation.mode}")
    private boolean simulationMode;

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        super.init("锁板");
    }

    /**
     * 通过柜子配置id，获取设备对象
     * (通过柜子配置id，拿到柜子配置通信类型和通信地址)
     */
    public QianMingLockDeviceService getDeviceServiceByCabinetId(Integer cabinetId) {
        return super.getDeviceServiceByCabinetId(cabinetId, "锁板");
    }

    /**
     * 如果存在则返回存在的，否则返回新的(新的不会存入Map)
     * @param cabinetConfig
     * @return
     */
    public QianMingLockDeviceService addDeviceServiceByNewCabinetConfig(CabinetConfig cabinetConfig) {
        return super.addDeviceServiceByNewCabinetConfig(cabinetConfig, "锁板");
    }

    /**
     * 删除柜子配置之后
     */
    public void afterDeleteCabinetConfigData(Integer cabinetConfigId, String commPort) {
        super.afterDeleteCabinetConfigData(cabinetConfigId, commPort);
    }

    @Override
    protected String getCommType(CabinetConfig config) {
        return config.getLockCommType();
    }

    @Override
    protected String getCommPort(CabinetConfig config) {
        return config.getLockCommPort();
    }

    @Override
    protected QianMingLockDeviceService createDeviceInstance(CabinetConfig config, CommDispatcher dispatcher) {
        QianMingLockDeviceService qianMingLockDeviceService = new QianMingLockDeviceService();
        qianMingLockDeviceService.setCommDispatcher(dispatcher);
        dispatcher.addDevice(qianMingLockDeviceService);
        qianMingLockDeviceService.setSimulationMode(simulationMode);
        return qianMingLockDeviceService;
    }

    @Override
    protected void doOpenConnection(QianMingLockDeviceService deviceService) throws Exception {
        deviceService.open();
    }

    @Override
    protected void doCloseConnection(QianMingLockDeviceService deviceService) throws Exception {
        deviceService.close();
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

    public boolean querySingleBoxStatusSync(int cellConfigId, long timeout) {
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
        try {
            QianMingLockDevice.BoxStatusData boxStatusData = deviceService.queryBoxStatusSync(mac, timeout);
            return boxStatusData.isOpen(mac);
        } catch (Exception e) {
            log.error("门锁状态获取失败");
            return true;
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
     * 查询单个柜子所有有效格口的储物状态
     */
    public Map<Integer, Boolean> querySingleCabinetGoodsStatusSync(Integer cabinetId, List<Integer> boxNos, long timeout) throws Exception {
        QianMingLockDeviceService deviceService = getDeviceServiceByCabinetId(cabinetId);
        QianMingLockDevice.BoxGoodsData boxGoodsData = deviceService.queryGoodsStatusSync(boxNos.get(0), timeout);

        Map<Integer, Boolean> result = new HashMap<>();
        for (Integer boxNo : boxNos) {
            result.put(boxNo, boxGoodsData.hasGoods(boxNo));
        }
        return result;
    }

    /**
     * 查询单个格口储物状态
     */
    public boolean querySingleGoodsStatusSync(int cellConfigId, long timeout) {
        CellConfig cellConfig = cellConfigMapper.selectById(cellConfigId);
        if (cellConfig == null) {
            throw new RuntimeException("格口配置不存在: " + cellConfigId);
        }

        QianMingLockDeviceService deviceService = this.getDeviceServiceByCellConfig(cellConfig);

        int mac;
        try {
            mac = Integer.parseInt(cellConfig.getMacAddress());
        } catch (Exception e) {
            throw new RuntimeException("格口储物状态获取失败：MAC地址格式不正确：" + cellConfig.getMacAddress(), e);
        }
        try {
            QianMingLockDevice.BoxGoodsData boxGoodsData = deviceService.queryGoodsStatusSync(mac, timeout);
            boolean res = boxGoodsData.hasGoods(mac);
            return res;
        } catch (Exception e) {
            log.error("格口储物状态获取失败，给有物");
            return true;
        }
    }

    /**
     * 校验指定柜子及锁板（mac）下的所有格口是否全部处于关闭状态
     */
    public Boolean isAllClosed(Integer cabinetConfigId, Integer mac) {
        if (cabinetConfigId == null || mac == null) {
            throw new RuntimeException("参数不能为空");
        }

        try {
            QianMingLockDeviceService deviceService = this.getDeviceServiceByCabinetId(cabinetConfigId);
            deviceService.open();

            long timeout = 3000L;
            QianMingLockDevice.BoxStatusData boxStatusData = deviceService.queryBoxStatusSync(mac, timeout);

            if (boxStatusData == null) {
                log.warn("柜子ID [{}] MAC [{}] 状态查询返回空数据", cabinetConfigId, mac);
                return false;
            }
            log.info(boxStatusData.toString());
            return boxStatusData.isAllClosed();

        } catch (Exception e) {
            log.error("检查全关状态失败，cabinetConfigId={}, mac={}, 原因: {}", cabinetConfigId, mac, e.getMessage());
            return false;
        }
    }

    /**
     * 校验指定柜子及锁板（mac）下的所有格口是否全部处于关闭状态(只检测使用的mac)
     */
    public Boolean isAllActiveCellsClosed(Integer cabinetConfigId, List<Integer> activeCellMacAddress) throws Exception {
        try {
            QianMingLockDeviceService deviceService = this.getDeviceServiceByCabinetId(cabinetConfigId);
            deviceService.open();

            long timeout = 3000L;
            QianMingLockDevice.BoxStatusData boxStatusData = deviceService.queryBoxStatusSync(activeCellMacAddress.get(0), timeout);

            if (boxStatusData == null) {
                log.warn("柜子ID [{}] MAC [{}] 状态查询返回空数据", cabinetConfigId, activeCellMacAddress.get(0));
                return false;
            }
            log.info(boxStatusData.toString());
            return boxStatusData.isAllClosed(activeCellMacAddress);

        } catch (Exception e) {
            log.error("检查全关状态失败，cabinetConfigId={}, mac={}, 原因: {}", cabinetConfigId, activeCellMacAddress.get(0), e.getMessage());
            throw e;
        }
    }

    /**
     * 仅凭通信类型和端口，动态构建或获取设备服务
     */
    public QianMingLockDeviceService getOrCreateDeviceServiceByRawArgs(String commType, String commPort) {
        if (!org.springframework.util.StringUtils.hasText(commType) || !org.springframework.util.StringUtils.hasText(commPort)) {
            throw new RuntimeException("通信类型或通信端口不能为空");
        }

        if (deviceServiceMap.containsKey(commPort)) {
            return deviceServiceMap.get(commPort);
        }

        if ("485".equalsIgnoreCase(commType)) {
            this.validateSerialPortConflict(commPort, "锁板");
        }

        CommDispatcher dispatcher = this.createDispatcher(commType, commPort);

        QianMingLockDeviceService qianMingLockDeviceService = new QianMingLockDeviceService();
        qianMingLockDeviceService.setCommDispatcher(dispatcher);
        dispatcher.addDevice(qianMingLockDeviceService);
        qianMingLockDeviceService.setSimulationMode(this.simulationMode);

        return qianMingLockDeviceService;
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
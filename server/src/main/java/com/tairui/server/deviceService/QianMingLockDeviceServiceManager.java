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
    @Value("${lock.simulation.mode:true}")
    private boolean simulationMode;

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        super.init("lockCommType", "lockCommPort", "锁板");
    }

    /**
     * 通过柜子配置id，获取设备对象
     */
    public QianMingLockDeviceService getDeviceServiceByCabinetId(Integer cabinetId) {
        return super.getDeviceServiceByCabinetId(cabinetId, "lockCommType", "lockCommPort");
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
     * 添加/校验新柜子锁服务（供Service层创建/修改柜子时调用）
     * 1. 如果存在完全相同的通信端口（TCP的IP:Port，或485的COM@Baud），直接返回已有服务
     * 2. 如果是485，且使用了相同的物理串口名称（如COM1），但波特率不同，则拒绝通过
     * 3. 校验通过后，仅创建并返回对象，【不打开连接】，【不存入Map】
     *
     * @param cabinetConfig 新增或修改后的柜子配置
     * @return 实例化后的锁服务对象
     */
    public QianMingLockDeviceService addDeviceServiceByNewCabinetConfig(CabinetConfig cabinetConfig) {
        return super.addDeviceServiceByNewCabinetConfig(cabinetConfig, "lockCommType", "lockCommPort", "锁板");
    }

    /**
     * 通过柜子配置创建设备对象并建立连接
     */
    @Override
    protected QianMingLockDeviceService createDeviceServiceByCabinetConfig(CabinetConfig cabinetConfig, String commTypeField, String commPortField) {
        String commType = cabinetConfig.getLockCommType();
        String commPort = cabinetConfig.getLockCommPort();

        CommDispatcher dispatcher = createDispatcher(commType, commPort);
        QianMingLockDeviceService qianMingLockDeviceService = createDeviceServiceWithoutCache(cabinetConfig, commTypeField, commPortField, dispatcher);

        deviceServiceMap.put(commPort, qianMingLockDeviceService);
        openAndCacheDevice(qianMingLockDeviceService, cabinetConfig, commPort);

        return qianMingLockDeviceService;
    }

    /**
     * 创建设备服务对象但不缓存
     */
    @Override
    protected QianMingLockDeviceService createDeviceServiceWithoutCache(CabinetConfig cabinetConfig, String commTypeField, String commPortField, CommDispatcher dispatcher) {
        QianMingLockDeviceService qianMingLockDeviceService = new QianMingLockDeviceService();
        qianMingLockDeviceService.setCommDispatcher(dispatcher);
        dispatcher.addDevice(qianMingLockDeviceService);
        qianMingLockDeviceService.setSimulationMode(simulationMode);

        return qianMingLockDeviceService;
    }

    /**
     * 关闭设备连接
     */
    @Override
    protected void closeDevice(QianMingLockDeviceService deviceService) throws Exception {
        deviceService.close();
    }

    /**
     * 获取通信类型
     */
    @Override
    protected String getCommType(CabinetConfig config, String fieldName) {
        return config.getLockCommType();
    }

    /**
     * 获取通信端口
     */
    @Override
    protected String getCommPort(CabinetConfig config, String fieldName) {
        return config.getLockCommPort();
    }

    /**
     * 从配置中获取通信类型（用于日志）
     */
    @Override
    protected String getCommTypeFromConfig(CabinetConfig config) {
        return config.getLockCommType();
    }

    /**
     * 删除柜子配置之后
     */
    public void afterDeleteCabinetConfigData(Integer cabinetConfigId, String commPort) {
        super.afterDeleteCabinetConfigData(cabinetConfigId, commPort, "lockCommPort");
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
     * @param cellConfigId
     * @param timeout
     * @return
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
     * @param cabinetId 柜子id
     * @param boxNos 有效格口号列表
     * @param timeout 超时时间
     * @return
     * @throws Exception
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
     * @param cellConfigId 格口配置id
     * @param timeout 超时时间
     * @return
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
     * * @param cabinetConfigId 柜子配置ID
     *
     * @param mac 锁板地址/板号
     * @return true 表示全关，false 表示至少有一个门是开着的或查询失败
     */
    public Boolean isAllClosed(Integer cabinetConfigId, Integer mac) {
        if (cabinetConfigId == null || mac == null) {
            throw new RuntimeException("参数不能为空");
        }

        try {
            // 通过柜子ID获取设备连接服务
            QianMingLockDeviceService deviceService = this.getDeviceServiceByCabinetId(cabinetConfigId);

            // 确保设备连接处于打开状态（参考 prepareDeviceContext 中的健壮性设计）
            deviceService.open();

            // 调用硬件接口查询该锁板(mac)的所有格口锁状态（设置 3000ms 超时时间）
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
     *
     * @param cabinetConfigId
     * @param activeCellMacAddress
     * @return
     */
    public Boolean isAllActiveCellsClosed(Integer cabinetConfigId, List<Integer> activeCellMacAddress) {

        try {
            // 通过柜子ID获取设备连接服务
            QianMingLockDeviceService deviceService = this.getDeviceServiceByCabinetId(cabinetConfigId);

            // 确保设备连接处于打开状态
            deviceService.open();

            // 调用硬件接口查询该锁板(mac)的所有格口锁状态（设置 3000ms 超时时间）
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
            return false;
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

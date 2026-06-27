package com.tairui.server.deviceService;

import com.tairui.server.device.dehumidifier.DehumidifierDevice;
import lombok.extern.slf4j.Slf4j;

/**
 * 除湿机设备服务
 */
@Slf4j
public class DehumidifierDeviceService extends DehumidifierDevice {
    /**
     * 设备地址(dec)
     *
     * @param address
     */
    public DehumidifierDeviceService(int address) {
        super(address);
    }

    /**
     * 设置控湿开始值
     *
     * @param humidityControlStart 控湿开始值
     */
    @Override
    public void setHumidityControlStart(int humidityControlStart) {
        super.setHumidityControlStart(humidityControlStart);
    }

    /**
     * 设置控湿停止值
     *
     * @param humidityControlStop 控湿停止值
     */
    @Override
    public void setHumidityControlStop(int humidityControlStop) {
        super.setHumidityControlStop(humidityControlStop);
    }

    /**
     * 设备主动上报
     *
     * @param readBytes 读取到的设备完整协议帧数据
     */
    @Override
    public void onDeviceReported(byte[] readBytes) {

    }

}

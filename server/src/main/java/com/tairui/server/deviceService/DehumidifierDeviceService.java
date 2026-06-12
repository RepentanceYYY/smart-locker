package com.tairui.server.deviceService;

import com.tairui.server.device.dehumidifier.DehumidifierDevice;

/**
 * 除湿机设备服务
 */
public class DehumidifierDeviceService extends DehumidifierDevice {
    /**
     * 设备地址(dec)
     *
     * @param address
     */
    public DehumidifierDeviceService(int address) {
        super(address);
    }


}

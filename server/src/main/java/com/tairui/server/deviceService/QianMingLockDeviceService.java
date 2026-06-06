package com.tairui.server.deviceService;

import com.tairui.server.device.qianMingLock.QianMingLockDevice;

/**
 * 千鸣锁设备服务
 */
public class QianMingLockDeviceService extends QianMingLockDevice {

    /**
     * 开启箱门锁
     *
     * @param boxNo   格口号
     * @param timeout 超时时间
     * @return
     * @throws Exception
     */
    @Override
    public boolean openBoxSync(int boxNo, long timeout) throws Exception {
        return super.openBoxSync(boxNo, timeout);
    }

    /**
     * 查询指定格口号所在板子所有格口的箱门锁状态
     *
     * @param boxNo   格口号
     * @param timeout 超时时间
     * @return
     * @throws Exception
     */
    @Override
    public BoxStatusData queryBoxStatusSync(int boxNo, long timeout) throws Exception {
        return super.queryBoxStatusSync(boxNo, timeout);
    }

    /**
     * 查询指定格口号所在板子所有的格口号的储物状态
     *
     * @param boxNo   格口号
     * @param timeout 超时时间
     * @return
     * @throws Exception
     */
    @Override
    public BoxGoodsData queryGoodsStatusSync(int boxNo, long timeout) throws Exception {
        return super.queryGoodsStatusSync(boxNo, timeout);
    }

    /**
     * 设置单块板子起始和结束格口号(需要手动去点击板子上的按钮作为响应)
     *
     * @param startBox
     * @param endBox
     * @param timeout
     * @return
     * @throws Exception
     */
    @Override
    public boolean setBoxRangeSync(int startBox, int endBox, long timeout) throws Exception {
        return super.setBoxRangeSync(startBox, endBox, timeout);
    }


}

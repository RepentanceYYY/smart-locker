package com.tairui.handler.manager;

import com.jni.face.Face;
import com.tairui.config.ServerConfig;
import com.tairui.dao.AppSystemConfigDao;
import com.tairui.entity.WsRequest;
import com.tairui.entity.WsResponse;
import com.tairui.entity.db.AppSystemConfig;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class FaceApiManager {
    public static Face api;
    /**
     * sdk初始化响应码
     */
    public static int sdkInitCode = 0;
    /**
     * sdk响应码和消息对照表
     */
    public static Map<Integer, String> codeTextMap = new HashMap<>();


    /**
     * 初始化Api
     */
    public static void load() {
        synchronized (Face.class) {
            if (api != null) {
                destroy();
            }

            AppSystemConfigDao appSystemConfigDao = new AppSystemConfigDao();
            AppSystemConfig appSystemConfig = appSystemConfigDao.getConfig();
            String licenseKey = appSystemConfig.getBaiduFaceLicenseKey();

            ServerConfig serverConfig = ServerConfig.getInstance();
            String faceModelDir = serverConfig.getUseFixedModelDir() ? serverConfig.getBaiduFaceModelDir() : System.getProperty("user.dir");
            File licenseKeyFile = new File(faceModelDir, "license/license.key");
            if (licenseKeyFile.exists()) {
                FileWriter writer = null;
                try {
                    writer = new FileWriter(licenseKeyFile, false);
                    if (licenseKey != null) {
                        writer.write(licenseKey.trim());
                    }

                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            api = new Face();
            sdkInitCode = api.sdkInit(faceModelDir);
            // 获取设备指纹
            String deviceId = api.getDeviceId();
            System.out.println("指纹id:" + deviceId);
            // 获取版本号
            String ver = api.sdkVersion();
            System.out.println("sdk版本:" + ver);
            System.out.println("百度人脸SDK初始化结果---> " + getErrorText());
            if (sdkInitCode == 0) {
                Face.loadDbFace();
            } else {
                destroy(); // 销毁SDK，防止内存泄露
            }
        }
    }

    /**
     * 卸载释放
     */
    public static void destroy() {
        synchronized (Face.class) {
            api.sdkDestroy();
            System.out.println("SDK已卸载");
        }
    }

    /**
     * 激活SDK
     */
    public static WsResponse activateSDK(WsRequest req) {
        synchronized (Face.class) {
            if (api != null) {
                destroy();
            }
            ServerConfig serverConfig = ServerConfig.getInstance();
            File licenseFile = new File(serverConfig.getBaiduFaceModelDir(), "license/license.key");
            if (!licenseFile.exists()) {
                return WsResponse.fail(req.getAction(), 400, "找不到证书路径，无法授权");
            }
            String licenseKey = (String) req.getData();
            if (licenseKey == null || licenseKey.trim().length() == 0) {
                return WsResponse.fail(req.getAction(), 400, "授权码不能为空");
            }
            FileWriter writer = null;
            try {
                writer = new FileWriter(licenseFile, false);
                writer.write(licenseKey.trim());
                writer.flush();
            } catch (IOException e) {
                return WsResponse.fail(req.getAction(), 500, "激活码写入证书失败,程序消息:" + e.getMessage());
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            // 写入数据库
            AppSystemConfigDao appSystemConfigDao = new AppSystemConfigDao();
            boolean updateDbRes = appSystemConfigDao.updateBaiduFaceLicenseKey(licenseKey);
            if (!updateDbRes) {
                return WsResponse.fail(req.getAction(), 500, "授权码写入数据库失败");
            }

            api = new Face();
            sdkInitCode = api.sdkInit(serverConfig.getBaiduFaceModelDir());
            if (sdkInitCode != 0) {
                destroy();
                return WsResponse.fail(req.getAction(), 500, getErrorText());
            }
            Face.loadDbFace();
            return WsResponse.success(req.getAction(), "百度人脸授权成功", null);
        }
    }

    public static WsResponse queryActivationStatus(WsRequest req) {

        if (sdkInitCode == 0) {
            return WsResponse.success(req.getAction(), "已授权", null);
        } else {
            return WsResponse.fail(req.getAction(), 500, "未授权");
        }
    }

    /**
     * 查询授权码
     *
     * @return 授权码
     */
    public static String queryActivationCode() {
        ServerConfig serverConfig = ServerConfig.getInstance();
        File licenseFile = new File(serverConfig.getBaiduFaceModelDir(), "license/license.key");

        if (!licenseFile.exists()) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(licenseFile))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            String activationCode = sb.toString().trim();

            return activationCode.isEmpty() ? null : activationCode;

        } catch (Exception e) {
            return null;
        }
    }

    public static String getErrorText() {
        return codeTextMap.getOrDefault(sdkInitCode, "未知错误");
    }

    /**
     * 根据错误码获取对应描述
     */
    public static String getErrorText(int errno) {
        return codeTextMap.getOrDefault(sdkInitCode, "未知错误");
    }

    static {
        codeTextMap.put(0, "成功");
        codeTextMap.put(-1, "失败或非法参数");
        codeTextMap.put(-2, "内存分配失败");
        codeTextMap.put(-3, "实例对象为空");
        codeTextMap.put(-4, "百度模型内容为空");
        codeTextMap.put(-5, "百度不支持的能力类型");
        codeTextMap.put(-6, "百度不支持的预测库类型");
        codeTextMap.put(-7, "预测库对象创建失败");
        codeTextMap.put(-8, "预测库对象初始化失败");
        codeTextMap.put(-9, "图像数据为空");
        codeTextMap.put(-10, "百度人脸能力初始化失败");
        codeTextMap.put(-11, "百度人脸能力未加载");
        codeTextMap.put(-12, "百度人脸能力已加载");
        codeTextMap.put(-13, "百度人脸未授权");
        codeTextMap.put(-14, "人脸能力运行异常");
        codeTextMap.put(-15, "不支持的图像类型");
        codeTextMap.put(-16, "图像转换失败");
        codeTextMap.put(-1001, "系统错误");
        codeTextMap.put(-1002, "参数错误");
        codeTextMap.put(-1003, "数据库操作失败");
        codeTextMap.put(-1004, "没有数据");
        codeTextMap.put(-1005, "记录不存在");
        codeTextMap.put(-1006, "记录已经存在");
        codeTextMap.put(-1007, "文件不存在");
        codeTextMap.put(-1008, "提取特征值失败");
        codeTextMap.put(-1009, "文件太大");
        codeTextMap.put(-1010, "人脸资源文件不存在");
        codeTextMap.put(-1011, "特征值长度错误");
        codeTextMap.put(-1012, "未检测到人脸");
        codeTextMap.put(-1013, "摄像头错误或不存在");
        codeTextMap.put(-1014, "人脸引擎初始化错误");
        codeTextMap.put(-1015, "百度人脸授权文件不存在,请重新授权");
        codeTextMap.put(-1016, "百度人脸授权序列号为空,请重新授权");
        codeTextMap.put(-1017, "百度人脸授权序列号无效,请重新授权");
        codeTextMap.put(-1018, "百度人脸授权序列号过期,请重新授权");
        codeTextMap.put(-1019, "百度人脸授权序列号已被使用,请重新授权");
        codeTextMap.put(-1020, "百度人脸设备指纹为空");
        codeTextMap.put(-1021, "网络超时");
        codeTextMap.put(-1022, "网络错误");
        codeTextMap.put(-1023, "百度人脸配置ini文件不存在");
        codeTextMap.put(-1024, "百度人脸禁用在Windows Server");
    }
}

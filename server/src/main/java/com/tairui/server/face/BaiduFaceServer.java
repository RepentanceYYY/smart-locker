package com.tairui.server.face;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jni.face.Face;
import com.jni.struct.EyeClose;
import com.jni.struct.FaceBox;
import com.jni.struct.LivenessInfo;
import com.jni.struct.Occlusion;
import com.tairui.server.entity.SystemConfig;
import com.tairui.server.mapper.SystemConfigMapper;
import com.tairui.server.webSocket.dto.WsRequest;
import com.tairui.server.webSocket.dto.WsResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Component
@Log4j2
@ConditionalOnProperty(name = "face-detect-provider", havingValue = "Baidu", matchIfMissing = true)
public class BaiduFaceServer implements IFaceServer {

    public static volatile Face api;

    private volatile int sdkInitCode = -1014;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private FaceThresholdConfig faceThresholdConfig;
    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Value("${baidu-face-config.use-fixed-path}")
    private Boolean useFixedPath;

    @Value("${baidu-face-config.fixed-path}")
    private String fixedPath;
    @Value("${baidu-face-config.group-id}")
    private String groupId;
    // 本地目录
    @Value("${spring.application.file.upload.borrow-photo-path}")
    private String uploadPath;
    // 网络路径
    @Value("${spring.application.file.upload.borrow-photo-access}")
    private String accessPath;

    /**
     * nir人脸检测
     */
    private final int NIR_DETECT = 1;
    /**
     * rgb可见光检测
     */
    private final int RGB_DETECT = 0;
    /**
     * 生活照
     */
    private final int LIFESTYLE_PHOTO = 0;

    public static Map<Integer, String> codeTextMap = new HashMap<>();

    // 全局锁对象，所有 SDK 访问必须通过此锁串行化
    private static final Object SDK_NATIVE_LOCK = new Object();

    /**
     * 人脸检测识别和注册
     */
    @Override
    public WsResponse faceDetect(WsRequest wsRequest) {

        FaceImage faceImage = objectMapper.convertValue(wsRequest.getData(), FaceImage.class);
        String rgbBase64 = faceImage.getRgbBase64();

        synchronized (SDK_NATIVE_LOCK) {

            MatOfByte rgbMatOfByte = null;
            Mat rgbRawMat = null;
            Mat rgbMat = null;

            try {
                if (sdkInitCode != 0) {
                    return WsResponse.fail(wsRequest.getAction(), 506, getErrorText(sdkInitCode));
                }
                if (rgbBase64 != null && rgbBase64.startsWith("data:image")) {
                    rgbBase64 = rgbBase64.substring(rgbBase64.indexOf(",") + 1);
                }

                byte[] rgbBytes = Base64.getDecoder().decode(rgbBase64);

                rgbMatOfByte = new MatOfByte(rgbBytes);
                rgbRawMat = Imgcodecs.imdecode(rgbMatOfByte, Imgcodecs.IMREAD_COLOR);

                if (rgbRawMat == null || rgbRawMat.empty()) {
                    return WsResponse.fail(wsRequest.getAction(), 400, "图片帧格式错误，OpenCV 无法解析该图片字节流");
                }

                rgbMat = rgbRawMat.clone();

                long rgbMatAddr = rgbMat.getNativeObjAddr();
                if (rgbMatAddr == 0) {
                    return WsResponse.fail(wsRequest.getAction(), 400, "OpenCV Native 对象地址为 0，拒绝传递给 C++ 层");
                }

                FaceBox[] faceBoxes = Face.detect(rgbMatAddr, RGB_DETECT);

                if (faceBoxes == null || faceBoxes.length == 0) {
                    return WsResponse.fail(wsRequest.getAction(), 404, "请面对摄像头");
                }
                if (faceBoxes.length > 1) {
                    return WsResponse.fail(wsRequest.getAction(), 404, "请保持画面只有一张人脸");
                }
                // 遮挡度检测
                Occlusion[] occlusions = Face.faceOcclusion(rgbMatAddr);
                Occlusion occlusion = occlusions[0];
                List<String> occludedParts = new ArrayList<>();

                if (occlusion.leftEye > faceThresholdConfig.getFaceOcclusion()) occludedParts.add("左眼");
                if (occlusion.rightEye > faceThresholdConfig.getFaceOcclusion()) occludedParts.add("右眼");
                if (occlusion.nose > faceThresholdConfig.getFaceOcclusion()) occludedParts.add("鼻子");
                if (occlusion.mouth > faceThresholdConfig.getFaceOcclusion()) occludedParts.add("嘴巴");
                if (occlusion.leftCheek > faceThresholdConfig.getFaceOcclusion()) occludedParts.add("左脸");
                if (occlusion.rightCheek > faceThresholdConfig.getFaceOcclusion()) occludedParts.add("右脸");
                if (occlusion.chin > faceThresholdConfig.getFaceOcclusion()) occludedParts.add("下巴");

                if (!occludedParts.isEmpty()) {
                    return WsResponse.fail(
                            wsRequest.getAction(),
                            404,
                            String.join("、", occludedParts) + "被挡住了"
                    );
                }


                // 嘴巴闭合检测
                float[] mouthCloseScore = Face.faceMouthClose(rgbMatAddr);
                if (mouthCloseScore[0] < faceThresholdConfig.getMouthCloseScoreMin()) {
                    return WsResponse.fail(wsRequest.getAction(), 404, "请闭合嘴巴");
                }

                // 眼睛闭合检测
                System.out.println("获取眼睛闭合参数");
                EyeClose[] eyeCloses = Face.faceEyeClose(rgbMatAddr);
                if (eyeCloses == null || eyeCloses.length < 1) {
                    return WsResponse.fail(wsRequest.getAction(), 404, "请睁开眼睛");
                }

                if (eyeCloses[0].leftEyeCloseConf > faceThresholdConfig.getEyeCloseMax() || eyeCloses[0].rightEyeCloseConf > faceThresholdConfig.getEyeCloseMax()) {
                    return WsResponse.fail(wsRequest.getAction(), 404, "请睁开眼睛");
                }

                // 人脸模糊度检测
                System.out.println("获取人脸模糊度");
                float[] blurList = Face.faceBlur(rgbMatAddr);
                if (blurList == null || blurList.length == 0) {
                    return WsResponse.fail(wsRequest.getAction(), 404, "请保持人脸在画面中");
                }
                if (blurList[0] > faceThresholdConfig.getBlurMax()) {
                    return WsResponse.fail(wsRequest.getAction(), 404, "人脸太模糊");
                }

                LivenessInfo[] liveInfos = Face.rgbLiveness(rgbMatAddr);
                if (liveInfos == null || liveInfos.length == 0 || liveInfos[0].box == null) {
                    return WsResponse.fail(wsRequest.getAction(), 404, "未检测到人脸");
                }

                float liveScore = liveInfos[0].livescore;
                if (liveScore < faceThresholdConfig.getLiveScoreMin()) {
                    return WsResponse.fail(wsRequest.getAction(), 404, String.format("检测到非活体,%.3f", liveScore));
                }
                Face.loadDbFace();
                // 查询人脸是否已经注册
                String identifyResultJson = Face.identifyWithAllByMat(rgbMatAddr, LIFESTYLE_PHOTO);
                FaceRecognitionReply reply = objectMapper.readValue(identifyResultJson, FaceRecognitionReply.class);
                List<FaceRecognitionReply.FaceRecognitionData.FaceRecognitionResult> faceRecognitionResults = reply.getData().getResult();

                // 人脸没有注册
                if (faceRecognitionResults == null || faceRecognitionResults.size() < 1) {
                    String url = this.userAddByMat(rgbBytes, rgbMatAddr);
                    return WsResponse.success(wsRequest.getAction(), url);
                }
                // 拿到人脸相似度最高的用户
                FaceRecognitionReply.FaceRecognitionData.FaceRecognitionResult best = faceRecognitionResults.stream()
                        .max(Comparator.comparingDouble(FaceRecognitionReply.FaceRecognitionData.FaceRecognitionResult::getScore))
                        .orElse(null);

                // 找到相似人脸，但相似度太低
                if (best.getScore() < faceThresholdConfig.getSimilarity()) {
                    String url = this.userAddByMat(rgbBytes, rgbMatAddr);
                    return WsResponse.success(wsRequest.getAction(), url);
                }

                String userId = best.getUserId();
                String userInfoJSON = Face.getUserInfo(userId, groupId);
                FaceUserResponse faceUserInfo = objectMapper.readValue(userInfoJSON, FaceUserResponse.class);
                if (faceUserInfo.getErrno() != 0) {
                    return WsResponse.fail(wsRequest.getAction(), 500, faceUserInfo.getMsg());
                }
                return WsResponse.success(wsRequest.getAction(), faceUserInfo.getData().getResult().get(0).getUserInfo());
            } catch (Exception e) {
                log.error("faceDetect 内部异常: ", e);
                throw new RuntimeException(e);
            } finally {
                if (rgbMatOfByte != null) rgbMatOfByte.release();
                if (rgbRawMat != null) rgbRawMat.release();
                if (rgbMat != null) rgbMat.release();
            }
        }
    }

    @Override
    public WsResponse activate(WsRequest wsRequest) throws Exception {
        String baiduFaceLicenseKey = objectMapper.convertValue(wsRequest.getData(), String.class);
        if (!StringUtils.hasText(baiduFaceLicenseKey)) {
            return WsResponse.fail(wsRequest.getAction(), 400, "未传输授权码，无法授权百度人脸");
        }
        this.destroy();
        synchronized (SDK_NATIVE_LOCK) {
            List<SystemConfig> systemConfigs = systemConfigMapper.selectList(null);
            if (systemConfigs.isEmpty()) {
                return WsResponse.fail(wsRequest.getAction(), 500, "系统配置表没有数据，无法授权百度人脸");
            }
            // 更新数据库
            SystemConfig systemConfig = systemConfigs.get(0);
            systemConfig.setBaiduFaceLicenseKey(baiduFaceLicenseKey);
            systemConfigMapper.updateById(systemConfig);
            // 将数据库中的授权码写入到授权文件中
            String faceModelDir = useFixedPath ? fixedPath : System.getProperty("user.dir");
            File licenseKeyFile = new File(faceModelDir, "license/license.key");
            if (licenseKeyFile.exists()) {
                FileWriter writer = null;
                try {
                    writer = new FileWriter(licenseKeyFile, false);
                    if (baiduFaceLicenseKey != null) {
                        writer.write(baiduFaceLicenseKey.trim());
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
            this.load();

            if (sdkInitCode == 0) {
                return WsResponse.success(wsRequest.getAction(), "百度人脸授权成功", null);
            } else {
                return WsResponse.fail(wsRequest.getAction(), 500, "百度人脸授权失败，原因：" + getErrorText(sdkInitCode));
            }
        }

    }

    @Override
    public WsResponse getActivationStatus(WsRequest wsRequest) throws Exception {
        if (sdkInitCode == 0) {
            return WsResponse.success(wsRequest.getAction(), "百度人脸已授权", null);
        } else {
            return WsResponse.fail(wsRequest.getAction(), 500, getErrorText(sdkInitCode));
        }
    }

    @Override
    public WsResponse getBaiDuActivationStatus(WsRequest wsRequest) throws Exception {
        return WsResponse.success(wsRequest.getAction(), null);
    }

    /**
     * 注册用户
     */
    public String userAddByMat(byte[] frame, long rgbMatAddr) throws IOException {
        synchronized (SDK_NATIVE_LOCK) {
            String userId = UUID.randomUUID().toString().replace("-", "");
            String fileName = userId + ".jpg";
            Path path = Paths.get(uploadPath, fileName);
            Files.write(path, frame);
            String url = accessPath + fileName;

            // 注册人脸到百度人脸库
            String userAddByMat = Face.userAddByMat(rgbMatAddr, userId, groupId, url);
            log.info("人脸注册结果{}", userAddByMat);

            return url;
        }
    }

    @Override
    public void start() throws Exception {
        synchronized (SDK_NATIVE_LOCK) {

        }
    }

    @PostConstruct
    public void diComplete() {
        List<SystemConfig> systemConfigs = systemConfigMapper.selectList(null);
        if (systemConfigs.isEmpty()) {
            log.info("系统配置表没有数据，跳过加载人脸SDK");
            return;
        }
        SystemConfig systemConfig = systemConfigs.get(0);
        Integer enableFaceCapture = systemConfig.getEnableFaceCapture();
        if (enableFaceCapture != 1) {
            log.info("未启用人脸抓拍，跳过加载人脸SDK");
            return;
        }
        String baiduFaceLicenseKey = systemConfig.getBaiduFaceLicenseKey();
        // 将数据库中的授权码写入到授权文件中
        String faceModelDir = useFixedPath ? fixedPath : System.getProperty("user.dir");
        File licenseKeyFile = new File(faceModelDir, "license/license.key");
        if (licenseKeyFile.exists()) {
            FileWriter writer = null;
            try {
                writer = new FileWriter(licenseKeyFile, false);
                if (baiduFaceLicenseKey != null) {
                    writer.write(baiduFaceLicenseKey.trim());
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
        this.load();
    }

    /**
     * 加载SDK
     */
    public void load() {
        synchronized (SDK_NATIVE_LOCK) {
            if (api != null) {
                destroy();
            }
            api = new Face();
            String resourcesPath;
            if (Boolean.TRUE.equals(useFixedPath)) {
                resourcesPath = fixedPath;
            } else {
                resourcesPath = System.getProperty("user.dir");
            }
            sdkInitCode = api.sdkInit(resourcesPath);
            // 获取设备指纹
            String deviceId = api.getDeviceId();
            log.info("指纹id:{}", deviceId);
            // 获取版本号
            String ver = api.sdkVersion();
            log.info("sdk版本:{}", ver);
            log.info("百度人脸SDK初始化结果:{}", getErrorText(sdkInitCode));
            if (sdkInitCode == 0) {
                // 系统启动时，全局只加载一次数据库到内存中
                Face.loadDbFace();
            } else {
                destroy(); // 销毁SDK，防止内存泄露
            }
        }
    }

    /**
     * 卸载释放
     */
    public void destroy() {
        synchronized (SDK_NATIVE_LOCK) {
            if (api == null) return;
            api.sdkDestroy();
            api = null;
            log.info("SDK已卸载");
        }
    }

    /**
     * 根据错误码获取对应描述
     */
    public String getErrorText(int code) {
        return codeTextMap.getOrDefault(code, "未知错误");
    }

    static {
        // 错误码映射表（保持原有）
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
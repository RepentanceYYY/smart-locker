package com.tairui.server.face;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jni.face.Face;
import com.jni.struct.FaceBox;
import com.jni.struct.LivenessInfo;
import com.tairui.server.entity.SystemConfig;
import com.tairui.server.mapper.SystemConfigMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Component
@Log4j2
@ConditionalOnProperty(name = "face-detect-provider", havingValue = "Baidu", matchIfMissing = true)
public class BaiduFaceServer implements IFaceServer {

    // 改为 volatile 确保多线程可见
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
    private final int LIFESTYLE_PHOTO = 1;

    public static Map<Integer, String> codeTextMap = new HashMap<>();

    // 全局锁对象，所有 SDK 访问必须通过此锁串行化
    private static final Object SDK_NATIVE_LOCK = new Object();

    /**
     * 人脸检测与比对（整个方法体由 SDK_NATIVE_LOCK 保护）
     */
    @Override
    public String faceDetect(FaceImage faceImage) throws Exception {
        String frame = faceImage.getRgbBase64();
        // 使用全局唯一锁，把整个 C++ 运算、矩阵解码、注册、比对全部锁死
        synchronized (SDK_NATIVE_LOCK) {
            log.info(">>>> 进入人脸检测方法，接收到 base64 长度: {}", frame != null ? frame.length() : "null");

            MatOfByte matOfByte = null;
            Mat rawMat = null;
            Mat rgbMat = null;

            try {
                if (sdkInitCode != 0) {
                    throw new IOException(getErrorText(sdkInitCode));
                }
                if (frame != null && frame.startsWith("data:image")) {
                    frame = frame.substring(frame.indexOf(",") + 1);
                }

                byte[] bytes = Base64.getDecoder().decode(frame);
                log.info("Base64 解码成功，图片字节大小: {} bytes", bytes.length);

                matOfByte = new MatOfByte(bytes);
                rawMat = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR);

                if (rawMat == null || rawMat.empty()) {
                    throw new RuntimeException("图片帧格式错误，OpenCV 无法解析该图片字节流");
                }

                rgbMat = new Mat();
                org.opencv.imgproc.Imgproc.cvtColor(rawMat, rgbMat, org.opencv.imgproc.Imgproc.COLOR_BGR2RGB);

                long rgbMatAddr = rgbMat.getNativeObjAddr();
                if (rgbMatAddr == 0) {
                    throw new RuntimeException("OpenCV Native 对象地址为 0，拒绝传递给 C++ 层");
                }

                // 此时 mkldnn.dll 开始介入矩阵运算，这里已被死死锁住，绝不会有第二个线程进来
                FaceBox[] faceBoxes = Face.detect(rgbMatAddr, RGB_DETECT);

                if (faceBoxes == null || faceBoxes.length == 0) {
                    throw new RuntimeException("未检测到人脸");
                }
                if (faceBoxes.length > 1) {
                    throw new RuntimeException("请保持画面只有一张人脸");
                }

                LivenessInfo[] liveInfos = Face.rgbLiveness(rgbMatAddr);
                if (liveInfos == null || liveInfos.length == 0 || liveInfos[0].box == null) {
                    throw new RuntimeException("未检测到人脸");
                }

                float liveScore = liveInfos[0].livescore;
                if (liveScore < faceThresholdConfig.getLiveScoreMin()) {
                    throw new RuntimeException(String.format("检测到非活体,%.3f", liveScore));
                }

                String identifyResultJson = Face.identifyWithAllByMat(rgbMatAddr, LIFESTYLE_PHOTO);
                FaceRecognitionReply reply = objectMapper.readValue(identifyResultJson, FaceRecognitionReply.class);
                List<FaceRecognitionReply.FaceRecognitionData.FaceRecognitionResult> faceRecognitionResults = reply.getData().getResult();

                // 人脸没有注册
                if (faceRecognitionResults == null || faceRecognitionResults.size() < 1) {
                    // 已经在锁内部，调用下面的注册方法是安全的
                    return this.userAddByMat(bytes, rgbMatAddr);
                }

                FaceRecognitionReply.FaceRecognitionData.FaceRecognitionResult best = faceRecognitionResults.stream()
                        .max(Comparator.comparingDouble(FaceRecognitionReply.FaceRecognitionData.FaceRecognitionResult::getScore))
                        .orElse(null);

                // 找到相似人脸，但相似度太低
                if (best.getScore() < faceThresholdConfig.getSimilarity()) {
                    return this.userAddByMat(bytes, rgbMatAddr);
                }

                String userId = best.getUserId();
                String userInfoJSON = Face.getUserInfo(userId, groupId);
                FaceUserInfo faceUserInfo = objectMapper.readValue(userInfoJSON, FaceUserInfo.class);

                return faceUserInfo.getData().getUserInfo();
            } catch (Exception e) {
                log.error("❌ faceDetect 内部异常: ", e);
                throw new RuntimeException(e);
            } finally {
                if (matOfByte != null) matOfByte.release();
                if (rawMat != null) rawMat.release();
                if (rgbMat != null) rgbMat.release();
            }
        } // 锁释放点
    }

    /**
     * 注册方法同样使用代码块锁，防止被外部意外调用导致并发
     */
    public String userAddByMat(byte[] frame, long rgbMatAddr) throws IOException {
        synchronized (SDK_NATIVE_LOCK) {
            String userId = UUID.randomUUID().toString().replace("-", "");
            String fileName = userId + ".jpg";
            Path path = Paths.get(uploadPath, fileName);
            Files.write(path, frame);
            String url = accessPath + fileName;

            // C++ 层的写入操作
            Face.userAddByMat(rgbMatAddr, userId, groupId, url);

            log.info("检测到新用户注册，刷新人脸库缓存...");
            Face.loadDbFace();

            return url;
        }
    }

    // ---------- 以下其他接口方法（目前空实现或简单返回），也统一加锁以防未来扩展 ----------
    @Override
    public void activate(String sdkKey) throws Exception {
        // 若有实际 SDK 调用，请加锁
        synchronized (SDK_NATIVE_LOCK) {
            // 空实现
        }
    }

    @Override
    public void activate(String appId, String sdkKey) throws Exception {
        synchronized (SDK_NATIVE_LOCK) {
            // 空实现
        }
    }

    @Override
    public Map<String, String> getActivationStatus() throws Exception {
        synchronized (SDK_NATIVE_LOCK) {
            // 空实现
            return Map.of();
        }
    }

    @Override
    public String getBaiDuActivationStatus() throws Exception {
        synchronized (SDK_NATIVE_LOCK) {
            return "";
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
            api = null;  // 避免重复销毁
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
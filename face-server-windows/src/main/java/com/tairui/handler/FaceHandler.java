package com.tairui.handler;

import com.jni.face.Face;
import com.jni.struct.EyeClose;
import com.jni.struct.LivenessInfo;
import com.tairui.config.FaceConfig;
import com.tairui.config.ServerConfig;
import com.tairui.entity.*;
import com.tairui.entity.baidu.FaceRecognitionResponse;
import com.tairui.entity.baidu.FaceUserResponse;
import com.tairui.handler.manager.FaceApiManager;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import com.tairui.utils.JsonUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class FaceHandler {
    private static ServerConfig serverConfig = ServerConfig.getInstance();

    /**
     * 检测和注册人脸
     *
     * @param request
     * @return
     */
    public static WsResponse registerFaceIfNotExist(WsRequest request) {
        if (FaceApiManager.sdkInitCode != 0) {
            return WsResponse.fail(request.getAction(), 500, FaceApiManager.getErrorText());
        }
        DualFace dualFace = JsonUtils.MAPPER.convertValue(request.getData(), DualFace.class);

        Mat rgbRawMat = null;
        Mat rgbMat = null;
        Mat irRawMat = null;
        Mat irMat = null;

        try {
            byte[] bytes = FaceHandler.decode(dualFace.getRgbBase64());
            byte[] irBytes = null;
            if (dualFace.getIrBase64() != null) {
                irBytes = FaceHandler.decode(dualFace.getIrBase64());
            }
            MatOfByte matOfByte = new MatOfByte(bytes);
            rgbRawMat = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR);
            if (rgbRawMat == null || rgbRawMat.empty()) {
                return WsResponse.fail(request.getAction(), 400, "可见光图片解码失败");
            }
            rgbMat = rgbRawMat.clone();
            long rgbMatAddr = rgbMat.getNativeObjAddr();


            if (irBytes != null && irBytes.length > 0) {
                MatOfByte irMatOfByte = new MatOfByte(irBytes);
                irRawMat = Imgcodecs.imdecode(irMatOfByte, Imgcodecs.IMREAD_COLOR);
                if (irRawMat == null || irRawMat.empty()) {
                    return WsResponse.fail(request.getAction(), 400, "红外图片解码失败");
                }
            }
            irMat = irRawMat.clone();
            long irMatAddr = irMat.getNativeObjAddr();

            synchronized (Face.class) {
                // RGB静默活体检测
                System.out.println("获取静默活体检测结果");
                LivenessInfo[] liveInfos = Face.rgbLiveness(rgbMatAddr);
                System.out.println(JsonUtils.MAPPER.writeValueAsString(liveInfos));

                if (liveInfos == null || liveInfos.length == 0 || liveInfos[0].box == null) {
                    return WsResponse.fail(request.getAction(), 400, "请面对摄像头");
                }
                if (liveInfos.length > 1) {
                    return WsResponse.fail(request.getAction(), 400, "请保持画面只有一张人脸");
                }
                // 获取到嘴巴闭合参数
                System.out.println("获取嘴巴闭合参数");
                float[] mouthCloseScore = Face.faceMouthClose(rgbMatAddr);
                // 嘴巴闭合检测
                if (mouthCloseScore[0] < FaceConfig.mouthCloseScoreMin) {
                    return WsResponse.fail(request.getAction(), 400, "请闭合嘴巴");
                }

                // 眼睛闭合检测
                System.out.println("获取眼睛闭合参数");
                EyeClose[] eyeCloses = Face.faceEyeClose(rgbMatAddr);
                if (eyeCloses == null || eyeCloses.length < 1) {
                    return WsResponse.fail(request.getAction(), 400, "请睁开眼睛");
                }
                if (eyeCloses[0].leftEyeCloseConf > FaceConfig.eyeCloseMax || eyeCloses[0].rightEyeCloseConf > FaceConfig.eyeCloseMax) {
                    return WsResponse.fail(request.getAction(), 400, "请睁开眼睛");
                }

                // 人脸模糊度检测
                System.out.println("获取人脸模糊度");
                float[] blurList = Face.faceBlur(rgbMatAddr);
                if (blurList == null || blurList.length == 0) {
                    return WsResponse.fail(request.getAction(), 400, "请保持人脸在画面中");
                }
                if (blurList[0] > FaceConfig.blurMax) {
                    System.out.println(String.format("模糊度太高，%.3f", blurList[0]));
                    return WsResponse.fail(request.getAction(), 400, "人脸太模糊");
                }
                if (irBytes != null && irBytes.length > 0) {
                    String time = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                    String irFileName = "face_ir_" + time + "_" + UUID.randomUUID().toString().substring(0, 8) + ".jpg";
                    LivenessInfo[] livenessInfos = Face.nirLiveness(irMatAddr);
                    System.out.println("活体检测结果:" + JsonUtils.MAPPER.writeValueAsString(livenessInfos));
                    saveFaceImage(irBytes,irFileName);
                }
                Face.loadDbFace();
                // 检测人脸是否已经注册
                String faceRecognitionResultUrl = faceRecognition(bytes, rgbMatAddr);
                if (faceRecognitionResultUrl != null) {
                    return WsResponse.success(request.getAction(), "人脸已注册", faceRecognitionResultUrl);
                }

                // 注册人脸
                String time = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String fileName = "face_" + time + "_" + UUID.randomUUID().toString().substring(0, 8) + ".jpg";

                String userId = time;
                String url = "/uploads/" + fileName;
                saveFaceImage(bytes, fileName);

                String userAddByMatJSON = Face.userAddByMat(rgbMatAddr, userId, serverConfig.getBaiduFaceDbDefaultGroup(), url);
                FaceRecognitionResponse userAddByMatFaceRecognitionResponse = JsonUtils.MAPPER.readValue(userAddByMatJSON, FaceRecognitionResponse.class);
                if (userAddByMatFaceRecognitionResponse.getErrno() != 0) {
                    return WsResponse.fail(request.getAction(), 500, userAddByMatFaceRecognitionResponse.getMsg());
                }
                System.out.println(userAddByMatJSON);

                return WsResponse.success(request.getAction(), "人脸注册成功", url);
            }

        } catch (Exception ex) {
            return WsResponse.fail(request.getAction(), 500, ex.getMessage());
        } finally {
            if (rgbMat != null) rgbMat.release();
            if (rgbRawMat != null) rgbRawMat.release();
        }

    }

    /**
     * 检测人脸是否已经注册
     *
     * @param rgbMatAddr
     * @return 人脸图片url
     * @throws Exception
     */
    public static String faceRecognition(byte[] bytes, long rgbMatAddr) throws Exception {
        // 查询人脸是否存在
        String identifyWithAllByMatJSON = Face.identifyWithAllByMat(rgbMatAddr, 0);
        FaceRecognitionResponse identifyWithAllByMatRes = JsonUtils.MAPPER.readValue(identifyWithAllByMatJSON, FaceRecognitionResponse.class);
        // 人脸已经存在
        if (identifyWithAllByMatRes.getErrno() == 0) {
            List<FaceRecognitionResponse.FaceRecognitionResult> result = identifyWithAllByMatRes.getData().getResult();
            double maxScore = 0d;
            String maxUserId = null;
            String maxGroupId = null;
            for (FaceRecognitionResponse.FaceRecognitionResult re : result) {
                if (re.getScore() > maxScore) {
                    maxScore = re.getScore();
                    maxUserId = re.getUserId();
                    maxGroupId = re.getGroupId();
                }
            }
            if (maxScore < 80.00d) {
                return null;
            }


            String userInfoJson = Face.getUserInfo(maxUserId, maxGroupId);
            FaceUserResponse faceUserResponse = JsonUtils.MAPPER.readValue(userInfoJson, FaceUserResponse.class);
            if (faceUserResponse.getErrno() != 0) {
                throw new RuntimeException(faceUserResponse.getMsg());
            }
            String faceUrl = faceUserResponse.getData().getResult().get(0).getUserInfo();
            String fileName = java.nio.file.Paths.get(faceUrl).getFileName().toString();
            String dirPath = serverConfig.getUseFixedFaceImageDir() ? serverConfig.getFaceImageDir() : System.getProperty("user.dir");
            File file = new File(dirPath, fileName);
            if (!file.exists()) {
                System.out.println("本地图片丢失，正在恢复：" + file.getAbsolutePath());
                try {
                    Files.write(file.toPath(), bytes);
                } catch (IOException e) {
                    throw new RuntimeException("恢复图片失败", e);
                }
            }

            return faceUrl;
        }
        return null;
    }

    /**
     * 将图片保存到本地
     *
     * @param imageBytes
     * @return url
     */
    private static String saveFaceImage(byte[] imageBytes, String fileName) {
        try {

            // 确保目录存在
            String dirPath = serverConfig.getUseFixedFaceImageDir() ? serverConfig.getFaceImageDir() : System.getProperty("user.dir");
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }


            File file = new File(dir, fileName);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(imageBytes);
                fos.flush();
            }

            // 返回url
            return "/upload/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException("人脸图片保存失败", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Base64解析失败", e);
        }
    }

    public static byte[] decode(String base64) {
        if (base64.contains(",")) {
            base64 = base64.split(",")[1];
        }
        return Base64.getDecoder().decode(base64);
    }

}

package com.tairui.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import com.tairui.utils.JsonUtils;

import java.io.InputStream;

/**
 * 服务配置单例
 */
@Data
public class ServerConfig {

    private static final ServerConfig INSTANCE;

    static {
        INSTANCE = loadConfig();
    }

    // 配置字段
    private int webSocketPort;
    private Boolean useFixedModelDir;
    private String baiduFaceModelDir;
    private String baiduFaceDbDefaultGroup;
    private String dbUrl;
    private Boolean useFixedFaceImageDir;
    private String faceImageDir;
    private String dbUsername;
    private String dbPassword;
    private String webSocketPath;

    // 私有构造
    private ServerConfig() {
    }

    /**
     * 获取单例实例
     */
    public static ServerConfig getInstance() {
        return INSTANCE;
    }

    /**
     * 加载配置
     */
    private static ServerConfig loadConfig() {
        ObjectMapper mapper = JsonUtils.MAPPER;
        try (InputStream is = ServerConfig.class.getClassLoader()
                .getResourceAsStream("config/server-config.json")) {
            if (is == null) {
                throw new RuntimeException("无法找到 server-config.json");
            }
            return mapper.readValue(is, ServerConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("加载 server-config.json 失败", e);
        }
    }

}

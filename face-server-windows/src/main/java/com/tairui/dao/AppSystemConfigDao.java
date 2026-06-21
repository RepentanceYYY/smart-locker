package com.tairui.dao;

import com.tairui.config.ServerConfig;
import com.tairui.entity.db.AppSystemConfig;

import java.sql.*;

public class AppSystemConfigDao {
    private ServerConfig serverConfig;

    private static final String SELECT_SQL = "SELECT * FROM system_config LIMIT 1";
    private static final String UPDATE_LICENSE_KEY_SQL = "UPDATE system_config SET baidu_face_license_key = ?";

    public AppSystemConfigDao() {
        try {
            serverConfig = ServerConfig.getInstance();
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public AppSystemConfig getConfig() {
        AppSystemConfig config = null;

        try (Connection conn = DriverManager.getConnection(
                serverConfig.getDbUrl(),
                serverConfig.getDbUsername(),
                serverConfig.getDbPassword());
             PreparedStatement ps = conn.prepareStatement(SELECT_SQL);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                config = new AppSystemConfig();

                config.setId(rs.getInt("id"));
                config.setSystemName(rs.getString("system_name"));
                config.setEngName(rs.getString("eng_name"));
                config.setSystemCode(rs.getString("system_code"));
                config.setLocation(rs.getString("location"));
                config.setAdminPwd(rs.getString("admin_pwd"));
                config.setBorrowPeriod(rs.getString("borrow_period"));
                config.setAutoReturnTimeoutMinutes(rs.getInt("auto_return_timeout_minutes"));
                config.setTempHumidityLogInterval(rs.getInt("temp_humidity_log_interval"));

                config.setEnableFaceCapture(rs.getInt("enable_face_capture") == 1);

                Object silent = rs.getObject("silent_liveness_enabled");
                if (silent != null) {
                    config.setSilentLivenessEnabled(rs.getInt("silent_liveness_enabled") == 1);
                }

                config.setBaiduFaceLicenseKey(rs.getString("baidu_face_license_key"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return config;
    }

    /**
     * 更新所有行的百度人脸识别 License Key
     * * @param licenseKey 新的 License Key 字符串
     *
     * @return 更新成功返回 true，失败返回 false
     */
    public boolean updateBaiduFaceLicenseKey(String licenseKey) {
        // 同样使用 try-with-resources 自动关闭资源
        try (Connection conn = DriverManager.getConnection(
                serverConfig.getDbUrl(),
                serverConfig.getDbUsername(),
                serverConfig.getDbPassword());
             PreparedStatement ps = conn.prepareStatement(UPDATE_LICENSE_KEY_SQL)) {

            // 绑定参数
            ps.setString(1, licenseKey);

            // 执行更新，affectedRows 代表受影响的行数
            int affectedRows = ps.executeUpdate();

            // 如果受影响行数 >= 0（哪怕表里本来就是空的没更新到也算执行成功，或者可以要求 > 0，取决于你的业务）
            return affectedRows >= 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}

package com.tairui.dao;

import com.tairui.config.SystemConfig;
import com.tairui.entity.db.User;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class UserDao {
    private SystemConfig systemConfig;

    public UserDao() {
        try {
            systemConfig = SystemConfig.getInstance();
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // 查询用户
    public User getUserByUserName(String userName) {
        if (userName == null || userName.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT * FROM user WHERE user_name = ?";
        try (Connection conn = DriverManager.getConnection(systemConfig.getDbUrl(), systemConfig.getDbUsername(), systemConfig.getDbPassword());
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.setId(rs.getLong("id"));
                    u.setNickName(rs.getString("nick_name"));
                    u.setUserName(rs.getString("user_name"));
                    u.setGender(rs.getString("gender"));
                    u.setCreatedAt(rs.getTimestamp("created_at"));
                    u.setEnabled(rs.getBoolean("enabled"));
                    u.setRole(rs.getString("role"));
                    u.setCardInfo(rs.getString("card_info"));
                    u.setFingerprintInfo(rs.getString("fingerprint_info"));
                    u.setFaceInfo(rs.getString("face_info"));
                    u.setPassword(rs.getString("password"));
                    return u;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取人脸id和完整图片路径
     *
     * @return
     */
    public Map<String, String> getUserIdWithFacePath() {
        Map<String, String> map = new HashMap<>();
        String sql = "SELECT user_name, face_info FROM user";

        try (Connection conn = DriverManager.getConnection(systemConfig.getDbUrl(), systemConfig.getDbUsername(), systemConfig.getDbPassword());
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String userName = rs.getString("user_name");
                String faceInfo = rs.getString("face_info");
                if (rs.wasNull()) {
                    continue;
                }
                if (faceInfo == null || faceInfo.trim().isEmpty()) {
                    continue;
                }
                // 提取文件名（带后缀）
                String fileName = faceInfo.substring(faceInfo.lastIndexOf("/") + 1);
                // 拼接完整路径
                String fullPath = systemConfig.getFaceImagePath() + "\\" + fileName;
                map.put(userName, fullPath);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return map;
    }
}

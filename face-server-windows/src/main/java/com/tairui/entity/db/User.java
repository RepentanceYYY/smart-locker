package com.tairui.entity.db;

import lombok.Data;

import java.util.Date;

@Data
public class User {
    private Long id;
    private String nickName;
    private String userName;
    private String password;
    private String gender;
    private Date createdAt;
    private Boolean enabled;
    private String role;
    private String cardInfo;
    private String fingerprintInfo;
    private String faceInfo;
}

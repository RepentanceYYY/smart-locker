// com/tairui/server/dto/LogListDTO.java
package com.tairui.server.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LogListDTO {
    private Integer id;
    private Integer cabinetId;
    private String cabinetTitle;
    private Integer cellNumber;
    private String toolName;
    private String borrowerPhoto;
    private String borrowerName;
    private String borrowerNumber;
    private LocalDateTime borrowTime;
    private String borrowRemark;
    private String returnPhoto;
    private String returnName;
    private String returnNumber;
    private LocalDateTime returnTime;
    private String returnRemark;
    private LocalDateTime expectedReturnTime;
}

// com/tairui/server/dto/BorrowRecordSubmitDTO.java
package com.tairui.server.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BorrowRecordSubmitDTO {
    private List<BorrowItemDTO> borrowItems = new ArrayList<>();
    private String borrowerName;
    private String borrowerNumber;
    private String expectedReturnTime;
    private String remark;
    private String borrowerPhoto;

    @Data
    public static class BorrowItemDTO {
        private Integer cabinetId;
        private String cabinetName;
        private Integer cellId;
        private String cellNumber;
        private String toolName;
        private String borrowTime;
    }
}

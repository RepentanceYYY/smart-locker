package com.tairui.server.webSocket.dto;

import lombok.Data;

@Data
public class BoxAddressConfig {
    private String communicationType;
    private String communicationAddress;
    private Integer startAddress;
    private Integer endAddress;
}

// com/tairui/server/controller/BorrowRecordController.java
package com.tairui.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tairui.server.dto.BorrowRecordSubmitDTO;
import com.tairui.server.dto.LogListDTO;
import com.tairui.server.dto.LogOverviewDTO;
import com.tairui.server.dto.ReturnRecordSubmitDTO;
import com.tairui.server.service.SysOperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SysOperLogController {

    @Autowired
    private SysOperLogService sysOperLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping(value = "/borrow/records", consumes = "multipart/form-data")
    public Map<String, Object> submitBorrowRecords(
            @RequestPart("data") String dataJson,
            @RequestPart(value = "photo", required = false) MultipartFile photoFile) throws Exception {

        BorrowRecordSubmitDTO dto = objectMapper.readValue(dataJson, BorrowRecordSubmitDTO.class);
        sysOperLogService.saveBorrowRecordsWithPhoto(dto, photoFile);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        return result;
    }

    @PostMapping(value = "/return/records", consumes = "multipart/form-data")
    public Map<String, Object> submitReturnRecords(
            @RequestPart("data") String dataJson,
            @RequestPart(value = "photo", required = false) MultipartFile photoFile) throws Exception {

        ReturnRecordSubmitDTO dto = objectMapper.readValue(dataJson, ReturnRecordSubmitDTO.class);
        sysOperLogService.saveReturnRecordsWithPhoto(dto, photoFile);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        return result;
    }
    @GetMapping("/log/overview")
    public Map<String, Object> getLogOverview() {
        LogOverviewDTO data = sysOperLogService.getLogOverview();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return result;
    }

    @GetMapping("/log/listAll")
    public Map<String, Object> getAllLogList(
            @RequestParam(required = false) String borrowerName,
            @RequestParam(required = false) String toolName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        List<LogListDTO> data = sysOperLogService.getAllLogList(borrowerName, toolName, status, startTime, endTime);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return result;
    }

}

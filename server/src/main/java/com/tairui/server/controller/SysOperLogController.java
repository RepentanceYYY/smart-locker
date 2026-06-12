// com/tairui/server/controller/BorrowRecordController.java
package com.tairui.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tairui.server.common.Result;
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
    public Result submitBorrowRecords(
            @RequestPart("data") String dataJson,
            @RequestPart(value = "photo", required = false) MultipartFile photoFile) throws Exception {

        BorrowRecordSubmitDTO dto = objectMapper.readValue(dataJson, BorrowRecordSubmitDTO.class);
        sysOperLogService.saveBorrowRecordsWithPhoto(dto, photoFile);
        return Result.success();
    }

    @PostMapping(value = "/return/records", consumes = "multipart/form-data")
    public Result submitReturnRecords(
            @RequestPart("data") String dataJson,
            @RequestPart(value = "photo", required = false) MultipartFile photoFile) throws Exception {

        ReturnRecordSubmitDTO dto = objectMapper.readValue(dataJson, ReturnRecordSubmitDTO.class);
        sysOperLogService.saveReturnRecordsWithPhoto(dto, photoFile);

        return Result.success();
    }

    @GetMapping("/log/overview")
    public Result getLogOverview() {
        LogOverviewDTO data = sysOperLogService.getLogOverview();
        return Result.success(data);
    }

    @GetMapping("/log/listAll")
    public Result getAllLogList(
            @RequestParam(required = false) String borrowerName,
            @RequestParam(required = false) String toolName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        List<LogListDTO> data = sysOperLogService.getAllLogList(borrowerName, toolName, status, startTime, endTime);
        return Result.success(data);
    }

}

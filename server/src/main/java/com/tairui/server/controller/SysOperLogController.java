// com/tairui/server/controller/BorrowRecordController.java
package com.tairui.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    @PostMapping(value = "/borrow/records")
    public Result submitBorrowRecords(@RequestBody BorrowRecordSubmitDTO dto) throws Exception {

        sysOperLogService.saveBorrowRecordsWithPhoto(dto);
        return Result.success();
    }

    @PostMapping(value = "/return/records")
    public Result submitReturnRecords(@RequestBody ReturnRecordSubmitDTO dto) throws Exception {

        sysOperLogService.saveReturnRecordsWithPhoto(dto);
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

    @GetMapping("/logs/{page}/{size}")
    public Result getLogList(
            @PathVariable Integer page,
            @PathVariable Integer size,
            @RequestParam(required = false) String borrowerName,
            @RequestParam(required = false) String toolName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        Page<LogListDTO> logList = sysOperLogService.getLogList(page, size, borrowerName, toolName, status, startTime, endTime);
        return Result.success(logList);
    }

}

package com.tairui.server.service;

import com.tairui.server.dto.BorrowRecordSubmitDTO;
import com.tairui.server.dto.LogListDTO;
import com.tairui.server.dto.LogOverviewDTO;
import com.tairui.server.dto.ReturnRecordSubmitDTO;
import com.tairui.server.entity.SysOperLog;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * 日志表 服务类
 * </p>
 *
 * @author system
 * @since 2026-05-22
 */
public interface SysOperLogService extends IService<SysOperLog> {


    void saveBorrowRecordsWithPhoto(BorrowRecordSubmitDTO dto, MultipartFile photoFile);

    void saveReturnRecordsWithPhoto(ReturnRecordSubmitDTO dto, MultipartFile photoFile);
    LogOverviewDTO getLogOverview();

    List<LogListDTO> getAllLogList(String borrowerName, String toolName, Integer status, String startTime, String endTime);

    void truncateLogTable();

}

package com.tairui.server.controller;

import com.tairui.server.common.Result;
import com.tairui.server.config.WebConfig;
import com.tairui.server.dto.CellConfigUpdateDTO;
import com.tairui.server.service.CellConfigService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/cellConfig/")
@Validated
public class CellConfigController {

    @Autowired
    private CellConfigService cellConfigService;

    @Autowired
    private WebConfig webConfig;

    /**
     * 图片单独上传接口
     */
    @PostMapping("/uploadImage")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        // 参数校验（Controller 层职责）
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只支持图片文件");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            return Result.error("图片大小不能超过2MB");
        }
        try {
            String imageUrl = webConfig.savePhotoFile(file);
            return Result.success(imageUrl);
        } catch (Exception e) {
            log.error("上传图片失败", e);
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 更新格口配置（含图片单元格）
     */
    @PutMapping("/update")
    public Result<?> updateCell(@Valid @RequestBody CellConfigUpdateDTO dto) {
        cellConfigService.updateCell(dto);
        return Result.success();
    }

    /**
     * 新增格口配置
     */
    @PostMapping("/add")
    public Result<?> addCell(@Valid @RequestBody CellConfigUpdateDTO dto) {
        cellConfigService.addCell(dto);
        return Result.success();
    }

    /**
     * 删除格口配置（根据ID）
     */
    @DeleteMapping("/delete/{id}")
    public Result<?> deleteCell(@PathVariable Integer id) {
        cellConfigService.deleteCell(id);
        return Result.success();
    }
}

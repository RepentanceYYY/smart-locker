package com.tairui.server.controller;

import com.tairui.server.dto.SystemConfigDTO;
import com.tairui.server.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

/**
 * <p>
 * 系统配置表 前端控制器
 * </p>
 *
 * @author system
 * @since 2026-06-02
 */
@RestController
@RequestMapping("/api/systemConfig")
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    @GetMapping
    public SystemConfigDTO getConfig() {
        return systemConfigService.getConfig();
    }

    @PutMapping
    public void updateConfig(@RequestBody SystemConfigDTO dto) {
        systemConfigService.updateConfig(dto);
    }

    @PostMapping("/reset")
    public void resetConfig() {
        systemConfigService.resetConfig();
    }
}

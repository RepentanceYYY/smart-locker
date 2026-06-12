// 文件位置：com/tairui/server/controller/CabinetConfigController.java
package com.tairui.server.controller;

import com.tairui.server.common.Result;
import com.tairui.server.dto.CabinetFullDTO;
import com.tairui.server.dto.CabinetUpdateDTO;
import com.tairui.server.service.CabinetConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cabinet")
public class CabinetConfigController {

    @Autowired
    private CabinetConfigService cabinetConfigService;

    @GetMapping("/list")
    public Result getCabinetList() {
        return Result.success(cabinetConfigService.getFullConfigList());
    }

    @PutMapping("/{id}")
    public Result updateCabinet(@PathVariable Integer id, @RequestBody CabinetUpdateDTO updateDTO) {
        cabinetConfigService.updateCabinet(id, updateDTO);
        return Result.success(200, "更新成功", null);
    }

    // 新增：创建柜子
    @PostMapping
    public Result createCabinet(@RequestBody CabinetUpdateDTO createDTO) {
        cabinetConfigService.createCabinet(createDTO);
        return Result.success(200, "创建成功", null);
    }

    @DeleteMapping("/{id}")
    public Result deleteCabinet(@PathVariable Integer id) {
        cabinetConfigService.deleteCabinet(id);
        return Result.success(200, "删除成功", null);
    }
}

// 文件位置：com/tairui/server/controller/CabinetConfigController.java
package com.tairui.server.controller;

import com.tairui.server.common.Result;
import com.tairui.server.common.exception.ClientException;
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
        // 参数校验
        if (createDTO.getTitle() == null || createDTO.getTitle().trim().isEmpty()) {
            return Result.error(422,"柜子名称不能为空");
        }
        if (createDTO.getWidth() == null || createDTO.getWidth().trim().isEmpty()) {
            return Result.error(422,"柜子宽度不能为空");
        }
        if (createDTO.getHeight() == null || createDTO.getHeight().trim().isEmpty()) {
            return Result.error(422,"柜子高度不能为空");
        }
        if (createDTO.getDehumidifierCommPort() == null || createDTO.getDehumidifierCommPort().trim().isEmpty()) {
            return Result.error(422,"除湿机通讯端口不能为空");
        }
        if (createDTO.getDehumidifierAddr() == null || createDTO.getDehumidifierAddr().trim().isEmpty()) {
            return Result.error(422,"除湿机地址不能为空");
        }
        if (createDTO.getLockCommPort() == null || createDTO.getLockCommPort().trim().isEmpty()) {
            return Result.error(422,"锁板通讯端口不能为空");
        }
        cabinetConfigService.createCabinet(createDTO);
        return Result.success(200, "创建成功", null);
    }

    @DeleteMapping("/{id}")
    public Result deleteCabinet(@PathVariable Integer id) {
        cabinetConfigService.deleteCabinet(id);
        return Result.success(200, "删除成功", null);
    }
}

package com.yxy.monitor.controller;
import com.yxy.monitor.common.PageResult;
import com.yxy.monitor.dto.LogQueryDTO;
import com.yxy.monitor.entity.WebAccessLog;
import com.yxy.monitor.service.WebAccessLogService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/log")
public class WebAccessLogController {
    @Resource
    private WebAccessLogService logService;

    @GetMapping("/list")
    public PageResult<WebAccessLog> list(LogQueryDTO dto) {
        return logService.queryPage(dto);
    }

    @GetMapping("/{id}")
    public WebAccessLog detail(@PathVariable Long id) {
        return logService.getById(id);
    }

    @PostMapping("/recheck/{id}")
    public String recheckOne(@PathVariable Long id) {
        logService.recheckSingle(id);
        return "单条AI重检测完成";
    }

    @PostMapping("/recheck/batch")
    public String recheckBatch(@RequestBody List<Long> idList) {
        logService.recheckBatch(idList);
        return "批量AI重检测完成";
    }
}
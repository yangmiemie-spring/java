package com.yxy.monitor.controller;

import com.yxy.monitor.common.PageResult;
import com.yxy.monitor.common.Result;
import com.yxy.monitor.entity.WebAttackLog;
import com.yxy.monitor.service.WebAttackLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/attack/log")
public class WebAttackLogController {
    @Autowired
    private WebAttackLogService webAttackLogService;

    @GetMapping("/page")
    public Result<PageResult<WebAttackLog>> page(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String attackType
    ){
        PageResult<WebAttackLog> page = webAttackLogService.getPage(pageNum, pageSize, attackType);
        return Result.success(page);
    }
}

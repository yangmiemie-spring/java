package com.yxy.monitor.controller;

import com.yxy.monitor.mapper.FlowStatCounterMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Resource
    private FlowStatCounterMapper statMapper;

    @GetMapping("/stat")
    public Map<String, Object> getStat() {
        try {
            return statMapper.getStat();
        } catch (Exception e) {
            // 极端兜底，保证三个字段都存在，前端永不空白
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("totalFlow", 0L);
            fallback.put("errorFlow", 0L);
            fallback.put("errorRate", 0.0);
            return fallback;
        }
    }
}
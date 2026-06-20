package com.yxy.monitor.controller;

import com.yxy.monitor.mapper.FlowStatCounterMapper;
import com.yxy.monitor.mapper.NetworkFlowLogMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Resource
    private FlowStatCounterMapper statMapper;

    @Resource
    private NetworkFlowLogMapper networkFlowLogMapper;

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
    @GetMapping("/flow/page")
    public Map<String, Object> getFlowPage(@RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize) {
        Map<String, Object> result = new HashMap<>();
        int offset = (pageNum - 1) * pageSize;
        result.put("list", networkFlowLogMapper.getFlowListByPage(offset, pageSize));
        result.put("total", networkFlowLogMapper.getFlowTotalCount());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    @GetMapping("/flow/trend")
    public List<Map<String, Object>> getFlowTrend() {
        return networkFlowLogMapper.getFlowTrend7Day();
    }
}
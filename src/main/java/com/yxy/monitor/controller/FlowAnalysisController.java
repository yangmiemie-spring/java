package com.yxy.monitor.controller;

import com.yxy.monitor.entity.NetworkFlowLog;
import com.yxy.monitor.service.NetworkFlowLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flow")
public class FlowAnalysisController {
    @Autowired
    private NetworkFlowLogService networkFlowLogService;

    @GetMapping("/generate")
    public String generateData(@RequestParam(defaultValue = "10000000") int count) {
        new Thread(() -> networkFlowLogService.generateFlowData(count)).start();
        return "流量数据生成任务后台已启动，预计生成 " + count + " 条数据";
    }

    @GetMapping("/detect")
    public String runDetection(){
        new Thread(() -> networkFlowLogService.runDetection()).start();
        return "流量异常检测任务后台已启动";
    }

    @GetMapping("/overview")
    public Map<String, Object> getOverview() {
        return networkFlowLogService.getOverviewStats();
    }

    @GetMapping("/protocol-distribution")
    public List<Map<String, Object>> getProtocolDistribution() {
        return networkFlowLogService.getProtocolDistribution();
    }

    @GetMapping("/top-ports")
    public List<Map<String, Object>> getTopPorts(@RequestParam(defaultValue = "10") int limit) {
        return networkFlowLogService.getTopDestPorts(limit);
    }

    @GetMapping("/top-abnormal-ips")
    public List<Map<String, Object>> getTopAbnormalIps(@RequestParam(defaultValue = "10") int limit) {
        return networkFlowLogService.getTopAbnormalIps(limit);
    }

    @GetMapping("/trend")
    public List<Map<String, Object>> getFlowTrend() {
        return networkFlowLogService.getFlowTrend();
    }

    @GetMapping("/clean")
    public String cleanFlow(@RequestParam(defaultValue = "7") int day){
        LocalDateTime expire = LocalDateTime.now().minusDays(day);
        networkFlowLogService.deleteExpiredFlow(expire);
        return "已清理" + day + "天前所有流量数据";
    }
}

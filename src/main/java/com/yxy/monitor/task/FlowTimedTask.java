package com.yxy.monitor.task;

import com.yxy.monitor.service.NetworkFlowLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class FlowTimedTask {
    @Autowired
    private NetworkFlowLogService networkFlowLogService;

    @Scheduled(fixedRate = 30000)
    public void autoCreateFlow(){
        networkFlowLogService.generateFlowData(500);
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void autoDetectFlow(){
        networkFlowLogService.runDetection();
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanOldFlowData() {
        // 删除7天前的流量日志
        LocalDateTime sevenDayAgo = LocalDateTime.now().minusDays(7);
        networkFlowLogService.deleteExpiredFlow(sevenDayAgo);
        System.out.println("已自动清理7天前过期流量数据");
    }
}

package com.yxy.monitor.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface NetworkFlowLogService {
    void generateFlowData(int totalCount);
    void runDetection();
    Map<String, Object> getOverviewStats();
    List<Map<String, Object>> getProtocolDistribution();
    List<Map<String, Object>> getTopDestPorts(int limit);
    List<Map<String, Object>> getTopAbnormalIps(int limit);
    List<Map<String, Object>> getFlowTrend();
    void deleteExpiredFlow(LocalDateTime expireTime);
}

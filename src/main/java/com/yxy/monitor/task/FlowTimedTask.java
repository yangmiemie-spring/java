package com.yxy.monitor.task;

import com.yxy.monitor.service.NetworkFlowLogService;
import com.yxy.monitor.mapper.NetworkFlowLogMapper;
import com.yxy.monitor.mapper.FlowStatCounterMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class FlowTimedTask {

    @Autowired
    private NetworkFlowLogService networkFlowLogService;
    @Autowired
    private NetworkFlowLogMapper flowLogMapper;
    @Autowired
    private FlowStatCounterMapper statMapper;

    // 阈值调低，确保模拟流量必触发检测
    private static final int SCAN_PORT_THRESHOLD = 2;
    private static final int DDOS_PACKET_THRESHOLD = 30;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 每30秒生成500条流量
     */
    @Scheduled(fixedRate = 30000)
    public void generateFlowBatch(){
        try {
            int batchSize = 500;
            networkFlowLogService.generateFlowData(batchSize);
            statMapper.addTotal(batchSize);
            statMapper.refreshRate();
            log.info("【1/3 流量生成】成功，本次新增{}条总流量", batchSize);
        } catch (Exception e) {
            log.error("【1/3 流量生成】执行失败", e);
        }
    }

    /**
     * 每分钟增量检测最近5分钟流量
     * 全流程打印日志，有没有执行、标记了多少一目了然
     */
    @Scheduled(fixedRate = 60000)
    public void incrementFlowDetect(){
        try {
            LocalDateTime endTime = LocalDateTime.now().withSecond(0).withNano(0);
            LocalDateTime startTime = endTime.minusMinutes(5);
            log.info("检测时间范围：{} ~ {}", startTime.format(formatter), endTime.format(formatter));

            // 1. 查询待检测的IP分组
            List<Map<String, Object>> stats = flowLogMapper.countByIpAndMinute(startTime, endTime);
            log.info("待检测IP分组数量：{} 个", stats.size());

            int scanCount = 0;
            int ddosCount = 0;
            int totalAbnormalPackets = 0;

            // 2. 逐组匹配规则
            for (Map<String, Object> stat : stats) {
                String srcIp = (String) stat.get("srcIp");
                String timeMinute = (String) stat.get("timeMinute");
                long portCount = ((Number) stat.get("portCount")).longValue();
                long packetCount = ((Number) stat.get("packetCount")).longValue();

                log.info("检测明细 | IP={} | 分钟={} | 不同端口数={} | 总包数={}",
                        srcIp, timeMinute, portCount, packetCount);

                LocalDateTime minuteStart = LocalDateTime.parse(timeMinute + ":00", formatter);
                LocalDateTime minuteEnd = minuteStart.plusMinutes(1);

                // 端口扫描规则
                if (portCount >= SCAN_PORT_THRESHOLD) {
                    flowLogMapper.batchMarkAbnormal(srcIp, minuteStart, minuteEnd, "scan");
                    totalAbnormalPackets += (int) packetCount;
                    scanCount++;
                    log.info("  → 触发端口扫描 | IP={} | 分钟={} | 端口数={} | 包数={}",
                            srcIp, timeMinute, portCount, packetCount);
                }

                // DDoS流量规则
                if (packetCount >= DDOS_PACKET_THRESHOLD) {
                    flowLogMapper.batchMarkAbnormal(srcIp, minuteStart, minuteEnd, "ddos");
                    totalAbnormalPackets += (int) packetCount;
                    ddosCount++;
                    log.info("  → 触发DDoS攻击 | IP={} | 分钟={} | 包数={}",
                            srcIp, timeMinute, packetCount);
                }
            }

            // 3. 更新计数器
            if (totalAbnormalPackets > 0) {
                statMapper.addAbnormal(totalAbnormalPackets);
                statMapper.refreshRate();
                log.info("【增量流量检测】完成 | 端口扫描{}个 | DDoS{}个 | 新增异常流量{}条",
                        scanCount, ddosCount, totalAbnormalPackets);
            } else {
                log.info("【增量流量检测】完成 | 本次未检测到异常流量");
            }

        } catch (Exception e) {
            log.error("【增量流量检测】执行失败", e);
        }
        log.info("========== 【增量流量检测】执行结束 ==========\n");
    }

    /**
     * 每天凌晨2点全量校准
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void fullFlowDetect(){
        try {
            log.info("【凌晨全量检测】开始执行全量流量校准");
            networkFlowLogService.runDetection();
            log.info("【凌晨全量检测】全量校准执行完成");
        } catch (Exception e) {
            log.error("【凌晨全量检测】执行失败", e);
        }
    }

    /**
     * 每天凌晨3点清理过期数据
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanOldFlowData() {
        try {
            LocalDateTime sevenDayAgo = LocalDateTime.now().minusDays(7);
            networkFlowLogService.deleteExpiredFlow(sevenDayAgo);
            log.info("【数据清理】已清理7天前过期流量数据");
        } catch (Exception e) {
            log.error("数据清理】执行失败", e);
        }
    }
}
package com.yxy.monitor.service.impl;

import com.yxy.monitor.entity.NetworkFlowLog;
import com.yxy.monitor.mapper.NetworkFlowLogMapper;
import com.yxy.monitor.service.NetworkFlowLogService;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class NetworkFlowLogServiceImpl implements NetworkFlowLogService {
    @Autowired
    private NetworkFlowLogMapper networkFlowLogMapper;
    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    private final Random random = new Random();
    // 常用正常端口
    private final int[] normalPorts = {80, 443, 8080, 22, 3306, 6379, 21, 3389};
    // 协议类型
    private final String[] protocols = {"TCP", "TCP", "TCP", "TCP", "UDP", "ICMP"};
    // 模拟攻击IP
    private final String[] attackIps = {
            "192.168.200.100", "192.168.200.101",
            "10.0.0.88", "172.16.0.99", "203.0.113.45"
    };

    // 检测阈值
    private static final int SCAN_PORT_THRESHOLD = 20;
    private static final int DDOS_PACKET_THRESHOLD = 500;
    // 批量提交大小：每1000条commit一次
    private static final int BATCH_SIZE = 1000;

    @Override
    public void generateFlowData(int totalCount) {
        // 开启批量模式会话，关闭自动提交，手动控制事务
        try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
            NetworkFlowLogMapper batchMapper = sqlSession.getMapper(NetworkFlowLogMapper.class);

            for (int i = 1; i <= totalCount; i++) {
                NetworkFlowLog log = new NetworkFlowLog();

                // 95% 正常流量，5% 攻击流量
                if (random.nextInt(100) < 95) {
                    // 正常流量
                    log.setSrcIp(generateNormalIp());
                    log.setDestIp("192.168.1.100");
                    log.setSrcPort(1024 + random.nextInt(60000));
                    log.setDestPort(normalPorts[random.nextInt(normalPorts.length)]);
                    log.setProtocol(protocols[random.nextInt(protocols.length)]);
                    log.setPacketSize(64 + random.nextInt(1400));
                    log.setFlowTime(generateRandomTime(24));
                } else {
                    // 攻击流量：一半端口扫描，一半DDoS
                    if (random.nextBoolean()) {
                        // 端口扫描：短时间内访问大量不同端口
                        log.setSrcIp(attackIps[random.nextInt(attackIps.length)]);
                        log.setDestIp("192.168.1.100");
                        log.setSrcPort(1024 + random.nextInt(60000));
                        log.setDestPort(1 + random.nextInt(65535));
                        log.setProtocol("TCP");
                        log.setPacketSize(40 + random.nextInt(60));
                        log.setFlowTime(generateRandomTime(1));
                    } else {
                        // DDoS攻击：短时间内发送大量小包
                        log.setSrcIp(attackIps[random.nextInt(attackIps.length)]);
                        log.setDestIp("192.168.1.100");
                        log.setSrcPort(1024 + random.nextInt(60000));
                        log.setDestPort(80);
                        log.setProtocol("TCP");
                        log.setPacketSize(20 + random.nextInt(40));
                        log.setFlowTime(generateRandomTime(1));
                    }
                }

                batchMapper.insert(log);

                // 每1000条提交一次事务，清空缓存避免内存溢出
                if (i % BATCH_SIZE == 0) {
                    sqlSession.commit();
                    sqlSession.clearCache();
                    System.out.println("已生成流量数据：" + i + " 条");
                }
            }

            // 提交最后剩余不足1000条的数据
            sqlSession.commit();
            System.out.println("流量数据全部生成完成，共 " + totalCount + " 条");
        }
    }

    @Override
    public void runDetection() {
        System.out.println("==== 开始执行流量异常检测 ====");
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(24);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 按IP+分钟维度统计
        List<Map<String, Object>> stats = networkFlowLogMapper.countByIpAndMinute(startTime, endTime);

        int scanCount = 0;
        int ddosCount = 0;

        for (Map<String, Object> stat : stats) {
            String srcIp = (String) stat.get("srcIp");
            String timeMinute = (String) stat.get("timeMinute");
            long portCount = ((Number) stat.get("portCount")).longValue();
            long packetCount = ((Number) stat.get("packetCount")).longValue();

            LocalDateTime minuteStart = LocalDateTime.parse(timeMinute + ":00", formatter);
            LocalDateTime minuteEnd = minuteStart.plusMinutes(1);

            // 端口扫描检测：1分钟内访问端口数超过阈值
            if (portCount >= SCAN_PORT_THRESHOLD) {
                networkFlowLogMapper.batchMarkAbnormal(srcIp, minuteStart, minuteEnd, "scan");
                scanCount++;
                System.out.println("检测到端口扫描 | IP=" + srcIp + " | 时间=" + timeMinute + " | 访问端口数=" + portCount);
            }

            // DDoS检测：1分钟内数据包数超过阈值
            if (packetCount >= DDOS_PACKET_THRESHOLD) {
                networkFlowLogMapper.batchMarkAbnormal(srcIp, minuteStart, minuteEnd, "ddos");
                ddosCount++;
                System.out.println("检测到DDoS攻击 | IP=" + srcIp + " | 时间=" + timeMinute + " | 数据包数=" + packetCount);
            }
        }

        System.out.println("==== 流量异常检测完成：共发现 " + scanCount + " 起端口扫描，" + ddosCount + " 起DDoS攻击 ====");
    }

    @Override
    public Map<String, Object> getOverviewStats() {
        return networkFlowLogMapper.getOverviewStats();
    }

    @Override
    public List<Map<String, Object>> getProtocolDistribution() {
        return networkFlowLogMapper.getProtocolDistribution();
    }

    @Override
    public List<Map<String, Object>> getTopDestPorts(int limit) {
        return networkFlowLogMapper.getTopDestPorts(limit);
    }

    @Override
    public List<Map<String, Object>> getTopAbnormalIps(int limit) {
        return networkFlowLogMapper.getTopAbnormalIps(limit);
    }

    @Override
    public List<Map<String, Object>> getFlowTrend() {
        return networkFlowLogMapper.getFlowTrend();
    }

    @Override
    public void deleteExpiredFlow(LocalDateTime expireTime) {
        networkFlowLogMapper.deleteExpiredData(expireTime);
    }

    /**
     * 生成随机正常内网IP
     */
    private String generateNormalIp() {
        return "192.168." + (1 + random.nextInt(50)) + "." + (1 + random.nextInt(254));
    }

    /**
     * 生成最近hours小时内的随机时间
     */
    private LocalDateTime generateRandomTime(int hours) {
        return LocalDateTime.now().minusMinutes(random.nextInt(hours * 60));
    }


}

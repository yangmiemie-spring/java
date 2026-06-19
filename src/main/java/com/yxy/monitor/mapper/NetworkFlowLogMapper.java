package com.yxy.monitor.mapper;

import com.yxy.monitor.entity.NetworkFlowLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface NetworkFlowLogMapper {
    /**
     * 单条插入流量数据（和你现有代码写法完全一致）
     */
    @Insert("INSERT INTO network_flow_log(src_ip, dest_ip, src_port, dest_port, protocol, packet_size, flow_time, is_abnormal) " +
            "VALUES (#{srcIp}, #{destIp}, #{srcPort}, #{destPort}, #{protocol}, #{packetSize}, #{flowTime}, 0)")
    void insert(NetworkFlowLog log);

    /**
     * 查询未检测的流量总数
     */
    @Select("SELECT COUNT(*) FROM network_flow_log WHERE is_abnormal = 0")
    long countUnchecked();

    /**
     * 按分钟+源IP分组，统计端口数和包数（用于异常检测）
     */
    /**
     * 优化版：只查最近1分钟未检测流量 + 走覆盖索引，毫秒级返回
     */
    @Select("SELECT src_ip AS srcIp, " +
            "DATE_FORMAT(flow_time, '%Y-%m-%d %H:%i') AS timeMinute, " +
            "COUNT(DISTINCT dest_port) AS portCount, " +
            "COUNT(*) AS packetCount " +
            "FROM network_flow_log " +
            "WHERE flow_time >= #{startTime} AND flow_time < #{endTime} " +
            "AND is_abnormal = 0 " +
            "GROUP BY src_ip, timeMinute " +
            "ORDER BY NULL")
    List<Map<String, Object>> countByIpAndMinute(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);
    /**
     * 批量标记异常流量
     */
    @Update("UPDATE network_flow_log " +
            "SET is_abnormal = 1, abnormal_type = #{abnormalType} " +
            "WHERE src_ip = #{srcIp} " +
            "AND flow_time BETWEEN #{startTime} AND #{endTime} " +
            "AND is_abnormal = 0")
    void batchMarkAbnormal(@Param("srcIp") String srcIp,
                           @Param("startTime") LocalDateTime startTime,
                           @Param("endTime") LocalDateTime endTime,
                           @Param("abnormalType") String abnormalType);

    /**
     * 统计总流量数、异常数、异常占比
     */
    @Select("SELECT " +
            "COUNT(*) AS totalFlow, " +
            "SUM(CASE WHEN is_abnormal = 1 THEN 1 ELSE 0 END) AS abnormalFlow, " +
            "ROUND(SUM(CASE WHEN is_abnormal = 1 THEN 1 ELSE 0 END) / COUNT(*) * 100, 2) AS abnormalRate " +
            "FROM network_flow_log")
    Map<String, Object> getOverviewStats();

    /**
     * 协议类型分布统计（饼图用）
     */
    @Select("SELECT protocol AS name, COUNT(*) AS value FROM network_flow_log GROUP BY protocol")
    List<Map<String, Object>> getProtocolDistribution();

    /**
     * 被访问端口Top排行（柱状图用）
     */
    @Select("SELECT dest_port AS port, COUNT(*) AS count " +
            "FROM network_flow_log " +
            "GROUP BY dest_port " +
            "ORDER BY count DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> getTopDestPorts(@Param("limit") int limit);

    /**
     * 异常攻击IP Top排行
     */
    @Select("SELECT src_ip AS ip, COUNT(*) AS attackCount, abnormal_type AS attackType " +
            "FROM network_flow_log " +
            "WHERE is_abnormal = 1 " +
            "GROUP BY src_ip " +
            "ORDER BY attackCount DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> getTopAbnormalIps(@Param("limit") int limit);

    /**
     * 最近24小时流量趋势（折线图用）
     */
    @Select("SELECT " +
            "DATE_FORMAT(flow_time, '%Y-%m-%d %H:00') AS hour, " +
            "COUNT(*) AS totalCount, " +
            "SUM(CASE WHEN is_abnormal = 1 THEN 1 ELSE 0 END) AS abnormalCount " +
            "FROM network_flow_log " +
            "WHERE flow_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR) " +
            "GROUP BY DATE_FORMAT(flow_time, '%Y-%m-%d %H:00') " +
            "ORDER BY hour ASC")
    List<Map<String, Object>> getFlowTrend();

    /**
     * 删除指定时间之前的过期流量数据
     */
    @Delete("DELETE FROM network_flow_log WHERE flow_time < #{expireTime}")
    void deleteExpiredData(@Param("expireTime") LocalDateTime expireTime);


    // tianjia
    /**
     * 查询指定时间范围内，每个源IP访问的不同端口数、总数据包数
     * 只查未检测的正常流量，小范围扫描极快
     */
    @Select("SELECT src_ip AS srcIp, COUNT(DISTINCT dest_port) AS portCount, COUNT(*) AS packetCount " +
            "FROM network_flow_log " +
            "WHERE flow_time BETWEEN #{startTime} AND #{endTime} AND is_abnormal = 0 " +
            "GROUP BY src_ip")
    List<Map<String, Object>> selectIpPortStatByTime(@Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 批量标记指定IP、指定时间范围内的流量为异常
     */
    @Update("UPDATE network_flow_log SET is_abnormal = 1, abnormal_type = #{type} " +
            "WHERE src_ip = #{srcIp} AND flow_time BETWEEN #{startTime} AND #{endTime} AND is_abnormal = 0")
    void batchMarkAbnormalByTime(@Param("srcIp") String srcIp,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime,
                                 @Param("type") String type);
}

package com.yxy.monitor.mapper;

import com.yxy.monitor.entity.NetworkFlowLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface NetworkFlowLogMapper {

    /**
     * 单条插入流量数据
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
     * 【优化版】按分钟+源IP分组，统计端口数和包数（异常检测核心方法）
     * 优化点：统一左闭右开时间范围、ORDER BY NULL跳过排序、走覆盖索引毫秒级返回
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
     * 【优化版】批量标记异常流量
     * 优化点：统一左闭右开时间范围，和查询逻辑对齐，避免漏标记/重复标记
     */
    @Update("UPDATE network_flow_log " +
            "SET is_abnormal = 1, abnormal_type = #{abnormalType} " +
            "WHERE src_ip = #{srcIp} " +
            "AND flow_time >= #{startTime} AND flow_time < #{endTime} " +
            "AND is_abnormal = 0")
    void batchMarkAbnormal(@Param("srcIp") String srcIp,
                           @Param("startTime") LocalDateTime startTime,
                           @Param("endTime") LocalDateTime endTime,
                           @Param("abnormalType") String abnormalType);

    /**
     * 全量统计总流量、异常数、占比（仅凌晨校准用，禁止前端高频调用）
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
     * 最近24小时流量趋势（小时粒度折线图用）
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
     * 近7天流量趋势（天粒度折线图用，对应你参考的页面效果）
     */
    @Select("SELECT " +
            "DATE(flow_time) AS date, " +
            "COUNT(*) AS totalCount, " +
            "SUM(IF(is_abnormal=1, 1, 0)) AS abnormalCount " +
            "FROM network_flow_log " +
            "WHERE flow_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "GROUP BY DATE(flow_time) " +
            "ORDER BY date ASC")
    List<Map<String, Object>> getFlowTrend7Day();

    /**
     * 分页查询流量明细（首页表格用）
     */
    @Select("SELECT id, src_ip AS srcIp, dest_ip AS destIp, src_port AS srcPort, dest_port AS destPort, " +
            "protocol, packet_size AS packetSize, is_abnormal AS isAbnormal, abnormal_type AS abnormalType, flow_time AS flowTime " +
            "FROM network_flow_log " +
            "ORDER BY flow_time DESC " +
            "LIMIT #{offset}, #{pageSize}")
    List<Map<String, Object>> getFlowListByPage(@Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 查询流量总条数（分页用）
     */
    @Select("SELECT COUNT(*) FROM network_flow_log")
    int getFlowTotalCount();

    /**
     * 删除指定时间之前的过期流量数据
     */
    @Delete("DELETE FROM network_flow_log WHERE flow_time < #{expireTime}")
    void deleteExpiredData(@Param("expireTime") LocalDateTime expireTime);


}
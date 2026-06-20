package com.yxy.monitor.mapper;

import com.yxy.monitor.dto.LogQueryDTO;
import com.yxy.monitor.entity.WebAccessLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

public interface WebAccessLogMapper {
    /**
     * 分页查询AI标记攻击记录，按id分页，避免一次性加载全量数据OOM
     */
    @Select("SELECT id, ip_addr AS ipAddr, request_url AS requestUrl, request_method AS requestMethod, ai_message AS aiMessage, create_time AS createTime " +
            "FROM web_access_log WHERE ai_label = 1 AND id > #{lastId} ORDER BY id ASC LIMIT #{pageSize}")
    List<Map<String, Object>> selectAiAttackPage(@Param("lastId") Long lastId, @Param("pageSize") int pageSize);

    // 分页列表 按创建时间倒序（纯静态SQL，无任何动态标签）
    @Select("""
        SELECT
            id,
            ip_addr AS ipAddr,
            request_url AS requestUrl,
            request_method AS requestMethod,
            create_time AS createTime,
            ai_label AS aiLabel,
            ai_message AS aiMessage,
            ai_score AS aiScore
        FROM web_access_log
        WHERE 1=1
          AND (#{dto.ipAddr} IS NULL OR ip_addr LIKE CONCAT('%',#{dto.ipAddr},'%'))
          AND (#{dto.aiLabel} IS NULL OR ai_label = #{dto.aiLabel})
          AND (#{dto.requestMethod} IS NULL OR request_method = #{dto.requestMethod})
          AND (#{dto.startTime} IS NULL OR create_time >= #{dto.startTime})
          AND (#{dto.endTime} IS NULL OR create_time <= #{dto.endTime})
        ORDER BY create_time DESC
        LIMIT #{offset}, #{dto.pageSize}
        """)
    List<WebAccessLog> selectLogPage(@Param("dto") LogQueryDTO dto, @Param("offset") int offset);

    // 统计总条数（纯静态SQL）
    @Select("""
        SELECT COUNT(*)
        FROM web_access_log
        WHERE 1=1
          AND (#{dto.ipAddr} IS NULL OR ip_addr LIKE CONCAT('%',#{dto.ipAddr},'%'))
          AND (#{dto.aiLabel} IS NULL OR ai_label = #{dto.aiLabel})
          AND (#{dto.requestMethod} IS NULL OR request_method = #{dto.requestMethod})
          AND (#{dto.startTime} IS NULL OR create_time >= #{dto.startTime})
          AND (#{dto.endTime} IS NULL OR create_time <= #{dto.endTime})
        """)
    Long selectLogCount(@Param("dto") LogQueryDTO dto);

    // 根据ID查单条详情
    @Select("""
        SELECT
            id,
            ip_addr AS ipAddr,
            request_url AS requestUrl,
            request_method AS requestMethod,
            create_time AS createTime,
            ai_label AS aiLabel,
            ai_message AS aiMessage,
            ai_score AS aiScore
        FROM web_access_log
        WHERE id = #{id}
        """)
    WebAccessLog selectById(@Param("id") Long id);

    // 更新AI检测结果
    @Update("""
        UPDATE web_access_log
        SET ai_label = #{log.aiLabel},
            ai_message = #{log.aiMessage},
            ai_score = #{log.aiScore}
        WHERE id = #{log.id}
        """)
    int updateAiResult(@Param("log") WebAccessLog log);
}

package com.yxy.monitor.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface WebAccessLogMapper {
    /**
     * 分页查询AI标记攻击记录，按id分页，避免一次性加载全量数据OOM
     */
    @Select("SELECT id, ip_addr AS ipAddr, request_url AS requestUrl, request_method AS requestMethod, ai_message AS aiMessage, create_time AS createTime " +
            "FROM web_access_log WHERE ai_label = 1 AND id > #{lastId} ORDER BY id ASC LIMIT #{pageSize}")
    List<Map<String, Object>> selectAiAttackPage(@Param("lastId") Long lastId, @Param("pageSize") int pageSize);

}

package com.yxy.monitor.mapper;

import com.yxy.monitor.entity.WebAttackLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface WebAttackLogMapper {
    // 新增攻击日志
    @Insert("INSERT INTO web_attack_log(client_ip, request_url, request_method, attack_type, attack_payload, is_block, create_time) " +
    "VALUES (#{clientIp}, #{requestUrl}, #{requestMethod}, #{attackType}, #{attackPayload}, #{isBlock}, #{createTime})")
    void insertAttackLog(WebAttackLog log);
    // 分页查询数据
    @Select("SELECT * FROM web_attack_log WHERE attack_type = #{attackType} ORDER BY create_time DESC LIMIT #{offset}, #{pageSize}")
    List<WebAttackLog> selectPageData(@Param("offset") Long offset,
                                      @Param("pageSize") Long pageSize,
                                      @Param("attackType") String attackType);
    // 统计总条数
    @Select("SELECT COUNT(*) FROM web_attack_log WHERE attack_type = #{attackType}")
    Long selectCount(@Param("attackType") String attackType);
    // 无条件分页（全部数据）
    @Select("SELECT * FROM web_attack_log ORDER BY create_time DESC LIMIT #{offset},#{pageSize}")
    List<WebAttackLog> selectAllPage(@Param("offset") Long offset,
                                     @Param("pageSize") Long pageSize);
    // 无条件统计所有条数
    @Select("SELECT COUNT(*) FROM web_attack_log")
    Long selectAllCount();
}

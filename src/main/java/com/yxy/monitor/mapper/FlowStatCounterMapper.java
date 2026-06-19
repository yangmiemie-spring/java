package com.yxy.monitor.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.Map;

@Mapper
public interface FlowStatCounterMapper {
    // 首页只读这个，单行查询，毫秒返回，连接瞬间释放
    @Select("SELECT total_flow AS totalFlow, abnormal_flow AS errorFlow, abnormal_rate AS errorRate FROM flow_stat_counter WHERE id = 1")
    Map<String, Object> getStat();

    // 生成流量后调用：总流量批量累加
    @Update("UPDATE flow_stat_counter SET total_flow = total_flow + #{num} WHERE id = 1")
    void addTotal(@Param("num") int num);

    // 检测出异常后调用：异常数批量累加
    @Update("UPDATE flow_stat_counter SET abnormal_flow = abnormal_flow + #{num} WHERE id = 1")
    void addAbnormal(@Param("num") int num);

    // 重新计算占比
    @Update("UPDATE flow_stat_counter SET abnormal_rate = ROUND(abnormal_flow / total_flow * 100, 2) WHERE id = 1")
    void refreshRate();
}
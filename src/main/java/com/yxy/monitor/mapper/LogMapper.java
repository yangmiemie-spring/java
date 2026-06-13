package com.yxy.monitor.mapper;


import com.yxy.monitor.entity.RequestLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LogMapper {
    @Insert("INSERT INTO request_log(ip,url,method,params,create_time) " +
            "VALUES(#{ip},#{url},#{method},#{params},#{createTime})")
    void insertRequestLog(RequestLog log);
}

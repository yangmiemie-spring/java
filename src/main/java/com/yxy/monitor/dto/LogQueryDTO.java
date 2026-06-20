package com.yxy.monitor.dto;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class LogQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    // 筛选
    private String ipAddr;
    private Integer aiLabel; // 0正常 /1攻击
    private String requestMethod;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date startTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date endTime;
}
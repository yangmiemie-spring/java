package com.yxy.monitor.entity;

import lombok.Data;

import java.util.Date;

@Data
public class WebAccessLog {
    private Long id;
    private String requestUrl;
    private String ipAddr;
    private String requestMethod;
    private Date createTime;
    private Integer aiLabel; //0正常 1攻击
    private String aiMessage;
    private Double aiScore;
}

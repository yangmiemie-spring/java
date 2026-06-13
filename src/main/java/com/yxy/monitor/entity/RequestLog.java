package com.yxy.monitor.entity;

import lombok.Data;

import java.util.Date;

@Data
public class RequestLog {
    private Long id;
    private String ip;
    private String url;
    private String method;
    private String params;
    private Integer isAttack;
    private Date createTime;
}

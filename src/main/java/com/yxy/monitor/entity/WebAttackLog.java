package com.yxy.monitor.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WebAttackLog {
    private Long id;
    private String clientIp;
    private String requestUrl;
    private String requestMethod;
    private String attackType;
    private String attackPayload;
    private Integer isBlock;
    private LocalDateTime createTime;
}

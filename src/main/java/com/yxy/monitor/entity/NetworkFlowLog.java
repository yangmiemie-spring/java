package com.yxy.monitor.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NetworkFlowLog {
    private Long id;
    private String srcIp;
    private String destIp;
    private Integer srcPort;
    private Integer destPort;
    private String protocol;
    private Integer packetSize;
    private LocalDateTime flowTime;
    private Integer isAbnormal;
    private String abnormalType;

}

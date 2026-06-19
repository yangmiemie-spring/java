package com.yxy.monitor.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysUser {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private LocalDateTime createTime;
    private Integer status;
}

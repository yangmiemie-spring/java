package com.yxy.monitor.service;

import com.yxy.monitor.entity.SysUser;

import java.util.Map;

public interface AuthService {
    String register(SysUser user);
    // 登录，返回token和用户信息
    Map<String, Object> login(String username, String password);
    // 根据用户名查用户
    SysUser getUserByUsername(String username);
    // 修改密码
    String updatePassword(String olaPwd, String newPwd);
}

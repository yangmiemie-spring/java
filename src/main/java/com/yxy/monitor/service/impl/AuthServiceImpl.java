package com.yxy.monitor.service.impl;

import com.yxy.monitor.entity.SysUser;
import com.yxy.monitor.entity.SysUserRole;
import com.yxy.monitor.mapper.SysUserMapper;
import com.yxy.monitor.mapper.SysUserRoleMapper;
import com.yxy.monitor.service.AuthService;
import com.yxy.monitor.util.JwtUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j // 日志注解，用于控制台打印日志
@Service
public class AuthServiceImpl implements AuthService {
    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private SysUserRoleMapper sysUserRoleMapper; // 新增角色关联Mapper
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private JwtUtil jwtUtil;

    @Override
    public String register(SysUser user) {
        try {
            SysUser exist = sysUserMapper.selectByUsername(user.getUsername());
            if (exist != null) {
                log.warn("注册失败：用户名{}已存在", user.getUsername());
                return "用户名已存在";
            }
            // 密码加密
            String encodePwd = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodePwd);
            user.setStatus(1);
            // 插入用户
            sysUserMapper.insert(user);
            log.info("用户{}注册成功，用户ID：{}", user.getUsername(), user.getId());

            // 绑定普通用户角色
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(2L);
            sysUserRoleMapper.insert(userRole);
            log.info("为用户{}绑定普通角色成功", user.getUsername());

            return "注册成功，请登录";
        } catch (Exception e) {
            // 捕获所有异常，打印完整堆栈日志
            log.error("注册用户{}发生异常", user.getUsername(), e);
            throw new RuntimeException("注册失败，系统异常");
        }
    }

    @Override
    public Map<String, Object> login(String username, String password) {
        try {
            SysUser user = sysUserMapper.selectByUsername(username);
            if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
                log.warn("登录失败：用户名{}，密码校验不通过", username);
                throw new RuntimeException("用户名或密码错误！");
            }
            // 生成token
            String token = jwtUtil.generateToken(user.getId(), username);
            List<String> roles = sysUserMapper.getUserRoleKeys(user.getId());
            log.info("用户{}登录成功，分配角色：{}", username, roles);

            Map<String, Object> map = new HashMap<>();
            map.put("token", token);
            map.put("userId", user.getId());
            map.put("username", user.getUsername());
            map.put("nickname", user.getNickname());
            map.put("roles", roles);
            return map;
        } catch (RuntimeException e) {
            log.warn("登录业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("登录用户{}发生未知异常", username, e);
            throw new RuntimeException("登录失败，系统异常");
        }
    }

    @Override
    public SysUser getUserByUsername(String username) {
        try {
            return sysUserMapper.selectByUsername(username);
        } catch (Exception e) {
            log.error("查询用户{}异常", username, e);
            throw new RuntimeException("查询用户信息失败");
        }
    }

    @Override
    public String updatePassword(String oldPassword, String newPassword) {
        // 获取当前登录用户
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken)
                SecurityContextHolder.getContext().getAuthentication();
        SysUser loginUser = (SysUser) auth.getPrincipal();
        SysUser dbUser = sysUserMapper.selectByUsername(loginUser.getUsername());

        // 校验旧密码
        if(!passwordEncoder.matches(oldPassword, dbUser.getPassword())){
            throw new RuntimeException("旧密码输入错误");
        }
        // 加密新密码更新
        String encodeNew = passwordEncoder.encode(newPassword);
        sysUserMapper.updatePwd(dbUser.getId(), encodeNew);
        log.info("用户{}修改密码成功", dbUser.getUsername());
        return "密码修改成功，请重新登录";
    }

    private SysUser getCurrentUser() {
        try {
            return (SysUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (Exception e) {
            log.error("获取当前登录用户异常", e);
            throw new RuntimeException("未登录，请重新登录");
        }
    }
}
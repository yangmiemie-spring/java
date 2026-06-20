package com.yxy.monitor.controller;

import com.yxy.monitor.dto.LoginDTO;
import com.yxy.monitor.dto.RegisterDTO;
import com.yxy.monitor.dto.UpdatePwdDTO;
import com.yxy.monitor.entity.SysUser;
import com.yxy.monitor.service.AuthService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(originPatterns = {"http://127.0.0.1:*","http://localhost:*"}, allowCredentials = "true")
public class AuthController {
    @Resource
    private AuthService authService;

    // 注册接口
    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterDTO dto) {
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        return authService.register(user);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginDTO dto) {
        return authService.login(dto.getUsername(), dto.getPassword());
    }

    // 读取当前登录用户信息
    @GetMapping("/info")
    public Map<String, Object> getInfo(){
        SysUser user = (SysUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return authService.login(user.getUsername(), "");
    }

    // 修改密码
    @PostMapping("/updatePwd")
    public String updatePassword(@Valid @RequestBody UpdatePwdDTO dto) {
        return authService.updatePassword(dto.getOldPassword(), dto.getNewPassword());
    }

    @PreAuthorize("hasAuthority('sys:flow:generate')")
    @GetMapping("/admin/test")
    public String adminOnly(){
        return "只有管理员能访问";
    }
}

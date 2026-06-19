package com.yxy.monitor.filter;

import com.yxy.monitor.entity.SysUser;
import com.yxy.monitor.mapper.SysUserMapper;
import com.yxy.monitor.util.JwtUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    @Resource
    private JwtUtil jwtUtil;
    @Resource
    private SysUserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 登录、注册接口直接跳过JWT校验，优先放行
        String uri = request.getRequestURI();
        if (uri.equals("/api/auth/login") || uri.equals("/api/auth/register")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("Authorization");
        if(token == null || !token.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
            return;
        }
        String realToken = token.substring(7);
        if(!jwtUtil.verifyToken(realToken)){
            filterChain.doFilter(request, response);
            return;
        }
        // 解析用户名，查询用户信息
        String username = jwtUtil.getUsername(realToken);
        SysUser user = userMapper.selectByUsername(username);
        List<String> roles = userMapper.getUserRoleKeys(user.getId());
        List<String> perms = userMapper.getUserPermKeys(user.getId());

        // 角色+权限全部装入权限集合
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        // 角色前缀ROLE_（SpringSecurity规范）
        authorities.addAll(roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList()));
        // 功能权限
        authorities.addAll(perms.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }
}
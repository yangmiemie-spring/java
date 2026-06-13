package com.yxy.monitor.filter;

import com.yxy.monitor.entity.WebAttackLog;
import com.yxy.monitor.service.WebAttackLogService;
import com.yxy.monitor.util.WebAttackRulerUtil;
import com.yxy.monitor.wrapper.XssRequestWrapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

@Component
public class WebAttackFilter implements Filter {
    @Autowired
    private WebAttackLogService attackLogService;

    private static final List<String> WHITE_LIST = Arrays.asList(
            "/batch/importLog"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest)  request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        String requestURL = httpReq.getRequestURI();
        // 白名单直接放行
        if(WHITE_LIST.contains(requestURL)) {
            chain.doFilter(request, response);
            return;
        }

        String clientip = getClientIp(httpReq);
        String url = httpReq.getRequestURL().toString();
        String method = httpReq.getMethod();

        // 检测url后台扫描
        String urlAttackType = WebAttackRulerUtil.checkAttackPayload(url);
        if(urlAttackType != null) {
            saveAndBlockAttack(httpResp, clientip, url, method, urlAttackType, url);
            return;
        }

        // 检测所有GET参数
        XssRequestWrapper wrapper = new XssRequestWrapper(httpReq);
        Enumeration<String> paramNames = wrapper.getParameterNames();
        while(paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String[] paramValues = wrapper.getParameterValues(paramName);
            for(String val : paramValues) {
                if(val == null) continue;
                String attackType = WebAttackRulerUtil.checkAttackPayload(val);
                if(attackType != null) {
                    saveAndBlockAttack(httpResp, clientip, url, method, attackType, val);
                    return;
                }
            }
        }

        // 去攻击放行
        chain.doFilter(wrapper, response);
    }

    // 拦截+存日志
    private void saveAndBlockAttack(HttpServletResponse response, String ip, String url, String method, String attackType, String payload) throws IOException {
        WebAttackLog attackLog = new WebAttackLog();
        attackLog.setClientIp(ip);
        attackLog.setRequestUrl(url);
        attackLog.setRequestMethod(method);
        attackLog.setAttackType(attackType);
        attackLog.setAttackPayload(payload);
        attackLog.setIsBlock(1);
        attackLog.setCreateTime(LocalDateTime.now());
        attackLogService.insertAttackLog(attackLog);

        // 返回403 JSON格式
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        String json = "{\"code\":403,\"msg\":\"安全防护：检测到" + attackType + "攻击，访问已拦截\",\"data\":null}";
        response.getWriter().write(json);
    }

    // 获取客户端IP
    private String getClientIp(HttpServletRequest request){
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if(xForwardedFor != null && !xForwardedFor.isEmpty()){
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

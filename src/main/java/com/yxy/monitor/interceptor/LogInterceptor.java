package com.yxy.monitor.interceptor;

import com.yxy.monitor.entity.RequestLog;
import com.yxy.monitor.mapper.LogMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Date;

@Component
public class LogInterceptor implements HandlerInterceptor {
    @Autowired
    private LogMapper logMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientIp = request.getRemoteAddr();
        String requesturl = request.getRequestURL().toString();
        String requestMethod = request.getMethod();
        String queryParams = request.getQueryString();

        RequestLog requestLog = new RequestLog();
        requestLog.setIp(clientIp);
        requestLog.setUrl(requesturl);
        requestLog.setMethod(requestMethod);
        requestLog.setParams(queryParams);
        requestLog.setCreateTime(new Date());

        logMapper.insertRequestLog(requestLog);
        return true;
    }
}

package com.yxy.monitor.wrapper;

import com.yxy.monitor.util.WebAttackRulerUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class XssRequestWrapper extends HttpServletRequestWrapper {

    public XssRequestWrapper(HttpServletRequest request) {
        super(request);
    }
    // 单个参数检测
    @Override
    public String getParameter(String name) {
        return super.getParameter(name);
    }

}

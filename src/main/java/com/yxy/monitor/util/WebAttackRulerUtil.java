package com.yxy.monitor.util;

import java.util.regex.Pattern;

public class WebAttackRulerUtil {
    // SQL注入正则规则
    private static final Pattern SQL_INJECT_PATTERN = Pattern.compile(
            "(select|insert|update|delete|drop|alter|union|or|and|exec|sleep|benchmark|#|--)",
            Pattern.CASE_INSENSITIVE
    );
    // XSS跨服脚本正则规则
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "<script|javascript:|onload=|onclick=|alert\\(|confirm\\(",
            Pattern.CASE_INSENSITIVE
    );

    // 路径遍历 ../读取文件
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
            "(\\.\\/|\\.\\.\\/|%2E%2E|etc\\/passwd|windows\\/system32)",
            Pattern.CASE_INSENSITIVE
    );
    // 恶意扫描url（后台路径、webshell、数据库管理地址）
    private static final Pattern SCAN_URL_PATTERN = Pattern.compile(
            "(admin|phpadmin|webshell|shell.jsp|backup|database|config\\.ini|\\.env)",
            Pattern.CASE_INSENSITIVE
    );
    // 白名单放行地址
    private static final String[] WHITE_LIST = {"/api/login", "/api/captcha"};

    public static boolean isWhiteList(String url){
        for(String item:WHITE_LIST){
            if(url.contains(item)) return true;
        }
        return false;
    }

    // 统一检测载荷，返回攻击类型，无攻击返回null
    public static String checkAttackPayload(String content) {
        if(content == null || content.isEmpty()){
            return null;
        }

        if(SQL_INJECT_PATTERN.matcher(content).find()){
            return "SQL注入";
        }

        if(XSS_PATTERN.matcher(content).find()){
            return "XSS跨站攻击";
        }

        if(PATH_TRAVERSAL_PATTERN.matcher(content).find()){
            return "路径遍历攻击";
        }

        if(SCAN_URL_PATTERN.matcher(content).find()){
            return "恶意后台扫描";
        }
        return null;
    }

    // 单独检测url路径（后台扫描）
    public static String checkUrlScan(String url){
        if(SCAN_URL_PATTERN.matcher(url).find()){
            return "恶意后台扫描攻击";
        }
        return null;
    }
}

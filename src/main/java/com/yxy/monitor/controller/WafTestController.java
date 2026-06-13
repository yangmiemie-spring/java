package com.yxy.monitor.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/waf")
public class WafTestController {
    @GetMapping("/test")
    public String test(@RequestParam(required = false) String param){
        return "正常访问，参数：" + param;
    }
}

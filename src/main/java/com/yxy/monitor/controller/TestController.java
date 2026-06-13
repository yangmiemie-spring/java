package com.yxy.monitor.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/test")
    public String testRequest(){
        return "web入侵检测后端服务运行成功！";
    }
}

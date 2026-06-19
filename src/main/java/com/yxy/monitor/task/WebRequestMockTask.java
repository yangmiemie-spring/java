package com.yxy.monitor.task;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Random;

@Slf4j
@Component
public class WebRequestMockTask {
    @Resource
    private RestTemplate restTemplate;
    private final Random random = new Random();
    private final String baseUrl = "http://localhost:8082";

    private final String[] getApi = {"/api/dashboard/stat", "/api/flow/list"};
    private final String[] attackUrls = {
            "/api/user?id=1 union select 1,2,3",
            "/api/post?content=<script>alert(1)</script>",
            "/api/comment?path=../../etc/passwd"
    };

    @Scheduled(fixedRate = 20000)
    public void mockWebAccess() {
        int normalGet = 0, attackReq = 0;
        try {
            // 正常GET
            for (int i = 0; i < 4; i++) {
                String url = getApi[random.nextInt(getApi.length)];
                restTemplate.getForObject(baseUrl + url, String.class);
                normalGet++;
            }
            // 攻击GET
            for (int i = 0; i < 2; i++) {
                String url = attackUrls[random.nextInt(attackUrls.length)];
                restTemplate.getForObject(baseUrl + url, String.class);
                attackReq++;
            }
            log.info("【Web请求模拟】正常GET:{} 攻击请求:{}", normalGet, attackReq);
        } catch (Exception e) {
            log.warn("【Web请求模拟】请求异常, 错误:{}", e.getMessage());
        }
    }
}
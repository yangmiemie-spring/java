package com.yxy.monitor.task;

import com.yxy.monitor.mapper.WebAccessLogMapper;
import com.yxy.monitor.mapper.WebAttackLogMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AiAttackSyncTask {
    @Resource
    private WebAccessLogMapper accessLogMapper;
    @Resource
    private WebAttackLogMapper attackLogMapper;

    private Long lastMaxId = 0L;
    private static final int PAGE_SIZE = 30; // 缩小分页，减少单次连接占用时间

    @Scheduled(fixedRate = 60000)
    public void syncAiAttackData() {
        int syncTotal = 0;
        try {
            while (true) {
                List<Map<String, Object>> page = accessLogMapper.selectAiAttackPage(lastMaxId, PAGE_SIZE);
                if (page.isEmpty()) break;
                for (Map<String, Object> item : page) {
                    Long id = ((Number) item.get("id")).longValue();
                    String type = item.get("aiMessage") + "(AI)";
                    attackLogMapper.insertAiAttack(
                            (String) item.get("ipAddr"),
                            (String) item.get("requestUrl"),
                            (String) item.get("requestMethod"),
                            type,
                            (String) item.get("requestUrl"),
                            0
                    );
                    lastMaxId = id;
                    syncTotal++;
                }
            }
            log.info("【AI攻击同步】本次同步{}条AI攻击记录", syncTotal);
        } catch (Exception e) {
            log.error("【AI攻击同步】同步异常，已同步{}条", syncTotal, e);
        }
    }
}
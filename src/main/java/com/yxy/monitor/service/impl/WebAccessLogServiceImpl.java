package com.yxy.monitor.service.impl;

import com.yxy.monitor.dto.LogQueryDTO;
import com.yxy.monitor.common.PageResult;
import com.yxy.monitor.entity.WebAccessLog;
import com.yxy.monitor.mapper.WebAccessLogMapper;
import com.yxy.monitor.python.PythonAiClient;
import com.yxy.monitor.service.WebAccessLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class WebAccessLogServiceImpl implements WebAccessLogService {
    @Resource
    private WebAccessLogMapper webAccessLogMapper;

    @Resource
    private PythonAiClient pythonAiClient;

    @Override
    public PageResult<WebAccessLog> queryPage(LogQueryDTO dto) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        List<WebAccessLog> records = webAccessLogMapper.selectLogPage(dto, offset);
        Long total = webAccessLogMapper.selectLogCount(dto);

        PageResult<WebAccessLog> page = new PageResult<>();
        page.setRecords(records);
        page.setTotal(total);
        page.setPageNum(Long.valueOf(dto.getPageNum()));
        page.setPageSize(Long.valueOf(dto.getPageSize()));
        return page;
    }

    @Override
    public WebAccessLog getById(Long id) {
        return webAccessLogMapper.selectById(id);
    }

    /**
     * 单条日志重新AI检测
     */
    @Override
    public void recheckSingle(Long logId) {
        WebAccessLog log = webAccessLogMapper.selectById(logId);
        if (log == null) {
            throw new RuntimeException("日志不存在");
        }
        // 封装单条url到列表，调用批量接口
        List<String> urlList = new ArrayList<>();
        urlList.add(log.getRequestUrl());
        List<Map<String, Object>> aiResultList = pythonAiClient.batchDetect(urlList);
        if (aiResultList.isEmpty()) {
            throw new RuntimeException("AI检测无返回结果");
        }
        Map<String, Object> aiResult = aiResultList.get(0);
        // 匹配你WebAccessLog字段：aiLabel(0/1) aiMessage aiScore
        Integer label = Integer.valueOf(aiResult.get("label").toString());
        String msg = aiResult.get("message").toString();
        Double score = Double.valueOf(aiResult.get("score").toString());

        log.setAiLabel(label);
        log.setAiMessage(msg);
        log.setAiScore(score);
        webAccessLogMapper.updateAiResult(log);
    }

    /**
     * 批量多条日志重新AI检测
     */
    @Override
    public void recheckBatch(List<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return;
        }
        // 1. 查出所有日志
        List<WebAccessLog> logList = new ArrayList<>();
        List<String> urlList = new ArrayList<>();
        for (Long id : idList) {
            WebAccessLog log = webAccessLogMapper.selectById(id);
            if (log != null) {
                logList.add(log);
                urlList.add(log.getRequestUrl());
            }
        }
        // 批量调用AI
        List<Map<String, Object>> aiResultList = pythonAiClient.batchDetect(urlList);
        // 一一对应更新数据库
        for (int i = 0; i < logList.size(); i++) {
            WebAccessLog log = logList.get(i);
            Map<String, Object> res = aiResultList.get(i);
            Integer label = Integer.valueOf(res.get("label").toString());
            String msg = res.get("message").toString();
            Double score = Double.valueOf(res.get("score").toString());
            log.setAiLabel(label);
            log.setAiMessage(msg);
            log.setAiScore(score);
            webAccessLogMapper.updateAiResult(log);
        }
    }
}
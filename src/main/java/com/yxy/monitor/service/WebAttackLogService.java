package com.yxy.monitor.service;

import com.yxy.monitor.common.PageResult;
import com.yxy.monitor.entity.WebAttackLog;

public interface WebAttackLogService {
    void insertAttackLog(WebAttackLog log);
    PageResult<WebAttackLog> getPage(Long pageNum, Long pageSize, String attackType);
}

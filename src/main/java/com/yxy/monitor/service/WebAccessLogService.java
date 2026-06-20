package com.yxy.monitor.service;

import com.yxy.monitor.common.PageResult;
import com.yxy.monitor.dto.LogQueryDTO;
import com.yxy.monitor.entity.WebAccessLog;

import java.util.List;

public interface WebAccessLogService {
    PageResult<WebAccessLog> queryPage(LogQueryDTO dto);
    WebAccessLog getById(Long id);
    void recheckSingle(Long logId);
    void recheckBatch(List<Long> idList);
}

package com.yxy.monitor.service.impl;

import com.yxy.monitor.common.PageResult;
import com.yxy.monitor.entity.WebAttackLog;
import com.yxy.monitor.mapper.WebAttackLogMapper;
import com.yxy.monitor.service.WebAttackLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WebAttackLogServiceImpl implements WebAttackLogService {
    @Autowired
    private WebAttackLogMapper webAttackLogMapper;

    @Override
    public void insertAttackLog(WebAttackLog log){
        webAttackLogMapper.insertAttackLog(log);
    }

    @Override
    public PageResult<WebAttackLog> getPage(Long pageNum, Long pageSize, String attackType) {
        PageResult<WebAttackLog> page = new PageResult<>();
        Long offset = (pageNum-1)*pageSize;

        // 前端没传攻击类型，赋值空字符串，SQL改成查询全部
        List<WebAttackLog> list;
        Long total;
        if(attackType == null || attackType.isBlank()){
            list = webAttackLogMapper.selectAllPage(offset, pageSize);
            total = webAttackLogMapper.selectAllCount();
        } else{
            list = webAttackLogMapper.selectPageData(offset, pageSize, attackType);
            total = webAttackLogMapper.selectCount(attackType);
        }

        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        page.setTotal(total);
        page.setRecords(list);
        return page;
    }

}

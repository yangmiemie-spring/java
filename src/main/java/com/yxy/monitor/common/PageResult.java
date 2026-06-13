package com.yxy.monitor.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private Long pageNum;
    private Long pageSize;
    private Long total;
    private List<T> records;
}
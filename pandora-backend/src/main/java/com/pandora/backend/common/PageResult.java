package com.pandora.backend.common;

import lombok.Data;

import java.util.List;

/**
 * 分页查询结果包装类。
 * 用于列表查询接口返回，包含当前页数据、总记录数、分页信息。
 * 被所有分页查询接口使用。
 *
 * @param <T> 记录类型
 */
@Data
public class PageResult<T> {
    private List<T> records;
    private long total;
    private int pages;
    private int page;
    private int size;

    /**
     * 构造分页结果。
     * 自动计算总页数（向上取整）。
     *
     * @param records 当前页记录列表
     * @param total 总记录数
     * @param page 当前页码（从1开始）
     * @param size 每页大小
     */
    public PageResult(List<T> records, long total, int page, int size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
        this.pages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
    }
}
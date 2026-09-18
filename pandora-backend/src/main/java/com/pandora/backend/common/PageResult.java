package com.pandora.backend.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records;
    private long total;
    private int pages;
    private int page;
    private int size;

    public PageResult(List<T> records, long total, int page, int size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
        this.pages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
    }
}
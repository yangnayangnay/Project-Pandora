package com.pandora.backend.permission;

import lombok.Getter;

@Getter
public enum FunctionCode {
    COMPANY_IMPORTANT_EDIT("COMPANY_IMPORTANT_EDIT", "公司十大重要事项编辑"),
    COMPANY_DISPATCH_EDIT("COMPANY_DISPATCH_EDIT", "公司十大任务派发编辑");

    private final String code;
    private final String name;

    FunctionCode(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static FunctionCode fromCode(String code) {
        for (FunctionCode fc : values()) {
            if (fc.code.equals(code)) {
                return fc;
            }
        }
        return null;
    }
}
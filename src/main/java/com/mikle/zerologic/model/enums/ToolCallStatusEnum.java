package com.mikle.zerologic.model.enums;

import lombok.Getter;

@Getter
public enum ToolCallStatusEnum {

    SUCCESS("success", "成功"),
    FAILED("failed", "失败"),
    REJECTED("rejected", "已拒绝");

    private final String value;

    private final String text;

    ToolCallStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}

package com.mikle.zerologic.model.enums;

import lombok.Getter;

@Getter
public enum ToolRiskLevelEnum {

    LOW("low", "低风险"),
    MEDIUM("medium", "中风险"),
    HIGH("high", "高风险");

    private final String value;

    private final String text;

    ToolRiskLevelEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}

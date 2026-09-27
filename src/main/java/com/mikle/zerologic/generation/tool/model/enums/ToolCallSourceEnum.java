package com.mikle.zerologic.generation.tool.model.enums;

import lombok.Getter;

/**
 * 工具调用来源。
 */
@Getter
public enum ToolCallSourceEnum {

    GENERATE("generate", "代码生成"),
    REPAIR("repair", "自动修复"),
    MANUAL("manual", "人工调用");

    private final String value;

    private final String text;

    ToolCallSourceEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}

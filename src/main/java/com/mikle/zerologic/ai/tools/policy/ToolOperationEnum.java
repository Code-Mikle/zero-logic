package com.mikle.zerologic.ai.tools.policy;

import lombok.Getter;

@Getter
public enum ToolOperationEnum {

    READ("read", "读取"),
    WRITE("write", "写入"),
    MODIFY("modify", "修改"),
    DELETE("delete", "删除"),
    CONTROL("control", "控制");

    private final String value;

    private final String text;

    ToolOperationEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}

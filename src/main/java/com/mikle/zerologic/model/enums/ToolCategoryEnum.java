package com.mikle.zerologic.model.enums;

import lombok.Getter;

@Getter
public enum ToolCategoryEnum {

    FILE("file", "文件工具"),
    BUILD("build", "构建工具"),
    DEPLOY("deploy", "部署工具"),
    KNOWLEDGE("knowledge", "知识库工具"),
    CONTROL("control", "控制工具");

    private final String value;

    private final String text;

    ToolCategoryEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}

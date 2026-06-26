package com.mikle.zerologic.model.enums;

import lombok.Getter;

@Getter
public enum ProjectVersionStatusEnum {

    CREATED("created", "已创建"),
    BUILT("built", "已构建"),
    FAILED("failed", "失败"),
    DEPLOYED("deployed", "已部署");

    private final String value;

    private final String text;

    ProjectVersionStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}

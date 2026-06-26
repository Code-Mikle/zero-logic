package com.mikle.zerologic.model.enums;

import lombok.Getter;

@Getter
public enum DeployTypeEnum {

    DEPLOY("deploy", "部署"),
    ROLLBACK("rollback", "回滚");

    private final String value;

    private final String text;

    DeployTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}

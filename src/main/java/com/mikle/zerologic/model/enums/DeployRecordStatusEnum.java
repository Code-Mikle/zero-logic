package com.mikle.zerologic.model.enums;

import lombok.Getter;

@Getter
public enum DeployRecordStatusEnum {

    RUNNING("running", "部署中"),
    SUCCESS("success", "成功"),
    FAILED("failed", "失败"),
    ROLLED_BACK("rolled_back", "已回滚");

    private final String value;

    private final String text;

    DeployRecordStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}

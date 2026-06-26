package com.mikle.zerologic.model.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

@Getter
public enum GenerationBuildStatusEnum {
    RUNNING("执行中", "running"),
    SUCCESS("成功", "success"),
    FAILED("失败", "failed"),
    TIMEOUT("超时", "timeout");

    private final String text;
    private final String value;

    GenerationBuildStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static GenerationBuildStatusEnum getEnumByValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (GenerationBuildStatusEnum status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null;
    }
}

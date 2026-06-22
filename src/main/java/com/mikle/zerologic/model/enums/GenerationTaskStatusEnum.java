package com.mikle.zerologic.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum GenerationTaskStatusEnum {

    PENDING("待执行", "pending"),
    RUNNING("执行中", "running"),
    SUCCESS("成功", "success"),
    FAILED("失败", "failed"),
    CANCELED("已取消", "canceled");

    private final String text;
    private final String value;

    GenerationTaskStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static GenerationTaskStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (GenerationTaskStatusEnum anEnum : GenerationTaskStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}

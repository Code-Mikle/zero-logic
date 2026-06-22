package com.mikle.zerologic.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum GenerationTaskTypeEnum {
    GENERATE("代码生成", "generate"),
    BUILD("构建", "build"),
    DEPLOY("部署", "deploy"),
    REPAIR("修复", "repair");

    private final String text;
    private final String value;

    GenerationTaskTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static GenerationTaskTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (GenerationTaskTypeEnum anEnum : GenerationTaskTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}

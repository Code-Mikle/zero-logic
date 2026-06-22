package com.mikle.zerologic.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum KnowledgeDocumentStatusEnum {
    ACTIVE("正常", "active"),
    DELETED("已删除", "deleted");

    private final String text;
    private final String value;

    KnowledgeDocumentStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static KnowledgeDocumentStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (KnowledgeDocumentStatusEnum anEnum : KnowledgeDocumentStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}

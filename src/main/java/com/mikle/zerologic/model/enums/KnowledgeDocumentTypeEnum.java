package com.mikle.zerologic.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum KnowledgeDocumentTypeEnum {
    ATTACHMENT("附件", "attachment"),
    REQUIREMENT("需求文档", "requirement"),
    API_DOC("接口文档", "api_doc"),
    COMPONENT_SPEC("组件规范", "component_spec");

    private final String text;
    private final String value;

    KnowledgeDocumentTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static KnowledgeDocumentTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (KnowledgeDocumentTypeEnum anEnum : KnowledgeDocumentTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}

package com.mikle.zerologic.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum AttachmentStatusEnum {

    TEMPORARY("temporary", "temporary"),
    BOUND("bound", "bound");

    private final String text;

    private final String value;

    AttachmentStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static AttachmentStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (AttachmentStatusEnum atEnum : AttachmentStatusEnum.values()) {
            if (atEnum.value.equals(value)) {
                return atEnum;
            }
        }
        return null;
    }

}

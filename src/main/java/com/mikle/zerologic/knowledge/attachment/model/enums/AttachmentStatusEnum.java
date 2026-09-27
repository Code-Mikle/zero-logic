package com.mikle.zerologic.knowledge.attachment.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum AttachmentStatusEnum {

    // 用户已经上传附件，但附件还没有绑定到具体应用。此时通常 appId = null。如果用户没有完成应用创建，定时任务会清理这种附件
    TEMPORARY("临时附件", "temporary"),
    // 附件已经绑定到某个应用，此时通常 appId != null，可以用于生成任务、知识库解析等业务
    BOUND("已绑定附件", "bound");

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

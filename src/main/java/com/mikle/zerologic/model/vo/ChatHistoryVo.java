package com.mikle.zerologic.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatHistoryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String message;

    private String messageType;

    private PromptAttachmentVO promptAttachmentVO;

    private LocalDateTime createTime;
}

package com.mikle.zerologic.conversation.model.vo;

import com.mikle.zerologic.knowledge.attachment.model.vo.PromptAttachmentVO;
import com.mikle.zerologic.knowledge.retrieval.model.vo.RagRetrievalVO;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatHistoryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long taskId;

    private String message;

    private String messageType;

    private PromptAttachmentVO promptAttachmentVO;

    private RagRetrievalVO ragRetrieval;

    private LocalDateTime createTime;
}

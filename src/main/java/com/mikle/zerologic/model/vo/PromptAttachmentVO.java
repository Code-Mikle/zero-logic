package com.mikle.zerologic.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class PromptAttachmentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String fileName;

    private String fileExtension;

    private String contentType;

    private Long fileSize;

}

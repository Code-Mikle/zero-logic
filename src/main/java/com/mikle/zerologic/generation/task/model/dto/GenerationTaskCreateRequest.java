package com.mikle.zerologic.generation.task.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class GenerationTaskCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long appId;

    private String message;

    private Long attachmentId;
}

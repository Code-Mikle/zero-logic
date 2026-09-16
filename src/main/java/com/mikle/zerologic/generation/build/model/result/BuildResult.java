package com.mikle.zerologic.generation.build.model.result;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class BuildResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long buildRecordId;

    private Boolean success;

    private String status;

    private String command;

    private Integer exitCode;

    private String logText;

    private Long durationMs;

    private Boolean timedOut;

    private String projectPath;

    private String artifactPath;
}

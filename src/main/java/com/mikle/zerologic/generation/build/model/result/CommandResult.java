package com.mikle.zerologic.generation.build.model.result;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CommandResult {
    private List<String> command;
    private Integer exitCode;
    private String output;
    private Long durationMs;
    private Boolean timedOut;
}

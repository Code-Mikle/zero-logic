package com.mikle.zerologic.core.repair.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CodeRepairResult {
    private boolean success;
    private String status;
    private String aiResponse;
    private String errorMessage;
    private List<String> changedFiles;
    private Long repairRecordId;
}

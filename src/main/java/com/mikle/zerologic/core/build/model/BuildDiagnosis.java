package com.mikle.zerologic.core.build.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class BuildDiagnosis implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private boolean repairable;
    private String summary;
    private List<String> suspectedFiles;
}

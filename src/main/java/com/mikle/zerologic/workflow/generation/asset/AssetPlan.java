package com.mikle.zerologic.workflow.generation.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetPlan implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Boolean enabled;

    private String reason;

    private List<AssetSearchTask> searchTasks;

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled) && searchTasks != null && !searchTasks.isEmpty();
    }
}

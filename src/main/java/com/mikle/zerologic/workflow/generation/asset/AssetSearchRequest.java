package com.mikle.zerologic.workflow.generation.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetSearchRequest {

    private Long taskId;

    private Long appId;

    private Long userId;

    private String keyword;

    private Integer limit;
}

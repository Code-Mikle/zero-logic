package com.mikle.zerologic.workflow.generation.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetSearchTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String keyword;

    private String scene;

    private Integer limit;
}

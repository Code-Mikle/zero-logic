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
public class AssetResource implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String keyword;

    private String title;

    private String url;

    private String source;

    private String description;
}

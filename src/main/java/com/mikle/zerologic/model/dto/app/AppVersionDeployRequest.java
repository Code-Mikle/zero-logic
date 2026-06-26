package com.mikle.zerologic.model.dto.app;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppVersionDeployRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long appId;

    private Long versionId;
}

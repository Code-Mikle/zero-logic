package com.mikle.zerologic.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "generation.repair")
public class RepairProperties {
    private boolean enabled = true;

    @Min(0)
    @Max(3)
    private int maxAttempts = 2;

    @Min(30)
    @Max(600)
    private int timeoutSeconds = 180;

    @Min(1000)
    @Max(30000)
    private int maxBuildLogChars = 12000;
}

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
@ConfigurationProperties(prefix = "generation.build")
public class BuildProperties {
    private boolean enabled = true;

    @Min(10)
    @Max(1800)
    private int installTimeoutSeconds = 300;

    @Min(10)
    @Max(1800)
    private int buildTimeoutSeconds = 180;

    @Min(1000)
    @Max(500000)
    private int maxLogChars = 60000;
}

package com.mikle.zerologic.model.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyGenerationStatVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String date;

    private Long taskCount;

    private Long successCount;

    private Long failedCount;
}

package com.mikle.zerologic.model.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationDashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long totalTaskCount;

    private Long successTaskCount;

    private Long failedTaskCount;

    private Long runningTaskCount;

    private BigDecimal successRate;

    private Long totalTokenUsage;

    private Long totalToolCallCount;

    private BigDecimal avgDurationSeconds;

    private Long buildSuccessCount;

    private Long buildFailedCount;

    private Long repairTotalCount;

    private Long repairSuccessCount;

    private Long highRiskToolCallCount;

    private List<DailyGenerationStatVO> dailyStats;
}

package com.mikle.zerologic.service.impl;

import com.mikle.zerologic.constant.UserConstant;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.exception.ThrowUtils;
import com.mikle.zerologic.model.entity.App;
import com.mikle.zerologic.model.entity.GenerationBuildRecord;
import com.mikle.zerologic.model.entity.GenerationRepairRecord;
import com.mikle.zerologic.model.entity.GenerationTask;
import com.mikle.zerologic.model.entity.ToolCallRecord;
import com.mikle.zerologic.model.entity.User;
import com.mikle.zerologic.model.enums.GenerationBuildStatusEnum;
import com.mikle.zerologic.model.enums.GenerationTaskStatusEnum;
import com.mikle.zerologic.model.enums.ToolRiskLevelEnum;
import com.mikle.zerologic.model.vo.dashboard.DailyGenerationStatVO;
import com.mikle.zerologic.model.vo.dashboard.GenerationDashboardVO;
import com.mikle.zerologic.service.AppService;
import com.mikle.zerologic.service.DashboardService;
import com.mikle.zerologic.service.GenerationBuildRecordService;
import com.mikle.zerologic.service.GenerationRepairRecordService;
import com.mikle.zerologic.service.GenerationTaskService;
import com.mikle.zerologic.service.ToolCallRecordService;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final int DAILY_STAT_DAYS = 7;
    private static final String REPAIR_SUCCESS_STATUS = "success";

    @Resource
    private AppService appService;

    @Resource
    private GenerationTaskService generationTaskService;

    @Resource
    private GenerationBuildRecordService generationBuildRecordService;

    @Resource
    private GenerationRepairRecordService generationRepairRecordService;

    @Resource
    private ToolCallRecordService toolCallRecordService;

    @Override
    public GenerationDashboardVO getGenerationDashboard(Long appId, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        boolean admin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        Long scopeUserId = admin ? null : loginUser.getId();
        if (appId != null) {
            App app = appService.getById(appId);
            ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
            if (!admin && !Objects.equals(app.getUserId(), loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该应用看板");
            }
            scopeUserId = null;
        }

        List<GenerationTask> tasks = generationTaskService.list(scopeQuery(appId, scopeUserId));
        List<GenerationBuildRecord> buildRecords = generationBuildRecordService.list(scopeQuery(appId, scopeUserId));
        List<GenerationRepairRecord> repairRecords = generationRepairRecordService.list(scopeQuery(appId, scopeUserId));
        List<ToolCallRecord> toolCallRecords = listToolCallRecordsByTasks(tasks);

        long totalTaskCount = tasks.size();
        long successTaskCount = count(tasks, task -> GenerationTaskStatusEnum.SUCCESS.getValue().equals(task.getStatus()));
        long failedTaskCount = count(tasks, task -> GenerationTaskStatusEnum.FAILED.getValue().equals(task.getStatus()));
        long runningTaskCount = count(tasks, task -> GenerationTaskStatusEnum.RUNNING.getValue().equals(task.getStatus())
                || GenerationTaskStatusEnum.PENDING.getValue().equals(task.getStatus()));

        long totalTokenUsage = tasks.stream()
                .map(GenerationTask::getTokenUsage)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
        long totalToolCallCount = toolCallRecords.size();

        BigDecimal avgDurationSeconds = calcAvgDurationSeconds(tasks);
        BigDecimal successRate = percent(successTaskCount, totalTaskCount);

        long buildSuccessCount = count(buildRecords,
                record -> GenerationBuildStatusEnum.SUCCESS.getValue().equals(record.getStatus()));
        long buildFailedCount = count(buildRecords,
                record -> GenerationBuildStatusEnum.FAILED.getValue().equals(record.getStatus())
                        || GenerationBuildStatusEnum.TIMEOUT.getValue().equals(record.getStatus()));
        long repairSuccessCount = count(repairRecords, record -> REPAIR_SUCCESS_STATUS.equals(record.getStatus()));
        long highRiskToolCallCount = count(toolCallRecords,
                record -> ToolRiskLevelEnum.HIGH.getValue().equals(record.getRiskLevel()));

        return GenerationDashboardVO.builder()
                .totalTaskCount(totalTaskCount)
                .successTaskCount(successTaskCount)
                .failedTaskCount(failedTaskCount)
                .runningTaskCount(runningTaskCount)
                .successRate(successRate)
                .totalTokenUsage(totalTokenUsage)
                .totalToolCallCount(totalToolCallCount)
                .avgDurationSeconds(avgDurationSeconds)
                .buildSuccessCount(buildSuccessCount)
                .buildFailedCount(buildFailedCount)
                .repairTotalCount((long) repairRecords.size())
                .repairSuccessCount(repairSuccessCount)
                .highRiskToolCallCount(highRiskToolCallCount)
                .dailyStats(buildDailyStats(tasks))
                .build();
    }

    private QueryWrapper scopeQuery(Long appId, Long userId) {
        return QueryWrapper.create()
                .eq("appId", appId)
                .eq("userId", userId);
    }

    private List<ToolCallRecord> listToolCallRecordsByTasks(List<GenerationTask> tasks) {
        List<Long> taskIds = tasks.stream()
                .map(GenerationTask::getId)
                .filter(Objects::nonNull)
                .toList();
        if (taskIds.isEmpty()) {
            return List.of();
        }
        return toolCallRecordService.list(QueryWrapper.create()
                .in("taskId", taskIds));
    }

    private <T> long count(List<T> list, Predicate<T> predicate) {
        return list.stream().filter(predicate).count();
    }

    private BigDecimal calcAvgDurationSeconds(List<GenerationTask> tasks) {
        List<Long> durations = tasks.stream()
                .filter(task -> task.getStartTime() != null && task.getEndTime() != null)
                .map(task -> ChronoUnit.MILLIS.between(task.getStartTime(), task.getEndTime()))
                .filter(duration -> duration >= 0)
                .toList();
        if (durations.isEmpty()) {
            return BigDecimal.ZERO;
        }
        double avgMs = durations.stream().mapToLong(Long::longValue).average().orElse(0);
        return BigDecimal.valueOf(avgMs / 1000).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private List<DailyGenerationStatVO> buildDailyStats(List<GenerationTask> tasks) {
        LocalDate start = LocalDate.now().minusDays(DAILY_STAT_DAYS - 1L);
        List<DailyGenerationStatVO> result = new ArrayList<>();
        for (int i = 0; i < DAILY_STAT_DAYS; i++) {
            LocalDate date = start.plusDays(i);
            List<GenerationTask> dailyTasks = tasks.stream()
                    .filter(task -> task.getCreateTime() != null)
                    .filter(task -> date.equals(task.getCreateTime().toLocalDate()))
                    .toList();
            result.add(DailyGenerationStatVO.builder()
                    .date(date.toString())
                    .taskCount((long) dailyTasks.size())
                    .successCount(count(dailyTasks,
                            task -> GenerationTaskStatusEnum.SUCCESS.getValue().equals(task.getStatus())))
                    .failedCount(count(dailyTasks,
                            task -> GenerationTaskStatusEnum.FAILED.getValue().equals(task.getStatus())))
                    .build());
        }
        return result;
    }
}

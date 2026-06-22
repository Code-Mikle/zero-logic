package com.mikle.zerologic.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.service.GenerationAppLockService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 后续 AppServiceImpl 和 GenerationTaskServiceImpl 都用它
 */
@Service
@Slf4j
public class GenerationAppLockServiceImpl implements GenerationAppLockService {

    private static final String KEY_PREFIX = "zero-logic:generation:app:";

    @Resource
    private RedissonClient redissonClient;

    @Value("${generation.app-permit-lease-minutes:30}")
    private long leaseMinutes;

    @Override
    public String acquire(Long appId) {
        RPermitExpirableSemaphore semaphore =
                redissonClient.getPermitExpirableSemaphore(KEY_PREFIX + appId);
        semaphore.trySetPermits(1);

        try {
            String permitId = semaphore.tryAcquire(0, leaseMinutes, TimeUnit.MINUTES);
            if (permitId == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前应用正在生成中，请稍后再试");
            }
            return permitId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取生成许可失败");
        }
    }

    @Override
    public void release(Long appId, String permitId) {
        if (StrUtil.isBlank(permitId)) {
            return;
        }
        try {
            redissonClient.getPermitExpirableSemaphore(KEY_PREFIX + appId).release(permitId);
        } catch (Exception e) {
            log.warn("释放生成许可失败，appId={}, permitId={}", appId, permitId, e);
        }
    }
}

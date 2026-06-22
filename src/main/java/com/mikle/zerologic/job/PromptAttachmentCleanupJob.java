package com.mikle.zerologic.job;

import com.mikle.zerologic.model.enums.AttachmentStatusEnum;
import com.mikle.zerologic.service.PromptAttachmentService;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class PromptAttachmentCleanupJob {

    @Resource
    private PromptAttachmentService promptAttachmentService;

    @Value("${attachment.temporary-ttl-hours:24}")
    private long temporaryTtlHours;

    /**
     * 解决的是以下情况：
     * - 主页上传成功后用户没完成创建
     * - 创建应用前后链路异常
     * - 用户刷新页面
     * - 临时附件没有绑定成功
     * 不解决：
     * - 对话页已绑定但未使用的附件
     * - 同一个app下多次上传后废弃的附件
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupTemporaryAttachments() {
        LocalDateTime expireTime = LocalDateTime.now().minusHours(temporaryTtlHours);

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("status", AttachmentStatusEnum.TEMPORARY.getValue())
                .isNull("appId")
                .lt("createTime", expireTime);

        boolean removed = promptAttachmentService.remove(queryWrapper);

        if (removed) {
            log.info("临时附件清理完成，expireTime={}", expireTime);
        }
    }
}

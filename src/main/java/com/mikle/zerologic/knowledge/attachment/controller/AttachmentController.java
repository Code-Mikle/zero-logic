package com.mikle.zerologic.knowledge.attachment.controller;

import com.mikle.zerologic.common.BaseResponse;
import com.mikle.zerologic.common.ResultUtils;
import com.mikle.zerologic.infrastructure.ratelimiter.annotation.RateLimit;
import com.mikle.zerologic.infrastructure.ratelimiter.enums.RateLimitType;
import com.mikle.zerologic.user.model.entity.User;
import com.mikle.zerologic.knowledge.attachment.model.vo.PromptAttachmentVO;
import com.mikle.zerologic.knowledge.attachment.service.PromptAttachmentService;
import com.mikle.zerologic.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/attachment")
public class AttachmentController {

    @Resource
    private UserService userService;

    @Resource
    private PromptAttachmentService promptAttachmentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(
            key = "attachment_upload",
            limitType = RateLimitType.USER,
            rate = 10,
            rateInterval = 60,
            message = "附件上传过于频繁，请稍后再试"
    )
    public BaseResponse<PromptAttachmentVO> upload(
            @RequestParam(value = "appId", required = false) Long appId,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);
        PromptAttachmentVO promptAttachmentVO = promptAttachmentService.upload(file, appId, loginUser);
        return ResultUtils.success(promptAttachmentVO);
    }
}

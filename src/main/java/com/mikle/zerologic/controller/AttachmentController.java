package com.mikle.zerologic.controller;

import com.mikle.zerologic.common.BaseResponse;
import com.mikle.zerologic.common.ResultUtils;
import com.mikle.zerologic.model.entity.User;
import com.mikle.zerologic.model.vo.PromptAttachmentVO;
import com.mikle.zerologic.service.PromptAttachmentService;
import com.mikle.zerologic.service.UserService;
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

    // 这个接口只接收 multipart/form-data 请求
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<PromptAttachmentVO> upload(
            @RequestParam(required = false) Long appId, // 表示 appId 可以不传，不传时是 null
            @RequestPart("file") MultipartFile file, // 表示从 multipart 请求的某个 part 中取出名为 file 的文件部分
            HttpServletRequest httpServletRequest) {

        User loginUser = userService.getLoginUser(httpServletRequest);
        PromptAttachmentVO promptAttachmentVO = promptAttachmentService.upload(file, appId, loginUser);
        return ResultUtils.success(promptAttachmentVO);
    }
}

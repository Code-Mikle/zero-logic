package com.mikle.zerologic.conversation.controller;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mikle.zerologic.user.auth.AuthCheck;
import com.mikle.zerologic.common.BaseResponse;
import com.mikle.zerologic.common.ResultUtils;
import com.mikle.zerologic.user.constant.UserConstant;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.exception.ThrowUtils;
import com.mikle.zerologic.conversation.model.dto.ChatHistoryQueryRequest;
import com.mikle.zerologic.conversation.model.entity.ChatHistory;
import com.mikle.zerologic.user.model.entity.User;
import com.mikle.zerologic.conversation.model.vo.ChatHistoryVo;
import com.mikle.zerologic.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import com.mikle.zerologic.conversation.service.ChatHistoryService;

import java.time.LocalDateTime;

/**
 * 对话历史 控制层
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    private static final int MAX_ADMIN_PAGE_SIZE = 50;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private UserService userService;

    /**
     * 分页查询某个应用的对话历史（游标查询）
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param request        请求
     * @return 对话历史分页
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistoryVo>> listAppChatHistory(@PathVariable Long appId,
                                                                @RequestParam(defaultValue = "10") int pageSize,
                                                                @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                                HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistoryVo> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);

        return ResultUtils.success(result);
    }

    /**
     * 管理员分页查询所有对话历史
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 对话历史分页
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAllChatHistoryByPageForAdmin(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageNum <= 0, ErrorCode.PARAMS_ERROR, "页码必须大于 0");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > MAX_ADMIN_PAGE_SIZE,
                ErrorCode.PARAMS_ERROR, "每页查询数量必须在 1 到 50 之间");
        // 查询数据
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> result = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(result);
    }
}

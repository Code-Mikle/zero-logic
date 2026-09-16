package com.mikle.zerologic.dashboard.controller;

import com.mikle.zerologic.common.BaseResponse;
import com.mikle.zerologic.common.ResultUtils;
import com.mikle.zerologic.user.model.entity.User;
import com.mikle.zerologic.dashboard.model.vo.GenerationDashboardVO;
import com.mikle.zerologic.dashboard.service.DashboardService;
import com.mikle.zerologic.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    @Resource
    private UserService userService;

    @GetMapping("/generation")
    public BaseResponse<GenerationDashboardVO> getGenerationDashboard(
            @RequestParam(required = false) Long appId,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(dashboardService.getGenerationDashboard(appId, loginUser));
    }
}

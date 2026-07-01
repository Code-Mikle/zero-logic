package com.mikle.zerologic.controller;

import com.mikle.zerologic.common.BaseResponse;
import com.mikle.zerologic.common.ResultUtils;
import com.mikle.zerologic.model.entity.User;
import com.mikle.zerologic.model.vo.dashboard.GenerationDashboardVO;
import com.mikle.zerologic.service.DashboardService;
import com.mikle.zerologic.service.UserService;
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

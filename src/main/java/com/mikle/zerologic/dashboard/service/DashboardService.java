package com.mikle.zerologic.dashboard.service;

import com.mikle.zerologic.user.model.entity.User;
import com.mikle.zerologic.dashboard.model.vo.GenerationDashboardVO;

public interface DashboardService {

    GenerationDashboardVO getGenerationDashboard(Long appId, User loginUser);
}

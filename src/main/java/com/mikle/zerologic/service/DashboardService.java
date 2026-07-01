package com.mikle.zerologic.service;

import com.mikle.zerologic.model.entity.User;
import com.mikle.zerologic.model.vo.dashboard.GenerationDashboardVO;

public interface DashboardService {

    GenerationDashboardVO getGenerationDashboard(Long appId, User loginUser);
}

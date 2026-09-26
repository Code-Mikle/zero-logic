package com.mikle.zerologic.app.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.mikle.zerologic.app.model.dto.AdminAppQueryRequest;
import com.mikle.zerologic.app.model.dto.AppAddRequest;
import com.mikle.zerologic.app.model.dto.GoodAppPageQueryRequest;
import com.mikle.zerologic.app.model.dto.MyAppQueryRequest;
import com.mikle.zerologic.app.model.entity.App;
import com.mikle.zerologic.user.model.entity.User;
import com.mikle.zerologic.app.model.vo.AppVO;
import com.mikle.zerologic.app.model.vo.GoodAppVO;
import com.mikle.zerologic.app.deployment.model.vo.DeployRecordVO;
import com.mikle.zerologic.app.version.model.vo.ProjectVersionVO;

import java.util.List;

/**
 * 应用 服务层。
 * @author <a href="https://github.com/Code-Mikle">Mikle</a>
 */
public interface AppService extends IService<App> {

    /**
     * 创建应用
     */
    Long createApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 应用部署
     *
     * @param appId     应用 ID
     * @param loginUser 登录用户
     * @return 可访问的部署地址
     */
    String deployApp(Long appId, User loginUser);

    String deployVersion(Long appId, Long versionId, User loginUser);

    String rollbackVersion(Long appId, Long versionId, User loginUser);

    List<ProjectVersionVO> listAppVersions(Long appId, User loginUser);

    List<DeployRecordVO> listDeployRecords(Long appId, User loginUser);

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    void generateAppScreenshotAsync(Long appId, String appUrl);

    /**
     * 获取应用封装类
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用封装类列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 分页查询当前用户创建的应用。
     */
    Page<AppVO> pageMyApps(MyAppQueryRequest queryRequest, Long userId);

    /**
     * 管理员分页查询应用。
     */
    Page<AppVO> pageAdminApps(AdminAppQueryRequest queryRequest);

    /**
     * 分页查询首页精选应用。
     */
    Page<GoodAppVO> pageGoodApps(GoodAppPageQueryRequest queryRequest);

}

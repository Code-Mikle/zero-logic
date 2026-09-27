package com.mikle.zerologic.app.service.impl;

import com.mikle.zerologic.app.model.dto.AdminAppQueryRequest;
import com.mikle.zerologic.app.model.dto.GoodAppPageQueryRequest;
import com.mikle.zerologic.app.model.entity.App;
import com.mikle.zerologic.app.model.vo.AppVO;
import com.mikle.zerologic.app.model.vo.GoodAppVO;
import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.knowledge.attachment.model.vo.PromptAttachmentVO;
import com.mikle.zerologic.knowledge.attachment.service.PromptAttachmentService;
import com.mikle.zerologic.user.model.entity.User;
import com.mikle.zerologic.user.model.vo.UserVO;
import com.mikle.zerologic.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private PromptAttachmentService promptAttachmentService;

    @InjectMocks
    private AppServiceImpl appService;

    @Test
    void getAppVOListShouldBatchLoadAssociatedData() {
        App firstApp = App.builder()
                .id(1L)
                .appName("first")
                .userId(7L)
                .initAttachmentId(11L)
                .build();
        App secondApp = App.builder()
                .id(2L)
                .appName("second")
                .userId(7L)
                .initAttachmentId(11L)
                .build();
        User user = User.builder().id(7L).userName("tester").build();
        UserVO userVO = new UserVO();
        userVO.setId(7L);
        PromptAttachmentVO attachmentVO = new PromptAttachmentVO();
        attachmentVO.setId(11L);
        attachmentVO.setFileName("requirements.md");

        when(userService.listByIds(Set.of(7L))).thenReturn(List.of(user));
        when(userService.getUserVO(user)).thenReturn(userVO);
        when(promptAttachmentService.getAttachmentVOMapByIds(Set.of(11L)))
                .thenReturn(Map.of(11L, attachmentVO));

        List<AppVO> result = appService.getAppVOList(List.of(firstApp, secondApp));

        assertEquals(2, result.size());
        assertSame(userVO, result.get(0).getUser());
        assertSame(userVO, result.get(1).getUser());
        assertEquals("requirements.md", result.get(0).getPromptAttachmentVO().getFileName());
        verify(userService).listByIds(Set.of(7L));
        verify(userService).getUserVO(user);
        verify(promptAttachmentService).getAttachmentVOMapByIds(Set.of(11L));
        verify(userService, never()).getById(anyLong());
        verify(promptAttachmentService, never()).getAttachmentVOById(anyLong());
    }

    @Test
    void pageAdminAppsShouldRejectExcessivePageSizeBeforeQueryingDatabase() {
        AdminAppQueryRequest request = new AdminAppQueryRequest();
        request.setPageSize(101);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> appService.pageAdminApps(request)
        );

        assertEquals("每页查询数量必须在 1 到 100 之间", exception.getMessage());
    }

    @Test
    void pageAdminAppsShouldRejectUnsupportedSortField() {
        AdminAppQueryRequest request = new AdminAppQueryRequest();
        request.setSortField("id desc; drop table app");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> appService.pageAdminApps(request)
        );

        assertEquals("不支持的排序字段", exception.getMessage());
    }

    @Test
    void adminQueryShouldIgnoreBlankCodeGenType() {
        AdminAppQueryRequest request = new AdminAppQueryRequest();
        request.setCodeGenType("");

        String sql = appService.buildAdminQueryWrapper(request, "createTime", false).toSQL();

        assertFalse(sql.contains("codeGenType"));
    }

    @Test
    void goodAppQueryShouldUseFixedFeaturedConditionAndStableSort() {
        String sql = appService.buildGoodAppQueryWrapper().toSQL().toLowerCase();
        int orderByIndex = sql.indexOf("order by");
        String orderByClause = orderByIndex < 0 ? "" : sql.substring(orderByIndex);

        assertTrue(sql.contains("priority"));
        assertTrue(orderByClause.contains("createtime"));
        assertTrue(orderByClause.contains("id"));
        assertTrue(orderByClause.indexOf("createtime") < orderByClause.indexOf("id"));
        assertFalse(sql.contains("initprompt"));
    }

    @Test
    void getGoodAppVOListShouldOnlyBatchLoadUsers() {
        App app = App.builder()
                .id(1L)
                .appName("featured")
                .cover("https://example.com/cover.jpg")
                .codeGenType("html")
                .deployKey("abc123")
                .userId(7L)
                .initAttachmentId(11L)
                .build();
        User user = User.builder().id(7L).userName("tester").build();
        UserVO userVO = new UserVO();
        userVO.setId(7L);

        when(userService.listByIds(Set.of(7L))).thenReturn(List.of(user));
        when(userService.getUserVO(user)).thenReturn(userVO);

        List<GoodAppVO> result = appService.getGoodAppVOList(List.of(app));

        assertEquals(1, result.size());
        assertEquals("featured", result.getFirst().getAppName());
        assertEquals("abc123", result.getFirst().getDeployKey());
        assertSame(userVO, result.getFirst().getUser());
        verifyNoInteractions(promptAttachmentService);
    }

    @Test
    void pageGoodAppsShouldValidatePageBoundsBeforeQueryingDatabase() {
        GoodAppPageQueryRequest invalidPage = new GoodAppPageQueryRequest();
        invalidPage.setPageNum(0);
        assertEquals(
                "页码必须大于 0",
                assertThrows(BusinessException.class, () -> appService.pageGoodApps(invalidPage)).getMessage()
        );

        GoodAppPageQueryRequest invalidPageSize = new GoodAppPageQueryRequest();
        invalidPageSize.setPageSize(21);
        assertEquals(
                "每页查询数量必须在 1 到 20 之间",
                assertThrows(BusinessException.class, () -> appService.pageGoodApps(invalidPageSize)).getMessage()
        );
    }

}

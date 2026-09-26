package com.mikle.zerologic.user.auth;

import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.user.constant.UserConstant;
import com.mikle.zerologic.user.model.entity.User;
import com.mikle.zerologic.user.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthInterceptorTest {

    private AuthInterceptor authInterceptor;
    private UserService userService;
    private ProceedingJoinPoint joinPoint;
    private AuthCheck authCheck;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        joinPoint = mock(ProceedingJoinPoint.class);
        authCheck = mock(AuthCheck.class);

        authInterceptor = new AuthInterceptor();
        ReflectionTestUtils.setField(authInterceptor, "userService", userService);
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void allowsAuthenticatedUserWhenRoleIsBlank() throws Throwable {
        mockLoginUser(UserConstant.DEFAULT_ROLE);
        when(authCheck.mustRole()).thenReturn("");
        when(joinPoint.proceed()).thenReturn("ok");

        assertEquals("ok", authInterceptor.doInterceptor(joinPoint, authCheck));
    }

    @Test
    void allowsAdminWhenAdminRoleIsRequired() throws Throwable {
        mockLoginUser(UserConstant.ADMIN_ROLE);
        when(authCheck.mustRole()).thenReturn(UserConstant.ADMIN_ROLE);
        when(joinPoint.proceed()).thenReturn("ok");

        assertEquals("ok", authInterceptor.doInterceptor(joinPoint, authCheck));
    }

    @Test
    void rejectsUserWhenAdminRoleIsRequired() {
        mockLoginUser(UserConstant.DEFAULT_ROLE);
        when(authCheck.mustRole()).thenReturn(UserConstant.ADMIN_ROLE);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authInterceptor.doInterceptor(joinPoint, authCheck));

        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
    }

    @Test
    void rejectsUnsupportedRequiredRole() {
        mockLoginUser(UserConstant.DEFAULT_ROLE);
        when(authCheck.mustRole()).thenReturn("super_admin");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authInterceptor.doInterceptor(joinPoint, authCheck));

        assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), exception.getCode());
    }

    @Test
    void rejectsInvocationWithoutRequestContext() {
        RequestContextHolder.resetRequestAttributes();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authInterceptor.doInterceptor(joinPoint, authCheck));

        assertEquals(ErrorCode.SYSTEM_ERROR.getCode(), exception.getCode());
        verify(userService, org.mockito.Mockito.never()).getLoginUser(any());
    }

    private void mockLoginUser(String role) {
        when(userService.getLoginUser(any())).thenReturn(User.builder()
                .id(1L)
                .userRole(role)
                .build());
    }
}

package com.mikle.zerologic.user.auth;

import com.mikle.zerologic.exception.BusinessException;
import com.mikle.zerologic.exception.ErrorCode;
import com.mikle.zerologic.user.model.entity.User;
import com.mikle.zerologic.user.model.enums.UserRoleEnum;
import com.mikle.zerologic.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行拦截
     *
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        User loginUser = userService.getLoginUser(request);

        String requiredRoleValue = authCheck.mustRole();
        if (requiredRoleValue == null || requiredRoleValue.isBlank()) {
            return joinPoint.proceed();
        }

        UserRoleEnum requiredRole = UserRoleEnum.getEnumByValue(requiredRoleValue);
        if (requiredRole == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "权限配置错误：不支持的角色 " + requiredRoleValue);
        }

        UserRoleEnum currentRole = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (!hasRequiredRole(currentRole, requiredRole)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        return joinPoint.proceed();
    }

    private HttpServletRequest getCurrentRequest() {
        if (!(RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes servletRequestAttributes)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "当前请求上下文不存在");
        }
        return servletRequestAttributes.getRequest();
    }

    private boolean hasRequiredRole(UserRoleEnum currentRole, UserRoleEnum requiredRole) {
        if (currentRole == null) {
            return false;
        }
        return switch (requiredRole) {
            case USER -> true;
            case ADMIN -> UserRoleEnum.ADMIN.equals(currentRole);
        };
    }
}
